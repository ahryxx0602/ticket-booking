package com.ticket.notification.infrastructure.mail;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

@Slf4j
@Component
public class QrCodeUtil {

    private static final int QR_WIDTH = 250;
    private static final int QR_HEIGHT = 250;

    @Value("${app.notification.qr.secret-key:#{null}}")
    private String configuredSecretKey;

    @Autowired(required = false)
    private Environment environment;

    @PostConstruct
    public void validateConfiguration() {
        if (environment != null) {
            boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");
            if (isProd && (configuredSecretKey == null || configuredSecretKey.isBlank() || configuredSecretKey.contains("default-local"))) {
                throw new IllegalStateException("FATAL: QR code signing secret [QR_SECRET_KEY] must be explicitly configured in production environment!");
            }
        }
    }

    public String generateHmacSignature(String bookingReference, Long tripId, String secretKey) {
        String key = (secretKey != null && !secretKey.trim().isEmpty()) ? secretKey : configuredSecretKey;
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalStateException("QR code signing key is not configured. Ticket QR generation blocked.");
        }
        try {
            Mac hmacSha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.trim().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmacSha256.init(secretKeySpec);
            String payload = bookingReference + "|" + (tripId != null ? tripId : 0L);
            byte[] hash = hmacSha256.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception ex) {
            log.error("[QR CODE ERROR] Failed to calculate HMAC signature: {}", ex.getMessage());
            throw new RuntimeException("Could not generate QR HMAC signature", ex);
        }
    }

    public boolean verifyHmacSignature(String bookingReference, Long tripId, String signature, String secretKey) {
        if (signature == null || signature.isBlank()) {
            return false;
        }
        try {
            String expected = generateHmacSignature(bookingReference, tripId, secretKey);
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception ex) {
            return false;
        }
    }

    public String buildQrPayload(String bookingReference, Long tripId, String secretKey) {
        String signature = generateHmacSignature(bookingReference, tripId, secretKey);
        return String.format("v1|%s|%d|%s", bookingReference, tripId != null ? tripId : 0L, signature);
    }

    public String generateQrCodeBase64(String content) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, QR_WIDTH, QR_HEIGHT);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            byte[] pngBytes = outputStream.toByteArray();
            return Base64.getEncoder().encodeToString(pngBytes);
        } catch (Exception ex) {
            log.error("[QR CODE ERROR] Failed to generate QR image: {}", ex.getMessage());
            throw new RuntimeException("Could not generate QR code PNG", ex);
        }
    }
}
