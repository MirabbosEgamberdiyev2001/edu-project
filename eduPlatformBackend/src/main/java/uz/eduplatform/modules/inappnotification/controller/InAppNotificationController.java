package uz.eduplatform.modules.inappnotification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.eduplatform.core.common.dto.ApiResponse;
import uz.eduplatform.core.common.dto.PagedResponse;
import uz.eduplatform.core.security.UserPrincipal;
import uz.eduplatform.modules.inappnotification.dto.InAppNotificationDto;
import uz.eduplatform.modules.inappnotification.service.InAppNotificationService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-app notification endpoints")
public class InAppNotificationController {

    private final InAppNotificationService notificationService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get notifications for the current user")
    public ResponseEntity<ApiResponse<PagedResponse<InAppNotificationDto>>> getNotifications(
            @org.springframework.security.core.annotation.AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<InAppNotificationDto> result =
                notificationService.getNotifications(principal.getId(), page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get count of unread notifications")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            @org.springframework.security.core.annotation.AuthenticationPrincipal UserPrincipal principal) {
        long count = notificationService.countUnread(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(Map.of("count", count)));
    }

    @PatchMapping("/mark-all-read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<ApiResponse<Void>> markAllRead(
            @org.springframework.security.core.annotation.AuthenticationPrincipal UserPrincipal principal) {
        notificationService.markAllRead(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/{id}/mark-read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark a single notification as read")
    public ResponseEntity<ApiResponse<Void>> markRead(
            @PathVariable UUID id,
            @org.springframework.security.core.annotation.AuthenticationPrincipal UserPrincipal principal) {
        notificationService.markRead(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
