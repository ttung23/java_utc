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
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import java.util.Map;

@RestController
@RequestMapping("/api/v1/copies")
@RequiredArgsConstructor
@Tag(name = "Copies")
public class BookCopyController {

    private final BookCopyService bookCopyService;

    @GetMapping("/lookup")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Tra cứu theo barcode")
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
    @Operation(summary = "Cập nhật bản sao")
    public ApiResponse<BookCopyResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCopyRequest request
    ) {
        return ApiResponse.ok(bookCopyService.update(id, request), "Cập nhật bản sao thành công");
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Lịch sử vòng đời bản sao")
    public ApiResponse<List<Map<String, Object>>> history(@PathVariable Long id) {
        return ApiResponse.ok(bookCopyService.history(id));
    }

    @PostMapping("/labels")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "In tem barcode PDF")
    public ResponseEntity<byte[]> labels(@RequestBody(required = false) LabelsReq req) {
        byte[] pdf = bookCopyService.labelsPdf(req == null ? null : req.getCopyIds());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=copy-labels.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @Data
    public static class LabelsReq {
        private List<Long> copyIds;
    }
}
