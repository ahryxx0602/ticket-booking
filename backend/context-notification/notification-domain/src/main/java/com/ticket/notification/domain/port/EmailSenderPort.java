package com.ticket.notification.domain.port;

import java.util.Map;

public interface EmailSenderPort {
    void sendHtmlEmail(String to, String subject, String htmlContent);
    void sendETicketEmail(String to, String subject, Map<String, Object> templateModel, String qrBase64);
}
