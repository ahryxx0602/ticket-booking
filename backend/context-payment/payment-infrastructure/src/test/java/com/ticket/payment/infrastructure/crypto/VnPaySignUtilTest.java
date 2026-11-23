package com.ticket.payment.infrastructure.crypto;

import com.ticket.payment.infrastructure.config.VnPayProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class VnPaySignUtilTest {

    private VnPayProperties properties;

    @BeforeEach
    void setUp() {
        properties = new VnPayProperties();
        properties.setTmnCode("TEST_TMN");
        properties.setHashSecret("secret_key_123456789");
        properties.setPayUrl("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        properties.setReturnUrl("http://localhost:8080/callback");
    }

    @Test
    void generatePaymentUrl_shouldContainRequiredParamsAndSecureHash() {
        LocalDateTime createDate = LocalDateTime.of(2026, 11, 22, 10, 0, 0);
        LocalDateTime expireDate = LocalDateTime.of(2026, 11, 22, 10, 10, 0);

        String url = VnPaySignUtil.generatePaymentUrl(
                properties,
                "PAY_TEST_001",
                BigDecimal.valueOf(1500000),
                "Thanh toan ve test",
                "127.0.0.1",
                createDate,
                expireDate
        );

        assertThat(url).startsWith("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?");
        assertThat(url).contains("vnp_Amount=150000000");
        assertThat(url).contains("vnp_TmnCode=TEST_TMN");
        assertThat(url).contains("vnp_TxnRef=PAY_TEST_001");
        assertThat(url).contains("vnp_CreateDate=");
        assertThat(url).contains("vnp_ExpireDate=");
        assertThat(url).contains("vnp_SecureHash=");
        // Assert format of dates
        assertThat(url).containsPattern("vnp_CreateDate=[0-9]{14}");
        assertThat(url).containsPattern("vnp_ExpireDate=[0-9]{14}");
    }

    @Test
    void verifySignature_whenValid_shouldReturnTrue() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "150000000");
        params.put("vnp_Command", "pay");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TxnRef", "PAY_TEST_002");

        // Calculate secure hash using the same secret
        String signData = "vnp_Amount=150000000&vnp_Command=pay&vnp_ResponseCode=00&vnp_TxnRef=PAY_TEST_002";
        String secureHash = HmacSha512Util.hmacSHA512("secret_key_123456789", signData);
        params.put("vnp_SecureHash", secureHash);

        boolean valid = VnPaySignUtil.verifySignature(params, "secret_key_123456789");
        assertThat(valid).isTrue();
    }

    @Test
    void verifySignature_whenTampered_shouldReturnFalse() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "100000000"); // Tampered amount
        params.put("vnp_TxnRef", "PAY_TEST_003");
        params.put("vnp_SecureHash", "invalid_fake_hash");

        boolean valid = VnPaySignUtil.verifySignature(params, "secret_key_123456789");
        assertThat(valid).isFalse();
    }

    @Test
    void verifySignature_whenMissingHash_shouldReturnFalse() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "100000000");

        boolean valid = VnPaySignUtil.verifySignature(params, "secret_key_123456789");
        assertThat(valid).isFalse();
    }

    @Test
    void generatePaymentUrl_andVerifySignatureRoundTrip_shouldBeValid() {
        LocalDateTime createDate = LocalDateTime.of(2026, 11, 22, 10, 0, 0);
        LocalDateTime expireDate = LocalDateTime.of(2026, 11, 22, 10, 10, 0);

        String url = VnPaySignUtil.generatePaymentUrl(
                properties,
                "PAY_ROUNDTRIP_01",
                BigDecimal.valueOf(2500000),
                "Thanh toan ve khu hoi",
                "0:0:0:0:0:0:0:1", // IPv6 should be normalized to 127.0.0.1
                createDate,
                expireDate
        );

        assertThat(url).contains("vnp_IpAddr=127.0.0.1");

        // Parse query string back into Map
        String queryString = url.substring(url.indexOf('?') + 1);
        Map<String, String> queryParams = new HashMap<>();
        for (String pair : queryString.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                String key = java.net.URLDecoder.decode(pair.substring(0, idx), java.nio.charset.StandardCharsets.US_ASCII);
                String val = java.net.URLDecoder.decode(pair.substring(idx + 1), java.nio.charset.StandardCharsets.US_ASCII);
                queryParams.put(key, val);
            }
        }

        boolean verified = VnPaySignUtil.verifySignature(queryParams, "secret_key_123456789");
        assertThat(verified).isTrue();
    }
}
