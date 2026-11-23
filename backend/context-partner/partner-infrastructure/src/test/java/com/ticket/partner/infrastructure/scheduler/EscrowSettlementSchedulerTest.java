package com.ticket.partner.infrastructure.scheduler;

import com.ticket.partner.application.usecase.PartnerQueryService;
import com.ticket.partner.domain.model.OperatorWallet;
import com.ticket.partner.domain.model.WalletTransaction;
import com.ticket.partner.domain.port.OperatorWalletRepository;
import com.ticket.partner.domain.port.PartnerCatalogPort;
import com.ticket.partner.domain.port.UnsettledTripDto;
import com.ticket.partner.domain.port.WalletTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Escrow Settlement Scheduler Unit Tests")
class EscrowSettlementSchedulerTest {

    @Mock
    private PartnerCatalogPort partnerCatalogPort;
    @Mock
    private PartnerQueryService partnerQueryService;
    @Mock
    private OperatorWalletRepository walletRepository;
    @Mock
    private WalletTransactionRepository transactionRepository;

    private EscrowSettlementScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new EscrowSettlementScheduler(
                partnerCatalogPort, partnerQueryService, walletRepository, transactionRepository
        );
    }

    @Test
    @DisplayName("ProcessEscrowSettlements - Successfully settles completed trips")
    void testProcessEscrowSettlementsSuccess() {
        LocalDateTime tripTime = LocalDateTime.now().minusHours(3);
        UnsettledTripDto trip1 = new UnsettledTripDto(101L, 1L, tripTime);

        when(partnerCatalogPort.findUnsettledTripsBefore(any(LocalDateTime.class)))
                .thenReturn(List.of(trip1));

        // Total sales: 2,000,000 VND
        when(partnerCatalogPort.getTotalTicketSalesByTripId(101L))
                .thenReturn(BigDecimal.valueOf(2000000));
        when(partnerCatalogPort.markTripSettledIfUnsettled(101L)).thenReturn(true);

        // Commission rate: 10%
        when(partnerQueryService.getEffectiveCommissionRate(1L))
                .thenReturn(BigDecimal.valueOf(10.0));

        OperatorWallet wallet = OperatorWallet.builder()
                .id(10L)
                .operatorId(1L)
                .pendingSettlementBalance(BigDecimal.valueOf(1800000))
                .availableBalance(BigDecimal.ZERO)
                .debtBalance(BigDecimal.ZERO)
                .build();
        when(walletRepository.findByOperatorId(1L)).thenReturn(Optional.of(wallet));

        scheduler.processEscrowSettlements();

        // 2,000,000 - 10% = 1,800,000 net credited to available balance
        assertEquals(BigDecimal.valueOf(1800000), wallet.getAvailableBalance());
        assertEquals(BigDecimal.ZERO, wallet.getPendingSettlementBalance());

        verify(walletRepository, times(1)).save(wallet);
        verify(transactionRepository, times(1)).save(any(WalletTransaction.class));
        verify(partnerCatalogPort, times(1)).markTripSettledIfUnsettled(101L);
    }

    @Test
    @DisplayName("ProcessEscrowSettlements - Zero sales trip marked settled without balance change")
    void testProcessEscrowSettlementsZeroSales() {
        LocalDateTime tripTime = LocalDateTime.now().minusHours(4);
        UnsettledTripDto trip = new UnsettledTripDto(102L, 1L, tripTime);

        when(partnerCatalogPort.findUnsettledTripsBefore(any(LocalDateTime.class)))
                .thenReturn(List.of(trip));
        when(partnerCatalogPort.getTotalTicketSalesByTripId(102L))
                .thenReturn(BigDecimal.ZERO);

        scheduler.processEscrowSettlements();

        verify(partnerCatalogPort, times(1)).markTripSettled(102L);
        verify(walletRepository, never()).save(any());
    }
}
