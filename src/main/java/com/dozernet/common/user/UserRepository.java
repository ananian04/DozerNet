package com.dozernet.common.user;

import com.dozernet.common.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository (DAO) pattern via Spring Data JPA - shared user data access.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByIdentityCardNumber(String identityCardNumber);

    List<User> findByRole(Role role);

    List<User> findByRoleAndVerifiedFalse(Role role);

    long countByRole(Role role);
}
