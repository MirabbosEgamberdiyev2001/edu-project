package uz.eduplatform.modules.analytics.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import uz.eduplatform.modules.analytics.dto.GroupStatisticsDto;
import uz.eduplatform.modules.analytics.dto.StudentAnalyticsDto;
import uz.eduplatform.modules.analytics.dto.TeacherDashboardDto;
import uz.eduplatform.modules.group.domain.StudentGroup;
import uz.eduplatform.modules.assessment.domain.AssignmentStatus;
import uz.eduplatform.modules.assessment.domain.AttemptStatus;
import uz.eduplatform.modules.assessment.domain.TestAssignment;
import uz.eduplatform.modules.assessment.domain.TestAttempt;
import uz.eduplatform.modules.assessment.repository.AnswerRepository;
import uz.eduplatform.modules.assessment.repository.TestAssignmentRepository;
import uz.eduplatform.modules.assessment.repository.TestAttemptRepository;
import uz.eduplatform.modules.auth.domain.Role;
import uz.eduplatform.modules.auth.domain.User;
import uz.eduplatform.modules.auth.repository.UserRepository;
import uz.eduplatform.modules.content.repository.SubjectRepository;
import uz.eduplatform.modules.group.domain.StudentGroup;
import uz.eduplatform.modules.group.repository.GroupMemberRepository;
import uz.eduplatform.modules.group.repository.StudentGroupRepository;
import uz.eduplatform.modules.test.repository.TestHistoryRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AnalyticsServiceTest {

    @Mock private TestAssignmentRepository assignmentRepository;
    @Mock private TestAttemptRepository attemptRepository;
    @Mock private AnswerRepository answerRepository;
    @Mock private StudentGroupRepository groupRepository;
    @Mock private GroupMemberRepository memberRepository;
    @Mock private UserRepository userRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private uz.eduplatform.modules.content.repository.TopicRepository topicRepository;
    @Mock private TestHistoryRepository testHistoryRepository;

    @InjectMocks private AnalyticsService analyticsService;

    private UUID teacherId;
    private UUID studentId;
    private User teacher;
    private User student;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        studentId = UUID.randomUUID();

        teacher = User.builder()
                .id(teacherId).firstName("Teacher").lastName("User")
                .role(Role.TEACHER).build();

        student = User.builder()
                .id(studentId).firstName("Student").lastName("User")
                .role(Role.STUDENT).build();

        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    }

    // ── Teacher Dashboard ──

    @Test
    void getTeacherDashboard_emptyData_returnsZeros() {
        Page<StudentGroup> emptyGroups = new PageImpl<>(List.of());
        when(groupRepository.findByTeacherIdOrderByCreatedAtDesc(eq(teacherId), any()))
                .thenReturn(emptyGroups);
        when(groupRepository.countByTeacherId(teacherId)).thenReturn(0L);

        Page<TestAssignment> emptyAssignments = new PageImpl<>(List.of());
        when(assignmentRepository.findByTeacherIdOrderByCreatedAtDesc(eq(teacherId), any()))
                .thenReturn(emptyAssignments);
        when(assignmentRepository.countByTeacherIdAndStatus(teacherId, AssignmentStatus.ACTIVE))
                .thenReturn(0L);

        TeacherDashboardDto result = analyticsService.getTeacherDashboard(teacherId);

        assertThat(result).isNotNull();
        assertThat(result.getTotalGroups()).isZero();
        assertThat(result.getTotalStudents()).isZero();
        assertThat(result.getTotalAssignments()).isZero();
        assertThat(result.getActiveAssignments()).isZero();
        assertThat(result.getOverallAverageScore()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getTopStudents()).isEmpty();
        assertThat(result.getAtRiskStudents()).isEmpty();
    }

    @Test
    void getTeacherDashboard_withData_returnsCorrectStats() {
        // Setup groups
        StudentGroup group = StudentGroup.builder().id(UUID.randomUUID())
                .teacherId(teacherId).name("Group 1").members(new ArrayList<>()).build();
        Page<StudentGroup> groupPage = new PageImpl<>(List.of(group));
        when(groupRepository.findByTeacherIdOrderByCreatedAtDesc(eq(teacherId), any()))
                .thenReturn(groupPage);
        when(groupRepository.countByTeacherId(teacherId)).thenReturn(1L);
        when(groupRepository.findStudentIdsByGroupId(group.getId()))
                .thenReturn(List.of(studentId));

        // Setup assignments
        UUID assignmentId = UUID.randomUUID();
        TestAssignment assignment = TestAssignment.builder()
                .id(assignmentId).teacherId(teacherId).title("Test 1")
                .status(AssignmentStatus.ACTIVE)
                .assignedStudentIds(List.of(studentId)).build();
        Page<TestAssignment> assignmentPage = new PageImpl<>(List.of(assignment));
        when(assignmentRepository.findByTeacherIdOrderByCreatedAtDesc(eq(teacherId), any()))
                .thenReturn(assignmentPage);
        when(assignmentRepository.countByTeacherIdAndStatus(teacherId, AssignmentStatus.ACTIVE))
                .thenReturn(1L);

        // Setup attempts
        TestAttempt attempt = TestAttempt.builder()
                .id(UUID.randomUUID()).studentId(studentId)
                .assignment(assignment).status(AttemptStatus.GRADED)
                .percentage(new BigDecimal("75.00")).build();
        when(attemptRepository.findByAssignmentIdOrderByCreatedAtDesc(assignmentId))
                .thenReturn(List.of(attempt));
        when(attemptRepository.countSubmittedByAssignmentId(assignmentId)).thenReturn(1L);
        when(attemptRepository.averagePercentageByAssignmentId(assignmentId)).thenReturn(75.0);

        TeacherDashboardDto result = analyticsService.getTeacherDashboard(teacherId);

        assertThat(result.getTotalGroups()).isEqualTo(1);
        assertThat(result.getTotalStudents()).isEqualTo(1);
        assertThat(result.getTotalAssignments()).isEqualTo(1);
        assertThat(result.getActiveAssignments()).isEqualTo(1);
        assertThat(result.getOverallAverageScore()).isEqualByComparingTo("75.00");
        assertThat(result.getTopStudents()).hasSize(1);
        assertThat(result.getRecentAssignments()).hasSize(1);
    }

    @Test
    void getTeacherDashboard_atRiskStudents_belowThreshold() {
        StudentGroup group = StudentGroup.builder().id(UUID.randomUUID())
                .teacherId(teacherId).name("Group 1").members(new ArrayList<>()).build();
        Page<StudentGroup> groupPage = new PageImpl<>(List.of(group));
        when(groupRepository.findByTeacherIdOrderByCreatedAtDesc(eq(teacherId), any()))
                .thenReturn(groupPage);
        when(groupRepository.countByTeacherId(teacherId)).thenReturn(1L);
        when(groupRepository.findStudentIdsByGroupId(group.getId()))
                .thenReturn(List.of(studentId));

        UUID assignmentId = UUID.randomUUID();
        TestAssignment assignment = TestAssignment.builder()
                .id(assignmentId).teacherId(teacherId).title("Test 1")
                .status(AssignmentStatus.COMPLETED)
                .assignedStudentIds(List.of(studentId)).build();
        Page<TestAssignment> assignmentPage = new PageImpl<>(List.of(assignment));
        when(assignmentRepository.findByTeacherIdOrderByCreatedAtDesc(eq(teacherId), any()))
                .thenReturn(assignmentPage);
        when(assignmentRepository.countByTeacherIdAndStatus(teacherId, AssignmentStatus.ACTIVE))
                .thenReturn(0L);

        // Student scored 30% — below 40% threshold
        TestAttempt attempt = TestAttempt.builder()
                .id(UUID.randomUUID()).studentId(studentId)
                .assignment(assignment).status(AttemptStatus.GRADED)
                .percentage(new BigDecimal("30.00")).build();
        when(attemptRepository.findByAssignmentIdOrderByCreatedAtDesc(assignmentId))
                .thenReturn(List.of(attempt));
        when(attemptRepository.countSubmittedByAssignmentId(assignmentId)).thenReturn(1L);
        when(attemptRepository.averagePercentageByAssignmentId(assignmentId)).thenReturn(30.0);

        TeacherDashboardDto result = analyticsService.getTeacherDashboard(teacherId);

        assertThat(result.getAtRiskStudents()).hasSize(1);
        assertThat(result.getAtRiskStudents().get(0).getAverageScore())
                .isEqualByComparingTo("30.00");
    }

    // ── Student Analytics ──

    @Test
    void getStudentAnalytics_emptyData_returnsZeros() {
        when(attemptRepository.findByStudentIdOrderByCreatedAtDesc(studentId))
                .thenReturn(List.of());

        Page<TestAssignment> emptyPage = new PageImpl<>(List.of());
        when(assignmentRepository.findAssignmentsForStudent(anyString(), any(Pageable.class)))
                .thenReturn(emptyPage);

        StudentAnalyticsDto result = analyticsService.getStudentAnalytics(studentId);

        assertThat(result).isNotNull();
        assertThat(result.getStudentName()).isEqualTo("Student User");
        assertThat(result.getTotalAssignments()).isZero();
        assertThat(result.getCompletedAssignments()).isZero();
        assertThat(result.getOverallAverageScore()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getScoreTrend()).isEmpty();
    }

    @Test
    void getStudentAnalytics_withData_returnsCorrectStats() {
        TestAssignment assignment = TestAssignment.builder()
                .id(UUID.randomUUID()).title("Physics Test")
                .testHistoryId(UUID.randomUUID()).build();

        TestAttempt attempt1 = TestAttempt.builder()
                .id(UUID.randomUUID()).studentId(studentId)
                .assignment(assignment).status(AttemptStatus.GRADED)
                .percentage(new BigDecimal("80.00"))
                .submittedAt(LocalDateTime.now().minusDays(2)).build();

        TestAttempt attempt2 = TestAttempt.builder()
                .id(UUID.randomUUID()).studentId(studentId)
                .assignment(assignment).status(AttemptStatus.GRADED)
                .percentage(new BigDecimal("90.00"))
                .submittedAt(LocalDateTime.now()).build();

        when(attemptRepository.findByStudentIdOrderByCreatedAtDesc(studentId))
                .thenReturn(List.of(attempt2, attempt1));

        Page<TestAssignment> assignmentPage = new PageImpl<>(List.of(assignment));
        when(assignmentRepository.findAssignmentsForStudent(anyString(), any(Pageable.class)))
                .thenReturn(assignmentPage);

        StudentAnalyticsDto result = analyticsService.getStudentAnalytics(studentId);

        assertThat(result.getTotalAssignments()).isEqualTo(1);
        assertThat(result.getCompletedAssignments()).isEqualTo(2);
        assertThat(result.getOverallAverageScore()).isEqualByComparingTo("85.00");
        assertThat(result.getScoreTrend()).hasSize(2);
    }

    // ── Group Statistics ──

    @Test
    void getGroupStatistics_emptyGroup_returnsZeros() {
        UUID groupId = UUID.randomUUID();
        StudentGroup group = StudentGroup.builder().id(groupId)
                .teacherId(teacherId).name("Test Group").members(new ArrayList<>()).build();

        when(groupRepository.findByIdAndTeacherId(groupId, teacherId))
                .thenReturn(Optional.of(group));
        when(groupRepository.findStudentIdsByGroupId(groupId)).thenReturn(List.of());

        Page<TestAssignment> emptyPage = new PageImpl<>(List.of());
        when(assignmentRepository.findByTeacherIdOrderByCreatedAtDesc(eq(teacherId), any()))
                .thenReturn(emptyPage);

        GroupStatisticsDto result = analyticsService.getGroupStatistics(groupId, teacherId);

        assertThat(result).isNotNull();
        assertThat(result.getGroupName()).isEqualTo("Test Group");
        assertThat(result.getTotalMembers()).isZero();
        assertThat(result.getGroupAverageScore()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getStudentScores()).isEmpty();
    }

    @Test
    void getGroupStatistics_withData_returnsCorrectStats() {
        UUID groupId = UUID.randomUUID();
        StudentGroup group = StudentGroup.builder().id(groupId)
                .teacherId(teacherId).name("8-A Sinf").members(new ArrayList<>()).build();

        when(groupRepository.findByIdAndTeacherId(groupId, teacherId))
                .thenReturn(Optional.of(group));
        when(groupRepository.findStudentIdsByGroupId(groupId)).thenReturn(List.of(studentId));

        UUID assignmentId = UUID.randomUUID();
        TestAssignment assignment = TestAssignment.builder()
                .id(assignmentId).teacherId(teacherId).title("Test 1")
                .status(AssignmentStatus.COMPLETED)
                .assignedStudentIds(List.of(studentId)).build();
        Page<TestAssignment> assignmentPage = new PageImpl<>(List.of(assignment));
        when(assignmentRepository.findByTeacherIdOrderByCreatedAtDesc(eq(teacherId), any()))
                .thenReturn(assignmentPage);

        TestAttempt attempt = TestAttempt.builder()
                .id(UUID.randomUUID()).studentId(studentId)
                .assignment(assignment).status(AttemptStatus.GRADED)
                .percentage(new BigDecimal("80.00")).build();
        when(attemptRepository.findByAssignmentIdOrderByCreatedAtDesc(assignmentId))
                .thenReturn(List.of(attempt));

        GroupStatisticsDto result = analyticsService.getGroupStatistics(groupId, teacherId);

        assertThat(result.getTotalMembers()).isEqualTo(1);
        assertThat(result.getTotalAssignments()).isEqualTo(1);
        assertThat(result.getGroupAverageScore()).isEqualByComparingTo("80.00");
        assertThat(result.getStudentScores()).hasSize(1);
        assertThat(result.getStudentScores().get(0).getStudentName()).isEqualTo("Student User");
    }
}
