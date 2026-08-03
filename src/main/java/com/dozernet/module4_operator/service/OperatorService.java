package com.dozernet.module4_operator.service;

import com.dozernet.common.exception.BusinessRuleException;
import com.dozernet.common.exception.ResourceNotFoundException;
import com.dozernet.common.model.Role;
import com.dozernet.common.notification.NotificationService;
import com.dozernet.common.user.AccountService;
import com.dozernet.common.user.User;
import com.dozernet.module2_booking.entity.Booking;
import com.dozernet.module2_booking.entity.BookingStatus;
import com.dozernet.module2_booking.service.BookingService;
import com.dozernet.module4_operator.dto.CompanyOperatorForm;
import com.dozernet.module4_operator.dto.DriverRegisterForm;
import com.dozernet.module4_operator.entity.Assignment;
import com.dozernet.module4_operator.entity.JobStatus;
import com.dozernet.module4_operator.entity.OperatorProfile;
import com.dozernet.module4_operator.repository.AssignmentRepository;
import com.dozernet.module4_operator.repository.OperatorProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Operator Management: company operator setup, independent driver registration
 * and verification, assigning operators to approved bookings, and job-status
 * updates.
 */
@Service
public class OperatorService {

    private final OperatorProfileRepository profileRepository;
    private final AssignmentRepository assignmentRepository;
    private final AccountService accountService;
    private final BookingService bookingService;
    private final NotificationService notificationService;

    public OperatorService(OperatorProfileRepository profileRepository,
                           AssignmentRepository assignmentRepository,
                           AccountService accountService,
                           BookingService bookingService,
                           NotificationService notificationService) {
        this.profileRepository = profileRepository;
        this.assignmentRepository = assignmentRepository;
        this.accountService = accountService;
        this.bookingService = bookingService;
        this.notificationService = notificationService;
    }

    // ---------- Registration ----------

    @Transactional
    public OperatorProfile addCompanyOperator(CompanyOperatorForm form) {
        checkLicence(form.getLicenceNumber());
        User user = accountService.createAccount(form.getFullName(), form.getEmail(), form.getPhone(),
                form.getIdentityCardNumber(),
                form.getPassword(), form.getPassword(), Role.OPERATOR, true);
        return saveProfile(user, form.getLicenceNumber(), form.getExperienceYears(), false, true);
    }

    @Transactional
    public OperatorProfile registerDriver(DriverRegisterForm form) {
        checkLicence(form.getLicenceNumber());
        User user = accountService.createAccount(form.getFullName(), form.getEmail(), form.getPhone(),
                form.getIdentityCardNumber(),
                form.getPassword(), form.getConfirmPassword(), Role.OPERATOR, true);
        // Independent drivers are unverified until an admin checks their licence.
        return saveProfile(user, form.getLicenceNumber(), form.getExperienceYears(), true, false);
    }

    @Transactional
    public void verify(Long profileId) {
        OperatorProfile p = getProfile(profileId);
        p.setVerified(true);
        profileRepository.save(p);
        notificationService.notify(p.getUser(), "Licence verified",
                "Your driver licence has been verified. You can now be assigned to jobs.");
    }

    // ---------- Assignment ----------

    @Transactional
    public Assignment assign(Long bookingId, User operator) {
        Booking booking = bookingService.getById(bookingId);
        if (booking.getStatus() != BookingStatus.APPROVED) {
            throw new BusinessRuleException("Operators can only be assigned to approved bookings.");
        }
        if (assignmentRepository.existsByBooking(booking)) {
            throw new BusinessRuleException("This booking already has an operator assigned.");
        }
        OperatorProfile profile = profileRepository.findByUser(operator)
                .orElseThrow(() -> new BusinessRuleException("Selected user is not an operator."));
        if (!profile.isVerified()) {
            throw new BusinessRuleException("This operator has not been verified yet.");
        }
        List<Assignment> clashes = assignmentRepository.findOperatorClashes(
                operator, booking.getStartDate(), booking.getEndDate());
        if (!clashes.isEmpty()) {
            throw new BusinessRuleException("This operator is already assigned to another job on those dates.");
        }

        Assignment assignment = assignmentRepository.save(new Assignment(booking, operator));
        notificationService.notify(operator, "New job assigned",
                "You have been assigned to " + booking.getMachine().getModel()
                        + " (" + booking.getStartDate() + " to " + booking.getEndDate() + ").");
        notificationService.notify(booking.getCustomer(), "Operator assigned",
                "An operator has been assigned to your booking of " + booking.getMachine().getModel() + ".");
        return assignment;
    }

    @Transactional
    public void updateJobStatus(Long assignmentId, JobStatus newStatus, User operator) {
        Assignment a = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Assignment", assignmentId));
        if (!a.getOperator().getId().equals(operator.getId())) {
            throw new BusinessRuleException("You can only update your own assignments.");
        }
        validateTransition(a.getJobStatus(), newStatus);
        a.setJobStatus(newStatus);
        assignmentRepository.save(a);

        if (newStatus == JobStatus.COMPLETED) {
            bookingService.markCompleted(a.getBooking().getId());
        }
    }

    private void validateTransition(JobStatus current, JobStatus next) {
        boolean ok = switch (current) {
            case ASSIGNED -> next == JobStatus.IN_PROGRESS;
            case IN_PROGRESS -> next == JobStatus.COMPLETED;
            case COMPLETED -> false;
        };
        if (!ok) {
            throw new BusinessRuleException("Cannot change job status from "
                    + current.getDisplayName() + " to " + next.getDisplayName() + ".");
        }
    }

    // ---------- Reads ----------

    public List<OperatorProfile> allOperators() {
        return profileRepository.findAllWithUser();
    }

    public List<OperatorProfile> pendingVerifications() {
        return profileRepository.findByVerifiedFalse();
    }

    public List<OperatorProfile> verifiedOperators() {
        return profileRepository.findByVerifiedTrue();
    }

    public List<Assignment> assignmentsFor(User operator) {
        return assignmentRepository.findByOperatorOrderByCreatedAtDesc(operator);
    }

    /** Approved bookings that still need an operator (for the admin assign screen). */
    public List<Booking> unassignedApprovedBookings() {
        return bookingService.approvedBookings().stream()
                .filter(b -> !assignmentRepository.existsByBooking(b))
                .toList();
    }

    private OperatorProfile getProfile(Long id) {
        return profileRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Operator", id));
    }

    private void checkLicence(String licence) {
        if (profileRepository.existsByLicenceNumber(licence.trim())) {
            throw new BusinessRuleException("An operator with that licence number already exists.");
        }
    }

    private OperatorProfile saveProfile(User user, String licence, int years,
                                        boolean independent, boolean verified) {
        OperatorProfile p = new OperatorProfile();
        p.setUser(user);
        p.setLicenceNumber(licence.trim());
        p.setExperienceYears(years);
        p.setIndependent(independent);
        p.setVerified(verified);
        return profileRepository.save(p);
    }
}
