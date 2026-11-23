package com.ticket.partner.application.service;

import com.ticket.partner.application.dto.*;
import com.ticket.partner.application.usecase.PartnerAdminService;
import com.ticket.partner.domain.model.*;
import com.ticket.partner.domain.port.*;
import com.ticket.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartnerAdminServiceImpl implements PartnerAdminService {

    public static final String SETTING_COMMISSION_DEFAULT = "COMMISSION_RATE_DEFAULT";

    private final BusOperatorRepository operatorRepository;
    private final OperatorWalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final WithdrawalRequestRepository withdrawalRepository;
    private final BannerRepository bannerRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final PartnerIdentityPort partnerIdentityPort;

    @Override
    @Transactional
    public OperatorResponse approveOperator(Long operatorId, Long adminId) {
        BusOperator operator = operatorRepository.findById(operatorId)
                .orElseThrow(() -> new BusinessException("OPERATOR_NOT_FOUND", "Operator not found: " + operatorId));

        operator.activate();
        BusOperator saved = operatorRepository.save(operator);

        // Cấp quyền ROLE_OPERATOR cho user
        partnerIdentityPort.grantOperatorRole(operator.getUserId());

        log.info("Admin ID={} approved operator ID={}, User ID={}", adminId, operatorId, operator.getUserId());
        return PartnerMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public OperatorResponse rejectOperator(Long operatorId, String reason, Long adminId) {
        BusOperator operator = operatorRepository.findById(operatorId)
                .orElseThrow(() -> new BusinessException("OPERATOR_NOT_FOUND", "Operator not found: " + operatorId));

        operator.reject(reason);
        BusOperator saved = operatorRepository.save(operator);
        log.info("Admin ID={} rejected operator ID={}, reason: {}", adminId, operatorId, reason);
        return PartnerMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public OperatorResponse suspendOperator(Long operatorId, String reason, Long adminId) {
        BusOperator operator = operatorRepository.findById(operatorId)
                .orElseThrow(() -> new BusinessException("OPERATOR_NOT_FOUND", "Operator not found: " + operatorId));

        operator.suspend(reason);
        BusOperator saved = operatorRepository.save(operator);
        log.warn("Admin ID={} suspended operator ID={}, reason: {}", adminId, operatorId, reason);
        return PartnerMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public OperatorResponse updateCommissionRate(Long operatorId, BigDecimal rate) {
        BusOperator operator = operatorRepository.findById(operatorId)
                .orElseThrow(() -> new BusinessException("OPERATOR_NOT_FOUND", "Operator not found: " + operatorId));

        operator.updateCommissionRate(rate);
        BusOperator saved = operatorRepository.save(operator);
        log.info("Updated custom commission rate for operator ID={} to {}%", operatorId, rate);
        return PartnerMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void updateGlobalCommissionRate(BigDecimal rate) {
        if (rate == null || rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessException("INVALID_COMMISSION_RATE", "Global commission rate must be between 0% and 100%");
        }
        SystemSetting setting = systemSettingRepository.findByKey(SETTING_COMMISSION_DEFAULT)
                .orElseGet(() -> SystemSetting.builder().settingKey(SETTING_COMMISSION_DEFAULT).build());

        setting.setSettingValue(rate.toPlainString());
        setting.setDescription("Global marketplace default commission rate (%)");
        systemSettingRepository.save(setting);
        log.info("Updated global commission rate to {}%", rate);
    }

    @Override
    @Transactional
    public WithdrawalResponse approveWithdrawal(Long withdrawalId, Long adminId, String note) {
        WithdrawalRequest request = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new BusinessException("WITHDRAWAL_NOT_FOUND", "Withdrawal request not found: " + withdrawalId));

        OperatorWallet wallet = walletRepository.findByOperatorId(request.getOperatorId())
                .orElseThrow(() -> new BusinessException("WALLET_NOT_FOUND", "Operator wallet not found"));

        request.approve(adminId, note);
        wallet.debitLocked(request.getAmount());

        withdrawalRepository.save(request);
        walletRepository.save(wallet);

        WalletTransaction tx = WalletTransaction.builder()
                .walletId(wallet.getId())
                .operatorId(request.getOperatorId())
                .transactionType(WalletTransactionType.WITHDRAWAL_PAYOUT)
                .netAmount(request.getAmount().negate())
                .balanceAfter(wallet.getAvailableBalance())
                .description("Approved withdrawal #" + request.getId() + ". Note: " + (note != null ? note : "None"))
                .build();
        transactionRepository.save(tx);

        log.info("Admin ID={} approved withdrawal #{} for operator ID={}", adminId, withdrawalId, request.getOperatorId());
        return PartnerMapper.toResponse(request);
    }

    @Override
    @Transactional
    public WithdrawalResponse rejectWithdrawal(Long withdrawalId, Long adminId, String reason) {
        WithdrawalRequest request = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new BusinessException("WITHDRAWAL_NOT_FOUND", "Withdrawal request not found: " + withdrawalId));

        OperatorWallet wallet = walletRepository.findByOperatorId(request.getOperatorId())
                .orElseThrow(() -> new BusinessException("WALLET_NOT_FOUND", "Operator wallet not found"));

        request.reject(adminId, reason);
        wallet.releaseLocked(request.getAmount());

        withdrawalRepository.save(request);
        walletRepository.save(wallet);

        WalletTransaction tx = WalletTransaction.builder()
                .walletId(wallet.getId())
                .operatorId(request.getOperatorId())
                .transactionType(WalletTransactionType.WITHDRAWAL_REFUND)
                .netAmount(request.getAmount())
                .balanceAfter(wallet.getAvailableBalance())
                .description("Rejected withdrawal #" + request.getId() + ". Reason: " + (reason != null ? reason : "None"))
                .build();
        transactionRepository.save(tx);

        log.info("Admin ID={} rejected withdrawal #{} for operator ID={}", adminId, withdrawalId, request.getOperatorId());
        return PartnerMapper.toResponse(request);
    }

    @Override
    @Transactional
    public BannerResponse approveBanner(Long bannerId, Long adminId) {
        Banner banner = bannerRepository.findById(bannerId)
                .orElseThrow(() -> new BusinessException("BANNER_NOT_FOUND", "Banner not found: " + bannerId));

        banner.approve();
        Banner saved = bannerRepository.save(banner);
        log.info("Admin ID={} approved banner ID={}", adminId, bannerId);
        return PartnerMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public BannerResponse rejectBanner(Long bannerId, String reason, Long adminId) {
        Banner banner = bannerRepository.findById(bannerId)
                .orElseThrow(() -> new BusinessException("BANNER_NOT_FOUND", "Banner not found: " + bannerId));

        banner.reject(reason);
        Banner saved = bannerRepository.save(banner);
        log.info("Admin ID={} rejected banner ID={}, reason: {}", adminId, bannerId, reason);
        return PartnerMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public BannerResponse createPlatformBanner(BannerCommand cmd, Long adminId) {
        Banner banner = Banner.builder()
                .operatorId(null) // platform banner
                .title(cmd.getTitle())
                .imageUrl(cmd.getImageUrl())
                .targetUrl(cmd.getTargetUrl())
                .position(cmd.getPosition())
                .startDate(cmd.getStartDate())
                .endDate(cmd.getEndDate())
                .priority(cmd.getPriority() != null ? cmd.getPriority() : 0)
                .status(BannerStatus.ACTIVE) // platform banner active ngay lập tức
                .build();
        banner.validate();

        Banner saved = bannerRepository.save(banner);
        log.info("Admin ID={} created platform banner ID={}", adminId, saved.getId());
        return PartnerMapper.toResponse(saved);
    }
}
