package uz.eduplatform.modules.assessment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.eduplatform.core.common.exception.BusinessException;
import uz.eduplatform.core.common.exception.ResourceNotFoundException;
import uz.eduplatform.modules.assessment.domain.TestAssignment;
import uz.eduplatform.modules.assessment.domain.TestAttempt;
import uz.eduplatform.modules.assessment.dto.AssignmentResultDto;
import uz.eduplatform.modules.assessment.dto.AttemptDto;
import uz.eduplatform.modules.assessment.repository.AnswerRepository;
import uz.eduplatform.modules.assessment.repository.TestAssignmentRepository;
import uz.eduplatform.modules.assessment.repository.TestAttemptRepository;
import uz.eduplatform.modules.auth.repository.UserRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResultService {

    private final TestAssignmentRepository assignmentRepository;
    private final TestAttemptRepository attemptRepository;
    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public AssignmentResultDto getAssignmentResults(UUID assignmentId, UUID teacherId) {
        TestAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("TestAssignment", "id", assignmentId));

        if (!assignment.getTeacherId().equals(teacherId)) {
            throw new BusinessException("error.access.denied", null, HttpStatus.FORBIDDEN);
        }

        List<TestAttempt> attempts = attemptRepository.findByAssignmentIdOrderByCreatedAtDesc(assignmentId);

        List<AttemptDto> attemptDtos = attempts.stream()
                .map(a -> mapAttemptToDto(a, assignment))
                .toList();

        // Calculate stats
        int totalAssigned = assignment.getAssignedStudentIds() != null
                ? assignment.getAssignedStudentIds().size() : 0;
        long totalStarted = attemptRepository.countDistinctStudentsByAssignmentId(assignmentId);
        long totalSubmitted = attemptRepository.countSubmittedByAssignmentId(assignmentId);

        Double avgPct = attemptRepository.averagePercentageByAssignmentId(assignmentId);
        Double maxPct = attemptRepository.maxPercentageByAssignmentId(assignmentId);
        Double minPct = attemptRepository.minPercentageByAssignmentId(assignmentId);

        return AssignmentResultDto.builder()
                .assignmentId(assignmentId)
                .assignmentTitle(assignment.getTitle())
                .teacherId(teacherId)
                .totalAssigned(totalAssigned)
                .totalStarted((int) totalStarted)
                .totalSubmitted((int) totalSubmitted)
                .totalGraded((int) attempts.stream()
                        .filter(a -> a.getPercentage() != null)
                        .count())
                .averageScore(avgPct != null
                        ? BigDecimal.valueOf(avgPct).setScale(2, java.math.RoundingMode.HALF_UP) : null)
                .highestScore(maxPct != null
                        ? BigDecimal.valueOf(maxPct).setScale(2, java.math.RoundingMode.HALF_UP) : null)
                .lowestScore(minPct != null
                        ? BigDecimal.valueOf(minPct).setScale(2, java.math.RoundingMode.HALF_UP) : null)
                .averagePercentage(avgPct != null
                        ? BigDecimal.valueOf(avgPct).setScale(2, java.math.RoundingMode.HALF_UP) : null)
                .attempts(attemptDtos)
                .build();
    }

    private AttemptDto mapAttemptToDto(TestAttempt attempt, TestAssignment assignment) {
        String studentName = userRepository.findById(attempt.getStudentId())
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .orElse(null);

        long totalQuestions = answerRepository.countByAttemptId(attempt.getId());
        long answeredQuestions = answerRepository.countByAttemptIdAndSelectedAnswerIsNotNull(attempt.getId());

        return AttemptDto.builder()
                .id(attempt.getId())
                .assignmentId(assignment.getId())
                .assignmentTitle(assignment.getTitle())
                .studentId(attempt.getStudentId())
                .studentName(studentName)
                .attemptNumber(attempt.getAttemptNumber())
                .startedAt(attempt.getStartedAt())
                .submittedAt(attempt.getSubmittedAt())
                .rawScore(attempt.getRawScore())
                .maxScore(attempt.getMaxScore())
                .percentage(attempt.getPercentage())
                .status(attempt.getStatus())
                .tabSwitchCount(attempt.getTabSwitchCount())
                .ipAddress(attempt.getIpAddress())
                .flagged(attempt.getFlagged())
                .flagReason(attempt.getFlagReason())
                .totalQuestions((int) totalQuestions)
                .answeredQuestions((int) answeredQuestions)
                .createdAt(attempt.getCreatedAt())
                .build();
    }
}
