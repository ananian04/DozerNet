package com.dozernet.module6_payment.service;

import com.dozernet.common.exception.BusinessRuleException;
import com.dozernet.common.exception.ResourceNotFoundException;
import com.dozernet.common.model.Role;
import com.dozernet.common.notification.NotificationService;
import com.dozernet.common.user.User;
import com.dozernet.common.user.UserRepository;
import com.dozernet.module2_booking.entity.Booking;
import com.dozernet.module2_booking.entity.BookingStatus;
import com.dozernet.module2_booking.service.BookingService;
import com.dozernet.module6_payment.entity.Invoice;
import com.dozernet.module6_payment.entity.InvoiceStatus;
import com.dozernet.module6_payment.entity.Payment;
import com.dozernet.module6_payment.entity.PaymentMethod;
import com.dozernet.module6_payment.pricing.PricingSelector;
import com.dozernet.module6_payment.repository.InvoiceRepository;
import com.dozernet.module6_payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Payment &amp; billing: invoices are issued when a booking is approved (pay before
 * operator assignment). Pricing uses the Strategy pattern; customers can pay via
 * a demo card portal or an admin can record cash/bank offline.
 */
@Service
public class PaymentService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final BookingService bookingService;
    private final PricingSelector pricingSelector;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public PaymentService(InvoiceRepository invoiceRepository,
                          PaymentRepository paymentRepository,
                          BookingService bookingService,
                          PricingSelector pricingSelector,
                          NotificationService notificationService,
                          UserRepository userRepository) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.bookingService = bookingService;
        this.pricingSelector = pricingSelector;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    /**
     * Called right after a booking is approved. Creates an UNPAID invoice, syncs
     * booking.totalAmount, and notifies the customer (in-app + email Observer)
     * that the invoice was sent to their registered email.
     */
    @Transactional
    public Invoice createInvoiceForApprovedBooking(Booking booking) {
        if (booking.getStatus() != BookingStatus.APPROVED) {
            throw new BusinessRuleException("Invoices are issued for approved bookings.");
        }
        if (invoiceRepository.existsByBooking(booking)) {
            return invoiceRepository.findByBooking(booking)
                    .orElseThrow(() -> new BusinessRuleException("An invoice already exists for this booking."));
        }

        BigDecimal amount = pricingSelector.price(booking);
        booking.setTotalAmount(amount);
        Invoice invoice = invoiceRepository.save(new Invoice(booking, booking.getCustomer(), amount));

        User customer = booking.getCustomer();
        String email = customer.getEmail();
        String site = booking.getJobSiteLabel();
        String message = "Your booking for " + booking.getMachine().getModel()
                + " has been approved. Amount due: Rs. " + amount + "."
                + (site == null || site.isBlank() ? "" : " Job site: " + site + ".")
                + " The invoice has been sent to " + email + "."
                + " Please pay online to confirm the rental (Invoices → Pay now).";
        notificationService.notify(customer, "Booking approved — payment required", message);
        return invoice;
    }

    @Transactional
    public Invoice generateInvoice(Long bookingId) {
        Booking booking = bookingService.getById(bookingId);
        if (booking.getStatus() != BookingStatus.APPROVED
                && booking.getStatus() != BookingStatus.COMPLETED) {
            throw new BusinessRuleException(
                    "An invoice can only be generated for an approved or completed booking.");
        }
        if (invoiceRepository.existsByBooking(booking)) {
            throw new BusinessRuleException("An invoice already exists for this booking.");
        }
        BigDecimal amount = pricingSelector.price(booking);
        booking.setTotalAmount(amount);
        Invoice invoice = invoiceRepository.save(new Invoice(booking, booking.getCustomer(), amount));

        User customer = booking.getCustomer();
        notificationService.notify(customer, "Invoice issued",
                "An invoice of Rs. " + amount + " for " + booking.getMachine().getModel()
                        + " has been sent to " + customer.getEmail() + ".");
        return invoice;
    }

    /** Recovery: issue invoices for approved/completed bookings that still lack one. */
    @Transactional
    public int generateForAllCompleted() {
        int created = 0;
        for (Booking b : bookingsWithoutInvoice()) {
            generateInvoice(b.getId());
            created++;
        }
        return created;
    }

    @Transactional
    public Payment recordPayment(Long invoiceId, BigDecimal amount, PaymentMethod method, String reference) {
        return recordPayment(invoiceId, amount, method, reference, true);
    }

    @Transactional
    public Payment recordPayment(Long invoiceId, BigDecimal amount, PaymentMethod method, String reference,
                                 boolean notifyCustomer) {
        Invoice invoice = getInvoice(invoiceId);
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BusinessRuleException("This invoice is already fully paid.");
        }
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BusinessRuleException("This invoice was cancelled and cannot accept payments.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleException("Payment amount must be greater than zero.");
        }
        if (amount.compareTo(invoice.getBalance()) > 0) {
            throw new BusinessRuleException("Payment exceeds the outstanding balance of Rs. " + invoice.getBalance() + ".");
        }

        Payment payment = paymentRepository.save(new Payment(invoice, amount, method, reference));
        invoice.setAmountPaid(invoice.getAmountPaid().add(amount));
        boolean nowPaid = invoice.getBalance().signum() <= 0;
        invoice.setStatus(nowPaid ? InvoiceStatus.PAID : InvoiceStatus.PARTIALLY_PAID);
        invoiceRepository.save(invoice);

        if (notifyCustomer) {
            notificationService.notify(invoice.getCustomer(), "Payment recorded",
                    "A payment of Rs. " + amount + " was recorded. Outstanding balance: Rs. " + invoice.getBalance() + ".");
        }

        if (nowPaid) {
            notifyAdminsToAssignOperator(invoice);
        }
        return payment;
    }

    private void notifyAdminsToAssignOperator(Invoice invoice) {
        Booking booking = invoice.getBooking();
        String title = "Payment received — assign an operator";
        String message = invoice.getCustomer().getFullName() + " paid for "
                + booking.getMachine().getModel()
                + " (" + booking.getStartDate() + " to " + booking.getEndDate() + "). "
                + "Please assign an operator now.";
        for (User admin : userRepository.findByRole(Role.ADMIN)) {
            notificationService.notify(admin, title, message);
        }
    }

    /**
     * Demo customer portal: pay the full outstanding balance with a fake card.
     * {@code cardLast4} is stored in the payment reference.
     */
    @Transactional
    public Payment recordCustomerPayment(Long invoiceId, User customer, String cardholderName, String cardLast4) {
        Invoice invoice = getInvoice(invoiceId);
        if (!invoice.getCustomer().getId().equals(customer.getId())) {
            throw ResourceNotFoundException.of("Invoice", invoiceId);
        }
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BusinessRuleException("This invoice is already fully paid.");
        }
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BusinessRuleException("This invoice was cancelled and cannot accept payments.");
        }
        if (cardholderName == null || cardholderName.isBlank()) {
            throw new BusinessRuleException("Enter the cardholder name.");
        }
        if (cardLast4 == null || !cardLast4.matches("\\d{4}")) {
            throw new BusinessRuleException("Enter the last 4 digits of the card.");
        }

        BigDecimal balance = invoice.getBalance();
        String reference = "CARD-****" + cardLast4 + " / " + cardholderName.trim();
        Payment payment = recordPayment(invoiceId, balance, PaymentMethod.CARD, reference, false);
        notificationService.notify(customer, "Payment successful",
                "Thank you. Your payment of Rs. " + balance + " was received. "
                        + "A confirmation was sent to " + customer.getEmail() + ".");
        return payment;
    }

    /** Cancels an unpaid invoice when an approved booking is revoked. */
    @Transactional
    public void voidUnpaidInvoiceForBooking(Booking booking) {
        invoiceRepository.findByBooking(booking).ifPresent(inv -> {
            if (inv.getStatus() == InvoiceStatus.PAID || inv.getStatus() == InvoiceStatus.PARTIALLY_PAID) {
                throw new BusinessRuleException("Cannot cancel a booking that has already been paid.");
            }
            if (inv.getStatus() != InvoiceStatus.CANCELLED) {
                inv.setStatus(InvoiceStatus.CANCELLED);
                invoiceRepository.save(inv);
            }
        });
    }

    // ---------- Reads ----------

    public Invoice getInvoice(Long id) {
        return invoiceRepository.findByIdWithDetails(id)
                .or(() -> invoiceRepository.findById(id))
                .orElseThrow(() -> ResourceNotFoundException.of("Invoice", id));
    }

    public Optional<Invoice> findByBooking(Booking booking) {
        return invoiceRepository.findByBooking(booking);
    }

    public boolean isInvoicePaidForBooking(Booking booking) {
        return invoiceRepository.findByBooking(booking)
                .map(i -> i.getStatus() == InvoiceStatus.PAID)
                .orElse(false);
    }

    public List<Invoice> allInvoices() {
        return invoiceRepository.findAllByOrderByIssuedDateDesc();
    }

    public List<Invoice> invoicesForCustomer(User customer) {
        return invoiceRepository.findByCustomerOrderByIssuedDateDesc(customer);
    }

    public long countUnpaidForCustomer(User customer) {
        return invoicesForCustomer(customer).stream()
                .filter(i -> i.getStatus() == InvoiceStatus.UNPAID
                        || i.getStatus() == InvoiceStatus.PARTIALLY_PAID)
                .count();
    }

    /** Map bookingId → unpaid invoice id for Pay now buttons. */
    public Map<Long, Long> unpaidInvoiceIdsByBooking(User customer) {
        Map<Long, Long> map = new LinkedHashMap<>();
        for (Invoice inv : invoicesForCustomer(customer)) {
            if (inv.getStatus() == InvoiceStatus.UNPAID || inv.getStatus() == InvoiceStatus.PARTIALLY_PAID) {
                map.putIfAbsent(inv.getBooking().getId(), inv.getId());
            }
        }
        return map;
    }

    /** Payment status labels for admin assignment screen (booking id → Paid/Unpaid/No invoice). */
    public Map<Long, String> paymentLabelsForBookings(List<Booking> bookings) {
        Map<Long, String> map = new LinkedHashMap<>();
        for (Booking b : bookings) {
            map.put(b.getId(), invoiceRepository.findByBooking(b)
                    .map(i -> i.getStatus() == InvoiceStatus.PAID ? "Paid" : "Unpaid")
                    .orElse("No invoice"));
        }
        return map;
    }

    public List<Payment> paymentsFor(Invoice invoice) {
        return paymentRepository.findByInvoiceOrderByPaidDateDesc(invoice);
    }

    public List<Booking> bookingsWithoutInvoice() {
        List<Booking> pending = new ArrayList<>();
        pending.addAll(bookingService.byStatus(BookingStatus.APPROVED).stream()
                .filter(b -> !invoiceRepository.existsByBooking(b))
                .toList());
        pending.addAll(bookingService.byStatus(BookingStatus.COMPLETED).stream()
                .filter(b -> !invoiceRepository.existsByBooking(b))
                .toList());
        return pending;
    }

    /** @deprecated Prefer {@link #bookingsWithoutInvoice()}; kept for older callers. */
    public List<Booking> completedBookingsWithoutInvoice() {
        return bookingsWithoutInvoice();
    }
}
