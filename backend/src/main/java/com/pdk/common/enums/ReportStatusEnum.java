package com.pdk.common.enums;

import lombok.Getter;

@Getter
public enum ReportStatusEnum {
    PENDING_FIRST(0, "待一审"),
    PENDING_FINAL(1, "待终审"),
    APPROVED(2, "已通过"),
    REJECTED(3, "已驳回"),
    CANCELLED(4, "已撤销");

    private final int code;
    private final String desc;

    ReportStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
