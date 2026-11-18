package com.library.catalog.controller;

import com.library.catalog.dto.LocationRequest;
import com.library.catalog.dto.LocationResponse;
import com.library.catalog.dto.PagedResult;
import com.library.catalog.service.LocationService;
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
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
@Tag(name = "Locations")
public class LocationController {

    private final LocationService locationService;

    @GetMapping
    @Operation(summary = "Danh sách vị trí kệ (public)")
    public ApiResponse<List<LocationResponse>> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        PagedResult<LocationResponse> result = locationService.list(q, page, size, sort);
        ApiResponse<List<LocationResponse>> response = ApiResponse.ok(result.getItems());
        response.setMeta(result.meta());
        return response;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết vị trí")
    public ApiResponse<LocationResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(locationService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Tạo vị trí")
    public ApiResponse<LocationResponse> create(@Valid @RequestBody LocationRequest request) {
        return ApiResponse.ok(locationService.create(request), "Tạo vị trí thành công");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Cập nhật vị trí")
    public ApiResponse<LocationResponse> update(@PathVariable Long id, @Valid @RequestBody LocationRequest request) {
        return ApiResponse.ok(locationService.update(id, request), "Cập nhật vị trí thành công");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Xóa mềm vị trí")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        locationService.delete(id);
        return ApiResponse.ok(null, "Xóa vị trí thành công");
    }
}
