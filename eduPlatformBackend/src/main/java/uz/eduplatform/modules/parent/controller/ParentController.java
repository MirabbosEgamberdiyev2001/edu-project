package uz.eduplatform.modules.parent.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uz.eduplatform.core.common.dto.ApiResponse;
import uz.eduplatform.core.security.UserPrincipal;
import uz.eduplatform.modules.parent.dto.*;
import uz.eduplatform.modules.parent.service.ParentService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/parent")
@RequiredArgsConstructor
@Tag(name = "Ota-ona va farzand", description = "Ota-ona va farzand bog'lanishini boshqarish API'lari — ulash, bekor qilish, kuzatish")
public class ParentController {

    private final ParentService parentService;

    @PostMapping("/pairing-code")
    @Operation(summary = "Ulash kodi yaratish", description = "O'quvchi o'zi uchun 6 xonali ulash kodi yaratadi. Kod 15 daqiqa amal qiladi. Faqat STUDENT roli.")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<GeneratePairingCodeResponse>> generatePairingCode(
            @AuthenticationPrincipal UserPrincipal principal) {

        GeneratePairingCodeResponse response = parentService.generatePairingCode(principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @PostMapping("/pair")
    @Operation(summary = "Farzand bilan bog'lanish", description = "Ota-ona o'quvchi bergan ulash kodi orqali farzandi bilan bog'lanadi. Faqat PARENT roli.")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<ApiResponse<ParentChildDto>> pairWithCode(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PairWithCodeRequest request) {

        ParentChildDto dto = parentService.pairWithCode(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(dto));
    }

    @PostMapping("/pairings/{id}/revoke")
    @Operation(summary = "Bog'lanishni bekor qilish", description = "Ota-ona yoki farzand tomonidan bog'lanishni bekor qilish.")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT')")
    public ResponseEntity<ApiResponse<Void>> revokePairing(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {

        parentService.revokePairing(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/children")
    @Operation(summary = "Farzandlarim ro'yxati", description = "Ota-onaning barcha bog'langan farzandlari ro'yxatini olish. Faqat PARENT roli.")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<ApiResponse<List<ParentChildDto>>> getMyChildren(
            @AuthenticationPrincipal UserPrincipal principal) {

        List<ParentChildDto> children = parentService.getMyChildren(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(children));
    }

    @GetMapping("/parents")
    @Operation(summary = "Ota-onalarim ro'yxati", description = "O'quvchining bog'langan ota-onalari ro'yxatini olish. Faqat STUDENT roli.")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<ParentChildDto>>> getMyParents(
            @AuthenticationPrincipal UserPrincipal principal) {

        List<ParentChildDto> parents = parentService.getMyParents(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(parents));
    }

    @GetMapping("/children/{childId}/dashboard")
    @Operation(summary = "Farzand o'quv dashboardi", description = "Farzandning o'quv natijalari — testlar, baholar, faollik statistikasini ko'rish. Faqat PARENT roli.")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<ApiResponse<ChildDashboardDto>> getChildDashboard(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID childId) {

        ChildDashboardDto dashboard = parentService.getChildDashboard(principal.getId(), childId);
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }
}
