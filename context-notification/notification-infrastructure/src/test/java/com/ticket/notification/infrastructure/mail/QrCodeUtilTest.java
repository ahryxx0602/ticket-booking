package com.ticket.notification.infrastructure.mail;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.awt.image.BufferedImage;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("QR Code Util & HMAC-SHA256 Verification Tests")
class QrCodeUtilTest {

    private final QrCodeUtil qrCodeUtil = new QrCodeUtil();

    @Test
    @DisplayName("Should generate valid HMAC-SHA256 signature and round-trip decode QR code")
    void testQrCodeGenerationAndDecode() throws Exception {
        String bookingRef = "BKG_TEST_123456";
        Long tripId = 99L;
        String secret = "SAMPLE_SECRET_KEY_2026";

        String payload = qrCodeUtil.buildQrPayload(bookingRef, tripId, secret);
        assertNotNull(payload);
        assertTrue(payload.startsWith("v1|BKG_TEST_123456|99|"));

        // Sinh ảnh Base64
        String base64Image = qrCodeUtil.generateQrCodeBase64(payload);
        assertNotNull(base64Image);
        assertFalse(base64Image.isEmpty());

        // Giải mã ngược ảnh QR để kiểm chứng độ chính xác
        byte[] imageBytes = Base64.getDecoder().decode(base64Image);
        BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
        assertNotNull(bufferedImage);

        BufferedImageLuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        Result decodedResult = new MultiFormatReader().decode(bitmap);

        assertNotNull(decodedResult);
        assertEquals(payload, decodedResult.getText());
    }

    @Test
    @DisplayName("Different bookingReference or tripId should yield different HMAC signatures")
    void testHmacSignatureUniqueness() {
        String sig1 = qrCodeUtil.generateHmacSignature("BKG_001", 10L, "SECRET");
        String sig2 = qrCodeUtil.generateHmacSignature("BKG_002", 10L, "SECRET");
        String sig3 = qrCodeUtil.generateHmacSignature("BKG_001", 20L, "SECRET");

        assertNotEquals(sig1, sig2);
        assertNotEquals(sig1, sig3);
    }
}
