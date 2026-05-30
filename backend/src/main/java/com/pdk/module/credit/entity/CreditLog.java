package com.pdk.module.credit.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_credit_log")
public class CreditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Integer delta;
    private Integer scoreAfter;

    /**
     * 事件类型：
     * 1-按时演出(+2) 2-迟到15分钟内(-5) 3-迟到超15分钟(-10)
     * 4-超时演出(-8)  5-扰民投诉(-15)   6-爽约(-20)  7-系统奖励(+10)
     */
    private Integer eventType;

    private Long orderId;
    private Long operatorId;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
