package uz.eduplatform.modules.content.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uz.eduplatform.core.common.dto.ApiResponse;
import uz.eduplatform.core.common.dto.PagedResponse;
import uz.eduplatform.core.common.utils.MessageService;
import uz.eduplatform.core.i18n.AcceptLanguage;
import uz.eduplatform.core.security.UserPrincipal;
import uz.eduplatform.modules.content.dto.*;
import uz.eduplatform.modules.content.service.SubjectService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/subjects")
@RequiredArgsConstructor
@Tag(name = "Fanlar", description = "Fanlarni boshqarish API'lari — yaratish, tahrirlash, arxivlash, tiklash")
public class SubjectController {

    private final SubjectService subjectService;
    private final MessageService messageService;

    @GetMapping
    @Operation(summary = "Foydalanuvchi fanlarini olish", description = "Joriy foydalanuvchining barcha fanlarini sahifalab olish (template fanlar ham ko'rinadi).")
    public ResponseEntity<ApiResponse<PagedResponse<SubjectDto>>> getSubjects(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer gradeLevel,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "sortOrder") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction) {

        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        PagedResponse<SubjectDto> response = subjectService.getSubjects(
                principal.getId(), search, gradeLevel, PageRequest.of(page, size, sort), language);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/archived")
    @Operation(summary = "Arxivlangan fanlarni olish", description = "Foydalanuvchi tomonidan arxivlangan fanlar ro'yxatini sahifalab olish.")
    public ResponseEntity<ApiResponse<PagedResponse<SubjectDto>>> getArchivedSubjects(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PagedResponse<SubjectDto> response = subjectService.getArchivedSubjects(
                principal.getId(), PageRequest.of(page, size), language);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    @Operation(summary = "Yangi fan yaratish", description = "Yangi fan yaratish. Faqat ADMIN va SUPER_ADMIN rollari uchun ruxsat berilgan.")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SubjectDto>> createSubject(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language,
            @Valid @RequestBody CreateSubjectRequest request) {

        SubjectDto subject = subjectService.createSubject(principal.getId(), request, language);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(subject, messageService.get("subject.created", language.toLocale())));
    }

    @PostMapping("/bulk")
    @Operation(summary = "Bir nechta fan yaratish", description = "Bir so'rovda bir nechta fanni ommaviy yaratish. Mavjud nomlar avtomatik o'tkazib yuboriladi.")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<BulkCreateResponse>> createSubjectsBulk(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language,
            @Valid @RequestBody BulkCreateSubjectRequest request) {

        BulkCreateResponse response = subjectService.createSubjectsBulk(principal.getId(), request, language);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, messageService.get("subject.bulk.created", language.toLocale(),
                        response.getCreated(), response.getSkipped())));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fanni ID bo'yicha olish", description = "Berilgan ID bo'yicha fan ma'lumotlarini olish — nomi, kategoriyasi, mavzular soni.")
    public ResponseEntity<ApiResponse<SubjectDto>> getSubject(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {

        SubjectDto subject = subjectService.getSubjectById(id, principal.getId(), language);
        return ResponseEntity.ok(ApiResponse.success(subject));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Fanni yangilash", description = "Fan ma'lumotlarini to'liq yangilash — nomi, kategoriyasi, tavsifi va boshqa maydonlar.")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SubjectDto>> updateSubject(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language,
            @Valid @RequestBody UpdateSubjectRequest request) {

        SubjectDto subject = subjectService.updateSubject(id, principal.getId(), request, language);
        return ResponseEntity.ok(ApiResponse.success(subject, messageService.get("subject.updated", language.toLocale())));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Fanni qisman yangilash", description = "Fanning faqat yuborilgan maydonlarini yangilash. Yuborilmagan maydonlar o'zgarmaydi.")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SubjectDto>> patchSubject(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language,
            @Valid @RequestBody UpdateSubjectRequest request) {

        SubjectDto subject = subjectService.updateSubject(id, principal.getId(), request, language);
        return ResponseEntity.ok(ApiResponse.success(subject, messageService.get("subject.updated", language.toLocale())));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Fanni o'chirish", description = "Fanni yumshoq o'chirish (soft delete). Ma'lumotlar bazadan o'chirilmaydi, faqat belgilanadi.")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSubject(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {

        subjectService.deleteSubject(id, principal.getId(), language);
        return ResponseEntity.ok(ApiResponse.success(null, messageService.get("subject.deleted", language.toLocale())));
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Fanni arxivlash", description = "Fanni arxivga ko'chirish. Arxivlangan fan test generatsiyasida ishlatilmaydi.")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SubjectDto>> archiveSubject(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {

        SubjectDto subject = subjectService.archiveSubject(id, principal.getId(), language);
        return ResponseEntity.ok(ApiResponse.success(subject, messageService.get("subject.archived", language.toLocale())));
    }

    @PostMapping("/{id}/restore")
    @Operation(summary = "Arxivdan tiklash", description = "Arxivlangan fanni qaytadan faol holatga keltirish.")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SubjectDto>> restoreSubject(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {

        SubjectDto subject = subjectService.restoreSubject(id, principal.getId(), language);
        return ResponseEntity.ok(ApiResponse.success(subject, messageService.get("subject.restored", language.toLocale())));
    }

}
