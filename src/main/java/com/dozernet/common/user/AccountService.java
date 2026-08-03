package com.dozernet.common.user;

import com.dozernet.common.exception.BusinessRuleException;
import com.dozernet.common.model.Role;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Shared account creation used by role-specific registration flows (owner,
 * operator). Centralises uniqueness checks, phone normalisation and password
 * hashing so every module registers users the same way.
 */
@Service
public class AccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User createAccount(String fullName, String email, String phone,
                              String identityCardNumber,
                              String rawPassword, String confirmPassword,
                              Role role, boolean verified) {
        String normalisedEmail = email.trim().toLowerCase();
        String normalisedPhone = normalisePhone(phone);
        String nic = normaliseNic(identityCardNumber);

        if (rawPassword == null || !rawPassword.equals(confirmPassword)) {
            throw new BusinessRuleException("Passwords do not match");
        }
        if (rawPassword.length() < 8) {
            throw new BusinessRuleException("Password must be at least 8 characters");
        }
        if (userRepository.existsByEmail(normalisedEmail)) {
            throw new BusinessRuleException("An account with this email already exists");
        }
        if (userRepository.existsByPhone(normalisedPhone)) {
            throw new BusinessRuleException("An account with this phone number already exists");
        }
        if (userRepository.existsByIdentityCardNumber(nic)) {
            throw new BusinessRuleException("An account with this identity card number already exists");
        }

        User user = new User(fullName.trim(), normalisedEmail, normalisedPhone, nic,
                passwordEncoder.encode(rawPassword), role);
        user.setVerified(verified);
        return userRepository.save(user);
    }

    /** Canonical NIC form: trim + uppercase letter suffix (V/X). */
    public static String normaliseNic(String raw) {
        return raw == null ? null : raw.trim().toUpperCase();
    }

    public static String normalisePhone(String raw) {
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.startsWith("94") && digits.length() == 11) {
            digits = "0" + digits.substring(2);
        }
        return digits;
    }
}
