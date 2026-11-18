package com.ticket.payment.infrastructure.service;

import com.ticket.payment.application.dto.CreatePaymentCommand;
import com.ticket.payment.application.dto.PaymentInitResponse;
import com.ticket.payment.application.dto.VnPayCallbackDto;
import com.ticket.payment.application.usecase.CreatePaymentUseCase;
import com.ticket.payment.application.usecase.HandlePaymentCallbackUseCase;
import com.ticket.payment.domain.model.Payment;
import com.ticket.payment.domain.model.PaymentStatus;
import com.ticket.payment.domain.repository.PaymentRepository;
import com.ticket.payment.infrastructure.crypto.HmacSha512Util;
import com.ticket.shared.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements CreatePaymentUseCase, HandlePaymentCallbackUseCase {

    private final PaymentRepository paymentRepository;

    @Value("${app.payment.vnpay.hash-secret:sandbox_vnpay_hash_secret_placeholder}")
    private String vnpayHashSecret;

    @Value("${app.payment.vnpay.pay-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String vnpayPayUrl;

    @Override
    public PaymentInitResponse execute(CreatePaymentCommand command) {
        String paymentRef = "PAY_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        Payment payment = Payment.builder()
                .paymentReference(paymentRef)
                .bookingId(command.getBookingId())
                .amount(command.getAmount())
                .currency("VND")
                .paymentMethod(command.getPaymentMethod())
                .status(PaymentStatus.PENDING)
                .build();

        paymentRepository.save(payment);

        // Mock generate VNPay URL with HMAC SHA512 signature
        String signData = "vnp_Amount=" + command.getAmount() + "&vnp_TxnRef=" + paymentRef;
        String secureHash = HmacSha512Util.hmacSHA512(vnpayHashSecret, signData);
        String redirectUrl = vnpayPayUrl + "?" + signData + "&vnp_SecureHash=" + secureHash;

        return PaymentInitResponse.builder()
                .paymentReference(paymentRef)
                .redirectPaymentUrl(redirectUrl)
                .build();
    }

    @Override
    public boolean execute(VnPayCallbackDto callbackDto) {
        Payment payment = paymentRepository.findByPaymentReference(callbackDto.getVnp_TxnRef())
                .orElseThrow(() -> new EntityNotFoundException("Payment", callbackDto.getVnp_TxnRef()));

        if ("00".equals(callbackDto.getVnp_ResponseCode())) {
            payment.markSuccess(callbackDto.getVnp_TransactionNo());
            paymentRepository.save(payment);
            log.info("Payment reference {} succeeded with TxnNo: {}", payment.getPaymentReference(), callbackDto.getVnp_TransactionNo());
            return true;
        } else {
            payment.markFailed();
            paymentRepository.save(payment);
            log.warn("Payment reference {} failed with response code: {}", payment.getPaymentReference(), callbackDto.getVnp_ResponseCode());
            return false;
        }
    }
}
