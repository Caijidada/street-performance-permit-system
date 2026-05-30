package com.pdk.module.credit.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pdk.common.result.Result;
import com.pdk.module.auth.entity.User;
import com.pdk.module.auth.service.UserService;
import com.pdk.module.credit.dto.CreditDeductDTO;
import com.pdk.module.credit.entity.CreditLog;
import com.pdk.module.credit.service.CreditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "信用管理")
@RestController
@RequiredArgsConstructor
public class CreditController {

    private final CreditService creditService;
    private final UserService userService;

    @Operation(summary = "查看艺人信用详情")
    @SaCheckLogin
    @GetMapping("/api/artist/{userId}/credit")
    public Result<User> getCredit(@PathVariable Long userId) {
        User user = userService.getById(userId);
        if (user != null) {
            user.setPassword(null);
            user.setIdCard(null);
        }
        return Result.success(user);
    }

    @Operation(summary = "管理员信用操作")
    @SaCheckRole("admin")
    @PostMapping("/api/admin/credit/operate")
    public Result<Void> deduct(@Valid @RequestBody CreditDeductDTO dto) {
        creditService.deductByAdmin(dto);
        return Result.success();
    }

    @Operation(summary = "信用日志列表")
    @SaCheckRole("admin")
    @GetMapping("/api/admin/credit/logs")
    public Result<Page<CreditLog>> logs(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(creditService.listLogs(userId, page, size));
    }

    @Operation(summary = "我的信用日志")
    @SaCheckLogin
    @GetMapping("/api/artist/credit/my-logs")
    public Result<Page<CreditLog>> myLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        cn.dev33.satoken.stp.StpUtil.checkLogin();
        Long userId = cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
        return Result.success(creditService.listLogs(userId, page, size));
    }
}
