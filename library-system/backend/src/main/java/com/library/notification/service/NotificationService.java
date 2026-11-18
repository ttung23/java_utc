package com.library.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.common.exception.BusinessException;
import com.library.notification.entity.Notification;
import com.library.notification.entity.NotificationTemplate;
import com.library.notification.repository.NotificationRepository;
import com.library.notification.repository.NotificationTemplateRepository;
import com.library.security.SecurityUtils;
import com.library.user.entity.User;
import com.library.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository templateRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<JavaMailSender> mailSender;

    public void notifyUser(Long userId, String type, String title, String body, Map<String, Object> data) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return;
        }
        Notification n = new Notification();
        n.setUser(user);
        n.setType(type);
        n.setTitle(title);
        n.setBody(body);
        n.setChannel("IN_APP");
        if (data != null) {
            n.setData(objectMapper.valueToTree(data));
        }
        notificationRepository.save(n);

        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender != null && user.getEmail() != null) {
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setTo(user.getEmail());
                msg.setSubject(title);
                msg.setText(body == null ? title : body);
                sender.send(msg);
            } catch (Exception ex) {
                log.warn("Không gửi được email tới {}: {}", user.getEmail(), ex.getMessage());
            }
        }
    }

    @Transactional(readOnly = true)
    public PageResult listMine(int page, int size) {
        Long userId = SecurityUtils.requireCurrentUser().getId();
        Page<Notification> result = notificationRepository.findByUser_IdOrderByCreatedAtDesc(
                userId, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
        List<Map<String, Object>> items = result.getContent().stream().map(this::toMap).toList();
        long unread = notificationRepository.countByUser_IdAndReadFalse(userId);
        return new PageResult(items, result.getNumber(), result.getSize(), result.getTotalElements(), unread);
    }

    public Map<String, Object> markRead(Long id) {
        Long userId = SecurityUtils.requireCurrentUser().getId();
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy thông báo", HttpStatus.NOT_FOUND));
        if (!n.getUser().getId().equals(userId)) {
            throw new BusinessException("FORBIDDEN", "Không có quyền", HttpStatus.FORBIDDEN);
        }
        n.setRead(true);
        n.setReadAt(Instant.now());
        notificationRepository.save(n);
        return toMap(n);
    }

    public int markAllRead() {
        Long userId = SecurityUtils.requireCurrentUser().getId();
        Page<Notification> page = notificationRepository.findByUser_IdOrderByCreatedAtDesc(userId, PageRequest.of(0, 500));
        int count = 0;
        Instant now = Instant.now();
        for (Notification n : page) {
            if (!n.isRead()) {
                n.setRead(true);
                n.setReadAt(now);
                notificationRepository.save(n);
                count++;
            }
        }
        return count;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listTemplates() {
        return templateRepository.findAll().stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("code", t.getCode());
            m.put("subject", t.getSubject());
            m.put("body", t.getBody());
            m.put("channel", t.getChannel());
            m.put("active", t.isActive());
            return m;
        }).toList();
    }

    public Map<String, Object> upsertTemplate(String code, String subject, String body, String channel, Boolean active) {
        NotificationTemplate t = templateRepository.findByCode(code).orElseGet(NotificationTemplate::new);
        t.setCode(code);
        t.setSubject(subject);
        t.setBody(body);
        t.setChannel(channel);
        if (active != null) {
            t.setActive(active);
        }
        templateRepository.save(t);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("code", t.getCode());
        m.put("subject", t.getSubject());
        m.put("body", t.getBody());
        m.put("channel", t.getChannel());
        m.put("active", t.isActive());
        return m;
    }

    private Map<String, Object> toMap(Notification n) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", n.getId());
        m.put("type", n.getType());
        m.put("title", n.getTitle());
        m.put("body", n.getBody());
        m.put("data", n.getData());
        m.put("channel", n.getChannel());
        m.put("read", n.isRead());
        m.put("readAt", n.getReadAt());
        m.put("createdAt", n.getCreatedAt());
        return m;
    }

    public record PageResult(List<Map<String, Object>> items, int page, int size, long total, long unread) {
    }
}
