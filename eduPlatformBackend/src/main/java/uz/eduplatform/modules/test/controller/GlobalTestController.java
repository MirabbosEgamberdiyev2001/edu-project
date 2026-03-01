package uz.eduplatform.modules.test.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import uz.eduplatform.core.common.dto.ApiResponse;
import uz.eduplatform.core.common.dto.PagedResponse;
import uz.eduplatform.core.common.exception.ResourceNotFoundException;
import uz.eduplatform.core.common.utils.MessageService;
import uz.eduplatform.core.i18n.AcceptLanguage;
import uz.eduplatform.core.security.UserPrincipal;
import uz.eduplatform.modules.assessment.domain.AssignmentStatus;
import uz.eduplatform.modules.assessment.domain.TestAssignment;
import uz.eduplatform.modules.assessment.dto.AttemptDto;
import uz.eduplatform.modules.assessment.repository.TestAssignmentRepository;
import uz.eduplatform.modules.assessment.service.TestTakingService;
import uz.eduplatform.modules.test.domain.GlobalStatus;
import uz.eduplatform.modules.test.domain.TestCategory;
import uz.eduplatform.modules.test.domain.TestHistory;
import uz.eduplatform.modules.test.dto.GlobalTestStartResponse;
import uz.eduplatform.modules.test.dto.TestHistoryDto;
import uz.eduplatform.modules.test.repository.TestHistoryRepository;
import uz.eduplatform.modules.test.service.TestHistoryService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/global-tests")
@RequiredArgsConstructor
@Tag(name = "Global Testlar", description = "Barcha foydalanuvchilarga ko'rinadigan global testlar. O'quvchilar to'g'ridan-to'g'ri boshlashi mumkin.")
public class GlobalTestController {

    private final TestHistoryService testHistoryService;
    private final TestHistoryRepository testHistoryRepository;
    private final TestAssignmentRepository assignmentRepository;
    private final TestTakingService testTakingService;
    private final MessageService messageService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Global testlar ro'yxati",
            description = "Barcha tasdiqlangan global testlarni ko'rish. Kategoriya, fan va sinf bo'yicha filtrlash mumkin.")
    public ResponseEntity<ApiResponse<PagedResponse<TestHistoryDto>>> getGlobalTests(
            @RequestParam(required = false) TestCategory category,
            @RequestParam(required = false) UUID subjectId,
            @RequestParam(required = false) Integer gradeLevel,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {

        PagedResponse<TestHistoryDto> response = testHistoryService.getApprovedGlobalTests(
                category, subjectId, gradeLevel, search,
                PageRequest.of(page, size),
                language);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
    @Transactional
    @Operation(summary = "Global testni boshlash",
            description = "Student global testni boshlaydi. Avtomatik assignment yaratiladi va attempt qaytariladi.")
    public ResponseEntity<ApiResponse<GlobalTestStartResponse>> startGlobalTest(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {

        UUID studentId = principal.getId();
        String studentIdJson = "[\"" + studentId + "\"]";

        // Validate: test must be approved global
        TestHistory test = testHistoryRepository.findByIdAndDeletedAtIsNull(id)
                .filter(t -> t.getGlobalStatus() == GlobalStatus.APPROVED)
                .orElseThrow(() -> new ResourceNotFoundException("GlobalTest", "id", id));

        // Find or create assignment for this student + global test
        TestAssignment assignment = assignmentRepository
                .findGlobalAssignmentForStudent(id, studentIdJson)
                .orElseGet(() -> assignmentRepository.save(TestAssignment.builder()
                        .testHistoryId(id)
                        .teacherId(test.getUserId())
                        .title(test.getTitle())
                        .description("Global test")
                        .durationMinutes(test.getPublicDurationMinutes() != null ? test.getPublicDurationMinutes() : 60)
                        .maxAttempts(Integer.MAX_VALUE)
                        .showResults(true)
                        .showCorrectAnswers(true)
                        .showProofs(true)
                        .shufflePerStudent(true)
                        .preventCopyPaste(false)
                        .preventTabSwitch(false)
                        .assignedStudentIds(new ArrayList<>(List.of(studentId)))
                        .status(AssignmentStatus.ACTIVE)
                        .build()));

        // Ensure student is in assignedStudentIds
        if (assignment.getAssignedStudentIds() == null ||
                !assignment.getAssignedStudentIds().contains(studentId)) {
            List<UUID> updated = new ArrayList<>(
                    assignment.getAssignedStudentIds() != null ? assignment.getAssignedStudentIds() : List.of()
            );
            updated.add(studentId);
            assignment.setAssignedStudentIds(updated);
            assignmentRepository.save(assignment);
        }

        // Extract IP address from HTTP request
        String ipAddress = getClientIpAddress(httpRequest);

        // Start attempt via existing TestTakingService (null request = no access code required for global tests)
        AttemptDto attempt = testTakingService.startAttempt(assignment.getId(), studentId, null, ipAddress);

        GlobalTestStartResponse response = GlobalTestStartResponse.builder()
                .attemptId(attempt.getId())
                .assignmentId(assignment.getId())
                .testTitle(assignment.getTitle())
                .durationMinutes(assignment.getDurationMinutes())
                .questionCount(attempt.getQuestions() != null ? attempt.getQuestions().size() : 0)
                .build();

        return ResponseEntity.ok(ApiResponse.success(response, messageService.get("test.started", language.toLocale())));
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
