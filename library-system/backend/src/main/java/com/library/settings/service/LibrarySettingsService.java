package com.library.settings.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.library.settings.repository.SettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LibrarySettingsService {

    private final SettingRepository settingRepository;

    public int getInt(String key, int defaultValue) {
        return find(key).map(JsonNode::asInt).orElse(defaultValue);
    }

    public long getLong(String key, long defaultValue) {
        return find(key).map(JsonNode::asLong).orElse(defaultValue);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return find(key).map(JsonNode::asBoolean).orElse(defaultValue);
    }

    public BigDecimal getDecimal(String key, BigDecimal defaultValue) {
        return find(key).map(n -> BigDecimal.valueOf(n.asDouble())).orElse(defaultValue);
    }

    public Set<String> getStringSet(String key, Set<String> defaultValue) {
        return find(key).map(node -> {
            Set<String> values = new HashSet<>();
            if (node.isArray()) {
                node.forEach(item -> values.add(item.asText()));
            } else if (node.isTextual()) {
                values.add(node.asText());
            }
            return values;
        }).orElse(defaultValue);
    }

    public int holdHours() {
        return getInt("hold_hours", 48);
    }

    public BigDecimal overdueFeePerDay() {
        return getDecimal("overdue_fee_per_day", BigDecimal.valueOf(2000));
    }

    public int graceDays() {
        return getInt("grace_days", 0);
    }

    public BigDecimal maxUnpaidFine() {
        return getDecimal("max_unpaid_fine", BigDecimal.valueOf(50000));
    }

    public boolean blockBorrowIfOverdue() {
        return getBoolean("block_borrow_if_overdue", true);
    }

    public boolean fineCapByPrice() {
        return getBoolean("fine_cap_by_price", true);
    }

    public boolean skipHolidaysInFine() {
        return getBoolean("skip_holidays_in_fine", false);
    }

    public boolean allowOnlineBorrow() {
        return getBoolean("allow_online_borrow", true);
    }

    public boolean allowSelfRenew() {
        return getBoolean("allow_self_renew", true);
    }

    public BigDecimal lostBookMultiplier() {
        return getDecimal("lost_book_multiplier", BigDecimal.valueOf(2));
    }

    public BigDecimal lostProcessingFee() {
        return getDecimal("lost_processing_fee", BigDecimal.valueOf(20000));
    }

    public int damagedMinorPercent() {
        return getInt("damaged_minor_percent", 20);
    }

    public int damagedMajorPercent() {
        return getInt("damaged_major_percent", 60);
    }

    public int dueSoonReminderDays() {
        return getInt("due_soon_reminder_days", 2);
    }

    public Set<String> weekendDays() {
        return getStringSet("weekend_days", Set.of("SUNDAY"));
    }

    private java.util.Optional<JsonNode> find(String key) {
        return settingRepository.findByKey(key).map(s -> s.getValue());
    }
}
