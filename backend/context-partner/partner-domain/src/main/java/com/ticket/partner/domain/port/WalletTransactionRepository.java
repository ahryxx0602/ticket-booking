package com.ticket.partner.domain.port;

import com.ticket.partner.domain.model.WalletTransaction;

import java.util.List;
import java.util.Optional;

public interface WalletTransactionRepository {
    Optional<WalletTransaction> findById(Long id);
    List<WalletTransaction> findByOperatorId(Long operatorId);
    List<WalletTransaction> findByWalletId(Long walletId);
    WalletTransaction save(WalletTransaction transaction);
}
