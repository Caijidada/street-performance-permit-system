package com.pdk.module.certificate.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.pdk.common.result.Result;
import com.pdk.module.certificate.entity.Certificate;
import com.pdk.module.certificate.service.CertificateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "电子准演证")
@RestController
@RequestMapping("/api/certificate")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;

    @Operation(summary = "扫码核验证件（公开接口）")
    @GetMapping("/verify/{certCode}")
    public Result<Certificate> verify(@PathVariable String certCode) {
        return Result.success(certificateService.verifyByCertCode(certCode));
    }

    @Operation(summary = "查询我的证件")
    @SaCheckLogin
    @GetMapping("/my/{orderId}")
    public Result<Certificate> getMyCert(@PathVariable Long orderId) {
        return Result.success(certificateService.getOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Certificate>()
                        .eq(Certificate::getOrderId, orderId)));
    }
}
