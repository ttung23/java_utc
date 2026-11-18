package com.library.auth.repository;

import com.library.auth.entity.PasswordReset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetRepository extends JpaRepository<PasswordReset, Long> {
    Optional<PasswordReset> findByTokenHashAndUsedAtIsNull(String tokenHash);
}
