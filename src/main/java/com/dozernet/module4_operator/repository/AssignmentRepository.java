package com.dozernet.module4_operator.repository;

import com.dozernet.common.user.User;
import com.dozernet.module2_booking.entity.Booking;
import com.dozernet.module4_operator.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findByOperatorOrderByCreatedAtDesc(User operator);

    Optional<Assignment> findByBooking(Booking booking);

    boolean existsByBooking(Booking booking);

    /**
     * Active assignments for an operator whose booking dates overlap the given
     * range - used to stop assigning one operator to two overlapping jobs.
     */
    @Query("""
            select a from Assignment a
            where a.operator = :operator
              and a.jobStatus <> com.dozernet.module4_operator.entity.JobStatus.COMPLETED
              and a.booking.startDate <= :end
              and a.booking.endDate   >= :start
            """)
    List<Assignment> findOperatorClashes(@Param("operator") User operator,
                                         @Param("start") LocalDate start,
                                         @Param("end") LocalDate end);
}
