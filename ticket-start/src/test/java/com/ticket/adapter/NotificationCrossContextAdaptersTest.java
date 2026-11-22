package com.ticket.adapter;

import com.ticket.booking.domain.model.Booking;
import com.ticket.booking.domain.model.BookingItem;
import com.ticket.booking.domain.model.BookingStatus;
import com.ticket.booking.domain.repository.BookingRepository;
import com.ticket.catalog.domain.model.Station;
import com.ticket.catalog.domain.model.Trip;
import com.ticket.catalog.domain.repository.TripRepository;
import com.ticket.identity.domain.model.User;
import com.ticket.identity.domain.model.UserRole;
import com.ticket.identity.domain.repository.UserRepository;
import com.ticket.notification.domain.port.NotificationBookingPort;
import com.ticket.notification.domain.port.NotificationCatalogPort;
import com.ticket.notification.domain.port.NotificationIdentityPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationCrossContextAdaptersTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TripRepository tripRepository;

    @InjectMocks
    private NotificationBookingPortAdapter bookingPortAdapter;
    @InjectMocks
    private NotificationIdentityPortAdapter identityPortAdapter;
    @InjectMocks
    private NotificationCatalogPortAdapter catalogPortAdapter;

    @Test
    void notificationBookingPortAdapter_whenFound_shouldReturnSnapshot() {
        Booking booking = Booking.builder()
                .id(101L)
                .bookingReference("BKG-NOTI-01")
                .userId(10L)
                .tripId(20L)
                .totalAmount(BigDecimal.valueOf(350000))
                .phone("0912345678")
                .status(BookingStatus.CONFIRMED)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .items(List.of(
                        BookingItem.builder().seatNumber("A01").build(),
                        BookingItem.builder().seatNumber("A02").build()
                ))
                .build();

        when(bookingRepository.findByReference("BKG-NOTI-01")).thenReturn(Optional.of(booking));

        Optional<NotificationBookingPort.BookingDetailsSnapshot> snapshot =
                bookingPortAdapter.getBookingDetails("BKG-NOTI-01");

        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().bookingId()).isEqualTo(101L);
        assertThat(snapshot.get().bookingReference()).isEqualTo("BKG-NOTI-01");
        assertThat(snapshot.get().customerPhone()).isEqualTo("0912345678");
        assertThat(snapshot.get().seatNumbers()).containsExactly("A01", "A02");
    }

    @Test
    void notificationBookingPortAdapter_byReferenceAndPhone_whenMatched_shouldReturnSnapshot() {
        Booking booking = Booking.builder()
                .id(102L)
                .bookingReference("BKG-NOTI-02")
                .userId(11L)
                .phone("0987654321")
                .status(BookingStatus.CONFIRMED)
                .build();

        when(bookingRepository.findByReference("BKG-NOTI-02")).thenReturn(Optional.of(booking));

        Optional<NotificationBookingPort.BookingDetailsSnapshot> matched =
                bookingPortAdapter.getBookingDetailsByReferenceAndPhone("BKG-NOTI-02", "0987654321");
        assertThat(matched).isPresent();

        Optional<NotificationBookingPort.BookingDetailsSnapshot> mismatch =
                bookingPortAdapter.getBookingDetailsByReferenceAndPhone("BKG-NOTI-02", "0123456789");
        assertThat(mismatch).isEmpty();
    }

    @Test
    void notificationBookingPortAdapter_byReferenceAndPhone_whenBookingPhoneNull_shouldFallbackToUserPhone() {
        Booking booking = Booking.builder()
                .id(103L)
                .bookingReference("BKG-NOTI-03")
                .userId(15L)
                .phone(null)
                .status(BookingStatus.CONFIRMED)
                .build();
        User user = User.builder()
                .id(15L)
                .phone("0933333333")
                .build();

        when(bookingRepository.findByReference("BKG-NOTI-03")).thenReturn(Optional.of(booking));
        when(userRepository.findById(15L)).thenReturn(Optional.of(user));

        Optional<NotificationBookingPort.BookingDetailsSnapshot> matched =
                bookingPortAdapter.getBookingDetailsByReferenceAndPhone("BKG-NOTI-03", "0933333333");
        assertThat(matched).isPresent();
    }

    @Test
    void notificationIdentityPortAdapter_whenFound_shouldReturnSnapshot() {
        User user = User.builder()
                .id(5L)
                .username("testuser")
                .email("test@example.com")
                .fullName("Test User")
                .phone("0900000000")
                .role(UserRole.ROLE_CUSTOMER)
                .build();

        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        Optional<NotificationIdentityPort.UserProfileSnapshot> byId = identityPortAdapter.getUserProfile(5L);
        assertThat(byId).isPresent();
        assertThat(byId.get().email()).isEqualTo("test@example.com");
        assertThat(byId.get().phone()).isEqualTo("0900000000");

        Optional<NotificationIdentityPort.UserProfileSnapshot> byUsername = identityPortAdapter.getUserProfileByUsername("testuser");
        assertThat(byUsername).isPresent();
        assertThat(byUsername.get().fullName()).isEqualTo("Test User");
    }

    @Test
    void notificationCatalogPortAdapter_whenFound_shouldReturnSnapshot() {
        Station dep = Station.builder().id(1L).name("Bến Xe Miền Đông").city("TP.HCM").build();
        Station arr = Station.builder().id(2L).name("Bến Xe Đà Lạt").city("Lâm Đồng").build();

        Trip trip = Trip.builder()
                .id(55L)
                .tripCode("TRIP-55")
                .departureStation(dep)
                .arrivalStation(arr)
                .departureTime(LocalDateTime.now().plusDays(1))
                .arrivalTime(LocalDateTime.now().plusDays(1).plusHours(6))
                .build();

        when(tripRepository.findById(55L)).thenReturn(Optional.of(trip));

        Optional<NotificationCatalogPort.TripDetailsSnapshot> snapshot = catalogPortAdapter.getTripDetails(55L);
        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().tripCode()).isEqualTo("TRIP-55");
        assertThat(snapshot.get().departureStationName()).isEqualTo("Bến Xe Miền Đông");
        assertThat(snapshot.get().arrivalStationName()).isEqualTo("Bến Xe Đà Lạt");
    }
}
