package uz.eduplatform.modules.assessment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
import uz.eduplatform.modules.test.domain.TestHistory;
import uz.eduplatform.modules.test.repository.TestHistoryRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AssignmentServiceTest {

    @Mock private TestAssignmentRepository assignmentRepository;
    @Mock private TestAttemptRepository attemptRepository;
    @Mock private TestHistoryRepository testHistoryRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private AssignmentService assignmentService;

    private UUID teacherId;
    private UUID testHistoryId;
    private UUID assignmentId;
    private User teacher;
    private TestHistory testHistory;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        testHistoryId = UUID.randomUUID();
        assignmentId = UUID.randomUUID();

        teacher = User.builder()
                .id(teacherId)
                .firstName("Aziz")
                .lastName("Karimov")
                .build();

        testHistory = TestHistory.builder()
                .id(testHistoryId)
                .userId(teacherId)
                .title("Matematika Test")
                .build();

        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
        when(attemptRepository.countDistinctStudentsByAssignmentId(any())).thenReturn(0L);
        when(attemptRepository.countSubmittedByAssignmentId(any())).thenReturn(0L);
    }

    // ==================== Create ====================

    @Test
    void createAssignment_validRequest_returnsDto() {
        CreateAssignmentRequest request = CreateAssignmentRequest.builder()
                .testHistoryId(testHistoryId)
                .title("1-Chorak Nazorat")
                .description("Matematika 8-sinf")
                .durationMinutes(45)
                .maxAttempts(1)
                .assignedStudentIds(List.of(UUID.randomUUID(), UUID.randomUUID()))
                .build();

        when(testHistoryRepository.findByIdAndUserIdAndDeletedAtIsNull(testHistoryId, teacherId))
                .thenReturn(Optional.of(testHistory));
        when(assignmentRepository.save(any())).thenAnswer(i -> {
            TestAssignment a = i.getArgument(0);
            a.setId(assignmentId);
            return a;
        });

        AssignmentDto result = assignmentService.createAssignment(teacherId, request);

        assertNotNull(result);
        assertEquals("1-Chorak Nazorat", result.getTitle());
        assertEquals(teacherId, result.getTeacherId());
        assertEquals("Aziz Karimov", result.getTeacherName());
        assertEquals(45, result.getDurationMinutes());
        assertEquals(2, result.getTotalStudents());
        assertEquals(AssignmentStatus.DRAFT, result.getStatus());
    }

    @Test
    void createAssignment_futureStartTime_statusScheduled() {
        CreateAssignmentRequest request = CreateAssignmentRequest.builder()
                .testHistoryId(testHistoryId)
                .title("Scheduled Test")
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(2))
                .build();

        when(testHistoryRepository.findByIdAndUserIdAndDeletedAtIsNull(testHistoryId, teacherId))
                .thenReturn(Optional.of(testHistory));
        when(assignmentRepository.save(any())).thenAnswer(i -> {
            TestAssignment a = i.getArgument(0);
            a.setId(assignmentId);
            return a;
        });

        AssignmentDto result = assignmentService.createAssignment(teacherId, request);

        assertEquals(AssignmentStatus.SCHEDULED, result.getStatus());
    }

    @Test
    void createAssignment_endBeforeStart_throwsException() {
        CreateAssignmentRequest request = CreateAssignmentRequest.builder()
                .testHistoryId(testHistoryId)
                .title("Invalid Time")
                .startTime(LocalDateTime.now().plusDays(2))
                .endTime(LocalDateTime.now().plusDays(1))
                .build();

        when(testHistoryRepository.findByIdAndUserIdAndDeletedAtIsNull(testHistoryId, teacherId))
                .thenReturn(Optional.of(testHistory));

        assertThrows(BusinessException.class, () ->
                assignmentService.createAssignment(teacherId, request));
    }

    @Test
    void createAssignment_testHistoryNotFound_throwsException() {
        CreateAssignmentRequest request = CreateAssignmentRequest.builder()
                .testHistoryId(testHistoryId)
                .title("Test")
                .build();

        when(testHistoryRepository.findByIdAndUserIdAndDeletedAtIsNull(testHistoryId, teacherId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                assignmentService.createAssignment(teacherId, request));
    }

    // ==================== Get ====================

    @Test
    void getTeacherAssignments_returnsPagedResponse() {
        TestAssignment assignment = buildAssignment(assignmentId, teacherId);
        PageImpl<TestAssignment> page = new PageImpl<>(List.of(assignment));
        Pageable pageable = PageRequest.of(0, 20);

        when(assignmentRepository.findByTeacherIdOrderByCreatedAtDesc(teacherId, pageable))
                .thenReturn(page);

        PagedResponse<AssignmentDto> result = assignmentService.getTeacherAssignments(teacherId, null, pageable);

        assertEquals(1, result.getContent().size());
        assertEquals("Test Assignment", result.getContent().get(0).getTitle());
    }

    @Test
    void getAssignment_asTeacher_returnsDto() {
        TestAssignment assignment = buildAssignment(assignmentId, teacherId);
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));

        AssignmentDto result = assignmentService.getAssignment(assignmentId, teacherId);

        assertNotNull(result);
        assertEquals(assignmentId, result.getId());
    }

    @Test
    void getAssignment_asAssignedStudent_returnsDto() {
        UUID studentId = UUID.randomUUID();
        TestAssignment assignment = buildAssignment(assignmentId, teacherId);
        assignment.setAssignedStudentIds(List.of(studentId));

        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));

        AssignmentDto result = assignmentService.getAssignment(assignmentId, studentId);

        assertNotNull(result);
    }

    @Test
    void getAssignment_asUnassignedStudent_throwsException() {
        UUID studentId = UUID.randomUUID();
        TestAssignment assignment = buildAssignment(assignmentId, teacherId);
        assignment.setAssignedStudentIds(List.of(UUID.randomUUID())); // different student

        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));

        assertThrows(BusinessException.class, () ->
                assignmentService.getAssignment(assignmentId, studentId));
    }

    // ==================== Update ====================

    @Test
    void updateAssignment_validUpdate_returnsUpdatedDto() {
        TestAssignment assignment = buildAssignment(assignmentId, teacherId);
        assignment.setStatus(AssignmentStatus.DRAFT);

        when(assignmentRepository.findByIdAndTeacherId(assignmentId, teacherId))
                .thenReturn(Optional.of(assignment));
        when(assignmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UpdateAssignmentRequest request = UpdateAssignmentRequest.builder()
                .title("Updated Title")
                .durationMinutes(60)
                .maxAttempts(3)
                .build();

        AssignmentDto result = assignmentService.updateAssignment(assignmentId, teacherId, request);

        assertEquals("Updated Title", result.getTitle());
        assertEquals(60, result.getDurationMinutes());
        assertEquals(3, result.getMaxAttempts());
    }

    @Test
    void updateAssignment_completedStatus_throwsException() {
        TestAssignment assignment = buildAssignment(assignmentId, teacherId);
        assignment.setStatus(AssignmentStatus.COMPLETED);

        when(assignmentRepository.findByIdAndTeacherId(assignmentId, teacherId))
                .thenReturn(Optional.of(assignment));

        UpdateAssignmentRequest request = UpdateAssignmentRequest.builder()
                .title("New Title")
                .build();

        assertThrows(BusinessException.class, () ->
                assignmentService.updateAssignment(assignmentId, teacherId, request));
    }

    // ==================== Activate ====================

    @Test
    void activateAssignment_draftStatus_activates() {
        TestAssignment assignment = buildAssignment(assignmentId, teacherId);
        assignment.setStatus(AssignmentStatus.DRAFT);

        when(assignmentRepository.findByIdAndTeacherId(assignmentId, teacherId))
                .thenReturn(Optional.of(assignment));
        when(assignmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AssignmentDto result = assignmentService.activateAssignment(assignmentId, teacherId);

        assertEquals(AssignmentStatus.ACTIVE, result.getStatus());
    }

    @Test
    void activateAssignment_alreadyActive_throwsException() {
        TestAssignment assignment = buildAssignment(assignmentId, teacherId);
        assignment.setStatus(AssignmentStatus.ACTIVE);

        when(assignmentRepository.findByIdAndTeacherId(assignmentId, teacherId))
                .thenReturn(Optional.of(assignment));

        assertThrows(BusinessException.class, () ->
                assignmentService.activateAssignment(assignmentId, teacherId));
    }

    // ==================== Cancel ====================

    @Test
    void cancelAssignment_activeStatus_cancels() {
        TestAssignment assignment = buildAssignment(assignmentId, teacherId);
        assignment.setStatus(AssignmentStatus.ACTIVE);

        when(assignmentRepository.findByIdAndTeacherId(assignmentId, teacherId))
                .thenReturn(Optional.of(assignment));
        when(assignmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AssignmentDto result = assignmentService.cancelAssignment(assignmentId, teacherId);

        assertEquals(AssignmentStatus.CANCELLED, result.getStatus());
    }

    @Test
    void cancelAssignment_completedStatus_throwsException() {
        TestAssignment assignment = buildAssignment(assignmentId, teacherId);
        assignment.setStatus(AssignmentStatus.COMPLETED);

        when(assignmentRepository.findByIdAndTeacherId(assignmentId, teacherId))
                .thenReturn(Optional.of(assignment));

        assertThrows(BusinessException.class, () ->
                assignmentService.cancelAssignment(assignmentId, teacherId));
    }

    // ==================== Delete ====================

    @Test
    void deleteAssignment_draftStatus_softDeletes() {
        TestAssignment assignment = buildAssignment(assignmentId, teacherId);
        assignment.setStatus(AssignmentStatus.DRAFT);

        when(assignmentRepository.findByIdAndTeacherId(assignmentId, teacherId))
                .thenReturn(Optional.of(assignment));
        when(assignmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        assignmentService.deleteAssignment(assignmentId, teacherId);

        verify(assignmentRepository).save(argThat(a -> a.getDeletedAt() != null));
    }

    @Test
    void deleteAssignment_activeStatus_throwsException() {
        TestAssignment assignment = buildAssignment(assignmentId, teacherId);
        assignment.setStatus(AssignmentStatus.ACTIVE);

        when(assignmentRepository.findByIdAndTeacherId(assignmentId, teacherId))
                .thenReturn(Optional.of(assignment));

        assertThrows(BusinessException.class, () ->
                assignmentService.deleteAssignment(assignmentId, teacherId));
    }

    // ==================== Helpers ====================

    private TestAssignment buildAssignment(UUID id, UUID teacherId) {
        return TestAssignment.builder()
                .id(id)
                .testHistoryId(testHistoryId)
                .teacherId(teacherId)
                .title("Test Assignment")
                .durationMinutes(45)
                .maxAttempts(1)
                .showResults(true)
                .showCorrectAnswers(false)
                .showProofs(false)
                .shufflePerStudent(true)
                .preventCopyPaste(true)
                .preventTabSwitch(false)
                .status(AssignmentStatus.DRAFT)
                .assignedStudentIds(List.of(UUID.randomUUID()))
                .build();
    }
}
