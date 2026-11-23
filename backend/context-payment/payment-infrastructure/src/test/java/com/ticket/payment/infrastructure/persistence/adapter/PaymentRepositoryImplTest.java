package com.ticket.payment.infrastructure.persistence.adapter;

import com.ticket.payment.domain.model.Payment;
import com.ticket.payment.domain.model.PaymentMethod;
import com.ticket.payment.domain.model.PaymentStatus;
import com.ticket.payment.infrastructure.persistence.entity.PaymentJpaEntity;
import com.ticket.payment.infrastructure.persistence.repository.SpringDataPaymentRepository;
import com.ticket.shared.exception.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentRepositoryImplTest {

    @Mock
    private SpringDataPaymentRepository springDataPaymentRepository;

    @InjectMocks
    private PaymentRepositoryImpl paymentRepository;

    private PaymentJpaEntity entity;

    @BeforeEach
    void setUp() {
        entity = new PaymentJpaEntity();
        entity.setId(10L);
        entity.setPaymentReference("PAY_TEST_VER");
        entity.setBookingId(100L);
        entity.setAmount(BigDecimal.valueOf(1500000));
        entity.setStatus(PaymentStatus.PENDING);
        entity.setVersion(1L);
    }

    @Test
    void save_whenVersionMatches_shouldUpdateAndReturnDomain() {
        Payment payment = Payment.builder()
                .id(10L)
                .paymentReference("PAY_TEST_VER")
                .bookingId(100L)
                .amount(BigDecimal.valueOf(1500000))
                .status(PaymentStatus.SUCCESS)
                .version(1L)
                .build();

        when(springDataPaymentRepository.findById(10L)).thenReturn(Optional.of(entity));
        when(springDataPaymentRepository.save(any(PaymentJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment updated = paymentRepository.save(payment);

        assertThat(updated).isNotNull();
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        verify(springDataPaymentRepository).save(entity);
    }

    @Test
    void save_whenVersionMismatch_shouldThrowObjectOptimisticLockingFailureException() {
        Payment payment = Payment.builder()
                .id(10L)
                .paymentReference("PAY_TEST_VER")
                .bookingId(100L)
                .amount(BigDecimal.valueOf(1500000))
                .status(PaymentStatus.SUCCESS)
                .version(0L) // Stale version 0 while DB has 1
                .build();

        when(springDataPaymentRepository.findById(10L)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> paymentRepository.save(payment))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        verify(springDataPaymentRepository, never()).save(any());
    }

    @Test
    void save_whenIdNotFound_shouldThrowEntityNotFoundException() {
        Payment payment = Payment.builder()
                .id(999L)
                .paymentReference("PAY_NONEXISTENT")
                .version(0L)
                .build();

        when(springDataPaymentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentRepository.save(payment))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Payment");

        verify(springDataPaymentRepository, never()).save(any());
    }

    @Test
    void save_whenNewPaymentWithoutId_shouldInsertSuccessfully() {
        Payment payment = Payment.builder()
                .paymentReference("PAY_NEW_001")
                .bookingId(200L)
                .amount(BigDecimal.valueOf(500000))
                .status(PaymentStatus.PENDING)
                .paymentMethod(PaymentMethod.VNPAY)
                .build();

        when(springDataPaymentRepository.save(any(PaymentJpaEntity.class))).thenAnswer(inv -> {
            PaymentJpaEntity e = inv.getArgument(0);
            e.setId(50L);
            e.setVersion(0L);
            return e;
        });

        Payment created = paymentRepository.save(payment);

        assertThat(created).isNotNull();
        assertThat(created.getId()).isEqualTo(50L);
        verify(springDataPaymentRepository, never()).findById(any());
    }
}
