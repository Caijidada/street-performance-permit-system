package com.pdk.module.report.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pdk.common.result.Result;
import com.pdk.module.report.dto.ConflictCheckDTO;
import com.pdk.module.report.dto.ReportSubmitDTO;
import com.pdk.module.report.entity.ReportOrder;
import com.pdk.module.report.service.ReportService;
import com.pdk.module.report.vo.TimeSlotVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "报备申请")
@RestController
@RequestMapping("/api/report")
@SaCheckLogin
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "时空冲突预检")
    @PostMapping("/pre-check")
    public Result<Map<String, Object>> preCheck(@Valid @RequestBody ConflictCheckDTO dto) {
        return Result.success(reportService.preCheckConflict(dto));
    }

    @Operation(summary = "提交报备申请")
    @PostMapping("/submit")
    public Result<ReportOrder> submit(@Valid @RequestBody ReportSubmitDTO dto) {
        return Result.success(reportService.submitReport(dto));
    }

    @Operation(summary = "我的报备列表")
    @GetMapping("/my-list")
    public Result<Page<ReportOrder>> myList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(reportService.listMyReports(page, size));
    }

    @Operation(summary = "报备详情")
    @GetMapping("/{id}")
    public Result<ReportOrder> detail(@PathVariable Long id) {
        return Result.success(reportService.getById(id));
    }

    @Operation(summary = "撤销报备")
    @DeleteMapping("/{id}")
    public Result<Void> cancel(@PathVariable Long id) {
        reportService.cancelReport(id);
        return Result.success();
    }

    @Operation(summary = "查询点位指定日期已占用时段（日历用）")
    @GetMapping("/slots")
    public Result<List<TimeSlotVO>> slots(
            @RequestParam Long venueId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.success(reportService.listOccupiedSlots(venueId, date));
    }
}
