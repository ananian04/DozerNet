package com.dozernet.module2_booking;

import com.dozernet.common.exception.BusinessRuleException;
import com.dozernet.common.model.Role;
import com.dozernet.common.notification.NotificationService;
import com.dozernet.common.user.User;
import com.dozernet.common.user.UserRepository;
import com.dozernet.module2_booking.entity.Booking;
import com.dozernet.module2_booking.entity.BookingStatus;
import com.dozernet.module2_booking.repository.BookingRepository;
import com.dozernet.module2_booking.service.BookingService;
import com.dozernet.module3_fleet.entity.Machine;
import com.dozernet.module3_fleet.entity.MachineStatus;
import com.dozernet.module3_fleet.entity.MachineType;
import com.dozernet.module3_fleet.service.FleetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the trickiest business rules in the system: booking dates and
 * double-booking prevention.
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock BookingRepository bookingRepository;
    @Mock FleetService fleetService;
    @Mock NotificationService notificationService;
    @Mock UserRepository userRepository;
    @Mock com.dozernet.module6_payment.service.PaymentService paymentService;

    @InjectMocks BookingService bookingService;

    private User customer;
    private Machine machine;

    @BeforeEach
    void setUp() {
        customer = new User("Chamara", "c@x.lk", "0771111111", "hash", Role.CUSTOMER);
        customer.setId(1L);

        machine = new Machine();
        machine.setId(10L);
        machine.setModel("JCB 3CX");
        machine.setType(MachineType.BACKHOE_LOADER);
        machine.setDailyRate(new BigDecimal("10000.00"));
        machine.setStatus(MachineStatus.AVAILABLE);
        machine.setVerified(true);

        lenient().when(fleetService.getById(10L)).thenReturn(machine);
        lenient().when(userRepository.findByRole(Role.ADMIN)).thenReturn(List.of());
        lenient().when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void rejectsStartDateInThePast() {
        assertThatThrownBy(() -> bookingService.validateDates(
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(2)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("past");
    }

    @Test
    void rejectsEndBeforeStart() {
        assertThatThrownBy(() -> bookingService.validateDates(
                LocalDate.now().plusDays(3), LocalDate.now().plusDays(1)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("on or after");
    }

    @Test
    void acceptsValidSameDayRange() {
        LocalDate today = LocalDate.now();
        bookingService.validateDates(today, today); // no exception
    }

    @Test
    void rejectsBookingOfUnavailableMachine() {
        machine.setStatus(MachineStatus.MAINTENANCE);
        assertThatThrownBy(() -> bookingService.create(customer, 10L,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(2),
                "Colombo", "Near Kaduwela flyover"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not currently available");
    }

    @Test
    void rejectsOverlappingBooking() {
        when(bookingRepository.findOverlapping(any(), any(), any()))
                .thenReturn(List.of(new Booking()));
        assertThatThrownBy(() -> bookingService.create(customer, 10L,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(3),
                "Gampaha", "Site gate A"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already booked");
    }

    @Test
    void createsBookingWithCorrectTotalAndPendingStatus() {
        when(bookingRepository.findOverlapping(any(), any(), any())).thenReturn(List.of());
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = start.plusDays(2); // 3 inclusive days

        Booking b = bookingService.create(customer, 10L, start, end,
                "Colombo", "Near Kaduwela flyover");

        assertThat(b.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(b.getDays()).isEqualTo(3);
        assertThat(b.getTotalAmount()).isEqualByComparingTo("30000.00");
        assertThat(b.getJobSiteDistrict()).isEqualTo("Colombo");
        assertThat(b.getJobSiteAddress()).isEqualTo("Near Kaduwela flyover");
        assertThat(b.getJobSiteLabel()).isEqualTo("Colombo — Near Kaduwela flyover");
    }

    @Test
    void rejectsInvalidJobSiteDistrict() {
        when(bookingRepository.findOverlapping(any(), any(), any())).thenReturn(List.of());
        assertThatThrownBy(() -> bookingService.create(customer, 10L,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(2),
                "Atlantis", "Somewhere"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("district");
    }

    @Test
    void cannotApproveNonPendingBooking() {
        Booking b = new Booking(customer, machine, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));
        b.setId(5L);
        b.setStatus(BookingStatus.APPROVED);
        when(bookingRepository.findById(5L)).thenReturn(java.util.Optional.of(b));

        assertThatThrownBy(() -> bookingService.approve(5L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("pending");
    }

    @Test
    void customerCannotCancelSomeoneElsesBooking() {
        User other = new User("Other", "o@x.lk", "0779999999", "hash", Role.CUSTOMER);
        other.setId(2L);
        Booking b = new Booking(other, machine, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));
        b.setId(7L);
        b.setStatus(BookingStatus.PENDING);
        when(bookingRepository.findById(7L)).thenReturn(java.util.Optional.of(b));

        assertThatThrownBy(() -> bookingService.cancel(customer, 7L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("your own");
    }
}
