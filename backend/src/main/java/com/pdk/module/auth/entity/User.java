package com.pdk.module.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String realName;

    private String idCard;

    private String phone;

    private String avatar;

    /** 角色：1-艺人 2-管理员 3-超管 */
    private Integer role;

    private Integer creditScore;

    private String qualification;

    /** 资质审核状态：0-待审 1-通过 2-拒绝 */
    private Integer qualifyStatus;

    /** 账号状态：0-禁用 1-正常 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
