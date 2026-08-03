package com.dozernet.module4_operator.repository;

import com.dozernet.common.user.User;
import com.dozernet.module4_operator.entity.OperatorProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OperatorProfileRepository extends JpaRepository<OperatorProfile, Long> {

    Optional<OperatorProfile> findByUser(User user);

    List<OperatorProfile> findByVerifiedFalse();

    List<OperatorProfile> findByVerifiedTrue();

    boolean existsByLicenceNumber(String licenceNumber);
}
