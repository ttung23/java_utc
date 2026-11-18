package com.library.catalog.controller;

import com.library.catalog.dto.LanguageRequest;
import com.library.catalog.dto.LanguageResponse;
import com.library.catalog.dto.PagedResult;
import com.library.catalog.service.LanguageService;
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
@RequestMapping("/api/v1/languages")
@RequiredArgsConstructor
@Tag(name = "Languages")
public class LanguageController {

    private final LanguageService languageService;

    @GetMapping
    @Operation(summary = "Danh sách ngôn ngữ (public)")
    public ApiResponse<List<LanguageResponse>> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        PagedResult<LanguageResponse> result = languageService.list(q, page, size, sort);
        ApiResponse<List<LanguageResponse>> response = ApiResponse.ok(result.getItems());
        response.setMeta(result.meta());
        return response;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết ngôn ngữ")
    public ApiResponse<LanguageResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(languageService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Tạo ngôn ngữ")
    public ApiResponse<LanguageResponse> create(@Valid @RequestBody LanguageRequest request) {
        return ApiResponse.ok(languageService.create(request), "Tạo ngôn ngữ thành công");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Cập nhật ngôn ngữ")
    public ApiResponse<LanguageResponse> update(@PathVariable Long id, @Valid @RequestBody LanguageRequest request) {
        return ApiResponse.ok(languageService.update(id, request), "Cập nhật ngôn ngữ thành công");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Xóa cứng ngôn ngữ (chỉ khi chưa dùng)")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        languageService.delete(id);
        return ApiResponse.ok(null, "Xóa ngôn ngữ thành công");
    }
}
