package com.dozernet.module5_maintenance.repository;

import com.dozernet.module3_fleet.entity.Machine;
import com.dozernet.module5_maintenance.entity.MaintenanceRecord;
import com.dozernet.module5_maintenance.entity.MaintenanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, Long> {

    List<MaintenanceRecord> findByMachineOrderByScheduledDateDesc(Machine machine);

    List<MaintenanceRecord> findByStatusOrderByScheduledDateAsc(MaintenanceStatus status);

    List<MaintenanceRecord> findAllByOrderByScheduledDateDesc();

    long countByStatus(MaintenanceStatus status);
}
