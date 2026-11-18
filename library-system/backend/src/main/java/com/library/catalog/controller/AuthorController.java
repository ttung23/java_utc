package com.library.catalog.controller;

import com.library.catalog.dto.AuthorRequest;
import com.library.catalog.dto.AuthorResponse;
import com.library.catalog.dto.PagedResult;
import com.library.catalog.service.AuthorService;
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
@RequestMapping("/api/v1/authors")
@RequiredArgsConstructor
@Tag(name = "Authors")
public class AuthorController {

    private final AuthorService authorService;

    @GetMapping
    @Operation(summary = "Danh sách tác giả (public)")
    public ApiResponse<List<AuthorResponse>> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        PagedResult<AuthorResponse> result = authorService.list(q, page, size, sort);
        ApiResponse<List<AuthorResponse>> response = ApiResponse.ok(result.getItems());
        response.setMeta(result.meta());
        return response;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết tác giả")
    public ApiResponse<AuthorResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(authorService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Tạo tác giả")
    public ApiResponse<AuthorResponse> create(@Valid @RequestBody AuthorRequest request) {
        return ApiResponse.ok(authorService.create(request), "Tạo tác giả thành công");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Cập nhật tác giả")
    public ApiResponse<AuthorResponse> update(@PathVariable Long id, @Valid @RequestBody AuthorRequest request) {
        return ApiResponse.ok(authorService.update(id, request), "Cập nhật tác giả thành công");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Xóa mềm tác giả")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        authorService.delete(id);
        return ApiResponse.ok(null, "Xóa tác giả thành công");
    }
}
