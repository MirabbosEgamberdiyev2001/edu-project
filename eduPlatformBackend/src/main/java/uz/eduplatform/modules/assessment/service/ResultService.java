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
import uz.eduplatform.modules.assessment.dto.StudentResultDto;
import uz.eduplatform.modules.assessment.repository.TestAssignmentRepository;
import uz.eduplatform.modules.assessment.repository.TestAttemptRepository;
import uz.eduplatform.modules.auth.domain.User;
import uz.eduplatform.modules.auth.repository.UserRepository;
import uz.eduplatform.modules.group.domain.StudentGroup;
import uz.eduplatform.modules.group.repository.StudentGroupRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResultService {

    private final TestAssignmentRepository assignmentRepository;
    private final TestAttemptRepository attemptRepository;
    private final UserRepository userRepository;
    private final StudentGroupRepository studentGroupRepository;

    @Transactional(readOnly = true)
    public AssignmentResultDto getAssignmentResults(UUID assignmentId, UUID teacherId) {
        TestAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("TestAssignment", "id", assignmentId));

        if (!assignment.getTeacherId().equals(teacherId)) {
            throw new BusinessException("error.access.denied", null, HttpStatus.FORBIDDEN);
        }

        List<TestAttempt> attempts = attemptRepository.findByAssignmentIdOrderByCreatedAtDesc(assignmentId);

        // Group attempts by student, take the best attempt per student
        Map<UUID, List<TestAttempt>> attemptsByStudent = attempts.stream()
                .collect(Collectors.groupingBy(TestAttempt::getStudentId));

        List<StudentResultDto> studentResults = new ArrayList<>();
        for (Map.Entry<UUID, List<TestAttempt>> entry : attemptsByStudent.entrySet()) {
            UUID studentId = entry.getKey();
            List<TestAttempt> studentAttempts = entry.getValue();

            // Find best attempt (highest percentage)
            TestAttempt bestAttempt = studentAttempts.stream()
                    .filter(a -> a.getPercentage() != null)
                    .max(Comparator.comparing(TestAttempt::getPercentage))
                    .orElse(studentAttempts.get(0));

            // Get student name
            User student = userRepository.findById(studentId).orElse(null);
            String firstName = student != null ? student.getFirstName() : null;
            String lastName = student != null ? student.getLastName() : null;

            // Calculate total tab switches across all attempts
            int totalTabSwitches = studentAttempts.stream()
                    .mapToInt(a -> a.getTabSwitchCount() != null ? a.getTabSwitchCount() : 0)
                    .sum();

            studentResults.add(StudentResultDto.builder()
                    .studentId(studentId)
                    .firstName(firstName)
                    .lastName(lastName)
                    .score(bestAttempt.getRawScore())
                    .maxScore(bestAttempt.getMaxScore())
                    .percentage(bestAttempt.getPercentage())
                    .submittedAt(bestAttempt.getSubmittedAt())
                    .attemptCount(studentAttempts.size())
                    .tabSwitches(totalTabSwitches)
                    .status(bestAttempt.getStatus() != null ? bestAttempt.getStatus().name() : null)
                    .build());
        }

        // Sort by percentage descending
        studentResults.sort((a, b) -> {
            if (a.getPercentage() == null && b.getPercentage() == null) return 0;
            if (a.getPercentage() == null) return 1;
            if (b.getPercentage() == null) return -1;
            return b.getPercentage().compareTo(a.getPercentage());
        });

        // Calculate stats
        int totalStudents = assignment.getAssignedStudentIds() != null
                ? assignment.getAssignedStudentIds().size() : 0;
        long completedStudents = attemptsByStudent.values().stream()
                .filter(atts -> atts.stream().anyMatch(a -> a.getSubmittedAt() != null))
                .count();

        Double avgPct = attemptRepository.averagePercentageByAssignmentId(assignmentId);
        Double maxPct = attemptRepository.maxPercentageByAssignmentId(assignmentId);
        Double minPct = attemptRepository.minPercentageByAssignmentId(assignmentId);

        // Resolve group info
        String groupName = null;
        if (assignment.getGroupId() != null) {
            groupName = studentGroupRepository.findById(assignment.getGroupId())
                    .map(StudentGroup::getName)
                    .orElse(null);
        }

        return AssignmentResultDto.builder()
                .assignmentId(assignmentId)
                .assignmentTitle(assignment.getTitle())
                .groupId(assignment.getGroupId())
                .groupName(groupName)
                .totalStudents(totalStudents)
                .completedStudents((int) completedStudents)
                .averageScore(avgPct != null
                        ? BigDecimal.valueOf(avgPct).setScale(2, RoundingMode.HALF_UP) : null)
                .highestScore(maxPct != null
                        ? BigDecimal.valueOf(maxPct).setScale(2, RoundingMode.HALF_UP) : null)
                .lowestScore(minPct != null
                        ? BigDecimal.valueOf(minPct).setScale(2, RoundingMode.HALF_UP) : null)
                .students(studentResults)
                .build();
    }
}
