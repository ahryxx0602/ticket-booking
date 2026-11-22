package com.ticket.notification.infrastructure.mail;

import com.ticket.notification.domain.port.EmailSenderPort;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JavaMailSenderAdapter implements EmailSenderPort {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final QrCodeUtil qrCodeUtil;

    @Value("${app.notification.mail.from:noreply@ticketbooking.local}")
    private String fromEmail;

    @Value("${app.notification.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.notification.qr.secret-key:${app.notification.qr.secret:TICKET_BOOKING_QR_SECRET_KEY_2026}}")
    private String qrSecret;

    @Override
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        if (!mailEnabled) {
            log.info("[MOCK MAIL] Mail disabled. Mock send HTML email to: {}, Subject: {}", to, subject);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("[MAIL SENT] Successfully sent email to {}", to);
        } catch (Exception ex) {
            log.error("[MAIL ERROR] Failed to send email to {}", to, ex);
            throw new RuntimeException("Failed to send email to " + to, ex);
        }
    }

    @Override
    public void sendETicketEmail(String to, String subject, Map<String, Object> templateModel, String qrPayload) {
        String finalPayload = qrPayload;
        if (qrPayload != null && !qrPayload.startsWith("v1|")) {
            Long tripId = 0L;
            if (templateModel != null && templateModel.get("tripId") instanceof Long tId) {
                tripId = tId;
            }
            finalPayload = qrCodeUtil.buildQrPayload(qrPayload, tripId, qrSecret);
        }
        // Sinh ảnh QR Base64
        String qrContent = qrCodeUtil.generateQrCodeBase64(finalPayload);
        Context context = new Context();
        context.setVariables(templateModel);
        context.setVariable("qrCodeBase64", qrContent);

        String html = templateEngine.process("mail/e-ticket", context);
        sendHtmlEmail(to, subject, html);
    }
}
