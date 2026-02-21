package uz.eduplatform.modules.assessment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uz.eduplatform.modules.assessment.domain.*;
import uz.eduplatform.modules.assessment.repository.TestAssignmentRepository;
import uz.eduplatform.modules.assessment.repository.TestAttemptRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AssignmentSchedulerTaskTest {

    @Mock private TestAttemptRepository attemptRepository;
    @Mock private TestAssignmentRepository assignmentRepository;
    @Mock private GradingService gradingService;

    @InjectMocks
    private AssignmentSchedulerTask schedulerTask;

    private TestAssignment assignment;
    private TestAttempt expiredAttempt;

    @BeforeEach
    void setUp() {
        assignment = TestAssignment.builder()
                .id(UUID.randomUUID())
                .teacherId(UUID.randomUUID())
                .title("Test Assignment")
                .status(AssignmentStatus.ACTIVE)
                .durationMinutes(30)
                .build();

        expiredAttempt = TestAttempt.builder()
                .id(UUID.randomUUID())
                .assignment(assignment)
                .studentId(UUID.randomUUID())
                .startedAt(LocalDateTime.now().minusHours(1))
                .status(AttemptStatus.IN_PROGRESS)
                .tabSwitchCount(0)
                .build();
    }

    @Test
    void autoSubmitExpiredAttempts_submitsAndGrades() {
        when(attemptRepository.findExpiredInProgressAttempts(any()))
                .thenReturn(List.of(expiredAttempt));
        when(attemptRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(gradingService.gradeAttempt(any())).thenAnswer(i -> i.getArgument(0));

        schedulerTask.autoSubmitExpiredAttempts();

        verify(attemptRepository).save(argThat(a ->
                a.getStatus() == AttemptStatus.SUBMITTED && a.getSubmittedAt() != null));
        verify(gradingService).gradeAttempt(expiredAttempt);
    }

    @Test
    void autoSubmitExpiredAttempts_noExpired_doesNothing() {
        when(attemptRepository.findExpiredInProgressAttempts(any()))
                .thenReturn(List.of());

        schedulerTask.autoSubmitExpiredAttempts();

        verify(attemptRepository, never()).save(any());
        verify(gradingService, never()).gradeAttempt(any());
    }

    @Test
    void updateAssignmentStatuses_activatesScheduled() {
        TestAssignment scheduledAssignment = TestAssignment.builder()
                .id(UUID.randomUUID())
                .status(AssignmentStatus.SCHEDULED)
                .startTime(LocalDateTime.now().minusMinutes(5))
                .build();

        when(assignmentRepository.findScheduledAssignmentsToActivate(any()))
                .thenReturn(List.of(scheduledAssignment));
        when(assignmentRepository.findActiveAssignmentsToComplete(any()))
                .thenReturn(List.of());
        when(assignmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        schedulerTask.updateAssignmentStatuses();

        verify(assignmentRepository).save(argThat(a -> a.getStatus() == AssignmentStatus.ACTIVE));
    }

    @Test
    void updateAssignmentStatuses_completesExpired() {
        TestAssignment activeAssignment = TestAssignment.builder()
                .id(UUID.randomUUID())
                .status(AssignmentStatus.ACTIVE)
                .endTime(LocalDateTime.now().minusMinutes(5))
                .build();

        when(assignmentRepository.findScheduledAssignmentsToActivate(any()))
                .thenReturn(List.of());
        when(assignmentRepository.findActiveAssignmentsToComplete(any()))
                .thenReturn(List.of(activeAssignment));
        when(assignmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        schedulerTask.updateAssignmentStatuses();

        verify(assignmentRepository).save(argThat(a -> a.getStatus() == AssignmentStatus.COMPLETED));
    }
}
