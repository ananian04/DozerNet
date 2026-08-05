package com.dozernet.module2_booking.service;

import com.dozernet.common.exception.BusinessRuleException;
import com.dozernet.common.exception.ResourceNotFoundException;
import com.dozernet.common.model.Role;
import com.dozernet.common.notification.NotificationService;
import com.dozernet.common.user.User;
import com.dozernet.common.user.UserRepository;
import com.dozernet.module2_booking.SriLankaDistricts;
import com.dozernet.module2_booking.entity.Booking;
import com.dozernet.module2_booking.entity.BookingStatus;
import com.dozernet.module2_booking.repository.BookingRepository;
import com.dozernet.module3_fleet.entity.Machine;
import com.dozernet.module3_fleet.service.FleetService;
import com.dozernet.module6_payment.service.PaymentService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Booking &amp; Rental Management business logic. Enforces the core rules that keep
 * the calendar conflict-free:
 *  - a machine must be available &amp; verified to be booked;
 *  - bookings cannot start in the past and must end on/after the start date;
 *  - date ranges cannot overlap an existing pending/approved booking.
 * Approval also issues an unpaid invoice (pay before operator assignment).
 */
@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final FleetService fleetService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final PaymentService paymentService;

    public BookingService(BookingRepository bookingRepository,
                          FleetService fleetService,
                          NotificationService notificationService,
                          UserRepository userRepository,
                          @Lazy PaymentService paymentService) {
        this.bookingRepository = bookingRepository;
        this.fleetService = fleetService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.paymentService = paymentService;
    }

    // ---------- Create ----------

    @Transactional
    public Booking create(User customer, Long machineId, LocalDate startDate, LocalDate endDate,
                          String jobSiteDistrict, String jobSiteAddress) {
        Machine machine = fleetService.getById(machineId);
        validateDates(startDate, endDate);
        validateJobSite(jobSiteDistrict, jobSiteAddress);

        if (!machine.isBookable()) {
            throw new BusinessRuleException("This machine is not currently available for booking.");
        }

        List<Booking> clashes = bookingRepository.findOverlapping(machine, startDate, endDate);
        if (!clashes.isEmpty()) {
            throw new BusinessRuleException(
                    "Those dates are already booked for this machine. Please choose a different range.");
        }

        Booking booking = new Booking(customer, machine, startDate, endDate);
        booking.setStatus(BookingStatus.PENDING);
        booking.setJobSiteDistrict(jobSiteDistrict.trim());
        booking.setJobSiteAddress(jobSiteAddress.trim());
        booking.setTotalAmount(machine.getDailyRate().multiply(BigDecimal.valueOf(booking.getDays())));
        Booking saved = bookingRepository.save(booking);

        notificationService.notify(customer, "Booking submitted",
                "Your booking request for " + machine.getModel() + " is pending admin approval.");
        notifyAdmins("New booking request",
                customer.getFullName() + " requested " + machine.getModel()
                        + " at " + saved.getJobSiteLabel()
                        + " (" + startDate + " to " + endDate + ").");
        return saved;
    }

    public void validateJobSite(String district, String address) {
        if (district == null || district.isBlank()) {
            throw new BusinessRuleException("Select the district where the machine will work.");
        }
        if (!SriLankaDistricts.isValid(district.trim())) {
            throw new BusinessRuleException("Select a valid Sri Lanka district.");
        }
        if (address == null || address.isBlank()) {
            throw new BusinessRuleException("Enter the job site address or landmark.");
        }
        if (address.trim().length() > 255) {
            throw new BusinessRuleException("Job site address must be at most 255 characters.");
        }
    }

    /** Shared date-rule validation (also reused by tests). */
    public void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BusinessRuleException("Start and end dates are required.");
        }
        if (startDate.isBefore(LocalDate.now())) {
            throw new BusinessRuleException("Start date cannot be in the past.");
        }
        if (endDate.isBefore(startDate)) {
            throw new BusinessRuleException("End date must be on or after the start date.");
        }
    }

    // ---------- State transitions ----------

    @Transactional
    public void cancel(User customer, Long bookingId) {
        Booking b = getById(bookingId);
        if (!b.getCustomer().getId().equals(customer.getId())) {
            throw new BusinessRuleException("You can only cancel your own bookings.");
        }
        cancelInternal(b, "Your booking for " + b.getMachine().getModel() + " was cancelled.");
    }

    /**
     * Admin cancels an approved booking that has not been paid yet, freeing the
     * calendar so the machine can be booked again.
     */
    @Transactional
    public void cancelUnpaidApproved(Long bookingId) {
        Booking b = getById(bookingId);
        if (b.getStatus() != BookingStatus.APPROVED) {
            throw new BusinessRuleException("Only approved bookings can be cancelled this way.");
        }
        if (paymentService.isInvoicePaidForBooking(b)) {
            throw new BusinessRuleException("Paid bookings cannot be cancelled. Contact support if needed.");
        }
        cancelInternal(b, "Your approved booking for " + b.getMachine().getModel()
                + " was cancelled by the administrator. The invoice is no longer due.");
    }

    private void cancelInternal(Booking b, String customerMessage) {
        if (b.getStatus() == BookingStatus.PENDING) {
            b.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(b);
            return;
        }
        if (b.getStatus() == BookingStatus.APPROVED) {
            if (paymentService.isInvoicePaidForBooking(b)) {
                throw new BusinessRuleException("Paid bookings cannot be cancelled.");
            }
            paymentService.voidUnpaidInvoiceForBooking(b);
            b.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(b);
            notificationService.notify(b.getCustomer(), "Booking cancelled", customerMessage);
            return;
        }
        throw new BusinessRuleException("Only pending or unpaid approved bookings can be cancelled.");
    }

    @Transactional
    public void approve(Long bookingId) {
        Booking b = getById(bookingId);
        if (b.getStatus() != BookingStatus.PENDING) {
            throw new BusinessRuleException("Only pending bookings can be approved.");
        }
        // Defensive re-check: ensure no approved booking overlaps this one.
        boolean approvedClash = bookingRepository.findOverlapping(b.getMachine(), b.getStartDate(), b.getEndDate())
                .stream()
                .anyMatch(o -> !o.getId().equals(b.getId()) && o.getStatus() == BookingStatus.APPROVED);
        if (approvedClash) {
            throw new BusinessRuleException("Another approved booking overlaps these dates.");
        }
        b.setStatus(BookingStatus.APPROVED);
        bookingRepository.save(b);
        // Issues UNPAID invoice and notifies customer (invoice emailed to their address).
        paymentService.createInvoiceForApprovedBooking(b);
    }

    @Transactional
    public void reject(Long bookingId) {
        Booking b = getById(bookingId);
        if (b.getStatus() != BookingStatus.PENDING) {
            throw new BusinessRuleException("Only pending bookings can be rejected.");
        }
        b.setStatus(BookingStatus.REJECTED);
        bookingRepository.save(b);
        notificationService.notify(b.getCustomer(), "Booking rejected",
                "Your booking for " + b.getMachine().getModel() + " was not approved.");
    }

    /** Called by the Operator module when the assigned job is completed. */
    @Transactional
    public void markCompleted(Long bookingId) {
        Booking b = getById(bookingId);
        if (b.getStatus() != BookingStatus.APPROVED) {
            throw new BusinessRuleException("Only approved bookings can be completed.");
        }
        b.setStatus(BookingStatus.COMPLETED);
        bookingRepository.save(b);
        notificationService.notify(b.getCustomer(), "Job completed",
                "Your rental of " + b.getMachine().getModel() + " is complete. Thank you for using DozerNet.");
    }

    // ---------- Reads ----------

    public Booking getById(Long id) {
        return bookingRepository.findByIdWithDetails(id)
                .or(() -> bookingRepository.findById(id))
                .orElseThrow(() -> ResourceNotFoundException.of("Booking", id));
    }

    public List<Booking> forCustomer(User customer) {
        return bookingRepository.findByCustomerOrderByStartDateDesc(customer);
    }

    public List<Booking> all() {
        return bookingRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Booking> byStatus(BookingStatus status) {
        return bookingRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public List<Booking> approvedBookings() {
        return bookingRepository.findByStatusOrderByCreatedAtDesc(BookingStatus.APPROVED);
    }

    public long countByStatus(BookingStatus status) {
        return bookingRepository.countByStatus(status);
    }

    /** Approved rentals whose window includes today — for admin fleet whereabouts. */
    public List<Booking> activeDeploymentsToday() {
        return bookingRepository.findActiveDeploymentsOn(LocalDate.now());
    }

    private void notifyAdmins(String title, String message) {
        for (User admin : userRepository.findByRole(Role.ADMIN)) {
            notificationService.notify(admin, title, message);
        }
    }
}
