package uz.eduplatform.modules.subscription.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uz.eduplatform.core.common.dto.ApiResponse;
import uz.eduplatform.core.common.dto.PagedResponse;
import uz.eduplatform.core.security.UserPrincipal;
import uz.eduplatform.modules.subscription.domain.SubscriptionStatus;
import uz.eduplatform.modules.subscription.dto.UserSubscriptionDto;
import uz.eduplatform.modules.subscription.dto.UsageDto;
import uz.eduplatform.modules.subscription.service.SubscriptionService;
import uz.eduplatform.modules.subscription.service.UsageTrackingService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Obunalar", description = "Foydalanuvchi obuna API'lari — joriy obuna, tarix, foydalanish statistikasi")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final UsageTrackingService usageTrackingService;

    @GetMapping("/my")
    @Operation(summary = "Joriy obunani olish", description = "Foydalanuvchining hozirda faol obuna ma'lumotlarini olish — reja turi, muddat, cheklovlar.")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserSubscriptionDto>> getMySubscription(
            @AuthenticationPrincipal UserPrincipal principal) {
        UserSubscriptionDto sub = subscriptionService.getActiveSubscription(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(sub));
    }

    @GetMapping("/my/history")
    @Operation(summary = "Obuna tarixini olish", description = "Foydalanuvchining barcha oldingi va joriy obunalar tarixini sahifalab olish.")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PagedResponse<UserSubscriptionDto>>> getMyHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<UserSubscriptionDto> response = subscriptionService.getUserSubscriptions(
                principal.getId(), PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/my/usage")
    @Operation(summary = "Foydalanish statistikasini olish", description = "Joriy obuna bo'yicha foydalanish — bugun/oyda nechta test yaratildi, eksport qilindi, qancha limit qoldi.")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<UsageDto>>> getMyUsage(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<UsageDto> usage = usageTrackingService.getUserUsageSummary(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(usage));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Obunani bekor qilish", description = "Joriy obunani bekor qilish. Obuna muddat tugagunicha faol qoladi, lekin yangilanmaydi.")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserSubscriptionDto>> cancelMySubscription(
            @PathVariable UUID id) {
        UserSubscriptionDto sub = subscriptionService.cancelSubscription(id);
        return ResponseEntity.ok(ApiResponse.success(sub));
    }
}
