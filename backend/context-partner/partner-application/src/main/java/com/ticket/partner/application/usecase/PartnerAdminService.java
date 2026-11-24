package com.ticket.partner.application.usecase;

import com.ticket.partner.application.dto.*;

import java.math.BigDecimal;
import java.util.Map;

public interface PartnerAdminService {
    OperatorResponse createOperatorByAdmin(RegisterOperatorCommand cmd, Long adminId);
    OperatorResponse updateOperatorByAdmin(Long operatorId, RegisterOperatorCommand cmd, Long adminId);
    void deleteOperatorByAdmin(Long operatorId, Long adminId);
    OperatorResponse approveOperator(Long operatorId, Long adminId);
    OperatorResponse rejectOperator(Long operatorId, String reason, Long adminId);
    OperatorResponse suspendOperator(Long operatorId, String reason, Long adminId);
    OperatorResponse updateCommissionRate(Long operatorId, BigDecimal rate);
    void updateGlobalCommissionRate(BigDecimal rate);
    void updateSystemSettings(Map<String, String> settings);
    WithdrawalResponse approveWithdrawal(Long withdrawalId, Long adminId, String note);
    WithdrawalResponse rejectWithdrawal(Long withdrawalId, Long adminId, String reason);
    BannerResponse approveBanner(Long bannerId, Long adminId);
    BannerResponse rejectBanner(Long bannerId, String reason, Long adminId);
    BannerResponse createPlatformBanner(BannerCommand cmd, Long adminId);
    BannerResponse updateBannerByAdmin(Long bannerId, BannerCommand cmd, Long adminId);
    void deleteBannerByAdmin(Long bannerId, Long adminId);
}
