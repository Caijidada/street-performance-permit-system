package com.pdk.module.auth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pdk.module.auth.dto.ChangePasswordDTO;
import com.pdk.module.auth.dto.LoginDTO;
import com.pdk.module.auth.dto.RegisterDTO;
import com.pdk.module.auth.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface UserService extends IService<User> {

    void register(RegisterDTO dto);

    Map<String, Object> login(LoginDTO dto);

    void logout();

    User getCurrentUser();

    void uploadQualification(MultipartFile file);

    void changePassword(ChangePasswordDTO dto);
}
