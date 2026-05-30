package com.pdk.module.statistics.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.pdk.common.result.Result;
import com.pdk.module.statistics.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "数据统计大屏")
@RestController
@RequestMapping("/api/admin/stats")
@SaCheckRole("admin")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Operation(summary = "大屏概览数据")
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.success(statisticsService.getOverview());
    }

    @Operation(summary = "演出热力图数据")
    @GetMapping("/heatmap")
    public Result<Object> heatmap() {
        return Result.success(statisticsService.getHeatmapData());
    }

    @Operation(summary = "点位预约率统计")
    @GetMapping("/venue-rate")
    public Result<Object> venueRate() {
        return Result.success(statisticsService.getVenueRate());
    }

    @Operation(summary = "艺人活跃度排行")
    @GetMapping("/artist-rank")
    public Result<Object> artistRank() {
        return Result.success(statisticsService.getArtistRank());
    }

    @Operation(summary = "演出类型分布")
    @GetMapping("/perform-type")
    public Result<Object> performType() {
        return Result.success(statisticsService.getPerformTypeDistribution());
    }
}
