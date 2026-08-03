package com.dozernet.module6_payment.repository;

import com.dozernet.common.user.User;
import com.dozernet.module2_booking.entity.Booking;
import com.dozernet.module6_payment.entity.Invoice;
import com.dozernet.module6_payment.entity.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findByCustomerOrderByIssuedDateDesc(User customer);

    List<Invoice> findAllByOrderByIssuedDateDesc();

    boolean existsByBooking(Booking booking);

    long countByStatus(InvoiceStatus status);
}
