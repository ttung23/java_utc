package com.library.borrow.controller;

import com.library.borrow.dto.ApproveBorrowRequest;
import com.library.borrow.dto.BorrowSlipResponse;
import com.library.borrow.dto.BorrowValidationResult;
import com.library.borrow.dto.CancelBorrowRequest;
import com.library.borrow.dto.CounterBorrowRequest;
import com.library.borrow.dto.HandoverRequest;
import com.library.borrow.dto.OnlineBorrowRequest;
import com.library.borrow.dto.RejectBorrowRequest;
import com.library.borrow.dto.ValidateBorrowRequest;
import com.library.borrow.service.BorrowPrintService;
import com.library.borrow.service.BorrowService;
import com.library.common.api.ApiResponse;
import com.library.common.enums.BorrowChannel;
import com.library.common.enums.BorrowSlipStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/borrows")
@RequiredArgsConstructor
@Tag(name = "Borrows")
public class BorrowController {

    private final BorrowService borrowService;
    private final BorrowPrintService borrowPrintService;

    @PostMapping("/validate")
    @Operation(summary = "Kiểm tra điều kiện mượn (dry-run)")
    public ApiResponse<BorrowValidationResult> validate(@RequestBody ValidateBorrowRequest request) {
        return ApiResponse.ok(borrowService.validate(request));
    }

    @PostMapping("/counter")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Tạo phiếu mượn tại quầy")
    public ApiResponse<BorrowSlipResponse> createCounter(@Valid @RequestBody CounterBorrowRequest request) {
        return ApiResponse.ok(borrowService.createCounter(request), "Tạo phiếu mượn thành công");
    }

    @PostMapping("/online")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Gửi yêu cầu mượn online")
    public ApiResponse<BorrowSlipResponse> createOnline(@Valid @RequestBody OnlineBorrowRequest request) {
        return ApiResponse.ok(borrowService.createOnline(request), "Đã gửi yêu cầu mượn");
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Danh sách phiếu mượn")
    public ApiResponse<List<BorrowSlipResponse>> list(
            @RequestParam(required = false) BorrowSlipStatus status,
            @RequestParam(required = false) BorrowChannel channel,
            @RequestParam(required = false) Long readerId,
            @RequestParam(required = false) Boolean overdueOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        BorrowService.PageResult result = borrowService.list(status, channel, readerId, overdueOnly, page, size);
        return withMeta(result);
    }

    @GetMapping("/me")
    @Operation(summary = "Phiếu mượn của tôi")
    public ApiResponse<List<BorrowSlipResponse>> listMine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return withMeta(borrowService.listMine(page, size));
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Danh sách item quá hạn")
    public ApiResponse<List<BorrowSlipResponse.ItemResponse>> overdue() {
        return ApiResponse.ok(borrowService.listOverdue());
    }

    @GetMapping("/due-soon")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Sách sắp đến hạn")
    public ApiResponse<List<BorrowSlipResponse.ItemResponse>> dueSoon() {
        return ApiResponse.ok(borrowService.listDueSoon());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết phiếu mượn")
    public ApiResponse<BorrowSlipResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(borrowService.getDetail(id));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Duyệt yêu cầu mượn online")
    public ApiResponse<BorrowSlipResponse> approve(
            @PathVariable Long id,
            @RequestBody(required = false) ApproveBorrowRequest request
    ) {
        return ApiResponse.ok(borrowService.approve(id, request == null ? new ApproveBorrowRequest() : request),
                "Đã duyệt yêu cầu");
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Từ chối yêu cầu mượn online")
    public ApiResponse<BorrowSlipResponse> reject(
            @PathVariable Long id,
            @Valid @RequestBody RejectBorrowRequest request
    ) {
        return ApiResponse.ok(borrowService.reject(id, request), "Đã từ chối");
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Hủy phiếu (USER chủ phiếu / STAFF+)")
    public ApiResponse<BorrowSlipResponse> cancel(
            @PathVariable Long id,
            @RequestBody(required = false) CancelBorrowRequest request
    ) {
        return ApiResponse.ok(borrowService.cancel(id, request == null ? new CancelBorrowRequest() : request),
                "Đã hủy phiếu");
    }

    @PostMapping("/{id}/handover")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Xác nhận giao sách")
    public ApiResponse<BorrowSlipResponse> handover(
            @PathVariable Long id,
            @Valid @RequestBody HandoverRequest request
    ) {
        return ApiResponse.ok(borrowService.handover(id, request), "Đã giao sách");
    }

    @GetMapping("/{id}/print")
    @Operation(summary = "In phiếu mượn PDF (A5)")
    public ResponseEntity<byte[]> print(@PathVariable Long id) {
        byte[] pdf = borrowPrintService.printSlipPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=slip-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private ApiResponse<List<BorrowSlipResponse>> withMeta(BorrowService.PageResult result) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("page", result.page());
        meta.put("limit", result.size());
        meta.put("total", result.total());
        ApiResponse<List<BorrowSlipResponse>> response = ApiResponse.ok(result.items());
        response.setMeta(meta);
        return response;
    }
}
