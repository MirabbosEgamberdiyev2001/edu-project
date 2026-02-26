package uz.eduplatform.modules.group.controller;

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
import uz.eduplatform.modules.group.domain.GroupStatus;
import uz.eduplatform.modules.group.dto.*;
import uz.eduplatform.modules.group.service.GroupService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
@Tag(name = "Guruhlar", description = "O'quvchi guruhlarini boshqarish API'lari — yaratish, a'zolarni qo'shish/o'chirish")
public class GroupController {

    private final GroupService groupService;
    private final MessageService messageService;

    @PostMapping
    @Operation(summary = "Yangi guruh yaratish", description = "O'quvchilar uchun yangi guruh yaratish. Faqat TEACHER roli uchun.")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<GroupDto>> createGroup(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language,
            @Valid @RequestBody CreateGroupRequest request) {

        GroupDto dto = groupService.createGroup(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(dto, messageService.get("success.group.created", language.toLocale())));
    }

    @GetMapping
    @Operation(summary = "O'qituvchi guruhlarini olish", description = "Joriy o'qituvchining barcha guruhlarini sahifalab olish. Holat va nom bo'yicha filtrlash mumkin.")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<PagedResponse<GroupDto>>> getMyGroups(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) GroupStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PagedResponse<GroupDto> response = groupService.getTeacherGroups(
                principal.getId(), status, search,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/my")
    @Operation(summary = "O'quvchi guruhlarini olish", description = "Joriy o'quvchi a'zo bo'lgan barcha guruhlar ro'yxatini olish.")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<PagedResponse<GroupDto>>> getStudentGroups(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PagedResponse<GroupDto> response = groupService.getStudentGroups(
                principal.getId(), PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Guruh tafsilotlarini olish", description = "Guruhning to'liq ma'lumotlari — nomi, a'zolar soni, holati, yaratilgan sanasi.")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<GroupDto>> getGroup(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {

        GroupDto dto = groupService.getGroup(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Guruhni yangilash", description = "Guruh ma'lumotlarini yangilash — nomi va tavsifi.")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<GroupDto>> updateGroup(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateGroupRequest request) {

        GroupDto dto = groupService.updateGroup(id, principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(dto, messageService.get("success.group.updated", language.toLocale())));
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Guruhni arxivlash", description = "Guruhni arxiv holatiga o'tkazish. Arxivlangan guruhga yangi a'zo qo'shib bo'lmaydi.")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<GroupDto>> archiveGroup(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language,
            @PathVariable UUID id) {

        GroupDto dto = groupService.archiveGroup(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(dto, messageService.get("success.group.archived", language.toLocale())));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Guruhni o'chirish", description = "Guruhni yumshoq o'chirish. Barcha a'zolik bog'lanishlari ham bekor qilinadi.")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<Void>> deleteGroup(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language,
            @PathVariable UUID id) {

        groupService.deleteGroup(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, messageService.get("success.group.deleted", language.toLocale())));
    }

    // ── Student Search (for teachers) ──

    @GetMapping("/students/search")
    @Operation(summary = "Talabalarni qidirish", description = "Guruhga qo'shish uchun talabalarni ism, email yoki telefon raqami bo'yicha qidirish.")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<PagedResponse<StudentSearchDto>>> searchStudents(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        PagedResponse<StudentSearchDto> response = groupService.searchStudents(
                search, PageRequest.of(page, Math.min(size, 50)));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── Member Management ──

    @GetMapping("/{id}/members")
    @Operation(summary = "Guruh a'zolarini olish", description = "Guruhga biriktirilgan barcha o'quvchilar ro'yxatini olish.")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<List<GroupMemberDto>>> getMembers(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {

        List<GroupMemberDto> members = groupService.getMembers(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(members));
    }

    @PostMapping("/{id}/members")
    @Operation(summary = "Guruhga o'quvchilar qo'shish", description = "Bir nechta o'quvchini guruhga biriktirish. Allaqachon a'zo bo'lganlar o'tkazib yuboriladi.")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<List<GroupMemberDto>>> addMembers(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language,
            @PathVariable UUID id,
            @Valid @RequestBody AddMembersRequest request) {

        List<GroupMemberDto> added = groupService.addMembers(id, principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(added, messageService.get("success.group.members.added", language.toLocale())));
    }

    @DeleteMapping("/{id}/members/{studentId}")
    @Operation(summary = "O'quvchini guruhdan chiqarish", description = "Berilgan o'quvchini guruh a'zoligidan chiqarish.")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language,
            @PathVariable UUID id,
            @PathVariable UUID studentId) {

        groupService.removeMember(id, principal.getId(), studentId);
        return ResponseEntity.ok(ApiResponse.success(null, messageService.get("success.group.member.removed", language.toLocale())));
    }

    @DeleteMapping("/{id}/members/batch")
    @Operation(summary = "Bir nechta o'quvchini guruhdan chiqarish", description = "Bir nechta o'quvchini birdan guruh a'zoligidan chiqarish.")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<Void>> removeMembersBatch(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language,
            @PathVariable UUID id,
            @Valid @RequestBody BatchRemoveMembersRequest request) {

        groupService.removeMembersBatch(id, principal.getId(), request.getStudentIds());
        return ResponseEntity.ok(ApiResponse.success(null, messageService.get("success.group.members.batch.removed", language.toLocale())));
    }
}
