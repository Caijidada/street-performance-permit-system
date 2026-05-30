package com.pdk.module.certificate.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pdk.module.certificate.entity.Certificate;

public interface CertificateService extends IService<Certificate> {

    Certificate generateCertificate(Long orderId);

    Certificate verifyByCertCode(String certCode);

    void invalidateByOrderId(Long orderId);
}
