package com.ticket.partner.presentation.controller;

import com.ticket.partner.application.dto.*;
import com.ticket.partner.application.usecase.PartnerCommandService;
import com.ticket.partner.application.usecase.PartnerQueryService;
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
@RequestMapping("/api/v1/partners")
@RequiredArgsConstructor
public class PartnerController {

    private final PartnerCommandService commandService;
    private final PartnerQueryService queryService;
    private final PartnerIdentityPort identityPort;

    private Long getUserId(Principal principal) {
        if (principal == null) {
            throw new ForbiddenException("UNAUTHORIZED", "User must be authenticated");
        }
        if (identityPort != null) {
            return identityPort.findUserIdByUsername(principal.getName())
                    .orElseGet(() -> {
                        try {
                            return Long.parseLong(principal.getName());
                        } catch (NumberFormatException e) {
                            throw new ForbiddenException("USER_NOT_FOUND", "User not found: " + principal.getName());
                        }
                    });
        }
        try {
            return Long.parseLong(principal.getName());
        } catch (NumberFormatException e) {
            throw new ForbiddenException("USER_NOT_FOUND", "Cannot resolve user ID: " + principal.getName());
        }
    }

    // --- 1. Operator Profile ---
    @PostMapping("/register")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'OPERATOR')")
    public ResponseEntity<ApiResponse<OperatorResponse>> register(
            Principal principal,
            @RequestBody RegisterOperatorCommand cmd
    ) {
        Long userId = getUserId(principal);
        OperatorResponse response = commandService.registerOperator(userId, cmd);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<OperatorResponse>> getMyProfile(Principal principal) {
        Long userId = getUserId(principal);
        OperatorResponse response = queryService.getMyOperatorProfile(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<ApiResponse<OperatorResponse>> updateMyProfile(
            Principal principal,
            @RequestBody UpdateOperatorProfileCommand cmd
    ) {
        Long userId = getUserId(principal);
        OperatorResponse response = commandService.updateOperatorProfile(userId, cmd);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // --- 2. Vehicle Fleet Management ---
    @GetMapping("/vehicles")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<ApiResponse<List<VehicleResponse>>> getMyVehicles(Principal principal) {
        Long userId = getUserId(principal);
        List<VehicleResponse> list = queryService.getMyVehicles(userId);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PostMapping("/vehicles")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<ApiResponse<VehicleResponse>> createVehicle(
            Principal principal,
            @RequestBody VehicleCommand cmd
    ) {
        Long userId = getUserId(principal);
        VehicleResponse response = commandService.createVehicle(userId, cmd);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @PutMapping("/vehicles/{vehicleId}")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<ApiResponse<VehicleResponse>> updateVehicle(
            Principal principal,
            @PathVariable Long vehicleId,
            @RequestBody VehicleCommand cmd
    ) {
        Long userId = getUserId(principal);
        VehicleResponse response = commandService.updateVehicle(userId, vehicleId, cmd);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/vehicles/{vehicleId}/maintenance")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<ApiResponse<String>> setVehicleMaintenance(
            Principal principal,
            @PathVariable Long vehicleId
    ) {
        Long userId = getUserId(principal);
        commandService.setVehicleMaintenance(userId, vehicleId);
        return ResponseEntity.ok(ApiResponse.ok("Vehicle status set to MAINTENANCE"));
    }

    @PutMapping("/vehicles/{vehicleId}/activate")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<ApiResponse<String>> activateVehicle(
            Principal principal,
            @PathVariable Long vehicleId
    ) {
        Long userId = getUserId(principal);
        commandService.activateVehicle(userId, vehicleId);
        return ResponseEntity.ok(ApiResponse.ok("Vehicle status set to ACTIVE"));
    }

    // --- 3. Drivers Management ---
    @GetMapping("/drivers")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<ApiResponse<List<DriverResponse>>> getMyDrivers(Principal principal) {
        Long userId = getUserId(principal);
        List<DriverResponse> list = queryService.getMyDrivers(userId);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PostMapping("/drivers")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<ApiResponse<DriverResponse>> createDriver(
            Principal principal,
            @RequestBody DriverCommand cmd
    ) {
        Long userId = getUserId(principal);
        DriverResponse response = commandService.createDriver(userId, cmd);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @PutMapping("/drivers/{driverId}")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<ApiResponse<DriverResponse>> updateDriver(
            Principal principal,
            @PathVariable Long driverId,
            @RequestBody DriverCommand cmd
    ) {
        Long userId = getUserId(principal);
        DriverResponse response = commandService.updateDriver(userId, driverId, cmd);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // --- 4. Wallet & Withdrawal ---
    @GetMapping("/wallet")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<ApiResponse<WalletResponse>> getMyWallet(Principal principal) {
        Long userId = getUserId(principal);
        WalletResponse response = queryService.getMyWallet(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/wallet/transactions")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<ApiResponse<List<WalletTransactionResponse>>> getMyTransactions(Principal principal) {
        Long userId = getUserId(principal);
        List<WalletTransactionResponse> list = queryService.getMyWalletTransactions(userId);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PostMapping("/wallet/withdraw")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<ApiResponse<WithdrawalResponse>> requestWithdrawal(
            Principal principal,
            @RequestBody WithdrawalCommand cmd
    ) {
        Long userId = getUserId(principal);
        WithdrawalResponse response = commandService.requestWithdrawal(userId, cmd);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping("/wallet/withdrawals")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<ApiResponse<List<WithdrawalResponse>>> getMyWithdrawals(Principal principal) {
        Long userId = getUserId(principal);
        List<WithdrawalResponse> list = queryService.getMyWithdrawalRequests(userId);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    // --- 5. Banners ---
    @GetMapping("/banners")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<ApiResponse<List<BannerResponse>>> getMyBanners(Principal principal) {
        Long userId = getUserId(principal);
        List<BannerResponse> list = queryService.getMyBanners(userId);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PostMapping("/banners")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<ApiResponse<BannerResponse>> registerBanner(
            Principal principal,
            @RequestBody BannerCommand cmd
    ) {
        Long userId = getUserId(principal);
        BannerResponse response = commandService.registerBanner(userId, cmd);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    // --- 6. Trip Scheduling ---
    @PostMapping("/trips")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<ApiResponse<PartnerTripResponse>> createTrip(
            Principal principal,
            @RequestBody CreatePartnerTripCommand cmd
    ) {
        Long userId = getUserId(principal);
        PartnerTripResponse response = commandService.createTrip(userId, cmd);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }
}
