package com.library.reader.service;

import com.library.common.enums.CardStatus;
import com.library.common.enums.UserRole;
import com.library.common.enums.UserStatus;
import com.library.common.exception.BusinessException;
import com.library.reader.dto.BlacklistRequest;
import com.library.reader.dto.BlockReaderRequest;
import com.library.reader.dto.CreateReaderRequest;
import com.library.reader.dto.EligibilityResponse;
import com.library.reader.dto.PagedResult;
import com.library.reader.dto.ReaderDetailResponse;
import com.library.reader.dto.ReaderListItem;
import com.library.reader.dto.UpdateReaderRequest;
import com.library.reader.entity.LibraryCard;
import com.library.reader.entity.Reader;
import com.library.reader.entity.ReaderType;
import com.library.reader.repository.LibraryCardRepository;
import com.library.reader.repository.ReaderRepository;
import com.library.reader.repository.ReaderSpecifications;
import com.library.reader.support.ReaderPageSupport;
import com.library.security.AuthUserPrincipal;
import com.library.security.SecurityUtils;
import com.library.user.entity.User;
import com.library.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReaderService {

    private static final String DEFAULT_PASSWORD = "User@123";

    private final ReaderRepository readerRepository;
    private final LibraryCardRepository libraryCardRepository;
    private final UserRepository userRepository;
    private final ReaderTypeService readerTypeService;
    private final LibraryCardService libraryCardService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public PagedResult<ReaderListItem> list(
            String q,
            Long readerTypeId,
            CardStatus cardStatus,
            Boolean blacklisted,
            int page,
            int size,
            String sort
    ) {
        Pageable pageable = ReaderPageSupport.pageable(page, size, sort, "registeredAt");
        Page<Reader> result = readerRepository.findAll(
                ReaderSpecifications.withFilters(q, readerTypeId, cardStatus, blacklisted),
                pageable
        );
        List<ReaderListItem> items = result.getContent().stream().map(this::toListItem).toList();
        return new PagedResult<>(items, result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ReaderDetailResponse getById(Long id) {
        return toDetail(requireActive(id));
    }

    @Transactional(readOnly = true)
    public ReaderDetailResponse getMe() {
        AuthUserPrincipal principal = SecurityUtils.requireCurrentUser();
        Reader reader = readerRepository.findByUserIdAndDeletedAtIsNull(principal.getId())
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy hồ sơ độc giả", HttpStatus.NOT_FOUND));
        return toDetail(reader);
    }

    @Transactional(readOnly = true)
    public ReaderDetailResponse lookupByCard(String card) {
        if (!StringUtils.hasText(card)) {
            throw new BusinessException("INVALID_REQUEST", "Thiếu mã thẻ");
        }
        LibraryCard libraryCard = libraryCardRepository.findByCardNumberOrBarcode(card.trim())
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy thẻ", HttpStatus.NOT_FOUND));
        return toDetail(libraryCard.getReader());
    }

    public ReaderDetailResponse create(CreateReaderRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("EMAIL_EXISTS", "Email đã được sử dụng");
        }
        if (StringUtils.hasText(request.getPhone())) {
            userRepository.findByPhone(request.getPhone().trim()).ifPresent(u -> {
                throw new BusinessException("PHONE_EXISTS", "Số điện thoại đã được sử dụng");
            });
        }

        ReaderType readerType = readerTypeService.requireActive(request.getReaderTypeId());
        if (!readerType.isActive()) {
            throw new BusinessException("READER_TYPE_INACTIVE", "Loại độc giả không còn hiệu lực");
        }

        String rawPassword = StringUtils.hasText(request.getPassword()) ? request.getPassword() : DEFAULT_PASSWORD;

        User user = new User();
        user.setEmail(email);
        user.setPhone(StringUtils.hasText(request.getPhone()) ? request.getPhone().trim() : null);
        user.setFullName(request.getFullName().trim());
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setFailedLoginCount(0);
        user.setCreatedBy(SecurityUtils.currentUserIdOrNull());
        user.setUpdatedBy(SecurityUtils.currentUserIdOrNull());
        user = userRepository.save(user);

        Reader reader = new Reader();
        reader.setUser(user);
        reader.setReaderCode(String.format("RD-%06d", user.getId()));
        reader.setReaderType(readerType);
        reader.setDob(request.getDob());
        reader.setGender(request.getGender());
        reader.setAddress(request.getAddress());
        reader.setIdNumber(request.getIdNumber());
        reader.setStudentCode(request.getStudentCode());
        reader.setOrganization(request.getOrganization());
        reader.setNote(request.getNote());
        reader.setRegisteredAt(Instant.now());
        reader.setCreatedBy(SecurityUtils.currentUserIdOrNull());
        reader.setUpdatedBy(SecurityUtils.currentUserIdOrNull());
        reader = readerRepository.save(reader);

        boolean issueCard = request.getIssueCard() == null || request.getIssueCard();
        if (issueCard) {
            libraryCardService.createActiveCard(reader, readerType.getCardFee());
        }

        return toDetail(reader);
    }

    public ReaderDetailResponse update(Long id, UpdateReaderRequest request) {
        Reader reader = requireActive(id);
        User user = reader.getUser();

        if (StringUtils.hasText(request.getPhone())) {
            String phone = request.getPhone().trim();
            userRepository.findByPhone(phone).ifPresent(existing -> {
                if (!existing.getId().equals(user.getId())) {
                    throw new BusinessException("PHONE_EXISTS", "Số điện thoại đã được sử dụng");
                }
            });
            user.setPhone(phone);
        } else {
            user.setPhone(null);
        }

        user.setFullName(request.getFullName().trim());
        user.setUpdatedBy(SecurityUtils.currentUserIdOrNull());
        userRepository.save(user);

        ReaderType readerType = readerTypeService.requireActive(request.getReaderTypeId());
        reader.setReaderType(readerType);
        reader.setDob(request.getDob());
        reader.setGender(request.getGender());
        reader.setAddress(request.getAddress());
        reader.setIdNumber(request.getIdNumber());
        reader.setStudentCode(request.getStudentCode());
        reader.setOrganization(request.getOrganization());
        reader.setNote(request.getNote());
        reader.setEmergencyContact(request.getEmergencyContact());
        reader.setUpdatedBy(SecurityUtils.currentUserIdOrNull());
        return toDetail(readerRepository.save(reader));
    }

    public ReaderDetailResponse block(Long id, BlockReaderRequest request) {
        Reader reader = requireActive(id);
        User user = reader.getUser();
        user.setStatus(UserStatus.LOCKED);
        user.setUpdatedBy(SecurityUtils.currentUserIdOrNull());
        if (request != null && StringUtils.hasText(request.getReason())) {
            String note = reader.getNote() == null ? "" : reader.getNote() + "\n";
            reader.setNote(note + "[KHÓA] " + request.getReason().trim());
            reader.setUpdatedBy(SecurityUtils.currentUserIdOrNull());
        }
        userRepository.save(user);
        return toDetail(readerRepository.save(reader));
    }

    public ReaderDetailResponse unblock(Long id) {
        Reader reader = requireActive(id);
        User user = reader.getUser();
        user.setStatus(UserStatus.ACTIVE);
        user.setLockedUntil(null);
        user.setFailedLoginCount(0);
        user.setUpdatedBy(SecurityUtils.currentUserIdOrNull());
        userRepository.save(user);
        reader.setUpdatedBy(SecurityUtils.currentUserIdOrNull());
        return toDetail(readerRepository.save(reader));
    }

    public ReaderDetailResponse blacklist(Long id, BlacklistRequest request) {
        Reader reader = requireActive(id);
        boolean flagged = Boolean.TRUE.equals(request.getBlacklisted());
        reader.setBlacklisted(flagged);
        if (flagged) {
            if (!StringUtils.hasText(request.getReason())) {
                throw new BusinessException("BLACKLIST_REASON_REQUIRED", "Cần nhập lý do đưa vào danh sách đen");
            }
            reader.setBlacklistReason(request.getReason().trim());
        } else {
            reader.setBlacklistReason(null);
        }
        reader.setUpdatedBy(SecurityUtils.currentUserIdOrNull());
        return toDetail(readerRepository.save(reader));
    }

    @Transactional(readOnly = true)
    public EligibilityResponse eligibility(Long id) {
        Reader reader = requireActive(id);
        User user = reader.getUser();
        List<EligibilityResponse.ViolationItem> violations = new ArrayList<>();

        LibraryCard activeCard = libraryCardRepository
                .findFirstByReader_IdAndStatusAndDeletedAtIsNullOrderByIssuedAtDesc(reader.getId(), CardStatus.ACTIVE)
                .orElse(null);

        if (activeCard == null) {
            violations.add(EligibilityResponse.ViolationItem.builder()
                    .code("CARD_INACTIVE")
                    .message("Không có thẻ ACTIVE")
                    .build());
        } else if (activeCard.getExpiresAt() != null && activeCard.getExpiresAt().isBefore(Instant.now())) {
            violations.add(EligibilityResponse.ViolationItem.builder()
                    .code("CARD_EXPIRED")
                    .message("Thẻ đã hết hạn")
                    .build());
        }

        if (user.getStatus() == UserStatus.LOCKED || user.getStatus() == UserStatus.INACTIVE) {
            violations.add(EligibilityResponse.ViolationItem.builder()
                    .code("ACCOUNT_LOCKED")
                    .message("Tài khoản bị khóa hoặc không hoạt động")
                    .build());
        }

        if (reader.isBlacklisted()) {
            violations.add(EligibilityResponse.ViolationItem.builder()
                    .code("READER_BLACKLISTED")
                    .message("Độc giả trong danh sách đen")
                    .build());
        }

        return EligibilityResponse.builder()
                .eligible(violations.isEmpty())
                .violations(violations)
                .build();
    }

    private Reader requireActive(Long id) {
        return readerRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy độc giả", HttpStatus.NOT_FOUND));
    }

    private ReaderListItem toListItem(Reader reader) {
        User user = reader.getUser();
        ReaderType type = reader.getReaderType();
        LibraryCard card = libraryCardService.findLatestActiveOrAny(reader.getId());
        return ReaderListItem.builder()
                .id(reader.getId())
                .readerCode(reader.getReaderCode())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .readerTypeCode(type.getCode())
                .readerTypeName(type.getName())
                .cardNumber(card != null ? card.getCardNumber() : null)
                .cardStatus(card != null ? card.getStatus() : null)
                .cardExpiresAt(card != null ? card.getExpiresAt() : null)
                .blacklisted(reader.isBlacklisted())
                .userStatus(user.getStatus())
                .build();
    }

    private ReaderDetailResponse toDetail(Reader reader) {
        User user = reader.getUser();
        ReaderType type = reader.getReaderType();
        LibraryCard card = libraryCardService.findLatestActiveOrAny(reader.getId());
        return ReaderDetailResponse.builder()
                .id(reader.getId())
                .userId(user.getId())
                .readerCode(reader.getReaderCode())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .userStatus(user.getStatus())
                .readerTypeId(type.getId())
                .readerTypeCode(type.getCode())
                .readerTypeName(type.getName())
                .dob(reader.getDob())
                .gender(reader.getGender())
                .address(reader.getAddress())
                .idNumber(reader.getIdNumber())
                .studentCode(reader.getStudentCode())
                .organization(reader.getOrganization())
                .emergencyContact(reader.getEmergencyContact())
                .note(reader.getNote())
                .blacklisted(reader.isBlacklisted())
                .blacklistReason(reader.getBlacklistReason())
                .noShowCount(reader.getNoShowCount())
                .registeredAt(reader.getRegisteredAt())
                .cardId(card != null ? card.getId() : null)
                .cardNumber(card != null ? card.getCardNumber() : null)
                .cardBarcode(card != null ? card.getBarcode() : null)
                .cardStatus(card != null ? card.getStatus() : null)
                .cardIssuedAt(card != null ? card.getIssuedAt() : null)
                .cardExpiresAt(card != null ? card.getExpiresAt() : null)
                .createdAt(reader.getCreatedAt())
                .updatedAt(reader.getUpdatedAt())
                .build();
    }
}
