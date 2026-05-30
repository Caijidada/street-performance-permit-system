package com.pdk.module.statistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pdk.common.enums.ReportStatusEnum;
import com.pdk.module.auth.entity.User;
import com.pdk.module.auth.service.UserService;
import com.pdk.module.report.entity.ReportOrder;
import com.pdk.module.report.service.ReportService;
import com.pdk.module.statistics.service.StatisticsService;
import com.pdk.module.venue.entity.Venue;
import com.pdk.module.venue.service.VenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final UserService userService;
    private final VenueService venueService;
    private final ReportService reportService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_OVERVIEW = "stats:overview";
    private static final String CACHE_HEATMAP = "stats:heatmap";
    private static final String CACHE_VENUE_RATE = "stats:venue_rate";
    private static final String CACHE_ARTIST_RANK = "stats:artist_rank";
    private static final String CACHE_PERFORM_TYPE = "stats:perform_type";
    private static final long CACHE_TTL_MINUTES = 10;

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getOverview() {
        try {
            Object cached = redisTemplate.opsForValue().get(CACHE_OVERVIEW);
            if (cached instanceof Map) return (Map<String, Object>) cached;
        } catch (Exception ignored) {}

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalArtists", userService.count(new LambdaQueryWrapper<User>().eq(User::getRole, 1)));
        data.put("totalVenues", venueService.count(new LambdaQueryWrapper<Venue>().eq(Venue::getStatus, 1)));
        data.put("totalReports", reportService.count());
        data.put("approvedReports", reportService.count(
                new LambdaQueryWrapper<ReportOrder>().eq(ReportOrder::getStatus, ReportStatusEnum.APPROVED.getCode())));
        data.put("pendingReports", reportService.count(
                new LambdaQueryWrapper<ReportOrder>().in(ReportOrder::getStatus,
                        ReportStatusEnum.PENDING_FIRST.getCode(), ReportStatusEnum.PENDING_FINAL.getCode())));
        data.put("todayReports", reportService.count(
                new LambdaQueryWrapper<ReportOrder>().eq(ReportOrder::getPerformDate, LocalDate.now())));

        try { redisTemplate.opsForValue().set(CACHE_OVERVIEW, data, CACHE_TTL_MINUTES, TimeUnit.MINUTES); } catch (Exception ignored) {}
        return data;
    }

    @Override
    public Object getHeatmapData() {
        try {
            Object cached = redisTemplate.opsForValue().get(CACHE_HEATMAP);
            if (cached != null) return cached;
        } catch (Exception ignored) {}

        List<ReportOrder> approvedOrders = reportService.list(
                new LambdaQueryWrapper<ReportOrder>()
                        .eq(ReportOrder::getStatus, ReportStatusEnum.APPROVED.getCode())
                        .select(ReportOrder::getVenueId));

        Map<Long, Long> venueCount = approvedOrders.stream()
                .collect(Collectors.groupingBy(ReportOrder::getVenueId, Collectors.counting()));

        List<Map<String, Object>> result = new ArrayList<>();
        if (!venueCount.isEmpty()) {
            venueService.listByIds(venueCount.keySet()).forEach(venue -> {
                Map<String, Object> item = new HashMap<>();
                item.put("lng", venue.getLongitude());
                item.put("lat", venue.getLatitude());
                item.put("count", venueCount.getOrDefault(venue.getId(), 0L));
                item.put("name", venue.getName());
                result.add(item);
            });
        }

        try { redisTemplate.opsForValue().set(CACHE_HEATMAP, result, CACHE_TTL_MINUTES, TimeUnit.MINUTES); } catch (Exception ignored) {}
        return result;
    }

    @Override
    public Object getVenueRate() {
        try {
            Object cached = redisTemplate.opsForValue().get(CACHE_VENUE_RATE);
            if (cached != null) return cached;
        } catch (Exception ignored) {}

        List<Venue> venues = venueService.list(new LambdaQueryWrapper<Venue>().eq(Venue::getStatus, 1));
        List<ReportOrder> orders = reportService.list(
                new LambdaQueryWrapper<ReportOrder>()
                        .eq(ReportOrder::getStatus, ReportStatusEnum.APPROVED.getCode())
                        .select(ReportOrder::getVenueId));

        Map<Long, Long> countMap = orders.stream()
                .collect(Collectors.groupingBy(ReportOrder::getVenueId, Collectors.counting()));

        List<Map<String, Object>> result = venues.stream().map(v -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("venueName", v.getName());
            item.put("district", v.getDistrict());
            item.put("count", countMap.getOrDefault(v.getId(), 0L));
            return item;
        }).sorted((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")))
                .collect(Collectors.toList());

        try { redisTemplate.opsForValue().set(CACHE_VENUE_RATE, result, CACHE_TTL_MINUTES, TimeUnit.MINUTES); } catch (Exception ignored) {}
        return result;
    }

    @Override
    public Object getArtistRank() {
        try {
            Object cached = redisTemplate.opsForValue().get(CACHE_ARTIST_RANK);
            if (cached != null) return cached;
        } catch (Exception ignored) {}

        List<ReportOrder> orders = reportService.list(
                new LambdaQueryWrapper<ReportOrder>()
                        .eq(ReportOrder::getStatus, ReportStatusEnum.APPROVED.getCode())
                        .select(ReportOrder::getUserId));

        Map<Long, Long> countMap = orders.stream()
                .collect(Collectors.groupingBy(ReportOrder::getUserId, Collectors.counting()));

        List<Map<String, Object>> result = new ArrayList<>();
        countMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(10)
                .forEach(entry -> {
                    User user = userService.getById(entry.getKey());
                    if (user != null) {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("userId", user.getId());
                        item.put("realName", user.getRealName());
                        item.put("creditScore", user.getCreditScore());
                        item.put("performCount", entry.getValue());
                        result.add(item);
                    }
                });

        try { redisTemplate.opsForValue().set(CACHE_ARTIST_RANK, result, CACHE_TTL_MINUTES, TimeUnit.MINUTES); } catch (Exception ignored) {}
        return result;
    }

    @Override
    public Object getPerformTypeDistribution() {
        try {
            Object cached = redisTemplate.opsForValue().get(CACHE_PERFORM_TYPE);
            if (cached != null) return cached;
        } catch (Exception ignored) {}

        List<ReportOrder> orders = reportService.list(
                new LambdaQueryWrapper<ReportOrder>()
                        .eq(ReportOrder::getStatus, ReportStatusEnum.APPROVED.getCode())
                        .select(ReportOrder::getPerformType));

        Map<String, Long> countMap = orders.stream()
                .collect(Collectors.groupingBy(ReportOrder::getPerformType, Collectors.counting()));

        List<Map<String, Object>> result = new ArrayList<>();
        countMap.forEach((type, count) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("name", type);
            item.put("value", count);
            result.add(item);
        });

        try { redisTemplate.opsForValue().set(CACHE_PERFORM_TYPE, result, CACHE_TTL_MINUTES, TimeUnit.MINUTES); } catch (Exception ignored) {}
        return result;
    }

    /** 清除所有统计缓存（数据变更后调用） */
    public void clearCache() {
        redisTemplate.delete(List.of(CACHE_OVERVIEW, CACHE_HEATMAP, CACHE_VENUE_RATE, CACHE_ARTIST_RANK, CACHE_PERFORM_TYPE));
    }
}
