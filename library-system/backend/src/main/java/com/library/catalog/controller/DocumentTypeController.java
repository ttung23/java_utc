package com.library.catalog.controller;

import com.library.catalog.dto.DocumentTypeRequest;
import com.library.catalog.dto.DocumentTypeResponse;
import com.library.catalog.dto.PagedResult;
import com.library.catalog.service.DocumentTypeService;
import com.library.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/document-types")
@RequiredArgsConstructor
@Tag(name = "Document Types")
public class DocumentTypeController {

    private final DocumentTypeService documentTypeService;

    @GetMapping
    @Operation(summary = "Danh sách loại tài liệu (public)")
    public ApiResponse<List<DocumentTypeResponse>> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        PagedResult<DocumentTypeResponse> result = documentTypeService.list(q, page, size, sort);
        ApiResponse<List<DocumentTypeResponse>> response = ApiResponse.ok(result.getItems());
        response.setMeta(result.meta());
        return response;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết loại tài liệu")
    public ApiResponse<DocumentTypeResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(documentTypeService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Tạo loại tài liệu")
    public ApiResponse<DocumentTypeResponse> create(@Valid @RequestBody DocumentTypeRequest request) {
        return ApiResponse.ok(documentTypeService.create(request), "Tạo loại tài liệu thành công");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Cập nhật loại tài liệu")
    public ApiResponse<DocumentTypeResponse> update(@PathVariable Long id, @Valid @RequestBody DocumentTypeRequest request) {
        return ApiResponse.ok(documentTypeService.update(id, request), "Cập nhật loại tài liệu thành công");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Xóa cứng loại tài liệu (chỉ khi chưa dùng)")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        documentTypeService.delete(id);
        return ApiResponse.ok(null, "Xóa loại tài liệu thành công");
    }
}
