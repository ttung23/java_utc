package com.library.settings;

import com.library.common.api.ApiResponse;
import com.library.settings.entity.Setting;
import com.library.settings.repository.SettingRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
@Tag(name = "Settings")
public class SettingsController {

    private final SettingRepository settingRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Danh sách cấu hình (ADMIN/STAFF)")
    public ApiResponse<List<Map<String, Object>>> list() {
        List<Map<String, Object>> data = settingRepository.findAll().stream().map(this::toMap).toList();
        return ApiResponse.ok(data);
    }

    private Map<String, Object> toMap(Setting setting) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", setting.getId());
        map.put("key", setting.getKey());
        map.put("value", setting.getValue());
        map.put("description", setting.getDescription());
        return map;
    }
}
