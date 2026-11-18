package com.library.reader.controller;

import com.library.common.api.ApiResponse;
import com.library.reader.dto.ReaderTypeRequest;
import com.library.reader.dto.ReaderTypeResponse;
import com.library.reader.service.ReaderTypeService;
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
@RequestMapping("/api/v1/reader-types")
@RequiredArgsConstructor
@Tag(name = "Reader Types")
public class ReaderTypeController {

    private final ReaderTypeService readerTypeService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Danh sách loại độc giả")
    public ApiResponse<List<ReaderTypeResponse>> list(
            @RequestParam(required = false) Boolean active
    ) {
        return ApiResponse.ok(readerTypeService.list(active));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Chi tiết loại độc giả")
    public ApiResponse<ReaderTypeResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(readerTypeService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo loại độc giả")
    public ApiResponse<ReaderTypeResponse> create(@Valid @RequestBody ReaderTypeRequest request) {
        return ApiResponse.ok(readerTypeService.create(request), "Tạo loại độc giả thành công");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật loại độc giả")
    public ApiResponse<ReaderTypeResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ReaderTypeRequest request
    ) {
        return ApiResponse.ok(readerTypeService.update(id, request), "Cập nhật loại độc giả thành công");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa mềm loại độc giả")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        readerTypeService.delete(id);
        return ApiResponse.ok(null, "Xóa loại độc giả thành công");
    }
}
