package com.pdk.module.report.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ConflictCheckDTO {

    @NotNull(message = "点位ID不能为空")
    private Long venueId;

    @NotNull(message = "演出日期不能为空")
    private LocalDate performDate;

    @NotNull(message = "开始时间不能为空")
    private LocalTime timeSlotStart;

    @NotNull(message = "结束时间不能为空")
    private LocalTime timeSlotEnd;

    /** 修改报备时排除自身 */
    private Long excludeOrderId;
}
