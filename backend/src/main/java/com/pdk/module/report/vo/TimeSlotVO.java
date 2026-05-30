package com.pdk.module.report.vo;

import lombok.Data;
import java.time.LocalTime;

@Data
public class TimeSlotVO {
    private Long orderId;
    private LocalTime start;
    private LocalTime end;
    private String performType;
    private Integer status;
}
