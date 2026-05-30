package com.pdk.module.credit.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pdk.common.exception.BusinessException;
import com.pdk.module.auth.entity.User;
import com.pdk.module.auth.service.UserService;
import com.pdk.module.credit.dto.CreditDeductDTO;
import com.pdk.module.credit.entity.CreditLog;
import com.pdk.module.credit.mapper.CreditLogMapper;
import com.pdk.module.credit.service.CreditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CreditServiceImpl extends ServiceImpl<CreditLogMapper, CreditLog> implements CreditService {

    private final UserService userService;

    // 事件类型对应分值
    private static final Map<Integer, Integer> EVENT_DELTA = Map.of(
            1, 2,    // 按时演出
            2, -5,   // 迟到15分钟内
            3, -10,  // 迟到超15分钟
            4, -8,   // 超时演出
            5, -15,  // 扰民投诉
            6, -20,  // 爽约
            7, 10    // 系统奖励
    );

    @Override
    @Transactional
    public void recordCredit(Long userId, Integer eventType, Long orderId, Long operatorId, String remark) {
        recordCredit(userId, eventType, null, orderId, operatorId, remark);
    }

    @Override
    @Transactional
    public void recordCredit(Long userId, Integer eventType, Integer customDelta, Long orderId, Long operatorId, String remark) {
        Integer delta;
        if (eventType == 0) {
            if (customDelta == null) throw new BusinessException(400, "自定义事件必须填写分值");
            delta = customDelta;
        } else {
            delta = EVENT_DELTA.get(eventType);
            if (delta == null) {
                throw new BusinessException(400, "无效的信用事件类型");
            }
        }

        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        int newScore = Math.max(0, Math.min(200, user.getCreditScore() + delta));

        // 更新用户信用分
        User update = new User();
        update.setId(userId);
        update.setCreditScore(newScore);
        userService.updateById(update);

        // 记录日志
        CreditLog log = new CreditLog();
        log.setUserId(userId);
        log.setDelta(delta);
        log.setScoreAfter(newScore);
        log.setEventType(eventType);
        log.setOrderId(orderId);
        log.setOperatorId(operatorId);
        log.setRemark(remark);
        save(log);
    }

    @Override
    public void deductByAdmin(CreditDeductDTO dto) {
        Long operatorId = StpUtil.getLoginIdAsLong();
        String remark = dto.getRemark();
        if (dto.getEventType() == 0 && dto.getCustomLabel() != null && !dto.getCustomLabel().isBlank()) {
            remark = "[" + dto.getCustomLabel() + "] " + (remark != null ? remark : "");
        }
        recordCredit(dto.getUserId(), dto.getEventType(), dto.getCustomDelta(), dto.getOrderId(), operatorId, remark);
    }

    @Override
    public Page<CreditLog> listLogs(Long userId, int page, int size) {
        LambdaQueryWrapper<CreditLog> wrapper = new LambdaQueryWrapper<CreditLog>()
                .orderByDesc(CreditLog::getCreateTime);
        if (userId != null) {
            wrapper.eq(CreditLog::getUserId, userId);
        }
        return page(new Page<>(page, size), wrapper);
    }
}
