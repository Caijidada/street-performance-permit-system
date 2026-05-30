package com.pdk.common.enums;

import lombok.Getter;

@Getter
public enum UserRoleEnum {
    ARTIST(1, "artist", "艺人"),
    ADMIN(2, "admin", "管理员"),
    SUPER_ADMIN(3, "superAdmin", "超级管理员");

    private final int code;
    private final String role;
    private final String desc;

    UserRoleEnum(int code, String role, String desc) {
        this.code = code;
        this.role = role;
        this.desc = desc;
    }
}
