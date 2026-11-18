package com.ticket.payment.application.dto;

import lombok.Data;
import java.util.Map;

@Data
public class VnPayCallbackDto {
    private String vnp_TxnRef;
    private String vnp_TransactionNo;
    private String vnp_ResponseCode;
    private String vnp_TransactionStatus;
    private String vnp_SecureHash;
    private Map<String, String> allParams;
}
