package com.dozernet.module5_maintenance.service;

import com.dozernet.common.exception.ResourceNotFoundException;
import com.dozernet.module3_fleet.entity.Machine;
import com.dozernet.module3_fleet.entity.MachineStatus;
import com.dozernet.module3_fleet.service.FleetService;
import com.dozernet.module5_maintenance.dto.ScheduleForm;
import com.dozernet.module5_maintenance.entity.MaintenanceRecord;
import com.dozernet.module5_maintenance.entity.MaintenanceStatus;
import com.dozernet.module5_maintenance.repository.MaintenanceRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Maintenance & Service Management. Scheduling maintenance pulls a machine out
 * of the booking pool (status MAINTENANCE); completing it records parts/cost
 * and can return the machine to service.
 */
@Service
public class MaintenanceService {

    private final MaintenanceRecordRepository repository;
    private final FleetService fleetService;

    public MaintenanceService(MaintenanceRecordRepository repository, FleetService fleetService) {
        this.repository = repository;
        this.fleetService = fleetService;
    }

    @Transactional
    public MaintenanceRecord schedule(ScheduleForm form) {
        Machine machine = fleetService.getById(form.getMachineId());
        MaintenanceRecord record = new MaintenanceRecord();
        record.setMachine(machine);
        record.setScheduledDate(form.getScheduledDate());
        record.setDescription(form.getDescription().trim());
        record.setStatus(MaintenanceStatus.SCHEDULED);
        MaintenanceRecord saved = repository.save(record);

        // Keep an unsafe/serviced machine out of the booking pool.
        fleetService.setStatus(machine.getId(), MachineStatus.MAINTENANCE);
        return saved;
    }

    @Transactional
    public void complete(Long recordId, String partsUsed, BigDecimal cost, boolean returnToService) {
        MaintenanceRecord record = getById(recordId);
        record.setStatus(MaintenanceStatus.COMPLETED);
        record.setCompletedDate(LocalDate.now());
        record.setPartsUsed(partsUsed);
        record.setCost(cost);
        repository.save(record);

        if (returnToService) {
            fleetService.setStatus(record.getMachine().getId(), MachineStatus.AVAILABLE);
        }
    }

    public List<MaintenanceRecord> all() {
        return repository.findAllByOrderByScheduledDateDesc();
    }

    public List<MaintenanceRecord> upcoming() {
        return repository.findByStatusOrderByScheduledDateAsc(MaintenanceStatus.SCHEDULED);
    }

    public List<MaintenanceRecord> historyFor(Long machineId) {
        return repository.findByMachineOrderByScheduledDateDesc(fleetService.getById(machineId));
    }

    public long countScheduled() {
        return repository.countByStatus(MaintenanceStatus.SCHEDULED);
    }

    public MaintenanceRecord getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Maintenance record", id));
    }
}
