package com.ticket.partner.presentation;

import com.ticket.partner.application.dto.BannerResponse;
import com.ticket.partner.application.usecase.PartnerQueryService;
import com.ticket.partner.domain.model.BannerPosition;
import com.ticket.partner.domain.model.BannerStatus;
import com.ticket.partner.presentation.controller.PublicBannerController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Public Banner Controller WebMvc Tests")
class PublicBannerControllerTest {

    @Mock
    private PartnerQueryService queryService;

    @InjectMocks
    private PublicBannerController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /api/v1/public/banners/active - Public accessible banners")
    void testGetActiveBanners() throws Exception {
        BannerResponse b = BannerResponse.builder()
                .id(1L)
                .title("Chao Xuan 2026")
                .imageUrl("https://cdn.vexere.com/banners/xuan.png")
                .position(BannerPosition.HOMEPAGE_HERO)
                .status(BannerStatus.ACTIVE)
                .build();
        when(queryService.getActiveBanners(any(), eq(10))).thenReturn(List.of(b));

        mockMvc.perform(get("/api/v1/public/banners/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("Chao Xuan 2026"));
    }
}
