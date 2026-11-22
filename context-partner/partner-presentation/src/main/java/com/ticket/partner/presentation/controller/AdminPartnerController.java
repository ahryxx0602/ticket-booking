package com.ticket.partner.presentation.controller;

import com.ticket.partner.application.dto.*;
import com.ticket.partner.application.usecase.PartnerAdminService;
import com.ticket.partner.application.usecase.PartnerQueryService;
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

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/partners")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPartnerController {

    private final PartnerAdminService adminService;
    private final PartnerQueryService queryService;
    private final PartnerIdentityPort identityPort;

    private Long getAdminId(Principal principal) {
        if (principal == null) {
            throw new ForbiddenException("UNAUTHORIZED", "Admin must be authenticated");
        }
        if (identityPort != null) {
            return identityPort.findUserIdByUsername(principal.getName())
                    .orElseGet(() -> {
                        try {
                            return Long.parseLong(principal.getName());
                        } catch (NumberFormatException e) {
                            return 1L; // fallback default admin ID
                        }
                    });
        }
        try {
            return Long.parseLong(principal.getName());
        } catch (NumberFormatException e) {
            return 1L;
        }
    }

    // --- 1. Operator Approval & Suspension ---
    @GetMapping("/operators")
    public ResponseEntity<ApiResponse<List<OperatorResponse>>> getAllOperators() {
        List<OperatorResponse> list = queryService.getAllOperators();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PutMapping("/operators/{operatorId}/approve")
    public ResponseEntity<ApiResponse<OperatorResponse>> approveOperator(
            Principal principal,
            @PathVariable Long operatorId
    ) {
        Long adminId = getAdminId(principal);
        OperatorResponse response = adminService.approveOperator(operatorId, adminId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/operators/{operatorId}/reject")
    public ResponseEntity<ApiResponse<OperatorResponse>> rejectOperator(
            Principal principal,
            @PathVariable Long operatorId,
            @RequestBody ProcessWithdrawalCommand cmd
    ) {
        Long adminId = getAdminId(principal);
        OperatorResponse response = adminService.rejectOperator(operatorId, cmd.getNote(), adminId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/operators/{operatorId}/suspend")
    public ResponseEntity<ApiResponse<OperatorResponse>> suspendOperator(
            Principal principal,
            @PathVariable Long operatorId,
            @RequestBody ProcessWithdrawalCommand cmd
    ) {
        Long adminId = getAdminId(principal);
        OperatorResponse response = adminService.suspendOperator(operatorId, cmd.getNote(), adminId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // --- 2. 2-Tier Commission Setting ---
    @PutMapping("/operators/{operatorId}/commission")
    public ResponseEntity<ApiResponse<OperatorResponse>> updateCustomCommission(
            @PathVariable Long operatorId,
            @RequestBody UpdateCommissionCommand cmd
    ) {
        OperatorResponse response = adminService.updateCommissionRate(operatorId, cmd.getCommissionRate());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/commission/global")
    public ResponseEntity<ApiResponse<String>> updateGlobalCommission(
            @RequestBody UpdateCommissionCommand cmd
    ) {
        adminService.updateGlobalCommissionRate(cmd.getCommissionRate());
        return ResponseEntity.ok(ApiResponse.ok("Global commission rate updated to " + cmd.getCommissionRate() + "%"));
    }

    @GetMapping("/commission/global")
    public ResponseEntity<ApiResponse<UpdateCommissionCommand>> getGlobalCommission() {
        var rate = queryService.getGlobalCommissionRate();
        return ResponseEntity.ok(ApiResponse.ok(new UpdateCommissionCommand(rate)));
    }

    // --- 3. Withdrawal Processing ---
    @GetMapping("/withdrawals")
    public ResponseEntity<ApiResponse<List<WithdrawalResponse>>> getWithdrawals(
            @RequestParam(required = false) WithdrawalStatus status
    ) {
        List<WithdrawalResponse> list = queryService.getWithdrawalsByStatus(status != null ? status : WithdrawalStatus.PENDING);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PutMapping("/withdrawals/{withdrawalId}/approve")
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

    @PutMapping("/withdrawals/{withdrawalId}/reject")
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
    @PutMapping("/banners/{bannerId}/approve")
    public ResponseEntity<ApiResponse<BannerResponse>> approveBanner(
            Principal principal,
            @PathVariable Long bannerId
    ) {
        Long adminId = getAdminId(principal);
        BannerResponse response = adminService.approveBanner(bannerId, adminId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/banners/{bannerId}/reject")
    public ResponseEntity<ApiResponse<BannerResponse>> rejectBanner(
            Principal principal,
            @PathVariable Long bannerId,
            @RequestBody ProcessWithdrawalCommand cmd
    ) {
        Long adminId = getAdminId(principal);
        BannerResponse response = adminService.rejectBanner(bannerId, cmd.getNote(), adminId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/banners/platform")
    public ResponseEntity<ApiResponse<BannerResponse>> createPlatformBanner(
            Principal principal,
            @RequestBody BannerCommand cmd
    ) {
        Long adminId = getAdminId(principal);
        BannerResponse response = adminService.createPlatformBanner(cmd, adminId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }
}
