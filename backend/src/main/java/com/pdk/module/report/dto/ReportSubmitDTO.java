package com.pdk.module.report.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ReportSubmitDTO {

    @NotNull(message = "请选择演出点位")
    private Long venueId;

    @NotNull(message = "请选择演出日期")
    @Future(message = "演出日期必须是未来的日期")
    private LocalDate performDate;

    @NotNull(message = "请选择演出开始时间")
    private LocalTime timeSlotStart;

    @NotNull(message = "请选择演出结束时间")
    private LocalTime timeSlotEnd;

    @NotBlank(message = "请选择演出类型")
    private String performType;

    private String performContent;

    private Integer expectedAudience;

    private String equipment;
}
