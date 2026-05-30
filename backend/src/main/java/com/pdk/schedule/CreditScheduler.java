package com.pdk.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pdk.common.enums.ReportStatusEnum;
import com.pdk.module.credit.service.CreditService;
import com.pdk.module.report.entity.ReportOrder;
import com.pdk.module.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 每日凌晨2点：对昨天已结束且通过的演出，自动加信用分
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreditScheduler {

    private final ReportService reportService;
    private final CreditService creditService;

    @Scheduled(cron = "0 0 2 * * ?")
    public void autoAddCreditForCompletedPerformance() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("开始执行自动信用加分任务，日期：{}", yesterday);

        List<ReportOrder> completedOrders = reportService.list(
                new LambdaQueryWrapper<ReportOrder>()
                        .eq(ReportOrder::getPerformDate, yesterday)
                        .eq(ReportOrder::getStatus, ReportStatusEnum.APPROVED.getCode())
        );

        for (ReportOrder order : completedOrders) {
            try {
                // eventType=1 按时演出，+2分（系统自动操作，operatorId=null）
                creditService.recordCredit(order.getUserId(), 1, order.getId(), null, "演出完成，系统自动加分");
                log.info("艺人 {} 报备 {} 加分成功", order.getUserId(), order.getId());
            } catch (Exception e) {
                log.error("艺人 {} 报备 {} 加分失败: {}", order.getUserId(), order.getId(), e.getMessage());
            }
        }

        log.info("自动信用加分任务完成，处理 {} 条记录", completedOrders.size());
    }
}
