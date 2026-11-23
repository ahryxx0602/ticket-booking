package com.ticket.notification.infrastructure.mail;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Component
public class QrCodeUtil {

    private static final String DEFAULT_SECRET = "TICKET_BOOKING_QR_SECRET_KEY_2026";
    private static final int QR_WIDTH = 250;
    private static final int QR_HEIGHT = 250;

    public String generateHmacSignature(String bookingReference, Long tripId, String secretKey) {
        String key = secretKey != null && !secretKey.trim().isEmpty() ? secretKey : DEFAULT_SECRET;
        try {
            Mac hmacSha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmacSha256.init(secretKeySpec);
            String payload = bookingReference + "|" + tripId;
            byte[] hash = hmacSha256.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception ex) {
            log.error("[QR CODE ERROR] Failed to calculate HMAC signature", ex);
            throw new RuntimeException("Could not generate QR HMAC signature", ex);
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
            log.error("[QR CODE ERROR] Failed to generate QR image", ex);
            throw new RuntimeException("Could not generate QR code PNG", ex);
        }
    }
}
