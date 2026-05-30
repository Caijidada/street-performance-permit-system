package com.pdk.config;

import cn.dev33.satoken.stp.StpInterface;
import com.pdk.module.auth.entity.User;
import com.pdk.module.auth.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token 角色与权限实现
 * role: 1=艺人(artist) 2=管理员(admin) 3=超管(admin+superadmin)
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final UserMapper userMapper;

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        User user = userMapper.selectById(Long.parseLong(loginId.toString()));
        List<String> roles = new ArrayList<>();
        if (user == null) return roles;
        switch (user.getRole()) {
            case 1 -> roles.add("artist");
            case 2 -> roles.add("admin");
            case 3 -> { roles.add("admin"); roles.add("superadmin"); }
        }
        return roles;
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return new ArrayList<>();
    }
}
