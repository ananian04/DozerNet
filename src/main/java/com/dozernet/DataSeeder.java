package com.dozernet;

import com.dozernet.common.model.Role;
import com.dozernet.common.user.User;
import com.dozernet.common.user.UserRepository;
import com.dozernet.module3_fleet.entity.Machine;
import com.dozernet.module3_fleet.entity.MachineType;
import com.dozernet.module3_fleet.entity.Ownership;
import com.dozernet.module3_fleet.repository.MachineRepository;
import com.dozernet.module4_operator.entity.OperatorProfile;
import com.dozernet.module4_operator.repository.OperatorProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

/**
 * Seeds demo accounts (with correctly BCrypt-hashed passwords) so the app is
 * demonstrable immediately. Runs first (Order 1); module-specific seed data
 * runs afterwards. Idempotent: only seeds when the users table is empty.
 *
 * Demo password for every seeded account: "password123".
 */
@Component
@Order(1)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    public static final String DEMO_PASSWORD = "password123";

    private static final Map<MachineType, String> TYPE_IMAGES = new EnumMap<>(MachineType.class);

    static {
        TYPE_IMAGES.put(MachineType.BACKHOE_LOADER, "/images/machines/backhoe-loader.jpg");
        TYPE_IMAGES.put(MachineType.EXCAVATOR, "/images/machines/excavator.jpg");
        TYPE_IMAGES.put(MachineType.WHEEL_LOADER, "/images/machines/wheel-loader.jpg");
        TYPE_IMAGES.put(MachineType.SKID_STEER, "/images/machines/skid-steer.jpg");
        TYPE_IMAGES.put(MachineType.TELEHANDLER, "/images/machines/telehandler.jpg");
        TYPE_IMAGES.put(MachineType.COMPACTOR, "/images/machines/compactor.jpg");
        TYPE_IMAGES.put(MachineType.BULLDOZER, "/images/machines/bulldozer.jpg");
    }

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MachineRepository machineRepository;
    private final OperatorProfileRepository operatorProfileRepository;

    public DataSeeder(UserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      MachineRepository machineRepository,
                      OperatorProfileRepository operatorProfileRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.machineRepository = machineRepository;
        this.operatorProfileRepository = operatorProfileRepository;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            log.info("Seeding demo users (password for all: {})", DEMO_PASSWORD);

            seed("Admin User", "admin@dozernet.lk", "0770000001", Role.ADMIN, true);
            seed("Chamara Perera", "customer@dozernet.lk", "0771111111", Role.CUSTOMER, true);
            seed("Nimal Fernando", "owner@dozernet.lk", "0772222222", Role.OWNER, true);
            User operator = seed("Sunil Bandara", "operator@dozernet.lk", "0773333333", Role.OPERATOR, true);

            seedMachines();
            seedOperatorProfile(operator);
        }

        // Always fill missing machine photos (existing DB rows included).
        ensureMachineImages();
    }

    private void ensureMachineImages() {
        int updated = 0;
        for (Machine m : machineRepository.findAll()) {
            if (m.getImageUrl() == null || m.getImageUrl().isBlank()) {
                m.setImageUrl(imageFor(m.getType()));
                machineRepository.save(m);
                updated++;
            }
        }
        if (updated > 0) {
            log.info("Assigned sample images to {} machine(s)", updated);
        }
    }

    private static String imageFor(MachineType type) {
        return TYPE_IMAGES.getOrDefault(type, "/images/machines/excavator.jpg");
    }

    private User seed(String name, String email, String phone, Role role, boolean verified) {
        User u = new User(name, email, phone, passwordEncoder.encode(DEMO_PASSWORD), role);
        u.setVerified(verified);
        return userRepository.save(u);
    }

    private void seedOperatorProfile(User operator) {
        OperatorProfile p = new OperatorProfile();
        p.setUser(operator);
        p.setLicenceNumber("B1234567");
        p.setExperienceYears(6);
        p.setIndependent(false);
        p.setVerified(true);
        operatorProfileRepository.save(p);
    }

    private void seedMachines() {
        company("JCB 3CX Backhoe Loader", MachineType.BACKHOE_LOADER, "WP CAB-3421", "Colombo", "18500.00",
                "Versatile backhoe loader ideal for digging, loading and site work.");
        company("JCB JS205 Excavator", MachineType.EXCAVATOR, "WP EXC-8890", "Gampaha", "32000.00",
                "20-tonne tracked excavator for heavy earthmoving.");
        company("JCB 433 Wheel Loader", MachineType.WHEEL_LOADER, "CP WLD-1200", "Kandy", "27500.00",
                "High-capacity wheel loader for aggregate and material handling.");
        company("JCB Robot 155 Skid Steer", MachineType.SKID_STEER, "WP SKD-4502", "Colombo", "14000.00",
                "Compact skid steer loader for tight urban sites.");
        company("JCB 540-170 Telehandler", MachineType.TELEHANDLER, "SP TEL-7781", "Galle", "24000.00",
                "17-metre telehandler for lifting and placing loads at height.");
        company("JCB VMT260 Compactor", MachineType.COMPACTOR, "NW CMP-3390", "Kurunegala", "16500.00",
                "Vibratory roller for road and foundation compaction.");
    }

    private void company(String model, MachineType type, String reg, String location,
                         String dailyRate, String description) {
        Machine m = new Machine();
        m.setModel(model);
        m.setType(type);
        m.setRegistrationNumber(reg);
        m.setLocation(location);
        m.setDailyRate(new BigDecimal(dailyRate));
        m.setDescription(description);
        m.setImageUrl(imageFor(type));
        m.setOwnership(Ownership.COMPANY);
        m.setVerified(true);
        machineRepository.save(m);
    }
}
