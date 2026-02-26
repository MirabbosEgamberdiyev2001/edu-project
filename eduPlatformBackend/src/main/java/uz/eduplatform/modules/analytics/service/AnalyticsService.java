package uz.eduplatform.modules.analytics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
import uz.eduplatform.modules.content.repository.TopicRepository;
import uz.eduplatform.modules.group.domain.StudentGroup;
import uz.eduplatform.modules.group.repository.GroupMemberRepository;
import uz.eduplatform.modules.group.repository.StudentGroupRepository;
import uz.eduplatform.modules.test.repository.TestHistoryRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
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
    private static final BigDecimal STRONG_AREA_THRESHOLD = new BigDecimal("80.00");

    // ────────────────────────────────────────────────
    //  Teacher Dashboard
    // ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TeacherDashboardDto getTeacherDashboard(UUID teacherId) {
        long totalGroups = groupRepository.countByTeacherId(teacherId);

        Set<UUID> studentIds = new HashSet<>();
        groupRepository.findByTeacherIdOrderByCreatedAtDesc(teacherId, PageRequest.of(0, 1000))
                .getContent()
                .forEach(g -> studentIds.addAll(groupRepository.findStudentIdsByGroupId(g.getId())));

        Page<TestAssignment> allAssignments = assignmentRepository
                .findByTeacherIdOrderByCreatedAtDesc(teacherId, PageRequest.of(0, 1000));
        long totalAssignments = allAssignments.getTotalElements();
        long activeAssignments = assignmentRepository
                .countByTeacherIdAndStatus(teacherId, AssignmentStatus.ACTIVE);

        List<TestAttempt> allAttempts = new ArrayList<>();
        allAssignments.getContent().forEach(a ->
                allAttempts.addAll(attemptRepository.findByAssignmentIdOrderByCreatedAtDesc(a.getId())));

        BigDecimal overallAverage = calculateAverage(allAttempts);

        long totalAssignedSlots = allAssignments.getContent().stream()
                .mapToLong(a -> a.getAssignedStudentIds() != null ? a.getAssignedStudentIds().size() : 0)
                .sum();
        long totalSubmitted = allAttempts.stream().filter(a -> a.getPercentage() != null).count();
        double completionRate = totalAssignedSlots > 0
                ? totalSubmitted * 100.0 / totalAssignedSlots : 0.0;

        long totalTests = testHistoryRepository.countByUserId(teacherId);

        List<TeacherDashboardDto.TrendPointDto> testCreationTrend =
                buildMonthlyTestTrend(teacherId);

        Map<UUID, List<TestAttempt>> attemptsByStudent = allAttempts.stream()
                .collect(Collectors.groupingBy(TestAttempt::getStudentId));

        List<TeacherDashboardDto.StudentPerformanceDto> studentPerformances =
                attemptsByStudent.entrySet().stream()
                        .map(e -> buildStudentPerformance(e.getKey(), e.getValue(),
                                allAssignments.getContent()))
                        .sorted(Comparator.comparing(
                                TeacherDashboardDto.StudentPerformanceDto::getAverageScore,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                        .toList();

        List<TeacherDashboardDto.StudentPerformanceDto> topStudents =
                studentPerformances.stream().limit(5).toList();

        List<TeacherDashboardDto.StudentPerformanceDto> atRiskStudents =
                studentPerformances.stream()
                        .filter(s -> s.getAverageScore() != null
                                && s.getAverageScore().compareTo(AT_RISK_THRESHOLD) < 0)
                        .toList();

        List<TeacherDashboardDto.AssignmentSummaryDto> recentAssignments =
                allAssignments.getContent().stream()
                        .limit(10)
                        .map(this::buildAssignmentSummary)
                        .toList();

        List<TeacherDashboardDto.ActivityDto> recentActivity =
                allAssignments.getContent().stream()
                        .limit(10)
                        .map(a -> TeacherDashboardDto.ActivityDto.builder()
                                .type("ASSIGNMENT")
                                .description(a.getTitle())
                                .createdAt(a.getCreatedAt() != null
                                        ? a.getCreatedAt().toString()
                                        : LocalDateTime.now().toString())
                                .build())
                        .toList();

        List<TeacherDashboardDto.TopicPerformanceDto> topicBreakdown =
                buildTopicBreakdown(allAssignments.getContent(), allAttempts);

        return TeacherDashboardDto.builder()
                .totalGroups((int) totalGroups)
                .totalStudents(studentIds.size())
                .totalAssignments((int) totalAssignments)
                .activeAssignments((int) activeAssignments)
                .totalTests((int) totalTests)
                .overallAverageScore(overallAverage)
                .averageScore(overallAverage.doubleValue())
                .completionRate(completionRate)
                .testCreationTrend(testCreationTrend)
                .recentActivity(recentActivity)
                .topStudents(topStudents)
                .atRiskStudents(atRiskStudents)
                .recentAssignments(recentAssignments)
                .topicBreakdown(topicBreakdown)
                .build();
    }

    // ────────────────────────────────────────────────
    //  Student Analytics
    // ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public StudentAnalyticsDto getStudentAnalytics(UUID studentId, UUID teacherId) {
        boolean hasAccess = false;
        for (StudentGroup group : groupRepository
                .findByTeacherIdOrderByCreatedAtDesc(teacherId, PageRequest.of(0, 1000))
                .getContent()) {
            if (groupRepository.findStudentIdsByGroupId(group.getId()).contains(studentId)) {
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

        List<TestAttempt> attempts = attemptRepository
                .findByStudentIdOrderByCreatedAtDesc(studentId);

        String studentIdJson = "[\"" + studentId + "\"]";
        Page<TestAssignment> studentAssignmentsPage = assignmentRepository
                .findAssignmentsForStudent(studentIdJson,
                        org.springframework.data.domain.Pageable.unpaged());
        long totalAssignments = studentAssignmentsPage.getTotalElements();
        long completedAttempts = attempts.stream().filter(a -> a.getPercentage() != null).count();

        BigDecimal overallAverage = calculateAverage(attempts);
        double completionRate = totalAssignments > 0
                ? completedAttempts * 100.0 / totalAssignments : 0.0;

        String firstName = student.getFirstName() != null ? student.getFirstName() : "";
        String lastName = student.getLastName() != null ? student.getLastName() : "";

        // Score trend — frontend shape: [{date, value}]
        List<StudentAnalyticsDto.TrendPointDto> scoreTrend = attempts.stream()
                .filter(a -> a.getPercentage() != null && a.getSubmittedAt() != null)
                .limit(20)
                .map(a -> StudentAnalyticsDto.TrendPointDto.builder()
                        .date(a.getSubmittedAt().toLocalDate().toString())
                        .value(a.getPercentage().doubleValue())
                        .build())
                .toList();

        // Legacy score trend details
        List<StudentAnalyticsDto.ScoreTrendDetailDto> scoreTrendDetails = attempts.stream()
                .filter(a -> a.getPercentage() != null)
                .limit(20)
                .map(a -> StudentAnalyticsDto.ScoreTrendDetailDto.builder()
                        .attemptId(a.getId())
                        .assignmentTitle(a.getAssignment() != null
                                ? a.getAssignment().getTitle() : "Unknown")
                        .percentage(a.getPercentage())
                        .submittedAt(a.getSubmittedAt())
                        .build())
                .toList();

        List<StudentAnalyticsDto.SubjectBreakdownDto> subjectBreakdown =
                buildSubjectBreakdown(attempts);

        List<StudentAnalyticsDto.WeeklyActivityItemDto> weeklyActivity =
                buildDailyActivity(attempts);

        StudentAnalyticsDto.WeeklyActivitySummaryDto weeklyActivitySummary =
                buildWeeklyActivitySummary(attempts);

        List<StudentAnalyticsDto.WeakAreaDto> weakAreas = buildWeakAreas(subjectBreakdown);
        List<StudentAnalyticsDto.WeakAreaDto> strongAreas = buildStrongAreas(subjectBreakdown);

        List<StudentAnalyticsDto.UpcomingAssignmentDto> upcomingAssignments =
                buildUpcomingAssignments(studentAssignmentsPage.getContent());

        List<StudentAnalyticsDto.InProgressAttemptDto> inProgressAttempts =
                buildInProgressAttempts(studentId);

        StudentAnalyticsDto.TimeManagementDto timeManagement = buildTimeManagement(attempts);

        return StudentAnalyticsDto.builder()
                .studentId(studentId)
                .studentName((firstName + " " + lastName).trim())
                .firstName(firstName)
                .lastName(lastName)
                .totalAssignments((int) totalAssignments)
                .completedAssignments((int) completedAttempts)
                .pendingAssignments((int) (totalAssignments - completedAttempts))
                .totalAttempts(attempts.size())
                .overallAverageScore(overallAverage)
                .overallAverage(overallAverage.doubleValue())
                .completionRate(completionRate)
                .scoreTrend(scoreTrend)
                .scoreTrendDetails(scoreTrendDetails)
                .subjectBreakdown(subjectBreakdown)
                .weeklyActivity(weeklyActivity)
                .weeklyActivitySummary(weeklyActivitySummary)
                .weakAreas(weakAreas)
                .strongAreas(strongAreas)
                .upcomingAssignments(upcomingAssignments)
                .inProgressAttempts(inProgressAttempts)
                .timeManagement(timeManagement)
                .build();
    }

    // ────────────────────────────────────────────────
    //  Group Statistics
    // ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public GroupStatisticsDto getGroupStatistics(UUID groupId, UUID teacherId) {
        StudentGroup group = groupRepository.findByIdAndTeacherId(groupId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentGroup", "id", groupId));

        List<UUID> studentIds = groupRepository.findStudentIdsByGroupId(groupId);

        Page<TestAssignment> teacherAssignments = assignmentRepository
                .findByTeacherIdOrderByCreatedAtDesc(teacherId, PageRequest.of(0, 1000));

        List<TestAssignment> groupAssignments = teacherAssignments.getContent().stream()
                .filter(a -> a.getAssignedStudentIds() != null
                        && a.getAssignedStudentIds().stream().anyMatch(studentIds::contains))
                .toList();

        List<TestAttempt> allGroupAttempts = new ArrayList<>();
        for (TestAssignment assignment : groupAssignments) {
            attemptRepository.findByAssignmentIdOrderByCreatedAtDesc(assignment.getId())
                    .stream()
                    .filter(a -> studentIds.contains(a.getStudentId()))
                    .forEach(allGroupAttempts::add);
        }

        BigDecimal groupAverage = calculateAverage(allGroupAttempts);

        BigDecimal highest = allGroupAttempts.stream()
                .filter(a -> a.getPercentage() != null)
                .map(TestAttempt::getPercentage)
                .max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);

        BigDecimal lowest = allGroupAttempts.stream()
                .filter(a -> a.getPercentage() != null)
                .map(TestAttempt::getPercentage)
                .min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);

        long totalAssignedSlots = groupAssignments.stream()
                .mapToLong(a -> a.getAssignedStudentIds().stream()
                        .filter(studentIds::contains).count())
                .sum();
        long totalSubmitted = allGroupAttempts.stream()
                .filter(a -> a.getPercentage() != null).count();
        BigDecimal completionRate = totalAssignedSlots > 0
                ? BigDecimal.valueOf(totalSubmitted * 100.0 / totalAssignedSlots)
                        .setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Map<UUID, List<TestAttempt>> attemptsByStudent = allGroupAttempts.stream()
                .collect(Collectors.groupingBy(TestAttempt::getStudentId));

        List<GroupStatisticsDto.StudentScoreDto> studentScores = studentIds.stream()
                .map(sid -> {
                    String name = userRepository.findById(sid)
                            .map(u -> u.getFirstName() + " " + u.getLastName())
                            .orElse("Unknown");
                    List<TestAttempt> sa = attemptsByStudent.getOrDefault(sid, List.of());
                    BigDecimal avg = calculateAverage(sa);
                    long completed = sa.stream().filter(a -> a.getPercentage() != null).count();
                    return GroupStatisticsDto.StudentScoreDto.builder()
                            .studentId(sid).studentName(name).averageScore(avg)
                            .totalAttempts(sa.size()).completedAttempts((int) completed)
                            .trend(calculateTrend(sa)).build();
                })
                .sorted(Comparator.comparing(GroupStatisticsDto.StudentScoreDto::getAverageScore,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        // Frontend student rankings
        List<GroupStatisticsDto.StudentRankingDto> studentRankings = new ArrayList<>();
        for (int i = 0; i < studentScores.size(); i++) {
            GroupStatisticsDto.StudentScoreDto s = studentScores.get(i);
            String[] parts = s.getStudentName().split(" ", 2);
            studentRankings.add(GroupStatisticsDto.StudentRankingDto.builder()
                    .rank(i + 1).studentId(s.getStudentId())
                    .firstName(parts[0]).lastName(parts.length > 1 ? parts[1] : "")
                    .averageScore(s.getAverageScore() != null
                            ? s.getAverageScore().doubleValue() : 0.0)
                    .attemptCount(s.getTotalAttempts()).build());
        }

        List<GroupStatisticsDto.ScoreDistributionDto> scoreDistribution =
                buildScoreDistribution(studentScores);

        List<GroupStatisticsDto.SubjectPerformanceDto> subjectPerformance =
                buildGroupSubjectPerformance(groupAssignments, allGroupAttempts);

        List<GroupStatisticsDto.AssignmentBreakdownDto> assignmentBreakdown =
                groupAssignments.stream()
                        .map(a -> {
                            List<TestAttempt> aa = allGroupAttempts.stream()
                                    .filter(att -> att.getAssignment() != null
                                            && att.getAssignment().getId().equals(a.getId()))
                                    .toList();
                            BigDecimal avg = calculateAverage(aa);
                            long sub = aa.stream().filter(att -> att.getPercentage() != null).count();
                            int totalAssigned = (int) a.getAssignedStudentIds().stream()
                                    .filter(studentIds::contains).count();
                            return GroupStatisticsDto.AssignmentBreakdownDto.builder()
                                    .assignmentId(a.getId()).title(a.getTitle())
                                    .averageScore(avg).submittedCount((int) sub)
                                    .totalAssigned(totalAssigned).status(a.getStatus().name())
                                    .build();
                        })
                        .toList();

        return GroupStatisticsDto.builder()
                .groupId(groupId).groupName(group.getName())
                .totalMembers(studentIds.size()).memberCount(studentIds.size())
                .totalAssignments(groupAssignments.size())
                .completedAssignments((int) groupAssignments.stream()
                        .filter(a -> a.getStatus() == AssignmentStatus.COMPLETED).count())
                .groupAverageScore(groupAverage).averageScore(groupAverage.doubleValue())
                .highestScore(highest).lowestScore(lowest)
                .completionRate(completionRate)
                .studentScores(studentScores).studentRankings(studentRankings)
                .scoreDistribution(scoreDistribution).subjectPerformance(subjectPerformance)
                .assignmentBreakdown(assignmentBreakdown)
                .build();
    }

    // ────────────────────────────────────────────────
    //  Private helpers
    // ────────────────────────────────────────────────

    private List<TeacherDashboardDto.TrendPointDto> buildMonthlyTestTrend(UUID teacherId) {
        Map<String, Long> monthlyCount = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        for (int i = 5; i >= 0; i--) {
            monthlyCount.put(now.minusMonths(i).format(fmt), 0L);
        }
        testHistoryRepository
                .findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(teacherId,
                        PageRequest.of(0, 1000))
                .getContent()
                .forEach(th -> {
                    if (th.getCreatedAt() != null) {
                        String key = th.getCreatedAt().format(fmt);
                        monthlyCount.computeIfPresent(key, (k, v) -> v + 1);
                    }
                });
        return monthlyCount.entrySet().stream()
                .map(e -> TeacherDashboardDto.TrendPointDto.builder()
                        .date(e.getKey()).value(e.getValue()).build())
                .toList();
    }

    private TeacherDashboardDto.StudentPerformanceDto buildStudentPerformance(
            UUID studentId, List<TestAttempt> attempts, List<TestAssignment> allAssignments) {

        User user = userRepository.findById(studentId).orElse(null);
        String firstName = user != null && user.getFirstName() != null ? user.getFirstName() : "";
        String lastName = user != null && user.getLastName() != null ? user.getLastName() : "";
        String studentName = (firstName + " " + lastName).trim();

        BigDecimal avgScore = calculateAverage(attempts);
        long completed = attempts.stream().filter(a -> a.getPercentage() != null).count();
        double completionRate = attempts.size() > 0
                ? completed * 100.0 / attempts.size() : 0.0;

        int missedAssignments = (int) allAssignments.stream()
                .filter(a -> a.getAssignedStudentIds() != null
                        && a.getAssignedStudentIds().contains(studentId))
                .filter(a -> attempts.stream().noneMatch(att ->
                        att.getAssignment() != null
                                && att.getAssignment().getId().equals(a.getId())
                                && att.getPercentage() != null))
                .count();

        String lastActivityAt = attempts.stream()
                .filter(a -> a.getSubmittedAt() != null)
                .max(Comparator.comparing(TestAttempt::getSubmittedAt))
                .map(a -> a.getSubmittedAt().toString())
                .orElse(null);

        return TeacherDashboardDto.StudentPerformanceDto.builder()
                .studentId(studentId).studentName(studentName)
                .firstName(firstName).lastName(lastName)
                .averageScore(avgScore).totalAttempts(attempts.size())
                .completedAttempts((int) completed).completionRate(completionRate)
                .missedAssignments(missedAssignments).lastActivityAt(lastActivityAt)
                .build();
    }

    private TeacherDashboardDto.AssignmentSummaryDto buildAssignmentSummary(
            TestAssignment assignment) {
        Double avgPercentage = attemptRepository
                .averagePercentageByAssignmentId(assignment.getId());
        long submittedCount = attemptRepository
                .countSubmittedByAssignmentId(assignment.getId());

        return TeacherDashboardDto.AssignmentSummaryDto.builder()
                .assignmentId(assignment.getId()).title(assignment.getTitle())
                .totalStudents(assignment.getAssignedStudentIds() != null
                        ? assignment.getAssignedStudentIds().size() : 0)
                .submittedCount((int) submittedCount)
                .averageScore(avgPercentage != null
                        ? BigDecimal.valueOf(avgPercentage).setScale(2, RoundingMode.HALF_UP)
                        : null)
                .status(assignment.getStatus().name())
                .createdAt(assignment.getCreatedAt() != null
                        ? assignment.getCreatedAt().toString() : null)
                .build();
    }

    private List<StudentAnalyticsDto.SubjectBreakdownDto> buildSubjectBreakdown(
            List<TestAttempt> attempts) {
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
                            .map(s -> TranslatedField.resolve(s.getName())).orElse("Unknown");
                    BigDecimal avgScore = calculateAverage(entry.getValue());
                    int count = entry.getValue().size();
                    return StudentAnalyticsDto.SubjectBreakdownDto.builder()
                            .subjectId(entry.getKey().toString())
                            .subjectName(subjectName)
                            .attemptCount(count).totalAttempts(count)
                            .averageScore(avgScore).build();
                })
                .sorted(Comparator.comparing(StudentAnalyticsDto.SubjectBreakdownDto::getSubjectName))
                .toList();
    }

    private List<StudentAnalyticsDto.WeeklyActivityItemDto> buildDailyActivity(
            List<TestAttempt> allAttempts) {
        List<StudentAnalyticsDto.WeeklyActivityItemDto> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
            long count = allAttempts.stream()
                    .filter(a -> a.getSubmittedAt() != null
                            && a.getSubmittedAt().isAfter(startOfDay)
                            && a.getSubmittedAt().isBefore(endOfDay))
                    .count();
            result.add(StudentAnalyticsDto.WeeklyActivityItemDto.builder()
                    .date(date.toString()).attemptCount((int) count).build());
        }
        return result;
    }

    private StudentAnalyticsDto.WeeklyActivitySummaryDto buildWeeklyActivitySummary(
            List<TestAttempt> allAttempts) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfThisWeek = now.with(ChronoField.DAY_OF_WEEK, 1)
                .toLocalDate().atStartOfDay();
        LocalDateTime startOfLastWeek = startOfThisWeek.minusWeeks(1);

        List<TestAttempt> thisWeek = allAttempts.stream()
                .filter(a -> a.getSubmittedAt() != null
                        && a.getSubmittedAt().isAfter(startOfThisWeek))
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

        return StudentAnalyticsDto.WeeklyActivitySummaryDto.builder()
                .testsCompletedThisWeek(thisWeek.size())
                .testsCompletedLastWeek(lastWeek.size())
                .averageScoreThisWeek(avgThisWeek)
                .totalTimeSpentMinutes(totalTimeMinutes)
                .build();
    }

    private List<StudentAnalyticsDto.WeakAreaDto> buildWeakAreas(
            List<StudentAnalyticsDto.SubjectBreakdownDto> subjectBreakdown) {
        return subjectBreakdown.stream()
                .filter(s -> s.getAverageScore() != null
                        && s.getAverageScore().compareTo(WEAK_AREA_THRESHOLD) < 0)
                .map(s -> StudentAnalyticsDto.WeakAreaDto.builder()
                        .topicId(s.getSubjectId())
                        .topicName(s.getSubjectName())
                        .subjectName(s.getSubjectName())
                        .averageScore(s.getAverageScore().doubleValue())
                        .attemptCount(s.getTotalAttempts())
                        .averageScoreDecimal(s.getAverageScore())
                        .build())
                .sorted(Comparator.comparingDouble(StudentAnalyticsDto.WeakAreaDto::getAverageScore))
                .toList();
    }

    private List<StudentAnalyticsDto.WeakAreaDto> buildStrongAreas(
            List<StudentAnalyticsDto.SubjectBreakdownDto> subjectBreakdown) {
        return subjectBreakdown.stream()
                .filter(s -> s.getAverageScore() != null
                        && s.getAverageScore().compareTo(STRONG_AREA_THRESHOLD) >= 0)
                .map(s -> StudentAnalyticsDto.WeakAreaDto.builder()
                        .topicId(s.getSubjectId())
                        .topicName(s.getSubjectName())
                        .subjectName(s.getSubjectName())
                        .averageScore(s.getAverageScore().doubleValue())
                        .attemptCount(s.getTotalAttempts())
                        .averageScoreDecimal(s.getAverageScore())
                        .build())
                .sorted(Comparator.comparingDouble(StudentAnalyticsDto.WeakAreaDto::getAverageScore)
                        .reversed())
                .toList();
    }

    private List<GroupStatisticsDto.ScoreDistributionDto> buildScoreDistribution(
            List<GroupStatisticsDto.StudentScoreDto> studentScores) {
        long r0to40 = studentScores.stream()
                .filter(s -> s.getAverageScore() != null && s.getAverageScore().doubleValue() < 40)
                .count();
        long r40to60 = studentScores.stream()
                .filter(s -> s.getAverageScore() != null
                        && s.getAverageScore().doubleValue() >= 40
                        && s.getAverageScore().doubleValue() < 60)
                .count();
        long r60to80 = studentScores.stream()
                .filter(s -> s.getAverageScore() != null
                        && s.getAverageScore().doubleValue() >= 60
                        && s.getAverageScore().doubleValue() < 80)
                .count();
        long r80to100 = studentScores.stream()
                .filter(s -> s.getAverageScore() != null && s.getAverageScore().doubleValue() >= 80)
                .count();

        return List.of(
                GroupStatisticsDto.ScoreDistributionDto.builder().range("0-40").count((int) r0to40).build(),
                GroupStatisticsDto.ScoreDistributionDto.builder().range("40-60").count((int) r40to60).build(),
                GroupStatisticsDto.ScoreDistributionDto.builder().range("60-80").count((int) r60to80).build(),
                GroupStatisticsDto.ScoreDistributionDto.builder().range("80-100").count((int) r80to100).build()
        );
    }

    private List<GroupStatisticsDto.SubjectPerformanceDto> buildGroupSubjectPerformance(
            List<TestAssignment> groupAssignments, List<TestAttempt> allGroupAttempts) {
        Map<UUID, List<TestAttempt>> attemptsBySubject = new HashMap<>();
        Map<UUID, String> subjectNames = new HashMap<>();
        Map<UUID, Long> assignmentCountBySubject = new HashMap<>();

        for (TestAssignment assignment : groupAssignments) {
            if (assignment.getTestHistoryId() == null) continue;
            testHistoryRepository.findById(assignment.getTestHistoryId()).ifPresent(th -> {
                UUID subjectId = th.getSubjectId();
                if (subjectId == null) return;
                subjectNames.computeIfAbsent(subjectId, k ->
                        subjectRepository.findById(k)
                                .map(s -> TranslatedField.resolve(s.getName())).orElse("Unknown"));
                List<TestAttempt> aa = allGroupAttempts.stream()
                        .filter(a -> a.getAssignment() != null
                                && a.getAssignment().getId().equals(assignment.getId()))
                        .toList();
                attemptsBySubject.computeIfAbsent(subjectId, k -> new ArrayList<>()).addAll(aa);
                assignmentCountBySubject.merge(subjectId, 1L, Long::sum);
            });
        }

        return attemptsBySubject.entrySet().stream()
                .map(e -> GroupStatisticsDto.SubjectPerformanceDto.builder()
                        .subjectId(e.getKey())
                        .subjectName(subjectNames.getOrDefault(e.getKey(), "Unknown"))
                        .averageScore(calculateAverage(e.getValue()).doubleValue())
                        .assignmentCount(Math.toIntExact(
                                assignmentCountBySubject.getOrDefault(e.getKey(), 0L)))
                        .build())
                .sorted(Comparator.comparing(GroupStatisticsDto.SubjectPerformanceDto::getSubjectName))
                .toList();
    }

    private List<TeacherDashboardDto.TopicPerformanceDto> buildTopicBreakdown(
            List<TestAssignment> assignments, List<TestAttempt> allAttempts) {
        Map<UUID, List<TestAttempt>> byTopic = new HashMap<>();
        Map<UUID, UUID> topicToSubject = new HashMap<>();

        for (TestAssignment assignment : assignments) {
            UUID testHistoryId = assignment.getTestHistoryId();
            if (testHistoryId == null) continue;
            testHistoryRepository.findById(testHistoryId).ifPresent(th -> {
                List<UUID> topicIds = th.getTopicIds();
                UUID subjectId = th.getSubjectId();
                if (topicIds == null || topicIds.isEmpty()) return;
                List<TestAttempt> aa = allAttempts.stream()
                        .filter(a -> a.getAssignment() != null
                                && a.getAssignment().getId().equals(assignment.getId()))
                        .toList();
                for (UUID topicId : topicIds) {
                    byTopic.computeIfAbsent(topicId, k -> new ArrayList<>()).addAll(aa);
                    topicToSubject.putIfAbsent(topicId, subjectId);
                }
            });
        }

        return byTopic.entrySet().stream()
                .map(entry -> {
                    UUID topicId = entry.getKey();
                    BigDecimal avg = calculateAverage(entry.getValue());
                    String topicName = topicRepository.findById(topicId)
                            .map(t -> TranslatedField.resolve(t.getName())).orElse("Unknown");
                    UUID subjectId = topicToSubject.get(topicId);
                    String subjectName = subjectId != null
                            ? subjectRepository.findById(subjectId)
                                    .map(s -> TranslatedField.resolve(s.getName())).orElse("Unknown")
                            : "Unknown";
                    String difficulty = avg.compareTo(new BigDecimal("80")) >= 0 ? "EASY"
                            : avg.compareTo(new BigDecimal("50")) >= 0 ? "MEDIUM" : "HARD";
                    return TeacherDashboardDto.TopicPerformanceDto.builder()
                            .topicId(topicId).topicName(topicName).subjectName(subjectName)
                            .averageScore(avg).attemptCount(entry.getValue().size())
                            .difficulty(difficulty).build();
                })
                .sorted(Comparator.comparing(TeacherDashboardDto.TopicPerformanceDto::getAverageScore))
                .toList();
    }

    private List<StudentAnalyticsDto.UpcomingAssignmentDto> buildUpcomingAssignments(
            List<TestAssignment> assignments) {
        LocalDateTime now = LocalDateTime.now();
        return assignments.stream()
                .filter(a -> a.getEndTime() != null && a.getEndTime().isAfter(now))
                .filter(a -> a.getStatus() == AssignmentStatus.ACTIVE
                        || a.getStatus() == AssignmentStatus.SCHEDULED)
                .limit(10)
                .map(a -> StudentAnalyticsDto.UpcomingAssignmentDto.builder()
                        .assignmentId(a.getId()).title(a.getTitle())
                        .startTime(a.getStartTime()).endTime(a.getEndTime())
                        .durationMinutes(a.getDurationMinutes()).status(a.getStatus().name())
                        .build())
                .toList();
    }

    private List<StudentAnalyticsDto.InProgressAttemptDto> buildInProgressAttempts(
            UUID studentId) {
        return attemptRepository.findByStudentIdAndStatus(studentId, AttemptStatus.IN_PROGRESS)
                .stream()
                .map(attempt -> {
                    TestAssignment assignment = attempt.getAssignment();
                    Long remainingSeconds = null;
                    if (assignment != null && attempt.getStartedAt() != null) {
                        LocalDateTime deadline = attempt.getStartedAt()
                                .plusMinutes(assignment.getDurationMinutes());
                        remainingSeconds = Math.max(0,
                                Duration.between(LocalDateTime.now(), deadline).getSeconds());
                    }
                    long answered = answerRepository
                            .countByAttemptIdAndSelectedAnswerIsNotNull(attempt.getId());
                    long total = answerRepository.countByAttemptId(attempt.getId());
                    return StudentAnalyticsDto.InProgressAttemptDto.builder()
                            .attemptId(attempt.getId())
                            .assignmentId(assignment != null ? assignment.getId() : null)
                            .assignmentTitle(assignment != null ? assignment.getTitle() : "Unknown")
                            .startedAt(attempt.getStartedAt()).remainingSeconds(remainingSeconds)
                            .answeredQuestions((int) answered).totalQuestions((int) total)
                            .build();
                })
                .toList();
    }

    private StudentAnalyticsDto.TimeManagementDto buildTimeManagement(
            List<TestAttempt> attempts) {
        List<TestAttempt> completed = attempts.stream()
                .filter(a -> a.getStartedAt() != null && a.getSubmittedAt() != null).toList();
        if (completed.isEmpty()) {
            return StudentAnalyticsDto.TimeManagementDto.builder()
                    .averageTimePerTestMinutes(BigDecimal.ZERO)
                    .averageTimePerQuestionSeconds(BigDecimal.ZERO).build();
        }
        long totalTestMinutes = completed.stream()
                .mapToLong(a -> Duration.between(a.getStartedAt(), a.getSubmittedAt()).toMinutes())
                .sum();
        BigDecimal avgTestMinutes = BigDecimal.valueOf(totalTestMinutes)
                .divide(BigDecimal.valueOf(completed.size()), 2, RoundingMode.HALF_UP);
        long totalQuestions = completed.stream()
                .mapToLong(a -> answerRepository.countByAttemptId(a.getId())).sum();
        long totalTestSeconds = completed.stream()
                .mapToLong(a -> Duration.between(a.getStartedAt(), a.getSubmittedAt()).getSeconds())
                .sum();
        BigDecimal avgTimePerQuestion = totalQuestions > 0
                ? BigDecimal.valueOf(totalTestSeconds)
                        .divide(BigDecimal.valueOf(totalQuestions), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        return StudentAnalyticsDto.TimeManagementDto.builder()
                .averageTimePerTestMinutes(avgTestMinutes)
                .averageTimePerQuestionSeconds(avgTimePerQuestion).build();
    }

    private String calculateTrend(List<TestAttempt> attempts) {
        List<TestAttempt> scored = attempts.stream()
                .filter(a -> a.getPercentage() != null && a.getSubmittedAt() != null)
                .sorted(Comparator.comparing(TestAttempt::getSubmittedAt).reversed()).toList();
        if (scored.size() < 2) return "STABLE";
        List<TestAttempt> recent = scored.stream().limit(3).toList();
        List<TestAttempt> previous = scored.stream().skip(3).limit(3).toList();
        if (previous.isEmpty()) return "STABLE";
        int cmp = calculateAverage(recent).compareTo(calculateAverage(previous));
        return cmp > 0 ? "UP" : cmp < 0 ? "DOWN" : "STABLE";
    }

    private BigDecimal calculateAverage(List<TestAttempt> attempts) {
        List<BigDecimal> scores = attempts.stream()
                .filter(a -> a.getPercentage() != null)
                .map(TestAttempt::getPercentage).toList();
        if (scores.isEmpty()) return BigDecimal.ZERO;
        return scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
    }
}
