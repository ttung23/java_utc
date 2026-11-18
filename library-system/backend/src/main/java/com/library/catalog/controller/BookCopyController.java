package com.library.catalog.controller;

import com.library.catalog.dto.BookCopyResponse;
import com.library.catalog.dto.PagedResult;
import com.library.catalog.dto.UpdateCopyRequest;
import com.library.catalog.service.BookCopyService;
import com.library.common.api.ApiResponse;
import com.library.common.enums.CopyStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/copies")
@RequiredArgsConstructor
@Tag(name = "Copies")
public class BookCopyController {

    private final BookCopyService bookCopyService;

    @GetMapping("/lookup")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Tra cứu bản sao theo barcode")
    public ApiResponse<BookCopyResponse> lookup(@RequestParam String barcode) {
        return ApiResponse.ok(bookCopyService.lookupByBarcode(barcode));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Danh sách bản sao")
    public ApiResponse<List<BookCopyResponse>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) CopyStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PagedResult<BookCopyResponse> result = bookCopyService.list(q, status, page, size);
        ApiResponse<List<BookCopyResponse>> response = ApiResponse.ok(result.getItems());
        response.setMeta(result.meta());
        return response;
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Cập nhật bản sao (trạng thái/vị trí/tình trạng/ghi chú)")
    public ApiResponse<BookCopyResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCopyRequest request
    ) {
        return ApiResponse.ok(bookCopyService.update(id, request), "Cập nhật bản sao thành công");
    }
}
