package com.library.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.audit.entity.AuditLog;
import com.library.audit.repository.AuditLogRepository;
import com.library.auth.dto.TokenResponse;
import com.library.security.SecurityUtils;
import com.library.user.entity.User;
import com.library.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @AfterReturning(pointcut = "execution(* com.library.auth.service.AuthService.login(..))", returning = "result")
    public void afterLogin(Object result) {
        if (result instanceof TokenResponse tokenResponse && tokenResponse.getUser() != null) {
            write("LOGIN", "User", tokenResponse.getUser().getId(),
                    Map.of("email", tokenResponse.getUser().getEmail()));
        }
    }

    @AfterReturning("execution(* com.library.auth.service.AuthService.changePassword(..))")
    public void afterChangePassword() {
        Long userId = SecurityUtils.currentUserIdOrNull();
        write("CHANGE_PASSWORD", "User", userId, Map.of("userId", String.valueOf(userId)));
    }

    @AfterReturning("execution(* com.library.auth.service.AuthService.resetPassword(..))")
    public void afterResetPassword() {
        write("RESET_PASSWORD", "User", null, Map.of("action", "reset"));
    }

    private void write(String action, String entityType, Long entityId, Object after) {
        try {
            AuditLog logEntry = new AuditLog();
            Long actorId = SecurityUtils.currentUserIdOrNull();
            if (actorId == null && entityId != null) {
                actorId = entityId;
            }
            if (actorId != null) {
                User actor = userRepository.findById(actorId).orElse(null);
                logEntry.setActor(actor);
            }
            logEntry.setAction(action);
            logEntry.setEntityType(entityType);
            logEntry.setEntityId(entityId);
            logEntry.setAfter(after == null ? null : objectMapper.valueToTree(after));
            logEntry.setCreatedAt(Instant.now());
            auditLogRepository.save(logEntry);
        } catch (Exception ex) {
            log.warn("Không ghi được audit log {}: {}", action, ex.getMessage());
        }
    }
}
