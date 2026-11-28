package com.ticket.partner.application.usecase;

import com.ticket.partner.application.dto.*;
import com.ticket.partner.domain.model.BannerStatus;
import com.ticket.partner.domain.model.WithdrawalStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface PartnerQueryService {
    OperatorResponse getMyOperatorProfile(Long userId);
    List<VehicleResponse> getMyVehicles(Long userId);
    List<DriverResponse> getMyDrivers(Long userId);
    WalletResponse getMyWallet(Long userId);
    List<WalletTransactionResponse> getMyWalletTransactions(Long userId);
    List<WithdrawalResponse> getMyWithdrawalRequests(Long userId);
    List<BannerResponse> getMyBanners(Long userId);
    com.ticket.shared.dto.PageResponse<PartnerTripResponse> getMyTrips(Long userId, String status, Long vehicleId, int page, int size);
    
    // Admin & Public queries
    List<OperatorResponse> getAllOperators();
    List<WithdrawalResponse> getWithdrawalsByStatus(WithdrawalStatus status);
    List<BannerResponse> getAllBanners(BannerStatus status);
    List<BannerResponse> getActiveBanners(LocalDateTime now, int limit);
    BigDecimal getEffectiveCommissionRate(Long operatorId);
    BigDecimal getGlobalCommissionRate();
    Map<String, String> getAllSystemSettings();
}
