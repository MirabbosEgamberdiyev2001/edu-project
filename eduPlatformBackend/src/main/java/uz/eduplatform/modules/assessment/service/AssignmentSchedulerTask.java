package uz.eduplatform.modules.assessment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uz.eduplatform.modules.assessment.domain.AssignmentStatus;
import uz.eduplatform.modules.assessment.domain.AttemptStatus;
import uz.eduplatform.modules.assessment.domain.TestAssignment;
import uz.eduplatform.modules.assessment.domain.TestAttempt;
import uz.eduplatform.modules.assessment.repository.TestAssignmentRepository;
import uz.eduplatform.modules.assessment.repository.TestAttemptRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AssignmentSchedulerTask {

    private final TestAttemptRepository attemptRepository;
    private final TestAssignmentRepository assignmentRepository;
    private final GradingService gradingService;

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void autoSubmitExpiredAttempts() {
        LocalDateTime now = LocalDateTime.now();
        List<TestAttempt> expiredAttempts = attemptRepository.findExpiredInProgressAttempts(now);

        for (TestAttempt attempt : expiredAttempts) {
            try {
                attempt.setSubmittedAt(now);
                attempt.setStatus(AttemptStatus.SUBMITTED);
                attemptRepository.save(attempt);
                gradingService.gradeAttempt(attempt);
                log.info("Auto-submitted expired attempt {} for student {}",
                        attempt.getId(), attempt.getStudentId());
            } catch (Exception e) {
                log.error("Failed to auto-submit attempt {}: {}", attempt.getId(), e.getMessage());
            }
        }

        if (!expiredAttempts.isEmpty()) {
            log.info("Auto-submitted {} expired attempts", expiredAttempts.size());
        }
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void updateAssignmentStatuses() {
        LocalDateTime now = LocalDateTime.now();

        // SCHEDULED -> ACTIVE
        List<TestAssignment> toActivate = assignmentRepository.findScheduledAssignmentsToActivate(now);
        for (TestAssignment assignment : toActivate) {
            assignment.setStatus(AssignmentStatus.ACTIVE);
            assignmentRepository.save(assignment);
            log.info("Auto-activated assignment: {}", assignment.getId());
        }

        // ACTIVE -> COMPLETED
        List<TestAssignment> toComplete = assignmentRepository.findActiveAssignmentsToComplete(now);
        for (TestAssignment assignment : toComplete) {
            assignment.setStatus(AssignmentStatus.COMPLETED);
            assignmentRepository.save(assignment);
            log.info("Auto-completed assignment: {}", assignment.getId());
        }
    }
}
