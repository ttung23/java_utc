package com.library.notification.repository;

import com.library.notification.entity.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {
    boolean existsByCode(String code);
}
