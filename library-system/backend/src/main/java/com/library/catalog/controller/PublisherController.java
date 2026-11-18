package com.library.catalog.controller;

import com.library.catalog.dto.PagedResult;
import com.library.catalog.dto.PublisherRequest;
import com.library.catalog.dto.PublisherResponse;
import com.library.catalog.service.PublisherService;
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
@RequestMapping("/api/v1/publishers")
@RequiredArgsConstructor
@Tag(name = "Publishers")
public class PublisherController {

    private final PublisherService publisherService;

    @GetMapping
    @Operation(summary = "Danh sách nhà xuất bản (public)")
    public ApiResponse<List<PublisherResponse>> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        PagedResult<PublisherResponse> result = publisherService.list(q, page, size, sort);
        ApiResponse<List<PublisherResponse>> response = ApiResponse.ok(result.getItems());
        response.setMeta(result.meta());
        return response;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết nhà xuất bản")
    public ApiResponse<PublisherResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(publisherService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Tạo nhà xuất bản")
    public ApiResponse<PublisherResponse> create(@Valid @RequestBody PublisherRequest request) {
        return ApiResponse.ok(publisherService.create(request), "Tạo nhà xuất bản thành công");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Cập nhật nhà xuất bản")
    public ApiResponse<PublisherResponse> update(@PathVariable Long id, @Valid @RequestBody PublisherRequest request) {
        return ApiResponse.ok(publisherService.update(id, request), "Cập nhật nhà xuất bản thành công");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Xóa mềm nhà xuất bản")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        publisherService.delete(id);
        return ApiResponse.ok(null, "Xóa nhà xuất bản thành công");
    }
}
