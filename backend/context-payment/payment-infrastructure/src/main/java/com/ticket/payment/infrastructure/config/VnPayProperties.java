package com.ticket.payment.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.payment.vnpay")
public class VnPayProperties {
    private String tmnCode = "CGXZLS0Z";
    private String hashSecret = "sandbox_vnpay_hash_secret_placeholder";
    private String payUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private String returnUrl = "http://localhost:8080/api/v1/payments/vnpay-return";
    private String frontendReturnUrl = "http://localhost:5173/checkout";
}
