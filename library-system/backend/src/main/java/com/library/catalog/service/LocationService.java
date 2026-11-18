package com.library.catalog.service;

import com.library.catalog.dto.LocationRequest;
import com.library.catalog.dto.LocationResponse;
import com.library.catalog.dto.PagedResult;
import com.library.catalog.entity.Location;
import com.library.catalog.repository.BookCopyRepository;
import com.library.catalog.repository.LocationRepository;
import com.library.catalog.support.CatalogPageSupport;
import com.library.common.exception.BusinessException;
import com.library.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class LocationService {

    private final LocationRepository locationRepository;
    private final BookCopyRepository bookCopyRepository;

    @Transactional(readOnly = true)
    public PagedResult<LocationResponse> list(String q, int page, int size, String sort) {
        Pageable pageable = CatalogPageSupport.pageable(page, size, sort, "code");
        Page<Location> result;
        if (StringUtils.hasText(q)) {
            result = locationRepository.findByDeletedAtIsNullAndCodeContainingIgnoreCase(q.trim(), pageable);
        } else {
            result = locationRepository.findByDeletedAtIsNull(pageable);
        }
        List<LocationResponse> items = result.getContent().stream().map(this::toResponse).toList();
        return new PagedResult<>(items, result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public LocationResponse getById(Long id) {
        return toResponse(requireActive(id));
    }

    public LocationResponse create(LocationRequest request) {
        validateUniqueCode(request.getCode(), null);
        Location location = new Location();
        apply(location, request);
        location.setCreatedBy(SecurityUtils.currentUserIdOrNull());
        location.setUpdatedBy(SecurityUtils.currentUserIdOrNull());
        return toResponse(locationRepository.save(location));
    }

    public LocationResponse update(Long id, LocationRequest request) {
        Location location = requireActive(id);
        validateUniqueCode(request.getCode(), id);
        apply(location, request);
        location.setUpdatedBy(SecurityUtils.currentUserIdOrNull());
        return toResponse(locationRepository.save(location));
    }

    public void delete(Long id) {
        Location location = requireActive(id);
        if (bookCopyRepository.countByLocation_Id(id) > 0) {
            throw new BusinessException("LOCATION_IN_USE", "Vị trí đang chứa bản sao sách, không thể xóa");
        }
        location.setDeletedAt(Instant.now());
        location.setUpdatedBy(SecurityUtils.currentUserIdOrNull());
        locationRepository.save(location);
    }

    private void apply(Location location, LocationRequest request) {
        location.setCode(request.getCode().trim());
        location.setZone(request.getZone());
        location.setFloor(request.getFloor());
        location.setRow(request.getRow());
        location.setShelf(request.getShelf());
        location.setSlot(request.getSlot());
        location.setCapacity(request.getCapacity());
        location.setNote(request.getNote());
    }

    private void validateUniqueCode(String code, Long excludeId) {
        String trimmed = code.trim();
        // DB unique on code includes soft-deleted rows
        var existing = locationRepository.findByCode(trimmed);
        if (existing.isPresent() && (excludeId == null || !existing.get().getId().equals(excludeId))) {
            throw new BusinessException("LOCATION_CODE_EXISTS", "Mã vị trí đã tồn tại");
        }
    }

    private Location requireActive(Long id) {
        return locationRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy vị trí", HttpStatus.NOT_FOUND));
    }

    private LocationResponse toResponse(Location location) {
        return LocationResponse.builder()
                .id(location.getId())
                .code(location.getCode())
                .zone(location.getZone())
                .floor(location.getFloor())
                .row(location.getRow())
                .shelf(location.getShelf())
                .slot(location.getSlot())
                .capacity(location.getCapacity())
                .note(location.getNote())
                .createdAt(location.getCreatedAt())
                .updatedAt(location.getUpdatedAt())
                .build();
    }
}
