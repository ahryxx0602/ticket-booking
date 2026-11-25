package com.ticket.partner.domain.port;

import com.ticket.partner.domain.model.WalletTransaction;
import com.ticket.partner.domain.model.WalletTransactionType;

import java.util.List;
import java.util.Optional;

public interface WalletTransactionRepository {
    Optional<WalletTransaction> findById(Long id);
    List<WalletTransaction> findByOperatorId(Long operatorId);
    List<WalletTransaction> findByWalletId(Long walletId);
    default boolean existsByBookingIdAndTransactionType(Long bookingId, WalletTransactionType transactionType) {
        return false;
    }
    WalletTransaction save(WalletTransaction transaction);
}
