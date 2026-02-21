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

    @PostMapping
    @Operation(summary = "Yangi guruh yaratish", description = "O'quvchilar uchun yangi guruh yaratish. Faqat TEACHER roli uchun.")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<GroupDto>> createGroup(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateGroupRequest request) {

        GroupDto dto = groupService.createGroup(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(dto));
    }

    @GetMapping
    @Operation(summary = "O'qituvchi guruhlarini olish", description = "Joriy o'qituvchining barcha guruhlarini sahifalab olish. Holat bo'yicha filtrlash mumkin.")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<PagedResponse<GroupDto>>> getMyGroups(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) GroupStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PagedResponse<GroupDto> response = groupService.getTeacherGroups(
                principal.getId(), status, PageRequest.of(page, size, Sort.by("createdAt").descending()));
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
            @PathVariable UUID id,
            @Valid @RequestBody UpdateGroupRequest request) {

        GroupDto dto = groupService.updateGroup(id, principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Guruhni arxivlash", description = "Guruhni arxiv holatiga o'tkazish. Arxivlangan guruhga yangi a'zo qo'shib bo'lmaydi.")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<GroupDto>> archiveGroup(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {

        GroupDto dto = groupService.archiveGroup(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Guruhni o'chirish", description = "Guruhni yumshoq o'chirish. Barcha a'zolik bog'lanishlari ham bekor qilinadi.")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<Void>> deleteGroup(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {

        groupService.deleteGroup(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
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
            @PathVariable UUID id,
            @Valid @RequestBody AddMembersRequest request) {

        List<GroupMemberDto> added = groupService.addMembers(id, principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(added));
    }

    @DeleteMapping("/{id}/members/{studentId}")
    @Operation(summary = "O'quvchini guruhdan chiqarish", description = "Berilgan o'quvchini guruh a'zoligidan chiqarish.")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @PathVariable UUID studentId) {

        groupService.removeMember(id, principal.getId(), studentId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
