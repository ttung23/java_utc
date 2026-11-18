package com.library.reader.controller;

import com.library.common.api.ApiResponse;
import com.library.common.enums.CardStatus;
import com.library.reader.dto.CardResponse;
import com.library.reader.dto.IssueCardRequest;
import com.library.reader.dto.PagedResult;
import com.library.reader.service.LibraryCardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
@Tag(name = "Library Cards")
public class LibraryCardController {

    private final LibraryCardService libraryCardService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Danh sách thẻ thư viện")
    public ApiResponse<List<CardResponse>> list(
            @RequestParam(required = false) Long readerId,
            @RequestParam(required = false) CardStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        PagedResult<CardResponse> result = libraryCardService.list(readerId, status, q, page, size, sort);
        ApiResponse<List<CardResponse>> response = ApiResponse.ok(result.getItems());
        response.setMeta(result.meta());
        return response;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Cấp thẻ mới cho độc giả")
    public ApiResponse<CardResponse> issue(@Valid @RequestBody IssueCardRequest request) {
        return ApiResponse.ok(libraryCardService.issue(request), "Cấp thẻ thành công");
    }

    @PatchMapping("/{id}/renew")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Gia hạn thẻ")
    public ApiResponse<CardResponse> renew(@PathVariable Long id) {
        return ApiResponse.ok(libraryCardService.renew(id), "Gia hạn thẻ thành công");
    }

    @PatchMapping("/{id}/report-lost")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Báo mất thẻ và cấp thẻ mới")
    public ApiResponse<CardResponse> reportLost(@PathVariable Long id) {
        return ApiResponse.ok(libraryCardService.reportLost(id), "Đã báo mất và cấp thẻ mới");
    }
}
