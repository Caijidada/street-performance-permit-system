package com.pdk.module.certificate.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("t_certificate")
public class Certificate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String certCode;
    private Long orderId;
    private Long userId;
    private Long venueId;
    private LocalDate performDate;
    private LocalTime timeSlotStart;
    private LocalTime timeSlotEnd;
    private String certUrl;

    /** 有效状态：0-已失效 1-有效 */
    private Integer validStatus;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime issueTime;
}
