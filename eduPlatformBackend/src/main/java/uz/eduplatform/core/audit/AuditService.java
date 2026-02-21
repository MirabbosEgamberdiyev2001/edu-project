package uz.eduplatform.core.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    public void log(UUID userId, String userRole, String action, String category,
                    String entityType, UUID entityId,
                    Map<String, Object> oldValues, Map<String, Object> newValues) {

        String ipAddress = null;
        String userAgent = null;

        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                ipAddress = getClientIp(request);
                userAgent = request.getHeader("User-Agent");
            }
        } catch (Exception ignored) {
        }

        AuditLog auditLog = AuditLog.builder()
                .userId(userId)
                .userRole(userRole)
                .action(action)
                .actionCategory(category)
                .entityType(entityType)
                .entityId(entityId)
                .oldValues(oldValues)
                .newValues(newValues)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        auditLogRepository.save(auditLog);
    }

    public void log(UUID userId, String userRole, String action, String category) {
        log(userId, userRole, action, category, null, null, null, null);
    }

    public void log(UUID userId, String userRole, String action, String category,
                    String entityType, UUID entityId) {
        log(userId, userRole, action, category, entityType, entityId, null, null);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
