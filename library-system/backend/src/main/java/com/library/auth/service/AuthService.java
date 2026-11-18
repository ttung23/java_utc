package com.library.auth.service;

import com.library.auth.dto.ChangePasswordRequest;
import com.library.auth.dto.ForgotPasswordRequest;
import com.library.auth.dto.LoginRequest;
import com.library.auth.dto.LogoutRequest;
import com.library.auth.dto.RefreshRequest;
import com.library.auth.dto.RegisterRequest;
import com.library.auth.dto.ResetPasswordRequest;
import com.library.auth.dto.TokenResponse;
import com.library.auth.dto.UserResponse;
import com.library.auth.dto.VerifyEmailRequest;
import com.library.auth.entity.PasswordReset;
import com.library.auth.entity.RefreshToken;
import com.library.auth.repository.PasswordResetRepository;
import com.library.auth.repository.RefreshTokenRepository;
import com.library.common.config.AuthProperties;
import com.library.common.config.JwtProperties;
import com.library.common.enums.UserRole;
import com.library.common.enums.UserStatus;
import com.library.common.exception.BusinessException;
import com.library.reader.entity.Reader;
import com.library.reader.entity.ReaderType;
import com.library.reader.repository.ReaderRepository;
import com.library.reader.repository.ReaderTypeRepository;
import com.library.security.AuthUserPrincipal;
import com.library.security.JwtService;
import com.library.security.SecurityUtils;
import com.library.security.TokenHashUtil;
import com.library.user.entity.User;
import com.library.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$");

    private final UserRepository userRepository;
    private final ReaderRepository readerRepository;
    private final ReaderTypeRepository readerTypeRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final AuthProperties authProperties;
    private final JavaMailSender mailSender;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        validatePassword(request.getPassword());
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new BusinessException("EMAIL_EXISTS", "Email đã được sử dụng");
        }
        if (StringUtils.hasText(request.getPhone())) {
            userRepository.findByPhone(request.getPhone()).ifPresent(u -> {
                throw new BusinessException("PHONE_EXISTS", "Số điện thoại đã được sử dụng");
            });
        }

        User user = new User();
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPhone(StringUtils.hasText(request.getPhone()) ? request.getPhone().trim() : null);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName().trim());
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.PENDING);
        user.setFailedLoginCount(0);
        user = userRepository.save(user);

        ReaderType readerType = resolveReaderType(request.getReaderTypeCode());
        Reader reader = new Reader();
        reader.setUser(user);
        reader.setReaderCode("RD-" + String.format("%06d", user.getId()));
        reader.setReaderType(readerType);
        reader.setDob(request.getDob());
        reader.setAddress(request.getAddress());
        reader.setIdNumber(request.getIdNumber());
        reader.setStudentCode(request.getStudentCode());
        reader.setRegisteredAt(Instant.now());
        readerRepository.save(reader);

        String verifyToken = UUID.randomUUID().toString();
        saveOneTimeToken(user, verifyToken);
        sendMailQuietly(
                user.getEmail(),
                "Xác thực email thư viện",
                "Xin chào " + user.getFullName() + ",\n\nMã xác thực email: " + verifyToken
                        + "\n\nHoặc gọi POST /api/v1/auth/verify-email với token này."
        );

        return UserResponse.from(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        User user = findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("INVALID_CREDENTIALS", "Email/SĐT hoặc mật khẩu không đúng",
                        HttpStatus.UNAUTHORIZED));

        boolean temporarilyLocked = user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now());
        if (temporarilyLocked || (user.getStatus() == UserStatus.LOCKED && user.getLockedUntil() == null)) {
            throw new BusinessException("ACCOUNT_LOCKED",
                    temporarilyLocked
                            ? "Tài khoản đang bị khóa đến " + user.getLockedUntil()
                            : "Tài khoản đã bị khóa",
                    HttpStatus.FORBIDDEN);
        }
        if (user.getStatus() == UserStatus.PENDING) {
            throw new BusinessException("ACCOUNT_PENDING",
                    "Tài khoản đang chờ duyệt. Vui lòng liên hệ thư viện.", HttpStatus.FORBIDDEN);
        }
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new BusinessException("ACCOUNT_INACTIVE", "Tài khoản đã bị vô hiệu hóa", HttpStatus.FORBIDDEN);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            int failed = user.getFailedLoginCount() + 1;
            user.setFailedLoginCount(failed);
            if (failed >= authProperties.getMaxFailedAttempts()) {
                user.setStatus(UserStatus.LOCKED);
                user.setLockedUntil(Instant.now().plus(authProperties.getLockMinutes(), ChronoUnit.MINUTES));
                user.setFailedLoginCount(0);
            }
            userRepository.save(user);
            throw new BusinessException("INVALID_CREDENTIALS", "Email/SĐT hoặc mật khẩu không đúng",
                    HttpStatus.UNAUTHORIZED);
        }

        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        if (user.getStatus() == UserStatus.LOCKED) {
            user.setStatus(UserStatus.ACTIVE);
        }
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        return issueTokens(user, httpRequest);
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request, HttpServletRequest httpRequest) {
        String hash = TokenHashUtil.sha256(request.getRefreshToken());
        RefreshToken stored = refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(hash)
                .orElseThrow(() -> new BusinessException("INVALID_REFRESH_TOKEN", "Refresh token không hợp lệ",
                        HttpStatus.UNAUTHORIZED));
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            stored.setRevokedAt(Instant.now());
            refreshTokenRepository.save(stored);
            throw new BusinessException("REFRESH_TOKEN_EXPIRED", "Refresh token đã hết hạn", HttpStatus.UNAUTHORIZED);
        }

        stored.setRevokedAt(Instant.now());
        refreshTokenRepository.save(stored);

        User user = stored.getUser();
        return issueTokens(user, httpRequest);
    }

    @Transactional
    public void logout(LogoutRequest request) {
        AuthUserPrincipal principal = SecurityUtils.requireCurrentUser();
        if (request != null && StringUtils.hasText(request.getRefreshToken())) {
            refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(TokenHashUtil.sha256(request.getRefreshToken()))
                    .ifPresent(token -> {
                        if (token.getUser().getId().equals(principal.getId())) {
                            token.setRevokedAt(Instant.now());
                            refreshTokenRepository.save(token);
                        }
                    });
        } else {
            refreshTokenRepository.findByUser_IdAndRevokedAtIsNull(principal.getId()).forEach(token -> {
                token.setRevokedAt(Instant.now());
                refreshTokenRepository.save(token);
            });
        }
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmailIgnoreCase(request.getEmail()).ifPresent(user -> {
            String rawToken = UUID.randomUUID().toString();
            saveOneTimeToken(user, rawToken);
            sendMailQuietly(
                    user.getEmail(),
                    "Đặt lại mật khẩu thư viện",
                    "Mã đặt lại mật khẩu (hết hạn "
                            + authProperties.getResetTokenMinutes() + " phút): " + rawToken
            );
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        validatePassword(request.getNewPassword());
        PasswordReset reset = passwordResetRepository.findByTokenHashAndUsedAtIsNull(
                        TokenHashUtil.sha256(request.getToken()))
                .orElseThrow(() -> new BusinessException("INVALID_TOKEN", "Token không hợp lệ hoặc đã dùng"));
        if (reset.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("TOKEN_EXPIRED", "Token đã hết hạn");
        }
        User user = reset.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        if (user.getStatus() == UserStatus.LOCKED) {
            user.setStatus(UserStatus.ACTIVE);
        }
        userRepository.save(user);
        reset.setUsedAt(Instant.now());
        passwordResetRepository.save(reset);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        AuthUserPrincipal principal = SecurityUtils.requireCurrentUser();
        validatePassword(request.getNewPassword());
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Không tìm thấy người dùng",
                        HttpStatus.NOT_FOUND));
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException("INVALID_OLD_PASSWORD", "Mật khẩu cũ không đúng");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        refreshTokenRepository.findByUser_IdAndRevokedAtIsNull(user.getId()).forEach(token -> {
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
        });
    }

    @Transactional(readOnly = true)
    public UserResponse me() {
        AuthUserPrincipal principal = SecurityUtils.requireCurrentUser();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Không tìm thấy người dùng",
                        HttpStatus.NOT_FOUND));
        return UserResponse.from(user);
    }

    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        PasswordReset reset = passwordResetRepository.findByTokenHashAndUsedAtIsNull(
                        TokenHashUtil.sha256(request.getToken()))
                .orElseThrow(() -> new BusinessException("INVALID_TOKEN", "Token xác thực không hợp lệ"));
        if (reset.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("TOKEN_EXPIRED", "Token xác thực đã hết hạn");
        }
        User user = reset.getUser();
        user.setEmailVerifiedAt(Instant.now());
        userRepository.save(user);
        reset.setUsedAt(Instant.now());
        passwordResetRepository.save(reset);
    }

    private TokenResponse issueTokens(User user, HttpServletRequest httpRequest) {
        String accessToken = jwtService.createAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshRaw = UUID.randomUUID().toString() + "." + UUID.randomUUID();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(TokenHashUtil.sha256(refreshRaw));
        refreshToken.setExpiresAt(Instant.now().plus(jwtProperties.getRefreshTokenDays(), ChronoUnit.DAYS));
        if (httpRequest != null) {
            refreshToken.setUserAgent(httpRequest.getHeader("User-Agent"));
            refreshToken.setIp(httpRequest.getRemoteAddr());
        }
        refreshTokenRepository.save(refreshToken);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshRaw)
                .expiresIn(jwtService.getAccessTokenExpiresInSeconds())
                .tokenType("Bearer")
                .user(UserResponse.from(user))
                .build();
    }

    private Optional<User> findByUsername(String username) {
        String value = username.trim();
        Optional<User> byEmail = userRepository.findByEmailIgnoreCase(value);
        if (byEmail.isPresent()) {
            return byEmail;
        }
        return userRepository.findByPhone(value);
    }

    private ReaderType resolveReaderType(String code) {
        if (StringUtils.hasText(code)) {
            return readerTypeRepository.findByCode(code.trim().toUpperCase())
                    .orElseThrow(() -> new BusinessException("READER_TYPE_NOT_FOUND", "Loại độc giả không tồn tại"));
        }
        return readerTypeRepository.findByCode("STUDENT")
                .orElseThrow(() -> new BusinessException("READER_TYPE_MISSING",
                        "Chưa cấu hình loại độc giả STUDENT. Hãy chạy seed."));
    }

    private void saveOneTimeToken(User user, String rawToken) {
        PasswordReset reset = new PasswordReset();
        reset.setUser(user);
        reset.setTokenHash(TokenHashUtil.sha256(rawToken));
        reset.setExpiresAt(Instant.now().plus(authProperties.getResetTokenMinutes(), ChronoUnit.MINUTES));
        passwordResetRepository.save(reset);
    }

    private void validatePassword(String password) {
        if (password == null || !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new BusinessException("WEAK_PASSWORD",
                    "Mật khẩu phải ≥ 8 ký tự, gồm chữ hoa, chữ thường và số");
        }
    }

    private void sendMailQuietly(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception ex) {
            log.warn("Không gửi được email tới {}: {}", to, ex.getMessage());
        }
    }
}
