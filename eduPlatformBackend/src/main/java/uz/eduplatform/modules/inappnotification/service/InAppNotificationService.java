package uz.eduplatform.modules.inappnotification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.eduplatform.core.common.dto.PagedResponse;
import uz.eduplatform.modules.inappnotification.domain.InAppNotification;
import uz.eduplatform.modules.inappnotification.dto.InAppNotificationDto;
import uz.eduplatform.modules.inappnotification.repository.InAppNotificationRepository;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InAppNotificationService {

    private final InAppNotificationRepository repository;

    /** Fire-and-forget: create a notification without blocking the caller. */
    @Async("notificationExecutor")
    public void createAsync(UUID userId, String type, String title, String body,
                            UUID referenceId, String referenceType) {
        try {
            InAppNotification notification = InAppNotification.builder()
                    .userId(userId)
                    .type(type)
                    .title(title)
                    .body(body)
                    .referenceId(referenceId)
                    .referenceType(referenceType)
                    .build();
            repository.save(notification);
        } catch (Exception e) {
            log.warn("Failed to create in-app notification for user {}: {}", userId, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public PagedResponse<InAppNotificationDto> getNotifications(UUID userId, int page, int size) {
        Page<InAppNotification> result = repository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(page, size));
        List<InAppNotificationDto> dtos = result.getContent().stream().map(this::toDto).toList();
        return PagedResponse.of(dtos, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Cacheable(value = "notification_count", key = "#userId")
    @Transactional(readOnly = true)
    public long countUnread(UUID userId) {
        return repository.countByUserIdAndIsReadFalse(userId);
    }

    @CacheEvict(value = "notification_count", key = "#userId")
    @Transactional
    public void markAllRead(UUID userId) {
        repository.markAllReadByUserId(userId);
    }

    @CacheEvict(value = "notification_count", key = "#userId")
    @Transactional
    public void markRead(UUID notificationId, UUID userId) {
        repository.markReadById(notificationId, userId);
    }

    private InAppNotificationDto toDto(InAppNotification n) {
        return InAppNotificationDto.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .body(n.getBody())
                .referenceId(n.getReferenceId())
                .referenceType(n.getReferenceType())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
