package com.library.borrow.controller;

import com.library.borrow.dto.BorrowSlipResponse;
import com.library.borrow.dto.RenewEligibilityResponse;
import com.library.borrow.dto.ReturnResultResponse;
import com.library.borrow.service.RenewalService;
import com.library.borrow.service.ReturnService;
import com.library.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/borrow-items")
@RequiredArgsConstructor
@Tag(name = "Borrow Items")
public class BorrowItemController {

    private final RenewalService renewalService;
    private final ReturnService returnService;

    @GetMapping("/{id}/renew-eligibility")
    @Operation(summary = "Kiểm tra điều kiện gia hạn")
    public ApiResponse<RenewEligibilityResponse> renewEligibility(@PathVariable Long id) {
        return ApiResponse.ok(renewalService.eligibility(id));
    }

    @PostMapping("/{id}/renew")
    @Operation(summary = "Gia hạn item")
    public ApiResponse<BorrowSlipResponse> renew(
            @PathVariable Long id,
            @RequestBody(required = false) NoteBody body
    ) {
        return ApiResponse.ok(
                renewalService.renew(id, body == null ? null : body.getNote()).slip(),
                "Gia hạn thành công"
        );
    }

    @PostMapping("/{id}/report-lost")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Báo mất sách")
    public ApiResponse<ReturnResultResponse> reportLost(
            @PathVariable Long id,
            @RequestBody(required = false) NoteBody body
    ) {
        return ApiResponse.ok(
                returnService.reportLost(id, body == null ? null : body.getNote()).result(),
                "Đã ghi nhận mất sách"
        );
    }

    @Data
    public static class NoteBody {
        private String note;
    }
}
