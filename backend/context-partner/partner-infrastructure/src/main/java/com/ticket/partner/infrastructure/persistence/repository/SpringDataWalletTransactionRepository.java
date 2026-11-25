package com.ticket.partner.infrastructure.persistence.repository;

import com.ticket.partner.domain.model.WalletTransactionType;
import com.ticket.partner.infrastructure.persistence.entity.WalletTransactionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataWalletTransactionRepository extends JpaRepository<WalletTransactionJpaEntity, Long> {
    List<WalletTransactionJpaEntity> findByOperatorIdOrderByCreatedAtDesc(Long operatorId);
    List<WalletTransactionJpaEntity> findByWalletIdOrderByCreatedAtDesc(Long walletId);
    boolean existsByBookingIdAndTransactionType(Long bookingId, WalletTransactionType transactionType);
}
