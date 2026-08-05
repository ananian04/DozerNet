package com.dozernet.module6_payment;

import com.dozernet.common.exception.BusinessRuleException;
import com.dozernet.common.model.Role;
import com.dozernet.common.notification.NotificationService;
import com.dozernet.common.user.User;
import com.dozernet.module2_booking.entity.Booking;
import com.dozernet.module2_booking.entity.BookingStatus;
import com.dozernet.module2_booking.service.BookingService;
import com.dozernet.module3_fleet.entity.Machine;
import com.dozernet.module3_fleet.entity.MachineType;
import com.dozernet.module6_payment.entity.Invoice;
import com.dozernet.module6_payment.entity.InvoiceStatus;
import com.dozernet.module6_payment.entity.Payment;
import com.dozernet.module6_payment.entity.PaymentMethod;
import com.dozernet.module6_payment.pricing.PricingSelector;
import com.dozernet.module6_payment.repository.InvoiceRepository;
import com.dozernet.module6_payment.repository.PaymentRepository;
import com.dozernet.module6_payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock InvoiceRepository invoiceRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock BookingService bookingService;
    @Mock PricingSelector pricingSelector;
    @Mock NotificationService notificationService;
    @Mock com.dozernet.common.user.UserRepository userRepository;

    @InjectMocks PaymentService paymentService;

    private User customer;
    private Booking approvedBooking;
    private Booking completedBooking;

    @BeforeEach
    void setUp() {
        customer = new User("Chamara", "c@x.lk", "0771111111", "hash", Role.CUSTOMER);
        customer.setId(1L);

        Machine machine = new Machine();
        machine.setId(10L);
        machine.setModel("JCB 3CX");
        machine.setType(MachineType.BACKHOE_LOADER);
        machine.setDailyRate(new BigDecimal("10000.00"));

        approvedBooking = new Booking(customer, machine,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));
        approvedBooking.setId(5L);
        approvedBooking.setStatus(BookingStatus.APPROVED);
        approvedBooking.setJobSiteDistrict("Colombo");
        approvedBooking.setJobSiteAddress("Site A");
        approvedBooking.setTotalAmount(new BigDecimal("30000.00"));

        completedBooking = new Booking(customer, machine,
                LocalDate.now().minusDays(3), LocalDate.now().minusDays(1));
        completedBooking.setId(6L);
        completedBooking.setStatus(BookingStatus.COMPLETED);
        completedBooking.setTotalAmount(new BigDecimal("30000.00"));
    }

    @Test
    void cannotInvoicePendingBooking() {
        approvedBooking.setStatus(BookingStatus.PENDING);
        when(bookingService.getById(5L)).thenReturn(approvedBooking);

        assertThatThrownBy(() -> paymentService.generateInvoice(5L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("approved or completed");
    }

    @Test
    void cannotCreateDuplicateInvoice() {
        when(bookingService.getById(5L)).thenReturn(completedBooking);
        when(invoiceRepository.existsByBooking(completedBooking)).thenReturn(true);

        assertThatThrownBy(() -> paymentService.generateInvoice(5L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void createsInvoiceForApprovedBooking() {
        when(invoiceRepository.existsByBooking(approvedBooking)).thenReturn(false);
        when(pricingSelector.price(approvedBooking)).thenReturn(new BigDecimal("27000.00"));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            i.setId(99L);
            return i;
        });

        Invoice invoice = paymentService.createInvoiceForApprovedBooking(approvedBooking);

        assertThat(invoice.getAmount()).isEqualByComparingTo("27000.00");
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.UNPAID);
        assertThat(approvedBooking.getTotalAmount()).isEqualByComparingTo("27000.00");
    }

    @Test
    void generatesInvoiceUsingPricingStrategy() {
        when(bookingService.getById(6L)).thenReturn(completedBooking);
        when(invoiceRepository.existsByBooking(completedBooking)).thenReturn(false);
        when(pricingSelector.price(completedBooking)).thenReturn(new BigDecimal("27000.00"));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice invoice = paymentService.generateInvoice(6L);

        assertThat(invoice.getAmount()).isEqualByComparingTo("27000.00");
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.UNPAID);
        assertThat(invoice.getCustomer()).isEqualTo(customer);
    }

    @Test
    void rejectsZeroOrNegativePayment() {
        Invoice invoice = new Invoice(completedBooking, customer, new BigDecimal("30000.00"));
        invoice.setId(9L);
        when(invoiceRepository.findById(9L)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> paymentService.recordPayment(9L, BigDecimal.ZERO, PaymentMethod.CASH, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("greater than zero");
    }

    @Test
    void rejectsPaymentExceedingBalance() {
        Invoice invoice = new Invoice(completedBooking, customer, new BigDecimal("30000.00"));
        invoice.setId(9L);
        when(invoiceRepository.findById(9L)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> paymentService.recordPayment(
                9L, new BigDecimal("30000.01"), PaymentMethod.BANK_TRANSFER, "REF"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("exceeds");
    }

    @Test
    void marksInvoicePaidWhenBalanceCleared() {
        Invoice invoice = new Invoice(completedBooking, customer, new BigDecimal("30000.00"));
        invoice.setId(9L);
        when(invoiceRepository.findById(9L)).thenReturn(Optional.of(invoice));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findByRole(Role.ADMIN)).thenReturn(List.of());

        paymentService.recordPayment(9L, new BigDecimal("30000.00"), PaymentMethod.CASH, "CASH-1");

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(invoice.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void notifiesAdminsToAssignOperatorWhenFullyPaid() {
        User admin = new User("Admin", "admin@dozernet.lk", "0770000000", "hash", Role.ADMIN);
        admin.setId(2L);
        Invoice invoice = new Invoice(approvedBooking, customer, new BigDecimal("30000.00"));
        invoice.setId(9L);
        when(invoiceRepository.findById(9L)).thenReturn(Optional.of(invoice));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findByRole(Role.ADMIN)).thenReturn(List.of(admin));

        paymentService.recordPayment(9L, new BigDecimal("30000.00"), PaymentMethod.CASH, "CASH-1");

        verify(notificationService).notify(eq(admin), eq("Payment received — assign an operator"), anyString());
    }

    @Test
    void customerPortalPaysFullBalanceWithCard() {
        Invoice invoice = new Invoice(approvedBooking, customer, new BigDecimal("30000.00"));
        invoice.setId(9L);
        when(invoiceRepository.findByIdWithDetails(9L)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.findById(9L)).thenReturn(Optional.of(invoice));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findByRole(Role.ADMIN)).thenReturn(List.of());

        Payment payment = paymentService.recordCustomerPayment(9L, customer, "Chamara Perera", "4242");

        assertThat(payment.getMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(payment.getReference()).contains("4242");
    }

    @Test
    void isInvoicePaidForBookingFalseWhenUnpaid() {
        Invoice invoice = new Invoice(approvedBooking, customer, new BigDecimal("30000.00"));
        when(invoiceRepository.findByBooking(approvedBooking)).thenReturn(Optional.of(invoice));

        assertThat(paymentService.isInvoicePaidForBooking(approvedBooking)).isFalse();
    }
}
