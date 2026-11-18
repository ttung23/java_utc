package com.library.settings;

import com.library.common.api.ApiResponse;
import com.library.settings.service.SettingsAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Settings")
public class SettingsController {

    private final SettingsAdminService settingsAdminService;

    @GetMapping("/settings")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Danh sách cấu hình")
    public ApiResponse<List<Map<String, Object>>> listSettings() {
        return ApiResponse.ok(settingsAdminService.listSettings());
    }

    @PutMapping("/settings")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật hàng loạt settings")
    public ApiResponse<Map<String, Object>> updateBulk(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(settingsAdminService.updateSettingsBulk(body), "Đã cập nhật cấu hình");
    }

    @PutMapping("/settings/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật 1 setting")
    public ApiResponse<Map<String, Object>> upsert(@PathVariable String key, @RequestBody SettingReq req) {
        return ApiResponse.ok(settingsAdminService.upsertSetting(key, req.getValue(), req.getDescription()));
    }

    @GetMapping("/holidays")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Danh sách ngày nghỉ")
    public ApiResponse<List<Map<String, Object>>> holidays() {
        return ApiResponse.ok(settingsAdminService.listHolidays());
    }

    @PostMapping("/holidays")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Thêm ngày nghỉ")
    public ApiResponse<Map<String, Object>> createHoliday(@RequestBody HolidayReq req) {
        return ApiResponse.ok(settingsAdminService.createHoliday(req.getDate(), req.getName(), req.getRecurring()));
    }

    @DeleteMapping("/holidays/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa ngày nghỉ")
    public ApiResponse<Void> deleteHoliday(@PathVariable Long id) {
        settingsAdminService.deleteHoliday(id);
        return ApiResponse.ok(null);
    }

    @Data
    public static class SettingReq {
        private Object value;
        private String description;
    }

    @Data
    public static class HolidayReq {
        private LocalDate date;
        private String name;
        private Boolean recurring;
    }
}
