package com.pdk.module.auth.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.pdk.common.result.Result;
import com.pdk.module.auth.dto.ChangePasswordDTO;
import com.pdk.module.auth.dto.LoginDTO;
import com.pdk.module.auth.dto.RegisterDTO;
import com.pdk.module.auth.entity.User;
import com.pdk.module.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "用户认证")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @Operation(summary = "艺人注册")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        userService.register(dto);
        return Result.success();
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(userService.login(dto));
    }

    @Operation(summary = "退出登录")
    @SaCheckLogin
    @PostMapping("/logout")
    public Result<Void> logout() {
        userService.logout();
        return Result.success();
    }

    @Operation(summary = "获取当前用户信息")
    @SaCheckLogin
    @GetMapping("/info")
    public Result<User> getUserInfo() {
        User user = userService.getCurrentUser();
        user.setPassword(null);
        user.setIdCard(null);
        return Result.success(user);
    }

    @Operation(summary = "上传演艺资质")
    @SaCheckLogin
    @PostMapping("/qualification")
    public Result<Void> uploadQualification(@RequestParam("file") MultipartFile file) {
        userService.uploadQualification(file);
        return Result.success();
    }

    @Operation(summary = "修改密码")
    @SaCheckLogin
    @PostMapping("/change-password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        userService.changePassword(dto);
        return Result.success();
    }
}
