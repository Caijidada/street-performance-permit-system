package com.pdk.module.approval.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pdk.module.approval.dto.ApprovalActionDTO;
import com.pdk.module.approval.entity.ApprovalRecord;
import com.pdk.module.approval.vo.ApprovalOrderVO;

public interface ApprovalService extends IService<ApprovalRecord> {

    Page<ApprovalOrderVO> listPendingApprovals(int page, int size, Integer status);

    void doApprove(Long orderId, ApprovalActionDTO dto);

    void adminCancelReport(Long orderId, String reason);
}
