package uz.eduplatform.modules.assessment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import uz.eduplatform.core.common.exception.BusinessException;
import uz.eduplatform.core.common.exception.ResourceNotFoundException;
import uz.eduplatform.modules.assessment.domain.*;
import uz.eduplatform.modules.assessment.dto.AssignmentResultDto;
import uz.eduplatform.modules.assessment.repository.TestAssignmentRepository;
import uz.eduplatform.modules.assessment.repository.TestAttemptRepository;
import uz.eduplatform.modules.auth.domain.User;
import uz.eduplatform.modules.auth.repository.UserRepository;
import uz.eduplatform.modules.group.repository.StudentGroupRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ResultServiceTest {

    @Mock private TestAssignmentRepository assignmentRepository;
    @Mock private TestAttemptRepository attemptRepository;
    @Mock private UserRepository userRepository;
    @Mock private StudentGroupRepository studentGroupRepository;

    @InjectMocks
    private ResultService resultService;

    private UUID teacherId;
    private UUID assignmentId;
    private TestAssignment assignment;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        assignmentId = UUID.randomUUID();

        assignment = TestAssignment.builder()
                .id(assignmentId)
                .teacherId(teacherId)
                .title("Matematika Nazorat")
                .status(AssignmentStatus.ACTIVE)
                .assignedStudentIds(List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()))
                .build();
    }

    @Test
    void getAssignmentResults_withAttempts_returnsStats() {
        UUID student1Id = UUID.randomUUID();
        UUID student2Id = UUID.randomUUID();

        TestAttempt attempt1 = TestAttempt.builder()
                .id(UUID.randomUUID())
                .assignment(assignment)
                .studentId(student1Id)
                .attemptNumber(1)
                .startedAt(LocalDateTime.now().minusMinutes(40))
                .submittedAt(LocalDateTime.now().minusMinutes(5))
                .rawScore(new BigDecimal("8.00"))
                .maxScore(new BigDecimal("10.00"))
                .percentage(new BigDecimal("80.00"))
                .status(AttemptStatus.AUTO_GRADED)
                .tabSwitchCount(0)
                .build();

        TestAttempt attempt2 = TestAttempt.builder()
                .id(UUID.randomUUID())
                .assignment(assignment)
                .studentId(student2Id)
                .attemptNumber(1)
                .startedAt(LocalDateTime.now().minusMinutes(35))
                .submittedAt(LocalDateTime.now().minusMinutes(3))
                .rawScore(new BigDecimal("6.00"))
                .maxScore(new BigDecimal("10.00"))
                .percentage(new BigDecimal("60.00"))
                .status(AttemptStatus.AUTO_GRADED)
                .tabSwitchCount(1)
                .build();

        User student1 = User.builder().id(student1Id).firstName("Aziz").lastName("K").build();
        User student2 = User.builder().id(student2Id).firstName("Malika").lastName("S").build();

        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
        when(attemptRepository.findByAssignmentIdOrderByCreatedAtDesc(assignmentId))
                .thenReturn(List.of(attempt1, attempt2));
        when(attemptRepository.averagePercentageByAssignmentId(assignmentId)).thenReturn(70.0);
        when(attemptRepository.maxPercentageByAssignmentId(assignmentId)).thenReturn(80.0);
        when(attemptRepository.minPercentageByAssignmentId(assignmentId)).thenReturn(60.0);
        when(userRepository.findById(student1Id)).thenReturn(Optional.of(student1));
        when(userRepository.findById(student2Id)).thenReturn(Optional.of(student2));

        AssignmentResultDto result = resultService.getAssignmentResults(assignmentId, teacherId);

        assertNotNull(result);
        assertEquals("Matematika Nazorat", result.getAssignmentTitle());
        assertEquals(3, result.getTotalStudents());
        assertEquals(2, result.getCompletedStudents());
        assertEquals(new BigDecimal("70.00"), result.getAverageScore());
        assertEquals(new BigDecimal("80.00"), result.getHighestScore());
        assertEquals(new BigDecimal("60.00"), result.getLowestScore());
        assertEquals(2, result.getStudents().size());
        assertEquals("Aziz", result.getStudents().get(0).getFirstName());
    }

    @Test
    void getAssignmentResults_noAttempts_returnsEmptyStats() {
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
        when(attemptRepository.findByAssignmentIdOrderByCreatedAtDesc(assignmentId))
                .thenReturn(List.of());
        when(attemptRepository.averagePercentageByAssignmentId(assignmentId)).thenReturn(null);
        when(attemptRepository.maxPercentageByAssignmentId(assignmentId)).thenReturn(null);
        when(attemptRepository.minPercentageByAssignmentId(assignmentId)).thenReturn(null);

        AssignmentResultDto result = resultService.getAssignmentResults(assignmentId, teacherId);

        assertNotNull(result);
        assertEquals(3, result.getTotalStudents());
        assertEquals(0, result.getCompletedStudents());
        assertNull(result.getAverageScore());
        assertNull(result.getHighestScore());
        assertTrue(result.getStudents().isEmpty());
    }

    @Test
    void getAssignmentResults_notTeacher_throwsException() {
        UUID otherUserId = UUID.randomUUID();
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));

        assertThrows(BusinessException.class, () ->
                resultService.getAssignmentResults(assignmentId, otherUserId));
    }

    @Test
    void getAssignmentResults_notFound_throwsException() {
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                resultService.getAssignmentResults(assignmentId, teacherId));
    }
}
