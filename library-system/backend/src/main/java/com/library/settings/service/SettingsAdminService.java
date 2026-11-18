package com.library.settings.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.common.exception.BusinessException;
import com.library.security.SecurityUtils;
import com.library.settings.entity.Holiday;
import com.library.settings.entity.Setting;
import com.library.settings.repository.HolidayRepository;
import com.library.settings.repository.SettingRepository;
import com.library.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class SettingsAdminService {

    private final SettingRepository settingRepository;
    private final HolidayRepository holidayRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listSettings() {
        return settingRepository.findAll().stream().map(this::settingMap).toList();
    }

    public Map<String, Object> upsertSetting(String key, Object value, String description) {
        Setting setting = settingRepository.findByKey(key).orElseGet(Setting::new);
        setting.setKey(key);
        setting.setValue(objectMapper.valueToTree(value));
        if (description != null) {
            setting.setDescription(description);
        }
        Long userId = SecurityUtils.currentUserIdOrNull();
        if (userId != null) {
            setting.setUpdatedBy(userRepository.findById(userId).orElse(null));
        }
        settingRepository.save(setting);
        return settingMap(setting);
    }

    public Map<String, Object> updateSettingsBulk(Map<String, Object> values) {
        values.forEach((k, v) -> upsertSetting(k, v, null));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("updated", values.size());
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listHolidays() {
        return holidayRepository.findAll().stream().map(this::holidayMap).toList();
    }

    public Map<String, Object> createHoliday(LocalDate date, String name, Boolean recurring) {
        Holiday h = new Holiday();
        h.setDate(date);
        h.setName(name);
        h.setRecurring(Boolean.TRUE.equals(recurring));
        holidayRepository.save(h);
        return holidayMap(h);
    }

    public void deleteHoliday(Long id) {
        if (!holidayRepository.existsById(id)) {
            throw new BusinessException("NOT_FOUND", "Không tìm thấy ngày nghỉ", HttpStatus.NOT_FOUND);
        }
        holidayRepository.deleteById(id);
    }

    private Map<String, Object> settingMap(Setting s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("key", s.getKey());
        m.put("value", s.getValue());
        m.put("description", s.getDescription());
        return m;
    }

    private Map<String, Object> holidayMap(Holiday h) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", h.getId());
        m.put("date", h.getDate());
        m.put("name", h.getName());
        m.put("recurring", h.isRecurring());
        return m;
    }
}
