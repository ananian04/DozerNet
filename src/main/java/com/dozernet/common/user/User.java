package com.dozernet.common.user;

import com.dozernet.common.model.BaseEntity;
import com.dozernet.common.model.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * Central account for every DozerNet actor (Customer, Owner, Operator, Admin).
 * Authentication and role-based access are driven off this single table so
 * login/logout works uniformly across all six modules.
 */
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String phone;

    /**
     * Sri Lankan National Identity Card number. Collected at registration and
     * never editable afterwards (shown read-only on the profile).
     * Nullable only to allow upgrading existing databases; new accounts always set it.
     */
    @Column(unique = true, length = 12)
    private String identityCardNumber;

    /** BCrypt hash - never the plain password. */
    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /** Account active flag (admin can disable an account). */
    @Column(nullable = false)
    private boolean enabled = true;

    /**
     * Whether the account is verified. Customers/Admins are verified on sign-up;
     * Owners and Operators start unverified until an admin approves them.
     */
    @Column(nullable = false)
    private boolean verified = true;

    public User() {
    }

    public User(String fullName, String email, String phone, String passwordHash, Role role) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public User(String fullName, String email, String phone, String identityCardNumber,
                String passwordHash, Role role) {
        this(fullName, email, phone, passwordHash, role);
        this.identityCardNumber = identityCardNumber;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getIdentityCardNumber() {
        return identityCardNumber;
    }

    public void setIdentityCardNumber(String identityCardNumber) {
        this.identityCardNumber = identityCardNumber;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }
}
