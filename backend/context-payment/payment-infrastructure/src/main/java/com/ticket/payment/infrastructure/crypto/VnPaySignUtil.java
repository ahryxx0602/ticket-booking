package com.ticket.payment.infrastructure.crypto;

import com.ticket.payment.infrastructure.config.VnPayProperties;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class VnPaySignUtil {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    public static String generatePaymentUrl(VnPayProperties props,
                                            String txnRef,
                                            BigDecimal amount,
                                            String orderInfo,
                                            String ipAddr,
                                            LocalDateTime createDate,
                                            LocalDateTime expireDate) {
        String clientIp = (ipAddr == null || ipAddr.isBlank() || "0:0:0:0:0:0:0:1".equals(ipAddr)) ? "127.0.0.1" : ipAddr;
        long vnpAmount = amount.multiply(BigDecimal.valueOf(100)).longValueExact();

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", props.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(vnpAmount));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", txnRef);
        vnpParams.put("vnp_OrderInfo", orderInfo != null ? orderInfo : "Payment for booking " + txnRef);
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", props.getReturnUrl());
        vnpParams.put("vnp_IpAddr", clientIp);
        vnpParams.put("vnp_CreateDate", formatVnDateTime(createDate));
        vnpParams.put("vnp_ExpireDate", formatVnDateTime(expireDate));

        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        for (String fieldName : fieldNames) {
            String fieldValue = vnpParams.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                try {
                    if (!hashData.isEmpty()) {
                        hashData.append('&');
                    }
                    hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));

                    if (!query.isEmpty()) {
                        query.append('&');
                    }
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()))
                            .append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                } catch (Exception e) {
                    throw new RuntimeException("Error encoding VNPay parameters", e);
                }
            }
        }

        String secureHash = HmacSha512Util.hmacSHA512(props.getHashSecret(), hashData.toString());
        return props.getPayUrl() + "?" + query.toString() + "&vnp_SecureHash=" + secureHash;
    }

    private static String formatVnDateTime(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return "";
        }
        ZonedDateTime zdt = localDateTime.atZone(ZoneId.systemDefault()).withZoneSameInstant(VN_ZONE);
        return zdt.format(FORMATTER);
    }

    public static boolean verifySignature(Map<String, String> allParams, String secretKey) {
        if (allParams == null || secretKey == null) {
            return false;
        }
        String vnpSecureHash = allParams.get("vnp_SecureHash");
        if (vnpSecureHash == null || vnpSecureHash.isBlank()) {
            return false;
        }

        Map<String, String> fields = new HashMap<>(allParams);
        fields.remove("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");

        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        for (String fieldName : fieldNames) {
            String fieldValue = fields.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                try {
                    if (!hashData.isEmpty()) {
                        hashData.append('&');
                    }
                    hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                } catch (Exception e) {
                    return false;
                }
            }
        }

        String calculatedHash = HmacSha512Util.hmacSHA512(secretKey, hashData.toString());
        return MessageDigest.isEqual(
                calculatedHash.toUpperCase().getBytes(StandardCharsets.UTF_8),
                vnpSecureHash.toUpperCase().getBytes(StandardCharsets.UTF_8)
        );
    }
}
