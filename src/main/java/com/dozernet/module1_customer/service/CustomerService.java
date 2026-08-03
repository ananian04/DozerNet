package com.dozernet.module1_customer.service;

import com.dozernet.common.exception.BusinessRuleException;
import com.dozernet.common.model.Role;
import com.dozernet.common.user.User;
import com.dozernet.common.user.UserRepository;
import com.dozernet.module1_customer.dto.ProfileForm;
import com.dozernet.module1_customer.dto.RegisterForm;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for customer accounts: registration, profile updates and
 * password changes. All rules that Bean Validation cannot express (uniqueness,
 * password confirmation) are enforced here.
 */
@Service
public class CustomerService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(RegisterForm form) {
        String email = form.getEmail().trim().toLowerCase();
        String phone = normalisePhone(form.getPhone());

        if (!form.getPassword().equals(form.getConfirmPassword())) {
            throw new BusinessRuleException("Passwords do not match");
        }
        if (userRepository.existsByEmail(email)) {
            throw new BusinessRuleException("An account with this email already exists");
        }
        if (userRepository.existsByPhone(phone)) {
            throw new BusinessRuleException("An account with this phone number already exists");
        }

        User user = new User(
                form.getFullName().trim(),
                email,
                phone,
                passwordEncoder.encode(form.getPassword()),
                Role.CUSTOMER);
        user.setVerified(true);
        return userRepository.save(user);
    }

    @Transactional
    public void updateProfile(User user, ProfileForm form) {
        String phone = normalisePhone(form.getPhone());
        if (!phone.equals(user.getPhone()) && userRepository.existsByPhone(phone)) {
            throw new BusinessRuleException("That phone number is already in use");
        }
        user.setFullName(form.getFullName().trim());
        user.setPhone(phone);
        userRepository.save(user);
    }

    @Transactional
    public void changePassword(User user, String currentPassword, String newPassword, String confirm) {
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BusinessRuleException("Current password is incorrect");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new BusinessRuleException("New password must be at least 8 characters");
        }
        if (!newPassword.equals(confirm)) {
            throw new BusinessRuleException("New passwords do not match");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /** Normalise SL phone numbers to a canonical 07XXXXXXXX form. */
    private String normalisePhone(String raw) {
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.startsWith("94") && digits.length() == 11) {
            digits = "0" + digits.substring(2);
        }
        return digits;
    }
}
