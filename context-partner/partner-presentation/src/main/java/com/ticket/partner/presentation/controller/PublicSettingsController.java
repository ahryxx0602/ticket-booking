package com.ticket.partner.presentation.controller;

import com.ticket.partner.application.usecase.PartnerQueryService;
import com.ticket.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/public/settings")
@RequiredArgsConstructor
public class PublicSettingsController {

    private final PartnerQueryService queryService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPublicSettings() {
        Map<String, Object> settings = Map.of(
                "defaultCommissionRate", queryService.getGlobalCommissionRate(),
                "platformName", "Ticket Booking Marketplace",
                "supportHotline", "1900-6067"
        );
        return ResponseEntity.ok(ApiResponse.ok(settings));
    }
}
