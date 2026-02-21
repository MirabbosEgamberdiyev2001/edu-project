package uz.eduplatform.modules.subscription.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.eduplatform.core.common.dto.ApiResponse;
import uz.eduplatform.core.common.utils.MessageService;
import uz.eduplatform.modules.subscription.dto.CreatePlanRequest;
import uz.eduplatform.modules.subscription.dto.SubscriptionPlanDto;
import uz.eduplatform.modules.subscription.dto.UpdatePlanRequest;
import uz.eduplatform.modules.subscription.service.SubscriptionPlanService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/subscription-plans")
@RequiredArgsConstructor
@Tag(name = "Obuna rejalari", description = "Obuna rejalarini boshqarish API'lari — ommaviy va admin")
public class SubscriptionPlanController {

    private final SubscriptionPlanService planService;
    private final MessageService messageService;

    @GetMapping
    @Operation(summary = "Faol obuna rejalarini olish", description = "Barcha faol obuna rejalarini olish. Autentifikatsiyasiz foydalanish mumkin.")
    public ResponseEntity<ApiResponse<List<SubscriptionPlanDto>>> getActivePlans() {
        List<SubscriptionPlanDto> plans = planService.getActivePlans();
        return ResponseEntity.ok(ApiResponse.success(plans));
    }

    @GetMapping("/all")
    @Operation(summary = "Barcha rejalar (admin)", description = "Faol va nofaol barcha obuna rejalarini olish. Faqat ADMIN/SUPER_ADMIN.")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<SubscriptionPlanDto>>> getAllPlans() {
        List<SubscriptionPlanDto> plans = planService.getAllPlans();
        return ResponseEntity.ok(ApiResponse.success(plans));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Reja tafsilotlari", description = "Obuna rejasining to'liq ma'lumotlarini olish — narx, cheklovlar, imkoniyatlar.")
    public ResponseEntity<ApiResponse<SubscriptionPlanDto>> getPlan(@PathVariable UUID id) {
        SubscriptionPlanDto plan = planService.getPlan(id);
        return ResponseEntity.ok(ApiResponse.success(plan));
    }

    @PostMapping
    @Operation(summary = "Yangi reja yaratish", description = "Yangi obuna rejasi yaratish — nomi, narxi, cheklovlar, imkoniyatlarni belgilash. Faqat ADMIN.")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionPlanDto>> createPlan(
            @Valid @RequestBody CreatePlanRequest request) {
        SubscriptionPlanDto plan = planService.createPlan(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(plan));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Rejani yangilash", description = "Mavjud obuna rejasini yangilash. O'zgarishlar yangi obunalarga ta'sir qiladi.")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionPlanDto>> updatePlan(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePlanRequest request) {
        SubscriptionPlanDto plan = planService.updatePlan(id, request);
        return ResponseEntity.ok(ApiResponse.success(plan));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Rejani o'chirish", description = "Obuna rejasini yumshoq o'chirish. Mavjud obunalar ta'sirlanmaydi. Faqat ADMIN.")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePlan(@PathVariable UUID id) {
        planService.deletePlan(id);
        return ResponseEntity.ok(ApiResponse.success(null, messageService.get("subscription.plan.deleted")));
    }
}
