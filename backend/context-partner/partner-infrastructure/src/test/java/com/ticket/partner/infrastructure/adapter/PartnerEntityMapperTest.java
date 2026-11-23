package com.ticket.partner.infrastructure.adapter;

import com.ticket.partner.domain.model.*;
import com.ticket.partner.infrastructure.persistence.adapter.PartnerEntityMapper;
import com.ticket.partner.infrastructure.persistence.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Partner Entity Mapper Persistence Unit Tests")
class PartnerEntityMapperTest {

    @Test
    @DisplayName("BusOperator - Entity to Domain and vice-versa")
    void testBusOperatorMapping() {
        BusOperatorJpaEntity entity = BusOperatorJpaEntity.builder()
                .id(1L)
                .userId(10L)
                .businessName("FUTA Express")
                .brandName("Phuong Trang")
                .taxCode("0301239999")
                .contactPhone("19006067")
                .contactEmail("hotro@futa.vn")
                .commissionRate(BigDecimal.valueOf(8.5))
                .status(OperatorStatus.ACTIVE)
                .version(1L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        BusOperator domain = PartnerEntityMapper.toDomain(entity);
        assertNotNull(domain);
        assertEquals(1L, domain.getId());
        assertEquals("FUTA Express", domain.getBusinessName());
        assertEquals(BigDecimal.valueOf(8.5), domain.getCommissionRate());

        BusOperatorJpaEntity mappedEntity = PartnerEntityMapper.toEntity(domain);
        assertNotNull(mappedEntity);
        assertEquals(1L, mappedEntity.getId());
        assertEquals("0301239999", mappedEntity.getTaxCode());
    }

    @Test
    @DisplayName("OperatorWallet - Entity to Domain and vice-versa")
    void testOperatorWalletMapping() {
        OperatorWalletJpaEntity entity = OperatorWalletJpaEntity.builder()
                .id(5L)
                .operatorId(1L)
                .availableBalance(BigDecimal.valueOf(500000))
                .pendingSettlementBalance(BigDecimal.valueOf(200000))
                .lockedBalance(BigDecimal.valueOf(100000))
                .debtBalance(BigDecimal.ZERO)
                .currency("VND")
                .version(2L)
                .build();

        OperatorWallet domain = PartnerEntityMapper.toDomain(entity);
        assertNotNull(domain);
        assertEquals(BigDecimal.valueOf(500000), domain.getAvailableBalance());
        assertEquals(BigDecimal.valueOf(100000), domain.getLockedBalance());

        OperatorWalletJpaEntity mappedEntity = PartnerEntityMapper.toEntity(domain);
        assertNotNull(mappedEntity);
        assertEquals(5L, mappedEntity.getId());
        assertEquals("VND", mappedEntity.getCurrency());
    }

    @Test
    @DisplayName("Banner - Entity to Domain and vice-versa")
    void testBannerMapping() {
        BannerJpaEntity entity = BannerJpaEntity.builder()
                .id(20L)
                .operatorId(1L)
                .title("Tet Sale")
                .imageUrl("https://img.com/tet.png")
                .position(BannerPosition.HOMEPAGE_HERO)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(5))
                .priority(10)
                .status(BannerStatus.ACTIVE)
                .build();

        Banner domain = PartnerEntityMapper.toDomain(entity);
        assertNotNull(domain);
        assertEquals(BannerPosition.HOMEPAGE_HERO, domain.getPosition());
        assertEquals(10, domain.getPriority());

        BannerJpaEntity mapped = PartnerEntityMapper.toEntity(domain);
        assertNotNull(mapped);
        assertEquals(20L, mapped.getId());
    }
}
