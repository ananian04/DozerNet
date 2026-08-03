package com.dozernet.module6_payment.service;

import com.dozernet.common.exception.BusinessRuleException;
import com.dozernet.common.exception.ResourceNotFoundException;
import com.dozernet.common.notification.NotificationService;
import com.dozernet.common.user.User;
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
import java.util.List;

/**
 * Payment & billing logic: invoice generation for completed bookings (using the
 * pricing Strategy) and recording payments against invoices.
 */
@Service
public class PaymentService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final BookingService bookingService;
    private final PricingSelector pricingSelector;
    private final NotificationService notificationService;

    public PaymentService(InvoiceRepository invoiceRepository,
                          PaymentRepository paymentRepository,
                          BookingService bookingService,
                          PricingSelector pricingSelector,
                          NotificationService notificationService) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.bookingService = bookingService;
        this.pricingSelector = pricingSelector;
        this.notificationService = notificationService;
    }

    @Transactional
    public Invoice generateInvoice(Long bookingId) {
        Booking booking = bookingService.getById(bookingId);
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new BusinessRuleException("An invoice can only be generated once the job is completed.");
        }
        if (invoiceRepository.existsByBooking(booking)) {
            throw new BusinessRuleException("An invoice already exists for this booking.");
        }
        BigDecimal amount = pricingSelector.price(booking);
        Invoice invoice = invoiceRepository.save(new Invoice(booking, booking.getCustomer(), amount));
        notificationService.notify(booking.getCustomer(), "Invoice issued",
                "An invoice of Rs. " + amount + " has been issued for " + booking.getMachine().getModel() + ".");
        return invoice;
    }

    /** Generate invoices for every completed booking that lacks one. */
    @Transactional
    public int generateForAllCompleted() {
        int created = 0;
        for (Booking b : completedBookingsWithoutInvoice()) {
            generateInvoice(b.getId());
            created++;
        }
        return created;
    }

    @Transactional
    public Payment recordPayment(Long invoiceId, BigDecimal amount, PaymentMethod method, String reference) {
        Invoice invoice = getInvoice(invoiceId);
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BusinessRuleException("This invoice is already fully paid.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleException("Payment amount must be greater than zero.");
        }
        if (amount.compareTo(invoice.getBalance()) > 0) {
            throw new BusinessRuleException("Payment exceeds the outstanding balance of Rs. " + invoice.getBalance() + ".");
        }

        Payment payment = paymentRepository.save(new Payment(invoice, amount, method, reference));
        invoice.setAmountPaid(invoice.getAmountPaid().add(amount));
        invoice.setStatus(invoice.getBalance().signum() <= 0
                ? InvoiceStatus.PAID : InvoiceStatus.PARTIALLY_PAID);
        invoiceRepository.save(invoice);

        notificationService.notify(invoice.getCustomer(), "Payment recorded",
                "A payment of Rs. " + amount + " was recorded. Outstanding balance: Rs. " + invoice.getBalance() + ".");
        return payment;
    }

    // ---------- Reads ----------

    public Invoice getInvoice(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Invoice", id));
    }

    public List<Invoice> allInvoices() {
        return invoiceRepository.findAllByOrderByIssuedDateDesc();
    }

    public List<Invoice> invoicesForCustomer(User customer) {
        return invoiceRepository.findByCustomerOrderByIssuedDateDesc(customer);
    }

    public List<Payment> paymentsFor(Invoice invoice) {
        return paymentRepository.findByInvoiceOrderByPaidDateDesc(invoice);
    }

    public List<Booking> completedBookingsWithoutInvoice() {
        return bookingService.byStatus(BookingStatus.COMPLETED).stream()
                .filter(b -> !invoiceRepository.existsByBooking(b))
                .toList();
    }
}
