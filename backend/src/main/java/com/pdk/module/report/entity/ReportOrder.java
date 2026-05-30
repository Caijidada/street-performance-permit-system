package com.pdk.module.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("t_report_order")
public class ReportOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;
    private Long userId;
    private Long venueId;
    private LocalDate performDate;
    private LocalTime timeSlotStart;
    private LocalTime timeSlotEnd;
    private String performType;
    private String performContent;
    private Integer expectedAudience;
    private String equipment;

    /** 状态：0-待一审 1-待终审 2-通过 3-驳回 4-撤销 */
    private Integer status;

    private String rejectReason;
    private Long certId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
