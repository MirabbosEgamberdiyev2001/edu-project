package uz.eduplatform.modules.assessment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.eduplatform.core.common.dto.PagedResponse;
import uz.eduplatform.core.common.exception.BusinessException;
import uz.eduplatform.core.common.exception.ResourceNotFoundException;
import uz.eduplatform.modules.assessment.domain.AssignmentStatus;
import uz.eduplatform.modules.assessment.domain.TestAssignment;
import uz.eduplatform.modules.assessment.dto.AssignmentDto;
import uz.eduplatform.modules.assessment.dto.CreateAssignmentRequest;
import uz.eduplatform.modules.assessment.dto.UpdateAssignmentRequest;
import uz.eduplatform.modules.assessment.repository.TestAssignmentRepository;
import uz.eduplatform.modules.assessment.repository.TestAttemptRepository;
import uz.eduplatform.modules.auth.domain.User;
import uz.eduplatform.modules.auth.repository.UserRepository;
import uz.eduplatform.modules.group.service.GroupService;
import uz.eduplatform.modules.test.domain.TestHistory;
import uz.eduplatform.modules.test.repository.TestHistoryRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final TestAssignmentRepository assignmentRepository;
    private final TestAttemptRepository attemptRepository;
    private final TestHistoryRepository testHistoryRepository;
    private final UserRepository userRepository;
    private final GroupService groupService;

    @Transactional
    public AssignmentDto createAssignment(UUID teacherId, CreateAssignmentRequest request) {
        // Validate test history exists and belongs to teacher
        TestHistory testHistory = testHistoryRepository.findByIdAndUserIdAndDeletedAtIsNull(
                        request.getTestHistoryId(), teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("TestHistory", "id", request.getTestHistoryId()));

        // Validate time range
        if (request.getStartTime() != null && request.getEndTime() != null
                && request.getEndTime().isBefore(request.getStartTime())) {
            throw BusinessException.ofKey("assignment.end.before.start");
        }

        // Resolve group members if groupId is provided
        List<UUID> assignedStudentIds = request.getAssignedStudentIds();
        if (request.getGroupId() != null) {
            // Validate teacher owns the group
            groupService.getGroup(request.getGroupId(), teacherId);

            List<UUID> groupStudentIds = groupService.getGroupStudentIds(request.getGroupId());
            if (groupStudentIds.isEmpty()) {
                throw BusinessException.ofKey("assignment.group.no.members");
            }
            // Merge: group students + any explicitly listed students (deduplicated)
            LinkedHashSet<UUID> merged = new LinkedHashSet<>(groupStudentIds);
            if (assignedStudentIds != null) {
                merged.addAll(assignedStudentIds);
            }
            assignedStudentIds = new ArrayList<>(merged);
        }

        // Determine initial status
        AssignmentStatus status = AssignmentStatus.DRAFT;
        if (request.getStartTime() != null && request.getStartTime().isAfter(LocalDateTime.now())) {
            status = AssignmentStatus.SCHEDULED;
        }

        TestAssignment assignment = TestAssignment.builder()
                .testHistoryId(testHistory.getId())
                .teacherId(teacherId)
                .title(request.getTitle())
                .description(request.getDescription())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .durationMinutes(request.getDurationMinutes())
                .maxAttempts(request.getMaxAttempts())
                .showResults(request.getShowResults())
                .showCorrectAnswers(request.getShowCorrectAnswers())
                .showProofs(request.getShowProofs())
                .shufflePerStudent(request.getShufflePerStudent())
                .preventCopyPaste(request.getPreventCopyPaste())
                .preventTabSwitch(request.getPreventTabSwitch())
                .tabSwitchThreshold(request.getTabSwitchThreshold())
                .tabSwitchAction(request.getTabSwitchAction())
                .accessCode(request.getAccessCode())
                .assignedStudentIds(assignedStudentIds)
                .status(status)
                .build();

        assignment = assignmentRepository.save(assignment);
        log.info("Created test assignment '{}' by teacher {}", assignment.getTitle(), teacherId);
        return mapToDto(assignment);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AssignmentDto> getTeacherAssignments(UUID teacherId, AssignmentStatus status, Pageable pageable) {
        Page<TestAssignment> page;
        if (status != null) {
            page = assignmentRepository.findByTeacherIdAndStatusOrderByCreatedAtDesc(teacherId, status, pageable);
        } else {
            page = assignmentRepository.findByTeacherIdOrderByCreatedAtDesc(teacherId, pageable);
        }

        List<AssignmentDto> dtos = page.getContent().stream()
                .map(this::mapToDto)
                .toList();

        return PagedResponse.of(dtos, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public AssignmentDto getAssignment(UUID assignmentId, UUID userId) {
        TestAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("TestAssignment", "id", assignmentId));

        // Teacher can view their own assignments; students can view if assigned
        if (!assignment.getTeacherId().equals(userId)) {
            if (assignment.getAssignedStudentIds() == null
                    || !assignment.getAssignedStudentIds().contains(userId)) {
                throw new BusinessException("error.access.denied", null, HttpStatus.FORBIDDEN);
            }
        }

        return mapToDto(assignment);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AssignmentDto> getStudentAssignments(UUID studentId, Pageable pageable) {
        String studentIdJson = "[\"" + studentId + "\"]";
        Page<TestAssignment> page = assignmentRepository.findAssignmentsForStudent(studentIdJson, pageable);

        List<AssignmentDto> dtos = page.getContent().stream()
                .map(this::mapToDto)
                .toList();

        return PagedResponse.of(dtos, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Transactional
    public AssignmentDto updateAssignment(UUID assignmentId, UUID teacherId, UpdateAssignmentRequest request) {
        TestAssignment assignment = assignmentRepository.findByIdAndTeacherId(assignmentId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("TestAssignment", "id", assignmentId));

        if (assignment.getStatus() == AssignmentStatus.COMPLETED
                || assignment.getStatus() == AssignmentStatus.CANCELLED) {
            throw BusinessException.ofKey("assignment.update.completed.cancelled");
        }

        if (request.getTitle() != null) assignment.setTitle(request.getTitle());
        if (request.getDescription() != null) assignment.setDescription(request.getDescription());
        if (request.getStartTime() != null) assignment.setStartTime(request.getStartTime());
        if (request.getEndTime() != null) assignment.setEndTime(request.getEndTime());
        if (request.getDurationMinutes() != null) assignment.setDurationMinutes(request.getDurationMinutes());
        if (request.getMaxAttempts() != null) assignment.setMaxAttempts(request.getMaxAttempts());
        if (request.getShowResults() != null) assignment.setShowResults(request.getShowResults());
        if (request.getShowCorrectAnswers() != null) assignment.setShowCorrectAnswers(request.getShowCorrectAnswers());
        if (request.getShowProofs() != null) assignment.setShowProofs(request.getShowProofs());
        if (request.getShufflePerStudent() != null) assignment.setShufflePerStudent(request.getShufflePerStudent());
        if (request.getPreventCopyPaste() != null) assignment.setPreventCopyPaste(request.getPreventCopyPaste());
        if (request.getPreventTabSwitch() != null) assignment.setPreventTabSwitch(request.getPreventTabSwitch());
        if (request.getTabSwitchThreshold() != null) assignment.setTabSwitchThreshold(request.getTabSwitchThreshold());
        if (request.getTabSwitchAction() != null) assignment.setTabSwitchAction(request.getTabSwitchAction());
        if (request.getAccessCode() != null) assignment.setAccessCode(request.getAccessCode());
        if (request.getAssignedStudentIds() != null) assignment.setAssignedStudentIds(request.getAssignedStudentIds());

        assignment = assignmentRepository.save(assignment);
        return mapToDto(assignment);
    }

    @Transactional
    public AssignmentDto activateAssignment(UUID assignmentId, UUID teacherId) {
        TestAssignment assignment = assignmentRepository.findByIdAndTeacherId(assignmentId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("TestAssignment", "id", assignmentId));

        if (assignment.getStatus() != AssignmentStatus.DRAFT
                && assignment.getStatus() != AssignmentStatus.SCHEDULED) {
            throw BusinessException.ofKey("assignment.activate.invalid.status");
        }

        assignment.setStatus(AssignmentStatus.ACTIVE);
        if (assignment.getStartTime() == null) {
            assignment.setStartTime(LocalDateTime.now());
        }

        assignment = assignmentRepository.save(assignment);
        log.info("Activated assignment {} by teacher {}", assignmentId, teacherId);
        return mapToDto(assignment);
    }

    @Transactional
    public AssignmentDto cancelAssignment(UUID assignmentId, UUID teacherId) {
        TestAssignment assignment = assignmentRepository.findByIdAndTeacherId(assignmentId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("TestAssignment", "id", assignmentId));

        if (assignment.getStatus() == AssignmentStatus.COMPLETED) {
            throw BusinessException.ofKey("assignment.cancel.completed");
        }

        assignment.setStatus(AssignmentStatus.CANCELLED);
        assignment = assignmentRepository.save(assignment);
        log.info("Cancelled assignment {} by teacher {}", assignmentId, teacherId);
        return mapToDto(assignment);
    }

    @Transactional
    public void deleteAssignment(UUID assignmentId, UUID teacherId) {
        TestAssignment assignment = assignmentRepository.findByIdAndTeacherId(assignmentId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("TestAssignment", "id", assignmentId));

        if (assignment.getStatus() == AssignmentStatus.ACTIVE) {
            throw BusinessException.ofKey("assignment.delete.active");
        }

        assignment.setDeletedAt(LocalDateTime.now());
        assignmentRepository.save(assignment);
        log.info("Soft-deleted assignment {} by teacher {}", assignmentId, teacherId);
    }

    private AssignmentDto mapToDto(TestAssignment a) {
        // Get teacher name
        String teacherName = userRepository.findById(a.getTeacherId())
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .orElse(null);

        // Get attempt stats
        long startedCount = attemptRepository.countDistinctStudentsByAssignmentId(a.getId());
        long submittedCount = attemptRepository.countSubmittedByAssignmentId(a.getId());

        return AssignmentDto.builder()
                .id(a.getId())
                .testHistoryId(a.getTestHistoryId())
                .teacherId(a.getTeacherId())
                .teacherName(teacherName)
                .title(a.getTitle())
                .description(a.getDescription())
                .startTime(a.getStartTime())
                .endTime(a.getEndTime())
                .durationMinutes(a.getDurationMinutes())
                .maxAttempts(a.getMaxAttempts())
                .showResults(a.getShowResults())
                .showCorrectAnswers(a.getShowCorrectAnswers())
                .showProofs(a.getShowProofs())
                .shufflePerStudent(a.getShufflePerStudent())
                .preventCopyPaste(a.getPreventCopyPaste())
                .preventTabSwitch(a.getPreventTabSwitch())
                .tabSwitchThreshold(a.getTabSwitchThreshold())
                .tabSwitchAction(a.getTabSwitchAction())
                .accessCode(a.getAccessCode())
                .assignedStudentIds(a.getAssignedStudentIds())
                .totalStudents(a.getAssignedStudentIds() != null ? a.getAssignedStudentIds().size() : 0)
                .startedCount((int) startedCount)
                .submittedCount((int) submittedCount)
                .status(a.getStatus())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
