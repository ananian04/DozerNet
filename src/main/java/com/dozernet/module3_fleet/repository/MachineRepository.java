package com.dozernet.module3_fleet.repository;

import com.dozernet.common.user.User;
import com.dozernet.module3_fleet.entity.Machine;
import com.dozernet.module3_fleet.entity.MachineStatus;
import com.dozernet.module3_fleet.entity.MachineType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository (DAO) pattern for machines, including the public catalogue search.
 */
public interface MachineRepository extends JpaRepository<Machine, Long> {

    List<Machine> findByOwner(User owner);

    List<Machine> findByVerifiedFalse();

    List<Machine> findByStatus(MachineStatus status);

    boolean existsByRegistrationNumber(String registrationNumber);

    long countByStatus(MachineStatus status);

    /**
     * Public catalogue search: only verified + available machines, filtered by
     * optional type and a location/model keyword.
     */
    @Query("""
            select m from Machine m
            where m.verified = true and m.status = com.dozernet.module3_fleet.entity.MachineStatus.AVAILABLE
              and (:type is null or m.type = :type)
              and (:keyword is null or lower(m.location) like lower(concat('%', :keyword, '%'))
                                    or lower(m.model)    like lower(concat('%', :keyword, '%')))
            order by m.dailyRate asc
            """)
    List<Machine> search(@Param("type") MachineType type, @Param("keyword") String keyword);
}
