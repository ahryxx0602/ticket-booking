package com.ticket.partner.domain.port;

import com.ticket.partner.domain.model.OperatorWallet;

import java.math.BigDecimal;
import java.util.Optional;

public interface OperatorWalletRepository {
    Optional<OperatorWallet> findById(Long id);
    Optional<OperatorWallet> findByOperatorId(Long operatorId);
    OperatorWallet save(OperatorWallet wallet);
    void creditPendingAtomic(Long operatorId, BigDecimal amount);
}
