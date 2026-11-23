package com.ticket.partner.infrastructure.persistence.entity;

import com.ticket.partner.domain.model.WalletTransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_transactions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransactionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @Column(name = "operator_id", nullable = false)
    private Long operatorId;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "trip_id")
    private Long tripId;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 40)
    private WalletTransactionType transactionType;

    @Column(name = "gross_amount", precision = 15, scale = 0)
    private BigDecimal grossAmount;

    @Column(name = "commission_rate", precision = 5, scale = 2)
    private BigDecimal commissionRate;

    @Column(name = "commission_amount", precision = 15, scale = 0)
    private BigDecimal commissionAmount;

    @Column(name = "net_amount", nullable = false, precision = 15, scale = 0)
    private BigDecimal netAmount;

    @Column(name = "balance_before", precision = 15, scale = 0)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", nullable = false, precision = 15, scale = 0)
    private BigDecimal balanceAfter;

    @Column(length = 500)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
