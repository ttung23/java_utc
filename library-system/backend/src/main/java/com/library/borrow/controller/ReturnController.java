package com.library.borrow.controller;

import com.library.borrow.dto.ReturnPreviewResponse;
import com.library.borrow.dto.ReturnRequest;
import com.library.borrow.dto.ReturnResultResponse;
import com.library.borrow.service.ReturnService;
import com.library.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/returns")
@RequiredArgsConstructor
@Tag(name = "Returns")
public class ReturnController {

    private final ReturnService returnService;

    @GetMapping("/borrowing")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Danh sách sách đang mượn (quầy trả)")
    public ApiResponse<List<ReturnPreviewResponse>> borrowing() {
        return ApiResponse.ok(returnService.listBorrowing());
    }

    @GetMapping("/preview")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Xem phạt dự kiến trước khi trả")
    public ApiResponse<ReturnPreviewResponse> preview(@RequestParam String barcode) {
        return ApiResponse.ok(returnService.preview(barcode));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Xác nhận trả sách")
    public ApiResponse<ReturnResultResponse> confirm(@Valid @RequestBody ReturnRequest request) {
        return ApiResponse.ok(returnService.confirm(request), "Trả sách thành công");
    }
}
