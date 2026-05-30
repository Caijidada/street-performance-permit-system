package com.pdk.module.report.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pdk.module.report.dto.ConflictCheckDTO;
import com.pdk.module.report.dto.ReportSubmitDTO;
import com.pdk.module.report.entity.ReportOrder;
import com.pdk.module.report.vo.TimeSlotVO;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ReportService extends IService<ReportOrder> {

    Map<String, Object> preCheckConflict(ConflictCheckDTO dto);

    ReportOrder submitReport(ReportSubmitDTO dto);

    Page<ReportOrder> listMyReports(int page, int size);

    void cancelReport(Long orderId);

    List<TimeSlotVO> listOccupiedSlots(Long venueId, LocalDate date);
}
