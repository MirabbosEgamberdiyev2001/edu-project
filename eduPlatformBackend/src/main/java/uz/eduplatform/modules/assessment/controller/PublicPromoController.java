package uz.eduplatform.modules.assessment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.eduplatform.core.common.dto.ApiResponse;
import uz.eduplatform.modules.assessment.dto.PromoEnrollResultDto;
import uz.eduplatform.modules.assessment.dto.PublicPromoEnrollRequest;
import uz.eduplatform.modules.assessment.dto.PublicPromoInfoDto;
import uz.eduplatform.modules.assessment.service.PublicPromoService;

@RestController
@RequestMapping("/api/v1/public/promo")
@RequiredArgsConstructor
@Tag(name = "Ommaviy promo API", description = "Autentifikatsiyasiz promokod tekshirish va ro'yxatdan o'tish")
public class PublicPromoController {

    private final PublicPromoService publicPromoService;

    @GetMapping("/{code}/verify")
    @Operation(summary = "Promokodni tekshirish", description = "Promokod haqiqiyligini va topshiriq ma'lumotlarini tekshirish. Autentifikatsiya talab qilinmaydi.")
    public ResponseEntity<ApiResponse<PublicPromoInfoDto>> verifyCode(@PathVariable String code) {
        PublicPromoInfoDto info = publicPromoService.getPublicPromoInfo(code);
        return ResponseEntity.ok(ApiResponse.success(info));
    }

    @PostMapping("/{code}/enroll")
    @Operation(summary = "Promokod orqali ro'yxatdan o'tish", description = "Ism va telefon raqam kiritib, topshiriqqa avtomatik ro'yxatdan o'tish. Yangi foydalanuvchi yaratiladi yoki mavjudi topiladi. JWT token qaytariladi.")
    public ResponseEntity<ApiResponse<PromoEnrollResultDto>> enroll(
            @PathVariable String code,
            @Valid @RequestBody PublicPromoEnrollRequest request) {
        PromoEnrollResultDto result = publicPromoService.autoEnrollStudent(code, request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
