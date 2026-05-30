package com.pdk.module.venue.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.pdk.common.result.Result;
import com.pdk.module.venue.entity.Venue;
import com.pdk.module.venue.service.VenueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "演出点位")
@RestController
@RequiredArgsConstructor
public class VenueController {

    private final VenueService venueService;

    @Operation(summary = "获取地图点位列表")
    @GetMapping("/api/venue/map-list")
    public Result<List<Venue>> listMapVenues() {
        return Result.success(venueService.listMapVenues());
    }

    @Operation(summary = "获取点位详情")
    @GetMapping("/api/venue/{id}")
    public Result<Venue> getVenueDetail(@PathVariable Long id) {
        return Result.success(venueService.getVenueDetail(id));
    }

    @Operation(summary = "新增点位（管理员）")
    @SaCheckRole("admin")
    @PostMapping("/api/admin/venue")
    public Result<Void> addVenue(@RequestBody Venue venue) {
        venueService.save(venue);
        return Result.success();
    }

    @Operation(summary = "修改点位（管理员）")
    @SaCheckRole("admin")
    @PutMapping("/api/admin/venue/{id}")
    public Result<Void> updateVenue(@PathVariable Long id, @RequestBody Venue venue) {
        venue.setId(id);
        venueService.updateById(venue);
        return Result.success();
    }

    @Operation(summary = "删除点位（管理员）")
    @SaCheckRole("admin")
    @DeleteMapping("/api/admin/venue/{id}")
    public Result<Void> deleteVenue(@PathVariable Long id) {
        venueService.removeById(id);
        return Result.success();
    }

    @Operation(summary = "获取全部点位列表（管理员）")
    @SaCheckRole("admin")
    @GetMapping("/api/admin/venue/list")
    public Result<List<Venue>> listAllVenues() {
        return Result.success(venueService.list());
    }
}
