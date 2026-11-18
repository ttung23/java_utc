package com.library.reader.service;

import com.library.common.exception.BusinessException;
import com.library.reader.dto.ReaderTypeRequest;
import com.library.reader.dto.ReaderTypeResponse;
import com.library.reader.entity.ReaderType;
import com.library.reader.repository.ReaderRepository;
import com.library.reader.repository.ReaderTypeRepository;
import com.library.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReaderTypeService {

    private final ReaderTypeRepository readerTypeRepository;
    private final ReaderRepository readerRepository;

    @Transactional(readOnly = true)
    public List<ReaderTypeResponse> list(Boolean activeOnly) {
        List<ReaderType> types = Boolean.TRUE.equals(activeOnly)
                ? readerTypeRepository.findByDeletedAtIsNullAndActiveTrueOrderByCodeAsc()
                : readerTypeRepository.findByDeletedAtIsNullOrderByCodeAsc();
        return types.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ReaderTypeResponse getById(Long id) {
        return toResponse(requireActive(id));
    }

    public ReaderTypeResponse create(ReaderTypeRequest request) {
        String code = request.getCode().trim().toUpperCase();
        if (readerTypeRepository.existsByCodeIgnoreCaseAndDeletedAtIsNull(code)) {
            throw new BusinessException("READER_TYPE_CODE_EXISTS", "Mã loại độc giả đã tồn tại");
        }
        ReaderType type = new ReaderType();
        apply(type, request, code);
        type.setCreatedBy(SecurityUtils.currentUserIdOrNull());
        type.setUpdatedBy(SecurityUtils.currentUserIdOrNull());
        return toResponse(readerTypeRepository.save(type));
    }

    public ReaderTypeResponse update(Long id, ReaderTypeRequest request) {
        ReaderType type = requireActive(id);
        String code = request.getCode().trim().toUpperCase();
        if (readerTypeRepository.existsByCodeIgnoreCaseAndDeletedAtIsNullAndIdNot(code, id)) {
            throw new BusinessException("READER_TYPE_CODE_EXISTS", "Mã loại độc giả đã tồn tại");
        }
        apply(type, request, code);
        type.setUpdatedBy(SecurityUtils.currentUserIdOrNull());
        return toResponse(readerTypeRepository.save(type));
    }

    public void delete(Long id) {
        ReaderType type = requireActive(id);
        if (readerRepository.countByReaderType_IdAndDeletedAtIsNull(id) > 0) {
            throw new BusinessException("READER_TYPE_IN_USE", "Loại độc giả đang được sử dụng, không thể xóa");
        }
        type.setDeletedAt(Instant.now());
        type.setActive(false);
        type.setUpdatedBy(SecurityUtils.currentUserIdOrNull());
        readerTypeRepository.save(type);
    }

    public ReaderType requireActive(Long id) {
        return readerTypeRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy loại độc giả", HttpStatus.NOT_FOUND));
    }

    private void apply(ReaderType type, ReaderTypeRequest request, String code) {
        type.setCode(code);
        type.setName(request.getName().trim());
        type.setMaxBooks(request.getMaxBooks());
        type.setLoanDays(request.getLoanDays());
        type.setMaxRenewals(request.getMaxRenewals());
        type.setRenewDays(request.getRenewDays());
        type.setMaxReservations(request.getMaxReservations());
        type.setCardFee(request.getCardFee() != null ? request.getCardFee() : BigDecimal.ZERO);
        type.setCardValidityMonths(request.getCardValidityMonths());
        type.setActive(request.getActive() == null || request.getActive());
    }

    private ReaderTypeResponse toResponse(ReaderType type) {
        return ReaderTypeResponse.builder()
                .id(type.getId())
                .code(type.getCode())
                .name(type.getName())
                .maxBooks(type.getMaxBooks())
                .loanDays(type.getLoanDays())
                .maxRenewals(type.getMaxRenewals())
                .renewDays(type.getRenewDays())
                .maxReservations(type.getMaxReservations())
                .cardFee(type.getCardFee())
                .cardValidityMonths(type.getCardValidityMonths())
                .active(type.isActive())
                .createdAt(type.getCreatedAt())
                .updatedAt(type.getUpdatedAt())
                .build();
    }
}
