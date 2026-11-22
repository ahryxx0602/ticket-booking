package com.ticket.partner.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "operator_wallets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorWalletJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operator_id", nullable = false, unique = true)
    private Long operatorId;

    @Column(name = "available_balance", nullable = false, precision = 15, scale = 0)
    private BigDecimal availableBalance;

    @Column(name = "pending_settlement_balance", nullable = false, precision = 15, scale = 0)
    private BigDecimal pendingSettlementBalance;

    @Column(name = "locked_balance", nullable = false, precision = 15, scale = 0)
    private BigDecimal lockedBalance;

    @Column(name = "debt_balance", nullable = false, precision = 15, scale = 0)
    private BigDecimal debtBalance;

    @Column(nullable = false, length = 10)
    private String currency;

    @Version
    @Column(nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
