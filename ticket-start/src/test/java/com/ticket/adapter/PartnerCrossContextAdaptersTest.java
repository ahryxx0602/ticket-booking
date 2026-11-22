package com.ticket.adapter;

import com.ticket.booking.infrastructure.persistence.repository.SpringDataBookingRepository;
import com.ticket.catalog.infrastructure.persistence.entity.TripJpaEntity;
import com.ticket.catalog.infrastructure.persistence.repository.SpringDataTripRepository;
import com.ticket.identity.domain.model.User;
import com.ticket.identity.domain.model.UserRole;
import com.ticket.identity.domain.repository.UserRepository;
import com.ticket.partner.domain.port.UnsettledTripDto;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Partner Cross-Context Adapters Unit Tests")
class PartnerCrossContextAdaptersTest {

    @Mock
    private SpringDataTripRepository tripRepository;
    @Mock
    private SpringDataBookingRepository bookingRepository;
    @Mock
    private UserRepository userRepository;

    private PartnerCatalogPortAdapter catalogAdapter;
    private PartnerIdentityPortAdapter identityAdapter;

    @BeforeEach
    void setUp() {
        catalogAdapter = new PartnerCatalogPortAdapter(tripRepository, bookingRepository);
        identityAdapter = new PartnerIdentityPortAdapter(userRepository);
    }

    @Test
    @DisplayName("CatalogAdapter - Check overlap and find operator ID")
    void testCatalogAdapterBasic() {
        when(tripRepository.checkVehicleScheduleOverlap(any(), any(), any(), any())).thenReturn(true);
        boolean overlap = catalogAdapter.checkVehicleScheduleOverlap(1L, LocalDateTime.now(), LocalDateTime.now().plusHours(5), null);
        assertTrue(overlap);

        TripJpaEntity trip = new TripJpaEntity();
        trip.setId(10L);
        trip.setOperatorId(55L);
        trip.setSettlementStatus("UNSETTLED");
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));

        Optional<Long> opId = catalogAdapter.findOperatorIdByTripId(10L);
        assertTrue(opId.isPresent());
        assertEquals(55L, opId.get());
        assertFalse(catalogAdapter.isTripSettled(10L));
    }

    @Test
    @DisplayName("CatalogAdapter - Find unsettled trips and mark settled")
    void testCatalogAdapterSettlementFlow() {
        TripJpaEntity trip = new TripJpaEntity();
        trip.setId(101L);
        trip.setOperatorId(2L);
        trip.setDepartureTime(LocalDateTime.now().minusHours(3));
        trip.setSettlementStatus("UNSETTLED");

        when(tripRepository.findUnsettledTripsBefore(any())).thenReturn(List.of(trip));
        when(bookingRepository.sumConfirmedAmountByTripId(101L)).thenReturn(BigDecimal.valueOf(1500000));
        when(tripRepository.findById(101L)).thenReturn(Optional.of(trip));

        List<UnsettledTripDto> list = catalogAdapter.findUnsettledTripsBefore(LocalDateTime.now());
        assertEquals(1, list.size());
        assertEquals(101L, list.get(0).tripId());

        BigDecimal total = catalogAdapter.getTotalTicketSalesByTripId(101L);
        assertEquals(BigDecimal.valueOf(1500000), total);

        catalogAdapter.markTripSettled(101L);
        assertEquals("SETTLED", trip.getSettlementStatus());
        verify(tripRepository, times(1)).save(trip);
    }

    @Test
    @DisplayName("IdentityAdapter - Find user ID and grant ROLE_OPERATOR")
    void testIdentityAdapterGrantRole() {
        User user = User.builder()
                .id(20L)
                .username("partner_boss")
                .role(UserRole.ROLE_CUSTOMER)
                .build();
        when(userRepository.findByUsername("partner_boss")).thenReturn(Optional.of(user));
        when(userRepository.findById(20L)).thenReturn(Optional.of(user));

        Optional<Long> uid = identityAdapter.findUserIdByUsername("partner_boss");
        assertTrue(uid.isPresent());
        assertEquals(20L, uid.get());

        identityAdapter.grantOperatorRole(20L);
        assertEquals(UserRole.ROLE_OPERATOR, user.getRole());
        verify(userRepository, times(1)).save(user);
    }
}
