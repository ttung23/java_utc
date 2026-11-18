package com.library.staff.controller;

import com.library.common.api.ApiResponse;
import com.library.common.enums.UserRole;
import com.library.common.enums.UserStatus;
import com.library.staff.service.StaffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Staff")
public class StaffController {

    private final StaffService staffService;

    @GetMapping
    @Operation(summary = "Danh sách nhân viên")
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.ok(staffService.list());
    }

    @PostMapping
    @Operation(summary = "Thêm nhân viên")
    public ApiResponse<Map<String, Object>> create(@RequestBody CreateReq req) {
        return ApiResponse.ok(staffService.create(
                req.getEmail(), req.getPhone(), req.getFullName(), req.getPassword(),
                req.getRole(), req.getStaffCode(), req.getPosition(), req.getShift()));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Đổi trạng thái nhân viên")
    public ApiResponse<Map<String, Object>> status(@PathVariable Long id, @RequestBody StatusReq req) {
        return ApiResponse.ok(staffService.updateStatus(id, req.getStatus()));
    }

    @PatchMapping("/{id}/role")
    @Operation(summary = "Đổi vai trò")
    public ApiResponse<Map<String, Object>> role(@PathVariable Long id, @RequestBody RoleReq req) {
        return ApiResponse.ok(staffService.updateRole(id, req.getRole()));
    }

    @PostMapping("/{id}/reset-password")
    @Operation(summary = "Reset mật khẩu nhân viên")
    public ApiResponse<Void> resetPassword(@PathVariable Long id, @RequestBody ResetReq req) {
        staffService.resetPassword(id, req.getPassword());
        return ApiResponse.ok(null, "Đã reset mật khẩu");
    }

    @Data
    public static class CreateReq {
        private String email;
        private String phone;
        private String fullName;
        private String password;
        private UserRole role = UserRole.STAFF;
        private String staffCode;
        private String position;
        private String shift;
    }

    @Data
    public static class StatusReq {
        private UserStatus status;
    }

    @Data
    public static class RoleReq {
        private UserRole role;
    }

    @Data
    public static class ResetReq {
        private String password;
    }
}
