package com.ticket.partner.application.usecase;

import com.ticket.partner.application.dto.*;
import com.ticket.partner.domain.model.WithdrawalStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PartnerQueryService {
    OperatorResponse getMyOperatorProfile(Long userId);
    List<VehicleResponse> getMyVehicles(Long userId);
    List<DriverResponse> getMyDrivers(Long userId);
    WalletResponse getMyWallet(Long userId);
    List<WalletTransactionResponse> getMyWalletTransactions(Long userId);
    List<WithdrawalResponse> getMyWithdrawalRequests(Long userId);
    List<BannerResponse> getMyBanners(Long userId);
    
    // Admin & Public queries
    List<OperatorResponse> getAllOperators();
    List<WithdrawalResponse> getWithdrawalsByStatus(WithdrawalStatus status);
    List<BannerResponse> getActiveBanners(LocalDateTime now, int limit);
    BigDecimal getEffectiveCommissionRate(Long operatorId);
    BigDecimal getGlobalCommissionRate();
}
