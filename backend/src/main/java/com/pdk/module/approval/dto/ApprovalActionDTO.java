package com.pdk.module.approval.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApprovalActionDTO {

    @NotNull(message = "审批动作不能为空")
    private Integer action;

    private String comment;
}
