package com.library.catalog.controller;

import com.library.catalog.dto.CategoryRequest;
import com.library.catalog.dto.CategoryResponse;
import com.library.catalog.dto.PagedResult;
import com.library.catalog.service.CategoryService;
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
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "Danh sách thể loại (public); tree=true trả cây cha-con")
    public ApiResponse<List<CategoryResponse>> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "false") boolean tree
    ) {
        if (tree) {
            List<CategoryResponse> data = categoryService.tree();
            ApiResponse<List<CategoryResponse>> response = ApiResponse.ok(data);
            response.setMeta(java.util.Map.of("page", 0, "limit", data.size(), "total", data.size()));
            return response;
        }
        PagedResult<CategoryResponse> result = categoryService.list(q, page, size, sort);
        ApiResponse<List<CategoryResponse>> response = ApiResponse.ok(result.getItems());
        response.setMeta(result.meta());
        return response;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết thể loại")
    public ApiResponse<CategoryResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(categoryService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Tạo thể loại")
    public ApiResponse<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        return ApiResponse.ok(categoryService.create(request), "Tạo thể loại thành công");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Cập nhật thể loại")
    public ApiResponse<CategoryResponse> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.ok(categoryService.update(id, request), "Cập nhật thể loại thành công");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Xóa mềm thể loại")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ApiResponse.ok(null, "Xóa thể loại thành công");
    }
}
