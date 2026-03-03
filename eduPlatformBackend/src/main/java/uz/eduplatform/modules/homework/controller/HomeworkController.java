package uz.eduplatform.modules.homework.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import uz.eduplatform.core.common.dto.ApiResponse;
import uz.eduplatform.core.common.dto.PagedResponse;
import uz.eduplatform.core.security.UserPrincipal;
import uz.eduplatform.modules.homework.dto.*;
import uz.eduplatform.modules.homework.service.HomeworkService;

import org.springframework.http.HttpHeaders;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/homeworks")
@RequiredArgsConstructor
@Tag(name = "Homeworks", description = "Homework management for teachers and students")
public class HomeworkController {

    private final HomeworkService homeworkService;

    // ---- Teacher endpoints ----

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create a new homework assignment")
    public ResponseEntity<ApiResponse<HomeworkDto>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid CreateHomeworkRequest request) {
        HomeworkDto dto = homeworkService.createHomework(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @GetMapping("/teacher")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get all homeworks created by teacher")
    public ResponseEntity<ApiResponse<PagedResponse<HomeworkDto>>> getTeacherHomeworks(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<HomeworkDto> result = homeworkService.getTeacherHomeworks(
                principal.getId(), PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}/submissions")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get all submissions for a homework")
    public ResponseEntity<ApiResponse<PagedResponse<HomeworkSubmissionDto>>> getSubmissions(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        PagedResponse<HomeworkSubmissionDto> result = homeworkService.getSubmissions(
                id, principal.getId(), PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PatchMapping("/submissions/{submissionId}/grade")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Grade a homework submission")
    public ResponseEntity<ApiResponse<HomeworkSubmissionDto>> grade(
            @PathVariable UUID submissionId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid GradeSubmissionRequest request) {
        HomeworkSubmissionDto dto = homeworkService.gradeSubmission(submissionId, principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @GetMapping("/{id}/submissions/export")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Export homework submissions as Excel")
    public ResponseEntity<byte[]> exportSubmissions(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        byte[] data = homeworkService.exportSubmissionsExcel(id, principal.getId());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "homework_submissions_" + id + ".xlsx");
        return ResponseEntity.ok().headers(headers).body(data);
    }

    // ---- Student endpoints ----

    @GetMapping("/student")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
    @Operation(summary = "Get homeworks available to the current student")
    public ResponseEntity<ApiResponse<PagedResponse<HomeworkDto>>> getStudentHomeworks(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) UUID groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<HomeworkDto> result = homeworkService.getStudentHomeworks(
                principal.getId(), groupId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping(value = "/{id}/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
    @Operation(summary = "Submit an answer for a homework")
    public ResponseEntity<ApiResponse<HomeworkSubmissionDto>> submit(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String textAnswer,
            @RequestParam(required = false) MultipartFile file) {
        HomeworkSubmissionDto dto = homeworkService.submitHomework(id, principal.getId(), textAnswer, file);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }
}
