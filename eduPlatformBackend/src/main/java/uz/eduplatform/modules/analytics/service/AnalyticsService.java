package uz.eduplatform.modules.analytics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.eduplatform.core.common.exception.ResourceNotFoundException;
import uz.eduplatform.core.i18n.TranslatedField;
import uz.eduplatform.modules.analytics.dto.GroupStatisticsDto;
import uz.eduplatform.modules.analytics.dto.StudentAnalyticsDto;
import uz.eduplatform.modules.analytics.dto.TeacherDashboardDto;
import uz.eduplatform.modules.assessment.domain.*;
import uz.eduplatform.modules.assessment.repository.AnswerRepository;
import uz.eduplatform.modules.assessment.repository.TestAssignmentRepository;
import uz.eduplatform.modules.assessment.repository.TestAttemptRepository;
import uz.eduplatform.modules.auth.domain.User;
import uz.eduplatform.modules.auth.repository.UserRepository;
import uz.eduplatform.modules.content.repository.SubjectRepository;
import uz.eduplatform.modules.content.domain.Topic;
import uz.eduplatform.modules.content.repository.TopicRepository;
import uz.eduplatform.modules.group.domain.StudentGroup;
import uz.eduplatform.modules.group.repository.GroupMemberRepository;
import uz.eduplatform.modules.group.repository.StudentGroupRepository;
import uz.eduplatform.modules.test.domain.TestHistory;
import uz.eduplatform.modules.test.repository.TestHistoryRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final TestAssignmentRepository assignmentRepository;
    private final TestAttemptRepository attemptRepository;
    private final AnswerRepository answerRepository;
    private final StudentGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final TestHistoryRepository testHistoryRepository;

    private static final BigDecimal AT_RISK_THRESHOLD = new BigDecimal("40.00");
    private static final BigDecimal WEAK_AREA_THRESHOLD = new BigDecimal("60.00");

    @Transactional(readOnly = true)
    public TeacherDashboardDto getTeacherDashboard(UUID teacherId) {
        // Group stats
        long totalGroups = groupRepository.countByTeacherId(teacherId);

        // Collect unique students across all teacher's groups
        Set<UUID> studentIds = new HashSet<>();
        groupRepository.findByTeacherIdOrderByCreatedAtDesc(
                teacherId, PageRequest.of(0, 1000)).getContent()
                .forEach(g -> studentIds.addAll(
                        groupRepository.findStudentIdsByGroupId(g.getId())));

        // Assignment stats
        Page<TestAssignment> allAssignments = assignmentRepository.findByTeacherIdOrderByCreatedAtDesc(
                teacherId, PageRequest.of(0, 1000));
        long totalAssignments = allAssignments.getTotalElements();
        long activeAssignments = assignmentRepository.countByTeacherIdAndStatus(teacherId, AssignmentStatus.ACTIVE);

        // Collect all attempts from teacher's assignments
        List<TestAttempt> allAttempts = new ArrayList<>();
        allAssignments.getContent().forEach(a ->
                allAttempts.addAll(attemptRepository.findByAssignmentIdOrderByCreatedAtDesc(a.getId())));

        // Overall average score
        BigDecimal overallAverage = calculateAverage(allAttempts);

        // Student performance aggregation
        Map<UUID, List<TestAttempt>> attemptsByStudent = allAttempts.stream()
                .collect(Collectors.groupingBy(TestAttempt::getStudentId));

        List<TeacherDashboardDto.StudentPerformanceDto> studentPerformances = attemptsByStudent.entrySet()
                .stream()
                .map(entry -> buildStudentPerformance(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(TeacherDashboardDto.StudentPerformanceDto::getAverageScore,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        // Top 5 students
        List<TeacherDashboardDto.StudentPerformanceDto> topStudents = studentPerformances.stream()
                .limit(5)
                .toList();

        // At-risk students (below threshold)
        List<TeacherDashboardDto.StudentPerformanceDto> atRiskStudents = studentPerformances.stream()
                .filter(s -> s.getAverageScore() != null && s.getAverageScore().compareTo(AT_RISK_THRESHOLD) < 0)
                .toList();

        // Recent 10 assignments with stats
        List<TeacherDashboardDto.AssignmentSummaryDto> recentAssignments = allAssignments.getContent()
                .stream()
                .limit(10)
                .map(this::buildAssignmentSummary)
                .toList();

        // Topic-level breakdown
        List<TeacherDashboardDto.TopicPerformanceDto> topicBreakdown = buildTopicBreakdown(allAssignments.getContent(), allAttempts);

        return TeacherDashboardDto.builder()
                .totalGroups((int) totalGroups)
                .totalStudents(studentIds.size())
                .totalAssignments((int) totalAssignments)
                .activeAssignments((int) activeAssignments)
                .overallAverageScore(overallAverage)
                .topStudents(topStudents)
                .atRiskStudents(atRiskStudents)
                .recentAssignments(recentAssignments)
                .topicBreakdown(topicBreakdown)
                .build();
    }

    @Transactional(readOnly = true)
    public StudentAnalyticsDto getStudentAnalytics(UUID studentId, UUID teacherId) {
        // Validate teacher has this student in at least one of their groups
        boolean hasAccess = false;
        List<StudentGroup> teacherGroups = groupRepository.findByTeacherIdOrderByCreatedAtDesc(
                teacherId, PageRequest.of(0, 1000)).getContent();
        for (StudentGroup group : teacherGroups) {
            List<UUID> memberIds = groupRepository.findStudentIdsByGroupId(group.getId());
            if (memberIds.contains(studentId)) {
                hasAccess = true;
                break;
            }
        }
        if (!hasAccess) {
            throw new uz.eduplatform.core.common.exception.BusinessException(
                    "error.access.denied", null, org.springframework.http.HttpStatus.FORBIDDEN);
        }

        return getStudentAnalytics(studentId);
    }

    @Transactional(readOnly = true)
    public StudentAnalyticsDto getStudentAnalytics(UUID studentId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", studentId));

        // All attempts for this student
        List<TestAttempt> attempts = attemptRepository.findByStudentIdOrderByCreatedAtDesc(studentId);

        // Total assignments assigned to this student
        String studentIdJson = "[\"" + studentId + "\"]";
        Page<TestAssignment> studentAssignmentsPage = assignmentRepository.findAssignmentsForStudent(
                studentIdJson, org.springframework.data.domain.Pageable.unpaged());
        long totalAssignments = studentAssignmentsPage.getTotalElements();

        long completedAttempts = attempts.stream()
                .filter(a -> a.getPercentage() != null)
                .count();

        BigDecimal overallAverage = calculateAverage(attempts);

        // Score trend (last 20 attempts)
        List<StudentAnalyticsDto.ScoreTrendDto> scoreTrend = attempts.stream()
                .filter(a -> a.getPercentage() != null)
                .limit(20)
                .map(a -> StudentAnalyticsDto.ScoreTrendDto.builder()
                        .attemptId(a.getId())
                        .assignmentTitle(a.getAssignment() != null ? a.getAssignment().getTitle() : "Unknown")
                        .percentage(a.getPercentage())
                        .submittedAt(a.getSubmittedAt())
                        .build())
                .toList();

        // Subject breakdown
        List<StudentAnalyticsDto.SubjectBreakdownDto> subjectBreakdown = buildSubjectBreakdown(attempts);

        // Upcoming assignments
        List<StudentAnalyticsDto.UpcomingAssignmentDto> upcomingAssignments = buildUpcomingAssignments(studentAssignmentsPage.getContent());

        // In-progress attempts
        List<StudentAnalyticsDto.InProgressAttemptDto> inProgressAttempts = buildInProgressAttempts(studentId);

        // Weekly activity
        StudentAnalyticsDto.WeeklyActivityDto weeklyActivity = buildWeeklyActivity(studentId, attempts);

        // Weak areas
        List<StudentAnalyticsDto.WeakAreaDto> weakAreas = buildWeakAreas(subjectBreakdown);

        // Time management
        StudentAnalyticsDto.TimeManagementDto timeManagement = buildTimeManagement(attempts);

        return StudentAnalyticsDto.builder()
                .studentId(studentId)
                .studentName(student.getFirstName() + " " + student.getLastName())
                .totalAssignments((int) totalAssignments)
                .completedAssignments((int) completedAttempts)
                .pendingAssignments((int) (totalAssignments - completedAttempts))
                .overallAverageScore(overallAverage)
                .scoreTrend(scoreTrend)
                .subjectBreakdown(subjectBreakdown)
                .upcomingAssignments(upcomingAssignments)
                .inProgressAttempts(inProgressAttempts)
                .weeklyActivity(weeklyActivity)
                .weakAreas(weakAreas)
                .timeManagement(timeManagement)
                .build();
    }

    @Transactional(readOnly = true)
    public GroupStatisticsDto getGroupStatistics(UUID groupId, UUID teacherId) {
        StudentGroup group = groupRepository.findByIdAndTeacherId(groupId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentGroup", "id", groupId));

        List<UUID> studentIds = groupRepository.findStudentIdsByGroupId(groupId);

        // Collect all teacher's assignments that include group students
        Page<TestAssignment> teacherAssignments = assignmentRepository.findByTeacherIdOrderByCreatedAtDesc(
                teacherId, PageRequest.of(0, 1000));

        // Filter assignments relevant to this group (assignments that have group students)
        List<TestAssignment> groupAssignments = teacherAssignments.getContent().stream()
                .filter(a -> a.getAssignedStudentIds() != null
                        && a.getAssignedStudentIds().stream().anyMatch(studentIds::contains))
                .toList();

        // Collect all attempts for group students
        List<TestAttempt> allGroupAttempts = new ArrayList<>();
        for (TestAssignment assignment : groupAssignments) {
            List<TestAttempt> assignmentAttempts = attemptRepository.findByAssignmentIdOrderByCreatedAtDesc(assignment.getId());
            assignmentAttempts.stream()
                    .filter(a -> studentIds.contains(a.getStudentId()))
                    .forEach(allGroupAttempts::add);
        }

        BigDecimal groupAverage = calculateAverage(allGroupAttempts);

        // Highest and lowest scores
        BigDecimal highest = allGroupAttempts.stream()
                .filter(a -> a.getPercentage() != null)
                .map(TestAttempt::getPercentage)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);

        BigDecimal lowest = allGroupAttempts.stream()
                .filter(a -> a.getPercentage() != null)
                .map(TestAttempt::getPercentage)
                .min(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);

        // Completion rate
        long totalAssignedSlots = groupAssignments.stream()
                .mapToLong(a -> a.getAssignedStudentIds().stream().filter(studentIds::contains).count())
                .sum();
        long totalSubmitted = allGroupAttempts.stream()
                .filter(a -> a.getPercentage() != null)
                .count();
        BigDecimal completionRate = totalAssignedSlots > 0
                ? BigDecimal.valueOf(totalSubmitted * 100.0 / totalAssignedSlots).setScale(2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Student scores
        Map<UUID, List<TestAttempt>> attemptsByStudent = allGroupAttempts.stream()
                .collect(Collectors.groupingBy(TestAttempt::getStudentId));

        List<GroupStatisticsDto.StudentScoreDto> studentScores = studentIds.stream()
                .map(sid -> {
                    String name = userRepository.findById(sid)
                            .map(u -> u.getFirstName() + " " + u.getLastName()).orElse("Unknown");
                    List<TestAttempt> studentAttempts = attemptsByStudent.getOrDefault(sid, List.of());
                    BigDecimal avg = calculateAverage(studentAttempts);
                    long completed = studentAttempts.stream().filter(a -> a.getPercentage() != null).count();

                    // Simple trend: compare last 3 vs previous 3
                    String trend = calculateTrend(studentAttempts);

                    return GroupStatisticsDto.StudentScoreDto.builder()
                            .studentId(sid)
                            .studentName(name)
                            .averageScore(avg)
                            .totalAttempts(studentAttempts.size())
                            .completedAttempts((int) completed)
                            .trend(trend)
                            .build();
                })
                .sorted(Comparator.comparing(GroupStatisticsDto.StudentScoreDto::getAverageScore,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        // Assignment breakdown
        List<GroupStatisticsDto.AssignmentBreakdownDto> assignmentBreakdown = groupAssignments.stream()
                .map(a -> {
                    List<TestAttempt> assignmentAttempts = allGroupAttempts.stream()
                            .filter(att -> att.getAssignment() != null && att.getAssignment().getId().equals(a.getId()))
                            .toList();
                    BigDecimal avg = calculateAverage(assignmentAttempts);
                    long submitted = assignmentAttempts.stream().filter(att -> att.getPercentage() != null).count();
                    int totalAssigned = (int) a.getAssignedStudentIds().stream().filter(studentIds::contains).count();

                    return GroupStatisticsDto.AssignmentBreakdownDto.builder()
                            .assignmentId(a.getId())
                            .title(a.getTitle())
                            .averageScore(avg)
                            .submittedCount((int) submitted)
                            .totalAssigned(totalAssigned)
                            .status(a.getStatus().name())
                            .build();
                })
                .toList();

        return GroupStatisticsDto.builder()
                .groupId(groupId)
                .groupName(group.getName())
                .totalMembers(studentIds.size())
                .totalAssignments(groupAssignments.size())
                .completedAssignments((int) groupAssignments.stream()
                        .filter(a -> a.getStatus() == AssignmentStatus.COMPLETED).count())
                .groupAverageScore(groupAverage)
                .highestScore(highest)
                .lowestScore(lowest)
                .completionRate(completionRate)
                .studentScores(studentScores)
                .assignmentBreakdown(assignmentBreakdown)
                .build();
    }

    private String calculateTrend(List<TestAttempt> attempts) {
        List<TestAttempt> scored = attempts.stream()
                .filter(a -> a.getPercentage() != null && a.getSubmittedAt() != null)
                .sorted(Comparator.comparing(TestAttempt::getSubmittedAt).reversed())
                .toList();

        if (scored.size() < 2) return "STABLE";

        List<TestAttempt> recent = scored.stream().limit(3).toList();
        List<TestAttempt> previous = scored.stream().skip(3).limit(3).toList();

        if (previous.isEmpty()) return "STABLE";

        BigDecimal recentAvg = calculateAverage(recent);
        BigDecimal previousAvg = calculateAverage(previous);

        int cmp = recentAvg.compareTo(previousAvg);
        if (cmp > 0) return "UP";
        if (cmp < 0) return "DOWN";
        return "STABLE";
    }

    // ── New Dashboard Helpers ──

    private List<StudentAnalyticsDto.UpcomingAssignmentDto> buildUpcomingAssignments(List<TestAssignment> assignments) {
        LocalDateTime now = LocalDateTime.now();
        return assignments.stream()
                .filter(a -> a.getEndTime() != null && a.getEndTime().isAfter(now))
                .filter(a -> a.getStatus() == AssignmentStatus.ACTIVE || a.getStatus() == AssignmentStatus.SCHEDULED)
                .limit(10)
                .map(a -> StudentAnalyticsDto.UpcomingAssignmentDto.builder()
                        .assignmentId(a.getId())
                        .title(a.getTitle())
                        .startTime(a.getStartTime())
                        .endTime(a.getEndTime())
                        .durationMinutes(a.getDurationMinutes())
                        .status(a.getStatus().name())
                        .build())
                .toList();
    }

    private List<StudentAnalyticsDto.InProgressAttemptDto> buildInProgressAttempts(UUID studentId) {
        List<TestAttempt> inProgress = attemptRepository.findByStudentIdAndStatus(studentId, AttemptStatus.IN_PROGRESS);
        return inProgress.stream()
                .map(attempt -> {
                    TestAssignment assignment = attempt.getAssignment();
                    Long remainingSeconds = null;
                    if (assignment != null) {
                        LocalDateTime deadline = attempt.getStartedAt().plusMinutes(assignment.getDurationMinutes());
                        Duration remaining = Duration.between(LocalDateTime.now(), deadline);
                        remainingSeconds = Math.max(0, remaining.getSeconds());
                    }
                    long answeredQuestions = answerRepository.countByAttemptIdAndSelectedAnswerIsNotNull(attempt.getId());
                    long totalQuestions = answerRepository.countByAttemptId(attempt.getId());

                    return StudentAnalyticsDto.InProgressAttemptDto.builder()
                            .attemptId(attempt.getId())
                            .assignmentId(assignment != null ? assignment.getId() : null)
                            .assignmentTitle(assignment != null ? assignment.getTitle() : "Unknown")
                            .startedAt(attempt.getStartedAt())
                            .remainingSeconds(remainingSeconds)
                            .answeredQuestions((int) answeredQuestions)
                            .totalQuestions((int) totalQuestions)
                            .build();
                })
                .toList();
    }

    private StudentAnalyticsDto.WeeklyActivityDto buildWeeklyActivity(UUID studentId, List<TestAttempt> allAttempts) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfThisWeek = now.with(ChronoField.DAY_OF_WEEK, 1).toLocalDate().atStartOfDay();
        LocalDateTime startOfLastWeek = startOfThisWeek.minusWeeks(1);

        List<TestAttempt> thisWeek = allAttempts.stream()
                .filter(a -> a.getSubmittedAt() != null && a.getSubmittedAt().isAfter(startOfThisWeek))
                .toList();

        List<TestAttempt> lastWeek = allAttempts.stream()
                .filter(a -> a.getSubmittedAt() != null
                        && a.getSubmittedAt().isAfter(startOfLastWeek)
                        && a.getSubmittedAt().isBefore(startOfThisWeek))
                .toList();

        BigDecimal avgThisWeek = calculateAverage(thisWeek);

        long totalTimeMinutes = thisWeek.stream()
                .filter(a -> a.getStartedAt() != null && a.getSubmittedAt() != null)
                .mapToLong(a -> Duration.between(a.getStartedAt(), a.getSubmittedAt()).toMinutes())
                .sum();

        return StudentAnalyticsDto.WeeklyActivityDto.builder()
                .testsCompletedThisWeek(thisWeek.size())
                .testsCompletedLastWeek(lastWeek.size())
                .averageScoreThisWeek(avgThisWeek)
                .totalTimeSpentMinutes(totalTimeMinutes)
                .build();
    }

    private List<StudentAnalyticsDto.WeakAreaDto> buildWeakAreas(List<StudentAnalyticsDto.SubjectBreakdownDto> subjectBreakdown) {
        return subjectBreakdown.stream()
                .filter(s -> s.getAverageScore() != null && s.getAverageScore().compareTo(WEAK_AREA_THRESHOLD) < 0)
                .map(s -> StudentAnalyticsDto.WeakAreaDto.builder()
                        .subjectName(s.getSubjectName())
                        .averageScore(s.getAverageScore())
                        .attemptCount(s.getAttemptCount())
                        .build())
                .sorted(Comparator.comparing(StudentAnalyticsDto.WeakAreaDto::getAverageScore))
                .toList();
    }

    private StudentAnalyticsDto.TimeManagementDto buildTimeManagement(List<TestAttempt> attempts) {
        List<TestAttempt> completedWithTime = attempts.stream()
                .filter(a -> a.getStartedAt() != null && a.getSubmittedAt() != null)
                .toList();

        if (completedWithTime.isEmpty()) {
            return StudentAnalyticsDto.TimeManagementDto.builder()
                    .averageTimePerTestMinutes(BigDecimal.ZERO)
                    .averageTimePerQuestionSeconds(BigDecimal.ZERO)
                    .build();
        }

        long totalTestMinutes = completedWithTime.stream()
                .mapToLong(a -> Duration.between(a.getStartedAt(), a.getSubmittedAt()).toMinutes())
                .sum();

        BigDecimal avgTestMinutes = BigDecimal.valueOf(totalTestMinutes)
                .divide(BigDecimal.valueOf(completedWithTime.size()), 2, RoundingMode.HALF_UP);

        // Average time per question
        long totalQuestions = completedWithTime.stream()
                .mapToLong(a -> answerRepository.countByAttemptId(a.getId()))
                .sum();

        long totalTestSeconds = completedWithTime.stream()
                .mapToLong(a -> Duration.between(a.getStartedAt(), a.getSubmittedAt()).getSeconds())
                .sum();

        BigDecimal avgTimePerQuestion = totalQuestions > 0
                ? BigDecimal.valueOf(totalTestSeconds).divide(BigDecimal.valueOf(totalQuestions), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return StudentAnalyticsDto.TimeManagementDto.builder()
                .averageTimePerTestMinutes(avgTestMinutes)
                .averageTimePerQuestionSeconds(avgTimePerQuestion)
                .build();
    }

    private List<TeacherDashboardDto.TopicPerformanceDto> buildTopicBreakdown(
            List<TestAssignment> assignments, List<TestAttempt> allAttempts) {
        // Group attempts by topic (via assignment -> testHistory -> topicIds)
        Map<UUID, List<TestAttempt>> byTopic = new HashMap<>();
        Map<UUID, UUID> topicToSubject = new HashMap<>();

        for (TestAssignment assignment : assignments) {
            UUID testHistoryId = assignment.getTestHistoryId();
            if (testHistoryId == null) continue;

            testHistoryRepository.findById(testHistoryId).ifPresent(th -> {
                List<UUID> topicIds = th.getTopicIds();
                UUID subjectId = th.getSubjectId();
                if (topicIds == null || topicIds.isEmpty()) return;

                List<TestAttempt> assignmentAttempts = allAttempts.stream()
                        .filter(a -> a.getAssignment() != null && a.getAssignment().getId().equals(assignment.getId()))
                        .toList();

                for (UUID topicId : topicIds) {
                    byTopic.computeIfAbsent(topicId, k -> new ArrayList<>()).addAll(assignmentAttempts);
                    topicToSubject.putIfAbsent(topicId, subjectId);
                }
            });
        }

        return byTopic.entrySet().stream()
                .map(entry -> {
                    UUID topicId = entry.getKey();
                    List<TestAttempt> topicAttempts = entry.getValue();
                    BigDecimal avg = calculateAverage(topicAttempts);

                    String topicName = topicRepository.findById(topicId)
                            .map(t -> TranslatedField.resolve(t.getName()))
                            .orElse("Unknown");

                    UUID subjectId = topicToSubject.get(topicId);
                    String subjectName = subjectId != null
                            ? subjectRepository.findById(subjectId)
                            .map(s -> TranslatedField.resolve(s.getName()))
                            .orElse("Unknown")
                            : "Unknown";

                    String difficulty;
                    if (avg.compareTo(new BigDecimal("80")) >= 0) {
                        difficulty = "EASY";
                    } else if (avg.compareTo(new BigDecimal("50")) >= 0) {
                        difficulty = "MEDIUM";
                    } else {
                        difficulty = "HARD";
                    }

                    return TeacherDashboardDto.TopicPerformanceDto.builder()
                            .topicId(topicId)
                            .topicName(topicName)
                            .subjectName(subjectName)
                            .averageScore(avg)
                            .attemptCount(topicAttempts.size())
                            .difficulty(difficulty)
                            .build();
                })
                .sorted(Comparator.comparing(TeacherDashboardDto.TopicPerformanceDto::getAverageScore))
                .toList();
    }

    // ── Existing Helpers ──

    private BigDecimal calculateAverage(List<TestAttempt> attempts) {
        List<BigDecimal> scores = attempts.stream()
                .filter(a -> a.getPercentage() != null)
                .map(TestAttempt::getPercentage)
                .toList();

        if (scores.isEmpty()) return BigDecimal.ZERO;

        BigDecimal total = scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
    }

    private TeacherDashboardDto.StudentPerformanceDto buildStudentPerformance(
            UUID studentId, List<TestAttempt> attempts) {
        String studentName = userRepository.findById(studentId)
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .orElse("Unknown");

        BigDecimal avgScore = calculateAverage(attempts);
        long completed = attempts.stream().filter(a -> a.getPercentage() != null).count();

        return TeacherDashboardDto.StudentPerformanceDto.builder()
                .studentId(studentId)
                .studentName(studentName)
                .averageScore(avgScore)
                .totalAttempts(attempts.size())
                .completedAttempts((int) completed)
                .build();
    }

    private TeacherDashboardDto.AssignmentSummaryDto buildAssignmentSummary(TestAssignment assignment) {
        Double avgPercentage = attemptRepository.averagePercentageByAssignmentId(assignment.getId());
        long submittedCount = attemptRepository.countSubmittedByAssignmentId(assignment.getId());

        return TeacherDashboardDto.AssignmentSummaryDto.builder()
                .assignmentId(assignment.getId())
                .title(assignment.getTitle())
                .totalStudents(assignment.getAssignedStudentIds() != null
                        ? assignment.getAssignedStudentIds().size() : 0)
                .submittedCount((int) submittedCount)
                .averageScore(avgPercentage != null
                        ? BigDecimal.valueOf(avgPercentage).setScale(2, RoundingMode.HALF_UP) : null)
                .status(assignment.getStatus().name())
                .build();
    }

    private List<StudentAnalyticsDto.SubjectBreakdownDto> buildSubjectBreakdown(List<TestAttempt> attempts) {
        // Group attempts by subject (via assignment -> testHistory -> subject)
        Map<UUID, List<TestAttempt>> bySubject = new HashMap<>();

        for (TestAttempt attempt : attempts) {
            if (attempt.getAssignment() == null) continue;
            UUID testHistoryId = attempt.getAssignment().getTestHistoryId();
            if (testHistoryId == null) continue;

            testHistoryRepository.findById(testHistoryId).ifPresent(th -> {
                UUID subjectId = th.getSubjectId();
                if (subjectId != null) {
                    bySubject.computeIfAbsent(subjectId, k -> new ArrayList<>()).add(attempt);
                }
            });
        }

        return bySubject.entrySet().stream()
                .map(entry -> {
                    String subjectName = subjectRepository.findById(entry.getKey())
                            .map(s -> TranslatedField.resolve(s.getName()))
                            .orElse("Unknown");

                    BigDecimal avgScore = calculateAverage(entry.getValue());

                    return StudentAnalyticsDto.SubjectBreakdownDto.builder()
                            .subjectName(subjectName)
                            .attemptCount(entry.getValue().size())
                            .averageScore(avgScore)
                            .build();
                })
                .sorted(Comparator.comparing(StudentAnalyticsDto.SubjectBreakdownDto::getSubjectName))
                .toList();
    }
}
