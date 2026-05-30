package com.pdk.module.certificate.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pdk.common.exception.BusinessException;
import com.pdk.module.certificate.entity.Certificate;
import com.pdk.module.certificate.mapper.CertificateMapper;
import com.pdk.module.certificate.service.CertificateService;
import com.pdk.module.report.entity.ReportOrder;
import com.pdk.module.report.service.ReportService;
import com.pdk.util.QrCodeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateServiceImpl extends ServiceImpl<CertificateMapper, Certificate> implements CertificateService {

    private final ReportService reportService;
    private final QrCodeUtil qrCodeUtil;

    @Value("${file.upload-path}")
    private String uploadPath;

    @Value("${file.access-url}")
    private String accessUrl;

    // 前端核验页域名，扫码后直接跳转
    @Value("${pdk.base-url}")
    private String baseUrl;

    @Override
    @Transactional
    public Certificate generateCertificate(Long orderId) {
        ReportOrder order = reportService.getById(orderId);
        if (order == null) {
            throw new BusinessException(404, "报备单不存在");
        }

        String certCode = "CERT-" + IdUtil.fastSimpleUUID().toUpperCase();

        // 二维码内容为完整核验 URL，扫码即可跳转验证页
        String verifyUrl = baseUrl + "/verify/" + certCode;
        String qrFileName = "cert_qr_" + orderId + ".png";
        String qrPath = uploadPath + "certificates/" + qrFileName;
        try {
            qrCodeUtil.generate(verifyUrl, qrPath);
        } catch (Exception e) {
            log.error("二维码生成失败: {}", e.getMessage());
        }

        Certificate cert = new Certificate();
        cert.setCertCode(certCode);
        cert.setOrderId(orderId);
        cert.setUserId(order.getUserId());
        cert.setVenueId(order.getVenueId());
        cert.setPerformDate(order.getPerformDate());
        cert.setTimeSlotStart(order.getTimeSlotStart());
        cert.setTimeSlotEnd(order.getTimeSlotEnd());
        cert.setCertUrl(accessUrl + "certificates/" + qrFileName);
        cert.setValidStatus(1);
        save(cert);

        ReportOrder update = new ReportOrder();
        update.setId(orderId);
        update.setCertId(cert.getId());
        reportService.updateById(update);

        return cert;
    }

    @Override
    public Certificate verifyByCertCode(String certCode) {
        Certificate cert = getOne(new LambdaQueryWrapper<Certificate>()
                .eq(Certificate::getCertCode, certCode));
        if (cert == null) {
            throw new BusinessException(404, "证件不存在或无效");
        }
        if (cert.getValidStatus() == 0) {
            throw new BusinessException(400, "证件已失效");
        }
        return cert;
    }

    @Override
    public void invalidateByOrderId(Long orderId) {
        update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Certificate>()
                .eq(Certificate::getOrderId, orderId)
                .set(Certificate::getValidStatus, 0));
    }
}
