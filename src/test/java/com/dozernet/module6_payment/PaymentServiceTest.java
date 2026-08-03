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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock InvoiceRepository invoiceRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock BookingService bookingService;
    @Mock PricingSelector pricingSelector;
    @Mock NotificationService notificationService;

    @InjectMocks PaymentService paymentService;

    private User customer;
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

        completedBooking = new Booking(customer, machine,
                LocalDate.now().minusDays(3), LocalDate.now().minusDays(1));
        completedBooking.setId(5L);
        completedBooking.setStatus(BookingStatus.COMPLETED);
        completedBooking.setTotalAmount(new BigDecimal("30000.00"));
    }

    @Test
    void cannotInvoiceNonCompletedBooking() {
        completedBooking.setStatus(BookingStatus.APPROVED);
        when(bookingService.getById(5L)).thenReturn(completedBooking);

        assertThatThrownBy(() -> paymentService.generateInvoice(5L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("completed");
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
    void generatesInvoiceUsingPricingStrategy() {
        when(bookingService.getById(5L)).thenReturn(completedBooking);
        when(invoiceRepository.existsByBooking(completedBooking)).thenReturn(false);
        when(pricingSelector.price(completedBooking)).thenReturn(new BigDecimal("27000.00"));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice invoice = paymentService.generateInvoice(5L);

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

        paymentService.recordPayment(9L, new BigDecimal("30000.00"), PaymentMethod.CASH, "CASH-1");

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(invoice.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
