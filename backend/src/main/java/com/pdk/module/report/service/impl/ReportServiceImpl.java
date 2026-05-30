package com.pdk.module.report.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pdk.common.constant.RedisConstants;
import com.pdk.common.enums.ReportStatusEnum;
import com.pdk.common.exception.BusinessException;
import com.pdk.module.auth.entity.User;
import com.pdk.module.auth.service.UserService;
import com.pdk.module.report.dto.ConflictCheckDTO;
import com.pdk.module.report.dto.ReportSubmitDTO;
import com.pdk.module.report.entity.ReportOrder;
import com.pdk.module.report.mapper.ReportOrderMapper;
import com.pdk.module.report.service.ReportService;
import com.pdk.module.report.vo.TimeSlotVO;
import com.pdk.module.venue.entity.Venue;
import com.pdk.module.venue.service.VenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl extends ServiceImpl<ReportOrderMapper, ReportOrder> implements ReportService {

    private final VenueService venueService;
    private final UserService userService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${pdk.max-reports-per-day:3}")
    private int maxReportsPerDay;

    @Value("${pdk.credit-limit-hot:60}")
    private int creditLimitHot;

    private final Snowflake snowflake = IdUtil.getSnowflake(1, 1);

    @Override
    public Map<String, Object> preCheckConflict(ConflictCheckDTO dto) {
        Map<String, Object> result = new HashMap<>();
        result.put("hasConflict", false);

        // 1. 同点位冲突
        ReportOrder sameConflict = baseMapper.findConflictInSameVenue(
                dto.getVenueId(), dto.getPerformDate(),
                dto.getTimeSlotStart(), dto.getTimeSlotEnd(),
                dto.getExcludeOrderId());
        if (sameConflict != null) {
            result.put("hasConflict", true);
            result.put("conflictType", "SAME_VENUE");
            result.put("message", "该点位在所选时间段已有报备，请更换时间或点位");
            return result;
        }

        // 2. 邻近点位（100米）冲突
        Venue venue = venueService.getById(dto.getVenueId());
        List<Venue> nearbyVenues = venueService.findNearbyVenues(
                venue.getLongitude().doubleValue(),
                venue.getLatitude().doubleValue(),
                dto.getVenueId());

        if (!nearbyVenues.isEmpty()) {
            List<Long> nearbyIds = nearbyVenues.stream().map(Venue::getId).collect(Collectors.toList());
            ReportOrder nearbyConflict = baseMapper.findConflictInNearbyVenues(
                    nearbyIds, dto.getPerformDate(),
                    dto.getTimeSlotStart(), dto.getTimeSlotEnd());
            if (nearbyConflict != null) {
                result.put("hasConflict", true);
                result.put("conflictType", "NEARBY_VENUE");
                result.put("message", "该点位100米范围内在所选时间段已有演出，可能产生噪音冲突");
                return result;
            }
        }

        return result;
    }

    @Override
    public ReportOrder submitReport(ReportSubmitDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        User artist = userService.getById(userId);

        // 1. 资质审核检查
        if (artist.getQualifyStatus() != 1) {
            throw new BusinessException(400, "您的演艺资质尚未通过审核，请先上传并等待审核通过");
        }

        // 2. 时间逻辑校验
        if (!dto.getTimeSlotStart().isBefore(dto.getTimeSlotEnd())) {
            throw new BusinessException(400, "演出开始时间必须早于结束时间");
        }

        // 3. 点位开放时间和开放日校验
        Venue venue = venueService.getById(dto.getVenueId());
        if (venue == null || venue.getStatus() == 0) {
            throw new BusinessException(400, "所选点位不存在或已关闭");
        }
        if (dto.getTimeSlotStart().isBefore(venue.getOpenTimeStart())
                || dto.getTimeSlotEnd().isAfter(venue.getOpenTimeEnd())) {
            throw new BusinessException(400,
                    String.format("演出时段须在点位开放时间 %s ~ %s 范围内",
                            venue.getOpenTimeStart(), venue.getOpenTimeEnd()));
        }
        int dayOfWeek = dto.getPerformDate().getDayOfWeek().getValue(); // 1=周一 7=周日
        List<String> openDays = Arrays.asList(venue.getOpenDays().split(","));
        if (!openDays.contains(String.valueOf(dayOfWeek))) {
            throw new BusinessException(400, "所选日期该点位不开放，请查看开放日历");
        }

        // 4. 演出类型校验
        List<String> allowedTypes = Arrays.asList(venue.getAllowTypes().split(","));
        if (!allowedTypes.contains(dto.getPerformType())) {
            throw new BusinessException(400, "该点位不允许「" + dto.getPerformType() + "」类型演出");
        }

        // 5. 观众人数校验
        if (dto.getExpectedAudience() != null && dto.getExpectedAudience() > venue.getMaxAudience()) {
            throw new BusinessException(400, "预计观众人数超过该点位上限（" + venue.getMaxAudience() + "人）");
        }

        // 6. 每日场次限制
        long todayCount = count(new LambdaQueryWrapper<ReportOrder>()
                .eq(ReportOrder::getUserId, userId)
                .eq(ReportOrder::getPerformDate, dto.getPerformDate())
                .in(ReportOrder::getStatus, 0, 1, 2));
        if (todayCount >= maxReportsPerDay) {
            throw new BusinessException(400, "同一天最多可报备 " + maxReportsPerDay + " 场演出");
        }

        // 7. 信用分热门点位限制（观众超过50人视为热门点位）
        if (venue.getMaxAudience() > 50 && artist.getCreditScore() < creditLimitHot) {
            throw new BusinessException(400,
                    "您的信用分（" + artist.getCreditScore() + "）低于" + creditLimitHot + "分，无法预约热门点位");
        }

        // 8. Redis 分布式锁防并发超卖
        String lockKey = RedisConstants.REPORT_LOCK
                + dto.getVenueId() + ":"
                + dto.getPerformDate().format(DateTimeFormatter.BASIC_ISO_DATE) + ":"
                + dto.getTimeSlotStart();
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, userId, RedisConstants.REPORT_LOCK_TTL, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(locked)) {
            throw new BusinessException(429, "当前点位该时段热度较高，请稍后重试");
        }

        try {
            // 9. Double Check 冲突
            ConflictCheckDTO check = new ConflictCheckDTO();
            check.setVenueId(dto.getVenueId());
            check.setPerformDate(dto.getPerformDate());
            check.setTimeSlotStart(dto.getTimeSlotStart());
            check.setTimeSlotEnd(dto.getTimeSlotEnd());
            Map<String, Object> conflictResult = preCheckConflict(check);
            if (Boolean.TRUE.equals(conflictResult.get("hasConflict"))) {
                throw new BusinessException(400, (String) conflictResult.get("message"));
            }

            ReportOrder order = new ReportOrder();
            order.setOrderNo("PDK" + snowflake.nextIdStr());
            order.setUserId(userId);
            order.setVenueId(dto.getVenueId());
            order.setPerformDate(dto.getPerformDate());
            order.setTimeSlotStart(dto.getTimeSlotStart());
            order.setTimeSlotEnd(dto.getTimeSlotEnd());
            order.setPerformType(dto.getPerformType());
            order.setPerformContent(dto.getPerformContent());
            order.setExpectedAudience(dto.getExpectedAudience());
            order.setEquipment(dto.getEquipment());
            order.setStatus(ReportStatusEnum.PENDING_FIRST.getCode());
            save(order);
            return order;

        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    @Override
    public Page<ReportOrder> listMyReports(int page, int size) {
        Long userId = StpUtil.getLoginIdAsLong();
        return page(new Page<>(page, size),
                new LambdaQueryWrapper<ReportOrder>()
                        .eq(ReportOrder::getUserId, userId)
                        .orderByDesc(ReportOrder::getCreateTime));
    }

    @Override
    public void cancelReport(Long orderId) {
        Long userId = StpUtil.getLoginIdAsLong();
        ReportOrder order = getById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(404, "报备单不存在");
        }
        if (order.getStatus() == ReportStatusEnum.APPROVED.getCode()) {
            throw new BusinessException(400, "已通过的报备不能直接撤销，请联系管理员");
        }
        ReportOrder update = new ReportOrder();
        update.setId(orderId);
        update.setStatus(ReportStatusEnum.CANCELLED.getCode());
        updateById(update);
    }

    @Override
    public List<TimeSlotVO> listOccupiedSlots(Long venueId, LocalDate date) {
        return baseMapper.listOccupiedSlots(venueId, date);
    }
}
