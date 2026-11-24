package com.ticket.partner.presentation.controller;

import com.ticket.partner.application.dto.*;
import com.ticket.partner.application.usecase.PartnerAdminService;
import com.ticket.partner.application.usecase.PartnerQueryService;
import com.ticket.partner.domain.model.BannerStatus;
import com.ticket.partner.domain.model.WithdrawalStatus;
import com.ticket.partner.domain.port.PartnerIdentityPort;
import com.ticket.shared.dto.ApiResponse;
import com.ticket.shared.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPartnerController {

    private final PartnerAdminService adminService;
    private final PartnerQueryService queryService;
    private final PartnerIdentityPort identityPort;

    private Long getAdminId(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new ForbiddenException("UNAUTHORIZED", "Admin must be authenticated");
        }
        if (identityPort != null) {
            return identityPort.findUserIdByUsername(principal.getName())
                    .orElseGet(() -> {
                        try {
                            return Long.parseLong(principal.getName());
                        } catch (NumberFormatException e) {
                            log.error("Failed to resolve admin ID for principal '{}': user not found in identity context", principal.getName());
                            throw new ForbiddenException("INVALID_ADMIN_PRINCIPAL", "Cannot resolve admin identity for audit logging: " + principal.getName());
                        }
                    });
        }
        try {
            return Long.parseLong(principal.getName());
        } catch (NumberFormatException e) {
            log.error("Failed to parse numeric admin ID for principal '{}'", principal.getName());
            throw new ForbiddenException("INVALID_ADMIN_PRINCIPAL", "Cannot resolve admin identity for audit logging: " + principal.getName());
        }
    }

    // --- 1. Operator CRUD, Approval & Suspension ---
    @GetMapping("/partners/operators")
    public ResponseEntity<ApiResponse<List<OperatorResponse>>> getAllOperators() {
        List<OperatorResponse> list = queryService.getAllOperators();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PostMapping("/partners/operators")
    public ResponseEntity<ApiResponse<OperatorResponse>> createOperator(
            Principal principal,
            @RequestBody RegisterOperatorCommand cmd
    ) {
        Long adminId = getAdminId(principal);
        OperatorResponse response = adminService.createOperatorByAdmin(cmd, adminId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @PutMapping("/partners/operators/{operatorId}")
    public ResponseEntity<ApiResponse<OperatorResponse>> updateOperator(
            Principal principal,
            @PathVariable Long operatorId,
            @RequestBody RegisterOperatorCommand cmd
    ) {
        Long adminId = getAdminId(principal);
        OperatorResponse response = adminService.updateOperatorByAdmin(operatorId, cmd, adminId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/partners/operators/{operatorId}")
    public ResponseEntity<ApiResponse<Void>> deleteOperator(
            Principal principal,
            @PathVariable Long operatorId
    ) {
        Long adminId = getAdminId(principal);
        adminService.deleteOperatorByAdmin(operatorId, adminId);
        return ResponseEntity.ok(ApiResponse.ok("Operator deleted successfully", null));
    }

    @PutMapping("/partners/operators/{operatorId}/approve")
    public ResponseEntity<ApiResponse<OperatorResponse>> approveOperator(
            Principal principal,
            @PathVariable Long operatorId
    ) {
        Long adminId = getAdminId(principal);
        OperatorResponse response = adminService.approveOperator(operatorId, adminId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/partners/operators/{operatorId}/reject")
    public ResponseEntity<ApiResponse<OperatorResponse>> rejectOperator(
            Principal principal,
            @PathVariable Long operatorId,
            @RequestBody ProcessWithdrawalCommand cmd
    ) {
        Long adminId = getAdminId(principal);
        OperatorResponse response = adminService.rejectOperator(operatorId, cmd.getNote(), adminId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/partners/operators/{operatorId}/suspend")
    public ResponseEntity<ApiResponse<OperatorResponse>> suspendOperator(
            Principal principal,
            @PathVariable Long operatorId,
            @RequestBody(required = false) ProcessWithdrawalCommand cmd
    ) {
        Long adminId = getAdminId(principal);
        String note = cmd != null ? cmd.getNote() : null;
        OperatorResponse response = adminService.suspendOperator(operatorId, note, adminId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // --- 2. 2-Tier Commission & Global Settings ---
    @PutMapping("/partners/operators/{operatorId}/commission")
    public ResponseEntity<ApiResponse<OperatorResponse>> updateCustomCommission(
            @PathVariable Long operatorId,
            @RequestBody UpdateCommissionCommand cmd
    ) {
        OperatorResponse response = adminService.updateCommissionRate(operatorId, cmd.getCommissionRate());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/partners/commission/global")
    public ResponseEntity<ApiResponse<String>> updateGlobalCommission(
            @RequestBody UpdateCommissionCommand cmd
    ) {
        adminService.updateGlobalCommissionRate(cmd.getCommissionRate());
        return ResponseEntity.ok(ApiResponse.ok("Global commission rate updated to " + cmd.getCommissionRate() + "%"));
    }

    @GetMapping("/partners/commission/global")
    public ResponseEntity<ApiResponse<UpdateCommissionCommand>> getGlobalCommission() {
        var rate = queryService.getGlobalCommissionRate();
        return ResponseEntity.ok(ApiResponse.ok(new UpdateCommissionCommand(rate)));
    }

    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<Map<String, String>>> getSystemSettings() {
        return ResponseEntity.ok(ApiResponse.ok(queryService.getAllSystemSettings()));
    }

    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<Map<String, String>>> updateSystemSettings(
            @RequestBody Map<String, String> settings
    ) {
        adminService.updateSystemSettings(settings);
        return ResponseEntity.ok(ApiResponse.ok("System settings updated successfully", queryService.getAllSystemSettings()));
    }

    // --- 3. Withdrawal Processing ---
    @GetMapping("/partners/withdrawals")
    public ResponseEntity<ApiResponse<List<WithdrawalResponse>>> getWithdrawals(
            @RequestParam(required = false) WithdrawalStatus status
    ) {
        List<WithdrawalResponse> list = queryService.getWithdrawalsByStatus(status != null ? status : WithdrawalStatus.PENDING);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PutMapping("/partners/withdrawals/{withdrawalId}/approve")
    public ResponseEntity<ApiResponse<WithdrawalResponse>> approveWithdrawal(
            Principal principal,
            @PathVariable Long withdrawalId,
            @RequestBody(required = false) ProcessWithdrawalCommand cmd
    ) {
        Long adminId = getAdminId(principal);
        String note = (cmd != null) ? cmd.getNote() : null;
        WithdrawalResponse response = adminService.approveWithdrawal(withdrawalId, adminId, note);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/partners/withdrawals/{withdrawalId}/reject")
    public ResponseEntity<ApiResponse<WithdrawalResponse>> rejectWithdrawal(
            Principal principal,
            @PathVariable Long withdrawalId,
            @RequestBody ProcessWithdrawalCommand cmd
    ) {
        Long adminId = getAdminId(principal);
        WithdrawalResponse response = adminService.rejectWithdrawal(withdrawalId, adminId, cmd.getNote());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // --- 4. Banners ---
    @GetMapping("/partners/banners")
    public ResponseEntity<ApiResponse<List<BannerResponse>>> getBanners(
            @RequestParam(required = false) BannerStatus status
    ) {
        List<BannerResponse> list = queryService.getAllBanners(status);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PutMapping("/partners/banners/{bannerId}/approve")
    public ResponseEntity<ApiResponse<BannerResponse>> approveBanner(
            Principal principal,
            @PathVariable Long bannerId
    ) {
        Long adminId = getAdminId(principal);
        BannerResponse response = adminService.approveBanner(bannerId, adminId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/partners/banners/{bannerId}/reject")
    public ResponseEntity<ApiResponse<BannerResponse>> rejectBanner(
            Principal principal,
            @PathVariable Long bannerId,
            @RequestBody(required = false) ProcessWithdrawalCommand cmd
    ) {
        Long adminId = getAdminId(principal);
        String note = cmd != null ? cmd.getNote() : null;
        BannerResponse response = adminService.rejectBanner(bannerId, note, adminId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/partners/banners/platform")
    public ResponseEntity<ApiResponse<BannerResponse>> createPlatformBanner(
            Principal principal,
            @RequestBody BannerCommand cmd
    ) {
        Long adminId = getAdminId(principal);
        BannerResponse response = adminService.createPlatformBanner(cmd, adminId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @PutMapping({"/partners/banners/{bannerId}", "/partners/banners/platform/{bannerId}"})
    public ResponseEntity<ApiResponse<BannerResponse>> updateBanner(
            Principal principal,
            @PathVariable Long bannerId,
            @RequestBody BannerCommand cmd
    ) {
        Long adminId = getAdminId(principal);
        BannerResponse response = adminService.updateBannerByAdmin(bannerId, cmd, adminId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping({"/partners/banners/{bannerId}", "/partners/banners/platform/{bannerId}"})
    public ResponseEntity<ApiResponse<Void>> deleteBanner(
            Principal principal,
            @PathVariable Long bannerId
    ) {
        Long adminId = getAdminId(principal);
        adminService.deleteBannerByAdmin(bannerId, adminId);
        return ResponseEntity.ok(ApiResponse.ok("Banner deleted successfully", null));
    }
}
