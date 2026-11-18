package com.library.user.repository;

import com.library.user.entity.StaffProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StaffProfileRepository extends JpaRepository<StaffProfile, Long> {
    Optional<StaffProfile> findByUser_Id(Long userId);

    boolean existsByStaffCode(String staffCode);
}
