package com.pdk.module.approval.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pdk.common.result.Result;
import com.pdk.module.approval.dto.ApprovalActionDTO;
import com.pdk.module.approval.service.ApprovalService;
import com.pdk.module.approval.vo.ApprovalOrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "审批管理")
@RestController
@RequestMapping("/api/admin/approval")
@SaCheckRole("admin")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    @Operation(summary = "报备列表（含艺人和点位信息，status为空时返回全部）")
    @GetMapping("/list")
    public Result<Page<ApprovalOrderVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        return Result.success(approvalService.listPendingApprovals(page, size, status));
    }

    @Operation(summary = "执行审批（通过/驳回）")
    @PostMapping("/{orderId}/action")
    public Result<Void> doApprove(@PathVariable Long orderId,
                                   @Valid @RequestBody ApprovalActionDTO dto) {
        approvalService.doApprove(orderId, dto);
        return Result.success();
    }

    @Operation(summary = "管理员撤销已通过报备（同步失效证件）")
    @PostMapping("/{orderId}/cancel")
    public Result<Void> adminCancel(@PathVariable Long orderId,
                                     @RequestBody Map<String, String> body) {
        approvalService.adminCancelReport(orderId, body.get("reason"));
        return Result.success();
    }
}
