package com.library.notification.repository;

import com.library.notification.entity.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {
    boolean existsByCode(String code);

    Optional<NotificationTemplate> findByCode(String code);
}
