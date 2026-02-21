package uz.eduplatform.modules.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.eduplatform.core.audit.AuditLog;
import uz.eduplatform.core.audit.AuditLogRepository;
import uz.eduplatform.core.common.dto.PagedResponse;
import uz.eduplatform.modules.admin.dto.AuditLogDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public PagedResponse<AuditLogDto> getAuditLogs(Pageable pageable) {
        Page<AuditLog> page = auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AuditLogDto> getAuditLogsByUserId(UUID userId, Pageable pageable) {
        Page<AuditLog> page = auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AuditLogDto> getAuditLogsByCategory(String category, Pageable pageable) {
        Page<AuditLog> page = auditLogRepository.findByActionCategoryOrderByCreatedAtDesc(category, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AuditLogDto> getAuditLogsByDateRange(LocalDateTime from, LocalDateTime to, Pageable pageable) {
        Page<AuditLog> page = auditLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(from, to, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AuditLogDto> getAuditLogsByEntity(String entityType, UUID entityId, Pageable pageable) {
        Page<AuditLog> page = auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
                entityType, entityId, pageable);
        return toPagedResponse(page);
    }

    private PagedResponse<AuditLogDto> toPagedResponse(Page<AuditLog> page) {
        List<AuditLogDto> dtos = page.getContent().stream()
                .map(this::mapToDto)
                .toList();
        return PagedResponse.of(dtos, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    private AuditLogDto mapToDto(AuditLog log) {
        return AuditLogDto.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .userRole(log.getUserRole())
                .action(log.getAction())
                .actionCategory(log.getActionCategory())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .oldValues(log.getOldValues())
                .newValues(log.getNewValues())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
