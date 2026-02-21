package uz.eduplatform.modules.assessment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.eduplatform.core.common.exception.BusinessException;
import uz.eduplatform.core.common.exception.ResourceNotFoundException;
import uz.eduplatform.modules.assessment.domain.AssignmentStatus;
import uz.eduplatform.modules.assessment.domain.AttemptStatus;
import uz.eduplatform.modules.assessment.domain.TestAssignment;
import uz.eduplatform.modules.assessment.domain.TestAttempt;
import uz.eduplatform.modules.assessment.dto.LiveMonitoringDto;
import uz.eduplatform.modules.assessment.dto.LiveTestEvent;
import uz.eduplatform.modules.assessment.repository.AnswerRepository;
import uz.eduplatform.modules.assessment.repository.TestAssignmentRepository;
import uz.eduplatform.modules.assessment.repository.TestAttemptRepository;
import uz.eduplatform.modules.auth.repository.UserRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LiveMonitoringService {

    private final SimpMessagingTemplate messagingTemplate;
    private final TestAssignmentRepository assignmentRepository;
    private final TestAttemptRepository attemptRepository;
    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;

    public void broadcastEvent(LiveTestEvent event) {
        try {
            String destination = "/topic/assignment/" + event.getAssignmentId() + "/progress";
            messagingTemplate.convertAndSend(destination, event);
        } catch (Exception e) {
            log.warn("Failed to broadcast WebSocket event: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public LiveMonitoringDto getLiveMonitoring(UUID assignmentId, UUID teacherId) {
        TestAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("TestAssignment", "id", assignmentId));

        if (!assignment.getTeacherId().equals(teacherId)) {
            throw new BusinessException("error.access.denied", null, HttpStatus.FORBIDDEN);
        }

        if (assignment.getStatus() != AssignmentStatus.ACTIVE) {
            throw BusinessException.ofKey("monitoring.assignment.not.active");
        }

        List<TestAttempt> attempts = attemptRepository.findByAssignmentIdOrderByCreatedAtDesc(assignmentId);

        List<LiveMonitoringDto.StudentProgressDto> students = attempts.stream()
                .map(attempt -> buildStudentProgress(attempt, assignment))
                .toList();

        int totalAssigned = assignment.getAssignedStudentIds() != null ? assignment.getAssignedStudentIds().size() : 0;
        long totalStarted = attempts.stream().map(TestAttempt::getStudentId).distinct().count();
        long inProgress = attempts.stream().filter(a -> a.getStatus() == AttemptStatus.IN_PROGRESS).count();
        long submitted = attempts.stream().filter(a -> a.getStatus() != AttemptStatus.IN_PROGRESS).count();

        return LiveMonitoringDto.builder()
                .assignmentId(assignmentId)
                .assignmentTitle(assignment.getTitle())
                .totalAssigned(totalAssigned)
                .totalStarted((int) totalStarted)
                .inProgress((int) inProgress)
                .submitted((int) submitted)
                .students(students)
                .build();
    }

    private LiveMonitoringDto.StudentProgressDto buildStudentProgress(TestAttempt attempt, TestAssignment assignment) {
        String studentName = userRepository.findById(attempt.getStudentId())
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .orElse("Unknown");

        long answeredQuestions = answerRepository.countByAttemptIdAndSelectedAnswerIsNotNull(attempt.getId());
        long totalQuestions = answerRepository.countByAttemptId(attempt.getId());

        Long remainingSeconds = null;
        if (attempt.getStatus() == AttemptStatus.IN_PROGRESS) {
            LocalDateTime deadline = attempt.getStartedAt().plusMinutes(assignment.getDurationMinutes());
            Duration remaining = Duration.between(LocalDateTime.now(), deadline);
            remainingSeconds = Math.max(0, remaining.getSeconds());
        }

        double percentage = totalQuestions > 0 ? (double) answeredQuestions / totalQuestions * 100 : 0;

        return LiveMonitoringDto.StudentProgressDto.builder()
                .studentId(attempt.getStudentId())
                .studentName(studentName)
                .attemptId(attempt.getId())
                .status(attempt.getStatus().name())
                .answeredQuestions((int) answeredQuestions)
                .totalQuestions((int) totalQuestions)
                .tabSwitchCount(attempt.getTabSwitchCount() != null ? attempt.getTabSwitchCount() : 0)
                .remainingSeconds(remainingSeconds)
                .percentage(percentage)
                .build();
    }
}
