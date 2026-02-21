package uz.eduplatform.modules.assessment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import uz.eduplatform.core.common.exception.BusinessException;
import uz.eduplatform.core.common.exception.ResourceNotFoundException;
import uz.eduplatform.modules.assessment.domain.*;
import uz.eduplatform.modules.assessment.dto.*;
import uz.eduplatform.modules.assessment.repository.AnswerRepository;
import uz.eduplatform.modules.assessment.repository.TestAssignmentRepository;
import uz.eduplatform.modules.assessment.repository.TestAttemptRepository;
import uz.eduplatform.modules.auth.domain.User;
import uz.eduplatform.modules.auth.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TestTakingServiceTest {

    @Mock private TestAssignmentRepository assignmentRepository;
    @Mock private TestAttemptRepository attemptRepository;
    @Mock private AnswerRepository answerRepository;
    @Mock private UserRepository userRepository;
    @Mock private GradingService gradingService;
    @Mock private LiveMonitoringService liveMonitoringService;
    @Mock private uz.eduplatform.modules.parent.service.ParentNotificationService parentNotificationService;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private TestTakingService testTakingService;

    private UUID assignmentId;
    private UUID studentId;
    private UUID attemptId;
    private TestAssignment assignment;
    private User student;

    @BeforeEach
    void setUp() {
        assignmentId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        attemptId = UUID.randomUUID();

        student = User.builder()
                .id(studentId)
                .firstName("Jasur")
                .lastName("Toshmatov")
                .build();

        assignment = TestAssignment.builder()
                .id(assignmentId)
                .teacherId(UUID.randomUUID())
                .title("Matematika Nazorat")
                .status(AssignmentStatus.ACTIVE)
                .durationMinutes(45)
                .maxAttempts(2)
                .assignedStudentIds(List.of(studentId))
                .build();

        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(answerRepository.countByAttemptId(any())).thenReturn(0L);
        when(answerRepository.countByAttemptIdAndSelectedAnswerIsNotNull(any())).thenReturn(0L);
    }

    // ==================== Start Attempt ====================

    @Test
    void startAttempt_validRequest_createsAttempt() {
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
        when(attemptRepository.findByAssignmentIdAndStudentIdAndStatus(assignmentId, studentId, AttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(attemptRepository.countByAssignmentIdAndStudentId(assignmentId, studentId)).thenReturn(0L);
        when(attemptRepository.save(any())).thenAnswer(i -> {
            TestAttempt a = i.getArgument(0);
            a.setId(attemptId);
            return a;
        });

        AttemptDto result = testTakingService.startAttempt(assignmentId, studentId, null, null);

        assertNotNull(result);
        assertEquals(studentId, result.getStudentId());
        assertEquals("Jasur Toshmatov", result.getStudentName());
        assertEquals(1, result.getAttemptNumber());
    }

    @Test
    void startAttempt_existingInProgress_resumesAttempt() {
        TestAttempt existingAttempt = TestAttempt.builder()
                .id(attemptId)
                .assignment(assignment)
                .studentId(studentId)
                .attemptNumber(1)
                .startedAt(LocalDateTime.now().minusMinutes(10))
                .status(AttemptStatus.IN_PROGRESS)
                .tabSwitchCount(0)
                .build();

        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
        when(attemptRepository.findByAssignmentIdAndStudentIdAndStatus(assignmentId, studentId, AttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.of(existingAttempt));

        AttemptDto result = testTakingService.startAttempt(assignmentId, studentId, null, null);

        assertNotNull(result);
        assertEquals(attemptId, result.getId());
        // Should NOT create a new attempt
        verify(attemptRepository, never()).save(any());
    }

    @Test
    void startAttempt_notActive_throwsException() {
        assignment.setStatus(AssignmentStatus.DRAFT);
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));

        assertThrows(BusinessException.class, () ->
                testTakingService.startAttempt(assignmentId, studentId, null, null));
    }

    @Test
    void startAttempt_notAssigned_throwsException() {
        assignment.setAssignedStudentIds(List.of(UUID.randomUUID())); // different student
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));

        assertThrows(BusinessException.class, () ->
                testTakingService.startAttempt(assignmentId, studentId, null, null));
    }

    @Test
    void startAttempt_maxAttemptsReached_throwsException() {
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
        when(attemptRepository.findByAssignmentIdAndStudentIdAndStatus(assignmentId, studentId, AttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(attemptRepository.countByAssignmentIdAndStudentId(assignmentId, studentId)).thenReturn(2L); // max is 2

        assertThrows(BusinessException.class, () ->
                testTakingService.startAttempt(assignmentId, studentId, null, null));
    }

    @Test
    void startAttempt_invalidAccessCode_throwsException() {
        assignment.setAccessCode("SECRET123");
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));

        StartAttemptRequest request = StartAttemptRequest.builder()
                .accessCode("WRONG")
                .build();

        assertThrows(BusinessException.class, () ->
                testTakingService.startAttempt(assignmentId, studentId, request, null));
    }

    @Test
    void startAttempt_validAccessCode_succeeds() {
        assignment.setAccessCode("SECRET123");
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
        when(attemptRepository.findByAssignmentIdAndStudentIdAndStatus(assignmentId, studentId, AttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(attemptRepository.countByAssignmentIdAndStudentId(assignmentId, studentId)).thenReturn(0L);
        when(attemptRepository.save(any())).thenAnswer(i -> {
            TestAttempt a = i.getArgument(0);
            a.setId(attemptId);
            return a;
        });

        StartAttemptRequest request = StartAttemptRequest.builder()
                .accessCode("SECRET123")
                .build();

        AttemptDto result = testTakingService.startAttempt(assignmentId, studentId, request, null);

        assertNotNull(result);
    }

    @Test
    void startAttempt_testNotStartedYet_throwsException() {
        assignment.setStartTime(LocalDateTime.now().plusHours(1));
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));

        assertThrows(BusinessException.class, () ->
                testTakingService.startAttempt(assignmentId, studentId, null, null));
    }

    @Test
    void startAttempt_testAlreadyEnded_throwsException() {
        assignment.setEndTime(LocalDateTime.now().minusHours(1));
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));

        assertThrows(BusinessException.class, () ->
                testTakingService.startAttempt(assignmentId, studentId, null, null));
    }

    // ==================== Save Answer ====================

    @Test
    void saveAnswer_newAnswer_createsAnswer() {
        UUID questionId = UUID.randomUUID();
        TestAttempt attempt = TestAttempt.builder()
                .id(attemptId)
                .assignment(assignment)
                .studentId(studentId)
                .startedAt(LocalDateTime.now())
                .status(AttemptStatus.IN_PROGRESS)
                .build();

        when(attemptRepository.findByIdAndStudentId(attemptId, studentId))
                .thenReturn(Optional.of(attempt));
        when(answerRepository.findByAttemptIdAndQuestionId(attemptId, questionId))
                .thenReturn(Optional.empty());
        when(answerRepository.save(any())).thenAnswer(i -> {
            Answer a = i.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        SubmitAnswerRequest request = SubmitAnswerRequest.builder()
                .questionId(questionId)
                .questionIndex(0)
                .selectedAnswer("A")
                .timeSpentSeconds(30)
                .bookmarked(false)
                .build();

        AnswerDto result = testTakingService.saveAnswer(attemptId, studentId, request);

        assertNotNull(result);
        assertEquals(questionId, result.getQuestionId());
        verify(answerRepository).save(any());
    }

    @Test
    void saveAnswer_existingAnswer_updatesAnswer() {
        UUID questionId = UUID.randomUUID();
        TestAttempt attempt = TestAttempt.builder()
                .id(attemptId)
                .assignment(assignment)
                .studentId(studentId)
                .startedAt(LocalDateTime.now())
                .status(AttemptStatus.IN_PROGRESS)
                .build();

        Answer existingAnswer = Answer.builder()
                .id(UUID.randomUUID())
                .attempt(attempt)
                .questionId(questionId)
                .questionIndex(0)
                .selectedAnswer("\"A\"")
                .build();

        when(attemptRepository.findByIdAndStudentId(attemptId, studentId))
                .thenReturn(Optional.of(attempt));
        when(answerRepository.findByAttemptIdAndQuestionId(attemptId, questionId))
                .thenReturn(Optional.of(existingAnswer));
        when(answerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SubmitAnswerRequest request = SubmitAnswerRequest.builder()
                .questionId(questionId)
                .questionIndex(0)
                .selectedAnswer("B")
                .build();

        AnswerDto result = testTakingService.saveAnswer(attemptId, studentId, request);

        assertNotNull(result);
        verify(answerRepository).save(any());
    }

    @Test
    void saveAnswer_attemptNotInProgress_throwsException() {
        TestAttempt attempt = TestAttempt.builder()
                .id(attemptId)
                .assignment(assignment)
                .studentId(studentId)
                .status(AttemptStatus.SUBMITTED)
                .build();

        when(attemptRepository.findByIdAndStudentId(attemptId, studentId))
                .thenReturn(Optional.of(attempt));

        SubmitAnswerRequest request = SubmitAnswerRequest.builder()
                .questionId(UUID.randomUUID())
                .questionIndex(0)
                .selectedAnswer("A")
                .build();

        assertThrows(BusinessException.class, () ->
                testTakingService.saveAnswer(attemptId, studentId, request));
    }

    // ==================== Submit Attempt ====================

    @Test
    void submitAttempt_valid_submitsAndGrades() {
        TestAttempt attempt = TestAttempt.builder()
                .id(attemptId)
                .assignment(assignment)
                .studentId(studentId)
                .startedAt(LocalDateTime.now().minusMinutes(30))
                .status(AttemptStatus.IN_PROGRESS)
                .tabSwitchCount(0)
                .build();

        when(attemptRepository.findByIdAndStudentId(attemptId, studentId))
                .thenReturn(Optional.of(attempt));
        when(attemptRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(gradingService.gradeAttempt(any())).thenAnswer(i -> {
            TestAttempt a = i.getArgument(0);
            a.setStatus(AttemptStatus.AUTO_GRADED);
            a.setRawScore(new java.math.BigDecimal("8.00"));
            a.setMaxScore(new java.math.BigDecimal("10.00"));
            a.setPercentage(new java.math.BigDecimal("80.00"));
            return a;
        });
        when(answerRepository.findByAttemptIdOrderByQuestionIndexAsc(attemptId)).thenReturn(List.of());

        AttemptDto result = testTakingService.submitAttempt(attemptId, studentId);

        assertNotNull(result);
        assertNotNull(result.getSubmittedAt());
        verify(gradingService).gradeAttempt(any());
    }

    @Test
    void submitAttempt_alreadySubmitted_throwsException() {
        TestAttempt attempt = TestAttempt.builder()
                .id(attemptId)
                .assignment(assignment)
                .studentId(studentId)
                .status(AttemptStatus.SUBMITTED)
                .build();

        when(attemptRepository.findByIdAndStudentId(attemptId, studentId))
                .thenReturn(Optional.of(attempt));

        assertThrows(BusinessException.class, () ->
                testTakingService.submitAttempt(attemptId, studentId));
    }

    // ==================== Tab Switch ====================

    @Test
    void reportTabSwitch_incrementsCounter() {
        TestAttempt attempt = TestAttempt.builder()
                .id(attemptId)
                .assignment(assignment)
                .studentId(studentId)
                .startedAt(LocalDateTime.now())
                .status(AttemptStatus.IN_PROGRESS)
                .tabSwitchCount(2)
                .build();

        when(attemptRepository.findByIdAndStudentId(attemptId, studentId))
                .thenReturn(Optional.of(attempt));
        when(attemptRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AttemptDto result = testTakingService.reportTabSwitch(attemptId, studentId);

        assertNotNull(result);
        verify(attemptRepository).save(argThat(a -> a.getTabSwitchCount() == 3));
    }

    // ==================== Get Attempt ====================

    @Test
    void getAttempt_asStudent_returnsDto() {
        TestAttempt attempt = TestAttempt.builder()
                .id(attemptId)
                .assignment(assignment)
                .studentId(studentId)
                .startedAt(LocalDateTime.now())
                .status(AttemptStatus.IN_PROGRESS)
                .tabSwitchCount(0)
                .build();

        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));

        AttemptDto result = testTakingService.getAttempt(attemptId, studentId);

        assertNotNull(result);
        assertEquals(attemptId, result.getId());
    }

    @Test
    void getAttempt_asTeacher_returnsDto() {
        UUID teacherId = assignment.getTeacherId();
        TestAttempt attempt = TestAttempt.builder()
                .id(attemptId)
                .assignment(assignment)
                .studentId(studentId)
                .startedAt(LocalDateTime.now())
                .status(AttemptStatus.IN_PROGRESS)
                .tabSwitchCount(0)
                .build();

        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));

        AttemptDto result = testTakingService.getAttempt(attemptId, teacherId);

        assertNotNull(result);
    }

    @Test
    void getAttempt_asUnrelatedUser_throwsException() {
        UUID randomUserId = UUID.randomUUID();
        TestAttempt attempt = TestAttempt.builder()
                .id(attemptId)
                .assignment(assignment)
                .studentId(studentId)
                .startedAt(LocalDateTime.now())
                .status(AttemptStatus.IN_PROGRESS)
                .tabSwitchCount(0)
                .build();

        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));

        assertThrows(BusinessException.class, () ->
                testTakingService.getAttempt(attemptId, randomUserId));
    }

    @Test
    void getAttempt_notFound_throwsException() {
        when(attemptRepository.findById(attemptId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                testTakingService.getAttempt(attemptId, studentId));
    }
}
