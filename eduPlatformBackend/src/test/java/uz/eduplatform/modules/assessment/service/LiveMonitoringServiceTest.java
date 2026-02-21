package uz.eduplatform.modules.assessment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import uz.eduplatform.core.common.exception.BusinessException;
import uz.eduplatform.modules.assessment.domain.*;
import uz.eduplatform.modules.assessment.dto.LiveMonitoringDto;
import uz.eduplatform.modules.assessment.dto.LiveTestEvent;
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
class LiveMonitoringServiceTest {

    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private TestAssignmentRepository assignmentRepository;
    @Mock private TestAttemptRepository attemptRepository;
    @Mock private AnswerRepository answerRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private LiveMonitoringService liveMonitoringService;

    private UUID assignmentId;
    private UUID teacherId;
    private UUID studentId;
    private TestAssignment assignment;

    @BeforeEach
    void setUp() {
        assignmentId = UUID.randomUUID();
        teacherId = UUID.randomUUID();
        studentId = UUID.randomUUID();

        assignment = TestAssignment.builder()
                .id(assignmentId)
                .teacherId(teacherId)
                .title("Math Test")
                .status(AssignmentStatus.ACTIVE)
                .durationMinutes(45)
                .assignedStudentIds(List.of(studentId))
                .build();
    }

    @Test
    void broadcastEvent_sendsToCorrectDestination() {
        LiveTestEvent event = LiveTestEvent.builder()
                .eventType(LiveTestEvent.EventType.STUDENT_STARTED)
                .assignmentId(assignmentId)
                .studentId(studentId)
                .build();

        liveMonitoringService.broadcastEvent(event);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/assignment/" + assignmentId + "/progress"),
                eq(event));
    }

    @Test
    void broadcastEvent_swallowsException() {
        doThrow(new RuntimeException("WebSocket error"))
                .when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        LiveTestEvent event = LiveTestEvent.builder()
                .eventType(LiveTestEvent.EventType.ANSWER_SAVED)
                .assignmentId(assignmentId)
                .build();

        assertDoesNotThrow(() -> liveMonitoringService.broadcastEvent(event));
    }

    @Test
    void getLiveMonitoring_returnsSnapshot() {
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));

        TestAttempt attempt = TestAttempt.builder()
                .id(UUID.randomUUID())
                .assignment(assignment)
                .studentId(studentId)
                .startedAt(LocalDateTime.now())
                .status(AttemptStatus.IN_PROGRESS)
                .tabSwitchCount(1)
                .build();

        when(attemptRepository.findByAssignmentIdOrderByCreatedAtDesc(assignmentId))
                .thenReturn(List.of(attempt));
        when(userRepository.findById(studentId))
                .thenReturn(Optional.of(User.builder().firstName("Ali").lastName("Valiyev").build()));
        when(answerRepository.countByAttemptId(any())).thenReturn(10L);
        when(answerRepository.countByAttemptIdAndSelectedAnswerIsNotNull(any())).thenReturn(5L);

        LiveMonitoringDto result = liveMonitoringService.getLiveMonitoring(assignmentId, teacherId);

        assertNotNull(result);
        assertEquals(assignmentId, result.getAssignmentId());
        assertEquals(1, result.getTotalAssigned());
        assertEquals(1, result.getTotalStarted());
        assertEquals(1, result.getInProgress());
        assertEquals(1, result.getStudents().size());
        assertEquals("Ali Valiyev", result.getStudents().get(0).getStudentName());
        assertEquals(5, result.getStudents().get(0).getAnsweredQuestions());
    }

    @Test
    void getLiveMonitoring_wrongTeacher_throwsForbidden() {
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));

        UUID otherTeacherId = UUID.randomUUID();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> liveMonitoringService.getLiveMonitoring(assignmentId, otherTeacherId));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void getLiveMonitoring_inactiveAssignment_throwsException() {
        assignment.setStatus(AssignmentStatus.DRAFT);
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));

        assertThrows(BusinessException.class,
                () -> liveMonitoringService.getLiveMonitoring(assignmentId, teacherId));
    }
}
