package com.ticket.partner.presentation.controller;

import com.ticket.partner.application.dto.BannerResponse;
import com.ticket.partner.application.usecase.PartnerQueryService;
import com.ticket.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/public/banners")
@RequiredArgsConstructor
public class PublicBannerController {

    private final PartnerQueryService queryService;

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<BannerResponse>>> getActiveBanners(
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<BannerResponse> activeBanners = queryService.getActiveBanners(LocalDateTime.now(), limit);
        return ResponseEntity.ok(ApiResponse.ok(activeBanners));
    }
}
