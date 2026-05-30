package com.pdk.module.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pdk.module.report.entity.ReportOrder;
import com.pdk.module.report.vo.TimeSlotVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Mapper
public interface ReportOrderMapper extends BaseMapper<ReportOrder> {

    /**
     * 同点位时间冲突检测（XML 实现，避免注入）
     */
    ReportOrder findConflictInSameVenue(@Param("venueId") Long venueId,
                                        @Param("date") LocalDate date,
                                        @Param("start") LocalTime start,
                                        @Param("end") LocalTime end,
                                        @Param("excludeId") Long excludeId);

    /**
     * 邻近点位时间冲突检测（XML foreach，安全参数化）
     */
    ReportOrder findConflictInNearbyVenues(@Param("venueIds") List<Long> venueIds,
                                           @Param("date") LocalDate date,
                                           @Param("start") LocalTime start,
                                           @Param("end") LocalTime end);

    /**
     * 查询点位指定日期已占用时段（用于日历展示）
     */
    List<TimeSlotVO> listOccupiedSlots(@Param("venueId") Long venueId,
                                       @Param("date") LocalDate date);
}
