package com.library.reader.service;

import com.library.common.enums.CardStatus;
import com.library.common.exception.BusinessException;
import com.library.reader.dto.CardResponse;
import com.library.reader.dto.IssueCardRequest;
import com.library.reader.dto.PagedResult;
import com.library.reader.entity.LibraryCard;
import com.library.reader.entity.Reader;
import com.library.reader.entity.ReaderType;
import com.library.reader.repository.LibraryCardRepository;
import com.library.reader.repository.LibraryCardSpecifications;
import com.library.reader.repository.ReaderRepository;
import com.library.reader.support.ReaderPageSupport;
import com.library.security.SecurityUtils;
import com.library.user.entity.User;
import com.library.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class LibraryCardService {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private final LibraryCardRepository libraryCardRepository;
    private final ReaderRepository readerRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PagedResult<CardResponse> list(Long readerId, CardStatus status, String q, int page, int size, String sort) {
        Pageable pageable = ReaderPageSupport.pageable(page, size, sort, "issuedAt");
        Page<LibraryCard> result = libraryCardRepository.findAll(
                LibraryCardSpecifications.withFilters(readerId, status, q), pageable);
        List<CardResponse> items = result.getContent().stream().map(this::toResponse).toList();
        return new PagedResult<>(items, result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public CardResponse getById(Long id) {
        return toResponse(requireActive(id));
    }

    public CardResponse issue(IssueCardRequest request) {
        Reader reader = readerRepository.findByIdAndDeletedAtIsNull(request.getReaderId())
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy độc giả", HttpStatus.NOT_FOUND));
        if (libraryCardRepository.existsByReader_IdAndStatusAndDeletedAtIsNull(reader.getId(), CardStatus.ACTIVE)) {
            throw new BusinessException("CARD_EXISTS", "Độc giả đã có thẻ ACTIVE");
        }
        LibraryCard card = createActiveCard(reader, reader.getReaderType().getCardFee());
        return toResponse(card);
    }

    public CardResponse renew(Long id) {
        LibraryCard card = requireActive(id);
        if (card.getStatus() == CardStatus.LOST || card.getStatus() == CardStatus.CANCELLED) {
            throw new BusinessException("CARD_NOT_RENEWABLE", "Thẻ mất/hủy không thể gia hạn");
        }
        ReaderType type = card.getReader().getReaderType();
        Instant base = card.getExpiresAt() != null && card.getExpiresAt().isAfter(Instant.now())
                ? card.getExpiresAt()
                : Instant.now();
        card.setExpiresAt(base.plus(type.getCardValidityMonths() * 30L, ChronoUnit.DAYS));
        card.setStatus(CardStatus.ACTIVE);
        card.setFeePaid(type.getCardFee() != null ? type.getCardFee() : BigDecimal.ZERO);
        card.setUpdatedBy(SecurityUtils.currentUserIdOrNull());
        return toResponse(libraryCardRepository.save(card));
    }

    public CardResponse reportLost(Long id) {
        LibraryCard oldCard = requireActive(id);
        if (oldCard.getStatus() == CardStatus.LOST || oldCard.getStatus() == CardStatus.CANCELLED) {
            throw new BusinessException("CARD_ALREADY_LOST", "Thẻ đã mất hoặc đã hủy");
        }
        Reader reader = oldCard.getReader();
        LibraryCard newCard = createActiveCard(reader, reader.getReaderType().getCardFee());

        oldCard.setStatus(CardStatus.LOST);
        oldCard.setReplacedByCard(newCard);
        oldCard.setSuspendedReason("Báo mất thẻ");
        oldCard.setUpdatedBy(SecurityUtils.currentUserIdOrNull());
        libraryCardRepository.save(oldCard);

        return toResponse(newCard);
    }

    public LibraryCard createActiveCard(Reader reader, BigDecimal feePaid) {
        String cardNumber = nextCardNumber();
        LibraryCard card = new LibraryCard();
        card.setReader(reader);
        card.setCardNumber(cardNumber);
        card.setBarcode(cardNumber);
        card.setIssuedAt(Instant.now());
        int months = reader.getReaderType().getCardValidityMonths();
        card.setExpiresAt(Instant.now().plus(months * 30L, ChronoUnit.DAYS));
        card.setStatus(CardStatus.ACTIVE);
        card.setFeePaid(feePaid != null ? feePaid : BigDecimal.ZERO);
        Long issuerId = SecurityUtils.currentUserIdOrNull();
        if (issuerId != null) {
            userRepository.findById(issuerId).ifPresent(card::setIssuedBy);
        }
        card.setCreatedBy(issuerId);
        card.setUpdatedBy(issuerId);
        card = libraryCardRepository.save(card);
        // barcode fallback CARD-{id} if needed for uniqueness with seed style
        if (card.getBarcode() == null || card.getBarcode().isBlank()) {
            card.setBarcode("CARD-" + card.getId());
            card = libraryCardRepository.save(card);
        }
        return card;
    }

    public String nextCardNumber() {
        int year = LocalDate.now(VN).getYear();
        String prefix = "TV-" + year + "-";
        List<String> latest = libraryCardRepository.findCardNumbersByPrefix(prefix, PageRequest.of(0, 1));
        int seq = 1;
        if (!latest.isEmpty()) {
            String last = latest.get(0);
            String suffix = last.substring(prefix.length());
            try {
                seq = Integer.parseInt(suffix) + 1;
            } catch (NumberFormatException ignored) {
                seq = 1;
            }
        }
        return String.format("%s%06d", prefix, seq);
    }

    @Transactional(readOnly = true)
    public LibraryCard findLatestActiveOrAny(Long readerId) {
        return libraryCardRepository
                .findFirstByReader_IdAndStatusAndDeletedAtIsNullOrderByIssuedAtDesc(readerId, CardStatus.ACTIVE)
                .orElseGet(() -> libraryCardRepository.findByReader_IdAndDeletedAtIsNullOrderByIssuedAtDesc(readerId)
                        .stream().findFirst().orElse(null));
    }

    private LibraryCard requireActive(Long id) {
        return libraryCardRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy thẻ", HttpStatus.NOT_FOUND));
    }

    private CardResponse toResponse(LibraryCard card) {
        Reader reader = card.getReader();
        User user = reader.getUser();
        return CardResponse.builder()
                .id(card.getId())
                .readerId(reader.getId())
                .readerCode(reader.getReaderCode())
                .readerFullName(user != null ? user.getFullName() : null)
                .cardNumber(card.getCardNumber())
                .barcode(card.getBarcode())
                .status(card.getStatus())
                .issuedAt(card.getIssuedAt())
                .expiresAt(card.getExpiresAt())
                .feePaid(card.getFeePaid())
                .replacedByCardId(card.getReplacedByCard() != null ? card.getReplacedByCard().getId() : null)
                .suspendedReason(card.getSuspendedReason())
                .createdAt(card.getCreatedAt())
                .updatedAt(card.getUpdatedAt())
                .build();
    }
}
