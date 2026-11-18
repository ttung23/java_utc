package com.slms.service;

import com.slms.auth.AuthSession;
import com.slms.auth.SessionStore;
import com.slms.model.Reader;
import com.slms.model.StaffUser;
import com.slms.model.enums.ReaderType;
import com.slms.model.enums.UserRole;
import com.slms.repository.ReaderRepository;
import com.slms.repository.StaffRepository;
import com.slms.util.IdGenerator;
import com.slms.util.PasswordUtil;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Đăng nhập / đăng ký / phiên làm việc.
 */
public class AuthService {

    public static final String DEFAULT_READER_PASSWORD = "123456";
    private static final long SESSION_HOURS = 12;

    private final ReaderRepository readerRepository = new ReaderRepository();
    private final StaffRepository staffRepository = new StaffRepository();

    public Map<String, Object> loginReader(String readerId, String password) {
        Optional<Reader> opt = readerRepository.findById(readerId);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy độc giả: " + readerId);
        }
        Reader reader = opt.get();
        if (reader.getPasswordHash() == null || reader.getPasswordHash().isBlank()) {
            throw new IllegalArgumentException("Tài khoản chưa thiết lập mật khẩu. Liên hệ thủ thư.");
        }
        if (!PasswordUtil.matches(password, reader.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu không đúng");
        }

        AuthSession session = createSession(UserRole.READER, reader.getReaderId(), reader.getFullName());
        return toLoginResponse(session, reader.getStatus(), reader.getReaderTypeName());
    }

    public Map<String, Object> loginStaff(String username, String password) {
        Optional<StaffUser> opt = staffRepository.findByUsername(username);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("Sai tài khoản hoặc mật khẩu");
        }
        StaffUser staff = opt.get();
        if (!"ACTIVE".equalsIgnoreCase(staff.getStatus())) {
            throw new IllegalArgumentException("Tài khoản nhân viên đã bị khóa");
        }
        if (!PasswordUtil.matches(password, staff.getPasswordHash())) {
            throw new IllegalArgumentException("Sai tài khoản hoặc mật khẩu");
        }

        AuthSession session = createSession(staff.getRole(), staff.getStaffId(), staff.getFullName(), staff.getAvatar());
        return toLoginResponse(session, staff.getStatus(), staff.getRole().name());
    }

    public Map<String, Object> registerReader(String fullName, String readerType, String email, String phone, String password) {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Họ tên không được để trống");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Mật khẩu tối thiểu 6 ký tự");
        }

        ReaderType type;
        try {
            type = ReaderType.valueOf(readerType == null ? "STUDENT" : readerType.trim().toUpperCase());
        } catch (Exception e) {
            type = ReaderType.STUDENT;
        }

        Reader reader = new Reader();
        reader.setReaderId(IdGenerator.nextReaderId());
        reader.setFullName(fullName.trim());
        reader.setType(type);
        reader.setExpiryDate(LocalDate.now().plusYears(2));
        reader.setBalanceFine(0);
        reader.setStatus("ACTIVE");
        reader.setEmail(email != null ? email.trim() : null);
        reader.setPhone(phone != null ? phone.trim() : null);
        reader.setPasswordHash(PasswordUtil.hash(password));

        readerRepository.save(reader);

        AuthSession session = createSession(UserRole.READER, reader.getReaderId(), reader.getFullName());
        Map<String, Object> result = toLoginResponse(session, reader.getStatus(), reader.getType().name());
        result.put("message", "Đăng ký thẻ thành công. Mã độc giả: " + reader.getReaderId());
        return result;
    }

    public void logout(String token) {
        SessionStore.remove(token);
    }

    public Optional<AuthSession> resolve(String token) {
        return SessionStore.get(token);
    }

    public Map<String, Object> updateReaderProfile(String readerId, String fullName, String email, String phone) {
        Optional<Reader> opt = readerRepository.findById(readerId);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy độc giả");
        }
        Reader reader = opt.get();
        if (fullName != null && !fullName.trim().isEmpty()) {
            reader.setFullName(fullName.trim());
        }
        if (email != null) {
            reader.setEmail(email.trim());
        }
        if (phone != null) {
            reader.setPhone(phone.trim());
        }
        readerRepository.update(reader);

        Map<String, Object> result = new HashMap<>();
        result.put("readerId", reader.getReaderId());
        result.put("fullName", reader.getFullName());
        result.put("email", reader.getEmail());
        result.put("phone", reader.getPhone());
        return result;
    }

    public void changePassword(String readerId, String oldPassword, String newPassword) {
        Optional<Reader> opt = readerRepository.findById(readerId);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy độc giả");
        }
        Reader reader = opt.get();
        if (!PasswordUtil.matches(oldPassword, reader.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu cũ không đúng");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("Mật khẩu mới tối thiểu 6 ký tự");
        }
        reader.setPasswordHash(PasswordUtil.hash(newPassword));
        readerRepository.updatePassword(reader.getReaderId(), reader.getPasswordHash());
    }

    public void updateStaffAvatar(String staffId, String avatarUrl) {
        staffRepository.updateAvatar(staffId, avatarUrl);
    }

    private AuthSession createSession(UserRole role, String userId, String displayName) {
        return createSession(role, userId, displayName, null);
    }

    private AuthSession createSession(UserRole role, String userId, String displayName, String avatar) {
        String token = SessionStore.newToken();
        Instant expires = Instant.now().plus(SESSION_HOURS, ChronoUnit.HOURS);
        AuthSession session = new AuthSession(token, role, userId, displayName, avatar, expires);
        SessionStore.put(session);
        return session;
    }

    private Map<String, Object> toLoginResponse(AuthSession session, String status, String typeOrRole) {
        Map<String, Object> result = new HashMap<>();
        result.put("token", session.getToken());
        result.put("role", session.getRole().name());
        result.put("userId", session.getUserId());
        result.put("displayName", session.getDisplayName());
        result.put("avatar", session.getAvatar());
        result.put("status", status);
        result.put("type", typeOrRole);
        result.put("expiresAt", session.getExpiresAt().toString());
        return result;
    }
}
