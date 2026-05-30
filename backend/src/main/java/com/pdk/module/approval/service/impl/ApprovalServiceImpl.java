package com.pdk.module.approval.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pdk.common.enums.ReportStatusEnum;
import com.pdk.common.exception.BusinessException;
import com.pdk.module.approval.dto.ApprovalActionDTO;
import com.pdk.module.approval.entity.ApprovalRecord;
import com.pdk.module.approval.mapper.ApprovalRecordMapper;
import com.pdk.module.approval.service.ApprovalService;
import com.pdk.module.approval.vo.ApprovalOrderVO;
import com.pdk.module.auth.entity.User;
import com.pdk.module.auth.service.UserService;
import com.pdk.module.certificate.service.CertificateService;
import com.pdk.module.notice.service.NoticeService;
import com.pdk.module.report.entity.ReportOrder;
import com.pdk.module.report.service.ReportService;
import com.pdk.module.venue.entity.Venue;
import com.pdk.module.venue.service.VenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApprovalServiceImpl extends ServiceImpl<ApprovalRecordMapper, ApprovalRecord> implements ApprovalService {

    private final ReportService reportService;
    private final UserService userService;
    private final VenueService venueService;
    @Lazy
    private final CertificateService certificateService;
    private final NoticeService noticeService;

    @Override
    public Page<ApprovalOrderVO> listPendingApprovals(int page, int size, Integer status) {
        LambdaQueryWrapper<ReportOrder> wrapper = new LambdaQueryWrapper<ReportOrder>()
                .orderByDesc(ReportOrder::getCreateTime);
        if (status != null) {
            wrapper.eq(ReportOrder::getStatus, status);
        }
        Page<ReportOrder> orderPage = reportService.page(new Page<>(page, size), wrapper);

        List<ApprovalOrderVO> vos = orderPage.getRecords().stream().map(order -> {
            ApprovalOrderVO vo = new ApprovalOrderVO();
            vo.setOrder(order);
            User artist = userService.getById(order.getUserId());
            if (artist != null) {
                vo.setArtistName(artist.getRealName());
                vo.setArtistPhone(artist.getPhone());
                vo.setCreditScore(artist.getCreditScore());
            }
            Venue venue = venueService.getById(order.getVenueId());
            if (venue != null) {
                vo.setVenueName(venue.getName());
                vo.setVenueAddress(venue.getAddress());
                vo.setVenueDistrict(venue.getDistrict());
            }
            return vo;
        }).collect(Collectors.toList());

        Page<ApprovalOrderVO> result = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        result.setRecords(vos);
        return result;
    }

    @Override
    @Transactional
    public void doApprove(Long orderId, ApprovalActionDTO dto) {
        Long approverId = StpUtil.getLoginIdAsLong();
        ReportOrder order = reportService.getById(orderId);
        if (order == null) {
            throw new BusinessException(404, "报备单不存在");
        }

        int action = dto.getAction();
        int newStatus;

        switch (action) {
            case 1 -> { // 一审通过
                if (order.getStatus() != ReportStatusEnum.PENDING_FIRST.getCode()) {
                    throw new BusinessException(400, "当前状态不可执行一审操作");
                }
                newStatus = ReportStatusEnum.PENDING_FINAL.getCode();
            }
            case 2 -> { // 一审驳回
                if (order.getStatus() != ReportStatusEnum.PENDING_FIRST.getCode()) {
                    throw new BusinessException(400, "当前状态不可执行一审驳回");
                }
                newStatus = ReportStatusEnum.REJECTED.getCode();
            }
            case 3 -> { // 终审通过
                if (order.getStatus() != ReportStatusEnum.PENDING_FINAL.getCode()) {
                    throw new BusinessException(400, "当前状态不可执行终审操作");
                }
                // 防止同一审批人完成两个审批环节
                boolean alreadyFirstReviewed = count(new LambdaQueryWrapper<ApprovalRecord>()
                        .eq(ApprovalRecord::getOrderId, orderId)
                        .eq(ApprovalRecord::getApproverId, approverId)
                        .eq(ApprovalRecord::getAction, 1)) > 0;
                if (alreadyFirstReviewed) {
                    throw new BusinessException(400, "您已完成该报备的一审，不能同时担任终审，请由其他管理员操作");
                }
                newStatus = ReportStatusEnum.APPROVED.getCode();
            }
            case 4 -> { // 终审驳回
                if (order.getStatus() != ReportStatusEnum.PENDING_FINAL.getCode()) {
                    throw new BusinessException(400, "当前状态不可执行终审驳回");
                }
                newStatus = ReportStatusEnum.REJECTED.getCode();
            }
            default -> throw new BusinessException(400, "无效的审批动作");
        }

        ReportOrder update = new ReportOrder();
        update.setId(orderId);
        update.setStatus(newStatus);
        if (newStatus == ReportStatusEnum.REJECTED.getCode()) {
            update.setRejectReason(dto.getComment());
        }
        reportService.updateById(update);

        ApprovalRecord record = new ApprovalRecord();
        record.setOrderId(orderId);
        record.setApproverId(approverId);
        record.setAction(action);
        record.setComment(dto.getComment());
        save(record);

        // 发送通知给艺人
        String noticeTitle;
        String noticeContent;
        switch (action) {
            case 1 -> {
                noticeTitle = "报备一审通过";
                noticeContent = "您的报备单 " + order.getOrderNo() + " 已通过一审，正在等待终审，请耐心等待。";
            }
            case 2 -> {
                noticeTitle = "报备一审驳回";
                noticeContent = "您的报备单 " + order.getOrderNo() + " 未通过一审。原因：" + dto.getComment();
            }
            case 3 -> {
                noticeTitle = "报备终审通过 🎉";
                noticeContent = "恭喜！您的报备单 " + order.getOrderNo() + " 已审批通过，电子准演证已生成，请前往「我的报备」查看。";
            }
            case 4 -> {
                noticeTitle = "报备终审驳回";
                noticeContent = "您的报备单 " + order.getOrderNo() + " 未通过终审。原因：" + dto.getComment();
            }
            default -> { noticeTitle = "报备状态更新"; noticeContent = "您的报备单状态已更新。"; }
        }
        noticeService.send(order.getUserId(), noticeTitle, noticeContent, 1);

        if (newStatus == ReportStatusEnum.APPROVED.getCode()) {
            certificateService.generateCertificate(orderId);
        }
    }

    @Override
    @Transactional
    public void adminCancelReport(Long orderId, String reason) {
        ReportOrder order = reportService.getById(orderId);
        if (order == null) throw new BusinessException(404, "报备单不存在");

        ReportOrder update = new ReportOrder();
        update.setId(orderId);
        update.setStatus(ReportStatusEnum.CANCELLED.getCode());
        update.setRejectReason(reason);
        reportService.updateById(update);

        // 同步失效证件
        if (order.getCertId() != null) {
            certificateService.invalidateByOrderId(orderId);
        }

        noticeService.send(order.getUserId(), "报备已撤销",
                "您的报备单 " + order.getOrderNo() + " 已被管理员撤销。原因：" + (reason != null ? reason : "无"),
                1);
    }
}
