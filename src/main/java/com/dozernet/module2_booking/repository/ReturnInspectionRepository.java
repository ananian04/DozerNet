package com.dozernet.module2_booking.repository;

import com.dozernet.module2_booking.entity.Booking;
import com.dozernet.module2_booking.entity.ReturnInspection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository (DAO) pattern via Spring Data JPA - return inspection reports.
 */
public interface ReturnInspectionRepository extends JpaRepository<ReturnInspection, Long> {

    Optional<ReturnInspection> findByBooking(Booking booking);

    boolean existsByBooking(Booking booking);

    List<ReturnInspection> findByDamageReportedTrueOrderByCreatedAtDesc();

    List<ReturnInspection> findAllByOrderByCreatedAtDesc();
}
