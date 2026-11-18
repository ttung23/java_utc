package com.library.staff.service;

import com.library.common.enums.UserRole;
import com.library.common.enums.UserStatus;
import com.library.common.exception.BusinessException;
import com.library.security.SecurityUtils;
import com.library.user.entity.StaffProfile;
import com.library.user.entity.User;
import com.library.user.repository.StaffProfileRepository;
import com.library.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class StaffService {

    private final UserRepository userRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.ADMIN || u.getRole() == UserRole.STAFF)
                .map(this::toMap)
                .toList();
    }

    public Map<String, Object> create(String email, String phone, String fullName, String password,
                                      UserRole role, String staffCode, String position, String shift) {
        if (role != UserRole.ADMIN && role != UserRole.STAFF) {
            throw new BusinessException("INVALID_ROLE", "Role phải là ADMIN hoặc STAFF");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("EMAIL_EXISTS", "Email đã tồn tại");
        }
        User user = new User();
        user.setEmail(email.trim().toLowerCase());
        user.setPhone(phone);
        user.setFullName(fullName);
        user.setPasswordHash(passwordEncoder.encode(StringUtils.hasText(password) ? password : "Staff@123"));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setFailedLoginCount(0);
        user.setCreatedBy(SecurityUtils.currentUserIdOrNull());
        userRepository.save(user);

        StaffProfile profile = new StaffProfile();
        profile.setUser(user);
        profile.setStaffCode(StringUtils.hasText(staffCode) ? staffCode : ("NV-" + user.getId()));
        profile.setPosition(position);
        profile.setShift(shift);
        profile.setHireDate(LocalDate.now());
        profile.setCreatedBy(SecurityUtils.currentUserIdOrNull());
        staffProfileRepository.save(profile);
        return toMap(user);
    }

    public Map<String, Object> updateStatus(Long userId, UserStatus status) {
        User user = requireStaffUser(userId);
        Long me = SecurityUtils.requireCurrentUser().getId();
        if (user.getId().equals(me) && status != UserStatus.ACTIVE) {
            throw new BusinessException("CANNOT_LOCK_SELF", "Không thể khóa chính mình");
        }
        if (user.getRole() == UserRole.ADMIN && status != UserStatus.ACTIVE) {
            long activeAdmins = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == UserRole.ADMIN && u.getStatus() == UserStatus.ACTIVE)
                    .count();
            if (activeAdmins <= 1) {
                throw new BusinessException("LAST_ADMIN", "Phải còn ít nhất 1 Admin ACTIVE");
            }
        }
        user.setStatus(status);
        userRepository.save(user);
        return toMap(user);
    }

    public Map<String, Object> updateRole(Long userId, UserRole role) {
        User user = requireStaffUser(userId);
        if (role != UserRole.ADMIN && role != UserRole.STAFF) {
            throw new BusinessException("INVALID_ROLE", "Role phải là ADMIN hoặc STAFF");
        }
        Long me = SecurityUtils.requireCurrentUser().getId();
        if (user.getId().equals(me) && role != UserRole.ADMIN) {
            throw new BusinessException("CANNOT_DOWNGRADE_SELF", "Không thể tự hạ quyền");
        }
        user.setRole(role);
        userRepository.save(user);
        return toMap(user);
    }

    public void resetPassword(Long userId, String newPassword) {
        User user = requireStaffUser(userId);
        user.setPasswordHash(passwordEncoder.encode(StringUtils.hasText(newPassword) ? newPassword : "Staff@123"));
        userRepository.save(user);
    }

    private User requireStaffUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy nhân viên", HttpStatus.NOT_FOUND));
        if (user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.STAFF) {
            throw new BusinessException("NOT_STAFF", "User không phải nhân viên");
        }
        return user;
    }

    private Map<String, Object> toMap(User user) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", user.getId());
        m.put("email", user.getEmail());
        m.put("phone", user.getPhone());
        m.put("fullName", user.getFullName());
        m.put("role", user.getRole());
        m.put("status", user.getStatus());
        staffProfileRepository.findByUser_Id(user.getId()).ifPresent(p -> {
            m.put("staffCode", p.getStaffCode());
            m.put("position", p.getPosition());
            m.put("shift", p.getShift());
            m.put("hireDate", p.getHireDate());
        });
        return m;
    }
}
