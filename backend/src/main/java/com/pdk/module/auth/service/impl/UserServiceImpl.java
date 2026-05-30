package com.pdk.module.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pdk.common.exception.BusinessException;
import com.pdk.module.auth.dto.ChangePasswordDTO;
import com.pdk.module.auth.dto.LoginDTO;
import com.pdk.module.auth.dto.RegisterDTO;
import com.pdk.module.auth.entity.User;
import com.pdk.module.auth.mapper.UserMapper;
import com.pdk.module.auth.service.UserService;
import com.pdk.util.FileUploadUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final FileUploadUtil fileUploadUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${pdk.aes-key}")
    private String aesKey;

    @Override
    public void register(RegisterDTO dto) {
        if (count(new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername())) > 0) {
            throw new BusinessException(400, "用户名已被注册");
        }
        if (count(new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone())) > 0) {
            throw new BusinessException(400, "手机号已被注册");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setIdCard(SecureUtil.aes(aesKey.getBytes()).encryptHex(dto.getIdCard()));
        user.setPhone(dto.getPhone());
        user.setRole(1);
        user.setCreditScore(100);
        user.setQualifyStatus(0);
        user.setStatus(1);
        save(user);
    }

    @Override
    public Map<String, Object> login(LoginDTO dto) {
        User user = getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(400, "用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException(403, "账号已被禁用，请联系管理员");
        }

        StpUtil.login(user.getId());
        StpUtil.getSession().set("role", user.getRole());

        Map<String, Object> result = new HashMap<>();
        result.put("token", StpUtil.getTokenValue());
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("realName", user.getRealName());
        result.put("role", user.getRole());
        result.put("creditScore", user.getCreditScore());
        result.put("avatar", user.getAvatar());
        result.put("qualifyStatus", user.getQualifyStatus());
        return result;
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @Override
    public User getCurrentUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        return getById(userId);
    }

    @Override
    public void uploadQualification(MultipartFile file) {
        Long userId = StpUtil.getLoginIdAsLong();
        String url = fileUploadUtil.upload(file, "qualification");
        User user = new User();
        user.setId(userId);
        user.setQualification(url);
        user.setQualifyStatus(0);
        updateById(user);
    }

    @Override
    public void changePassword(ChangePasswordDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = getById(userId);
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(400, "原密码错误");
        }
        User update = new User();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        updateById(update);
        // 修改密码后强制重新登录
        StpUtil.logout();
    }
}
