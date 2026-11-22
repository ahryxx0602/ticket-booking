package com.ticket.partner.domain.port;

import com.ticket.partner.domain.model.WithdrawalRequest;
import com.ticket.partner.domain.model.WithdrawalStatus;

import java.util.List;
import java.util.Optional;

public interface WithdrawalRequestRepository {
    Optional<WithdrawalRequest> findById(Long id);
    List<WithdrawalRequest> findByOperatorId(Long operatorId);
    List<WithdrawalRequest> findByStatus(WithdrawalStatus status);
    WithdrawalRequest save(WithdrawalRequest request);
}
