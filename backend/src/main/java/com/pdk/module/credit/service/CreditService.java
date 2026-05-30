package com.pdk.module.credit.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pdk.module.credit.dto.CreditDeductDTO;
import com.pdk.module.credit.entity.CreditLog;

public interface CreditService extends IService<CreditLog> {

    void recordCredit(Long userId, Integer eventType, Long orderId, Long operatorId, String remark);

    void recordCredit(Long userId, Integer eventType, Integer customDelta, Long orderId, Long operatorId, String remark);

    void deductByAdmin(CreditDeductDTO dto);

    Page<CreditLog> listLogs(Long userId, int page, int size);
}
