package com.dozernet.module2_booking.repository;

import com.dozernet.common.user.User;
import com.dozernet.module2_booking.entity.Booking;
import com.dozernet.module2_booking.entity.BookingStatus;
import com.dozernet.module3_fleet.entity.Machine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByCustomerOrderByStartDateDesc(User customer);

    List<Booking> findByStatusOrderByCreatedAtDesc(BookingStatus status);

    List<Booking> findAllByOrderByCreatedAtDesc();

    long countByStatus(BookingStatus status);

    boolean existsByMachine(Machine machine);

    /**
     * Overlapping bookings for a machine that still occupy the calendar
     * (PENDING or APPROVED). Two ranges overlap when start <= otherEnd AND
     * end >= otherStart. Used to prevent double-booking.
     */
    @Query("""
            select b from Booking b
            where b.machine = :machine
              and b.status in (com.dozernet.module2_booking.entity.BookingStatus.PENDING,
                               com.dozernet.module2_booking.entity.BookingStatus.APPROVED)
              and b.startDate <= :end
              and b.endDate   >= :start
            """)
    List<Booking> findOverlapping(@Param("machine") Machine machine,
                                  @Param("start") LocalDate start,
                                  @Param("end") LocalDate end);
}
