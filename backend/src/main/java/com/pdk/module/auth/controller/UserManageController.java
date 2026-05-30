package com.pdk.module.auth.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pdk.common.result.Result;
import com.pdk.module.auth.entity.User;
import com.pdk.module.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "用户管理（管理员）")
@RestController
@RequestMapping("/api/admin/user")
@SaCheckRole("admin")
@RequiredArgsConstructor
public class UserManageController {

    private final UserService userService;

    @Operation(summary = "艺人列表")
    @GetMapping("/list")
    public Result<Page<User>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<User> result = userService.page(new Page<>(page, size),
                new LambdaQueryWrapper<User>().eq(User::getRole, 1).orderByDesc(User::getCreateTime));
        result.getRecords().forEach(u -> { u.setPassword(null); u.setIdCard(null); });
        return Result.success(result);
    }

    @Operation(summary = "资质审核")
    @PostMapping("/{userId}/qualify")
    public Result<Void> qualify(@PathVariable Long userId, @RequestBody Map<String, Integer> body) {
        User update = new User();
        update.setId(userId);
        update.setQualifyStatus(body.get("status"));
        userService.updateById(update);
        return Result.success();
    }

    @Operation(summary = "查询单个艺人信息")
    @GetMapping("/{userId}")
    public Result<User> getById(@PathVariable Long userId) {
        User user = userService.getById(userId);
        if (user != null) { user.setPassword(null); user.setIdCard(null); }
        return Result.success(user);
    }

    @Operation(summary = "禁用/启用账号")
    @PostMapping("/{userId}/toggle-status")
    public Result<Void> toggleStatus(@PathVariable Long userId) {
        User user = userService.getById(userId);
        User update = new User();
        update.setId(userId);
        update.setStatus(user.getStatus() == 1 ? 0 : 1);
        userService.updateById(update);
        return Result.success();
    }
}
