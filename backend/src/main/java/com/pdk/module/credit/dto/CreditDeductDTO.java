package com.pdk.module.credit.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreditDeductDTO {

    @NotNull(message = "艺人ID不能为空")
    private Long userId;

    @NotNull(message = "事件类型不能为空")
    private Integer eventType;

    private Long orderId;

    private String remark;

    /** 自定义事件时的分值（eventType=0 时必填）*/
    private Integer customDelta;

    /** 自定义事件标签，记录到 remark 前缀 */
    private String customLabel;
}
