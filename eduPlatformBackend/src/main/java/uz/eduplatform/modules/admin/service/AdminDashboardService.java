package uz.eduplatform.modules.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.eduplatform.core.audit.AuditLog;
import uz.eduplatform.core.audit.AuditLogRepository;
import uz.eduplatform.modules.admin.dto.DashboardStatsDto;
import uz.eduplatform.modules.auth.domain.Role;
import uz.eduplatform.modules.auth.domain.UserStatus;
import uz.eduplatform.modules.auth.repository.UserRepository;
import uz.eduplatform.modules.auth.repository.UserSessionRepository;
import uz.eduplatform.modules.content.domain.Difficulty;
import uz.eduplatform.modules.content.domain.QuestionStatus;
import uz.eduplatform.modules.content.domain.QuestionType;
import uz.eduplatform.modules.content.repository.QuestionRepository;
import uz.eduplatform.modules.content.repository.SubjectRepository;
import uz.eduplatform.modules.content.repository.TopicRepository;
import uz.eduplatform.modules.notification.domain.NotificationChannel;
import uz.eduplatform.modules.notification.domain.NotificationStatus;
import uz.eduplatform.modules.notification.repository.NotificationHistoryRepository;
import uz.eduplatform.modules.test.repository.TestHistoryRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final SubjectRepository subjectRepository;
    private final QuestionRepository questionRepository;
    private final TestHistoryRepository testHistoryRepository;
    private final TopicRepository topicRepository;
    private final NotificationHistoryRepository notificationHistoryRepository;
    private final AuditLogRepository auditLogRepository;

    @Cacheable(value = "dashboard_stats", key = "'global'")
    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats() {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();

        return DashboardStatsDto.builder()
                .totalUsers(userRepository.count())
                .activeUsers(userRepository.countByStatus(UserStatus.ACTIVE))
                .newUsersThisWeek(userRepository.countByCreatedAtAfter(oneWeekAgo))
                .totalSubjects(subjectRepository.countByIsArchivedFalse())
                .totalTopics(topicRepository.count())
                .totalQuestions(questionRepository.count())
                .pendingQuestions(questionRepository.countByStatus(QuestionStatus.PENDING))
                .activeQuestions(questionRepository.countByStatus(QuestionStatus.ACTIVE))
                .totalTests(testHistoryRepository.countByDeletedAtIsNull())
                .testsThisWeek(testHistoryRepository.countByCreatedAtAfterAndDeletedAtIsNull(oneWeekAgo))
                .activeSessionsToday(userSessionRepository.countDistinctActiveUsersSince(todayStart))
                .totalDownloads(testHistoryRepository.sumDownloadCount())
                .usersByRole(DashboardStatsDto.UsersByRoleDto.builder()
                        .superAdmins(userRepository.countByRole(Role.SUPER_ADMIN))
                        .admins(userRepository.countByRole(Role.ADMIN))
                        .moderators(userRepository.countByRole(Role.MODERATOR))
                        .teachers(userRepository.countByRole(Role.TEACHER))
                        .parents(userRepository.countByRole(Role.PARENT))
                        .students(userRepository.countByRole(Role.STUDENT))
                        .build())
                .usersByStatus(DashboardStatsDto.UsersByStatusDto.builder()
                        .active(userRepository.countByStatus(UserStatus.ACTIVE))
                        .inactive(userRepository.countByStatus(UserStatus.INACTIVE))
                        .blocked(userRepository.countByStatus(UserStatus.BLOCKED))
                        .pendingVerification(userRepository.countByStatus(UserStatus.PENDING_VERIFICATION))
                        .build())
                .questionsByDifficulty(DashboardStatsDto.QuestionsByDifficultyDto.builder()
                        .easy(questionRepository.countByDifficulty(Difficulty.EASY))
                        .medium(questionRepository.countByDifficulty(Difficulty.MEDIUM))
                        .hard(questionRepository.countByDifficulty(Difficulty.HARD))
                        .build())
                .questionsByType(DashboardStatsDto.QuestionsByTypeDto.builder()
                        .mcqSingle(questionRepository.countByQuestionType(QuestionType.MCQ_SINGLE))
                        .mcqMulti(questionRepository.countByQuestionType(QuestionType.MCQ_MULTI))
                        .trueFalse(questionRepository.countByQuestionType(QuestionType.TRUE_FALSE))
                        .fillBlank(questionRepository.countByQuestionType(QuestionType.FILL_BLANK))
                        .matching(questionRepository.countByQuestionType(QuestionType.MATCHING))
                        .ordering(questionRepository.countByQuestionType(QuestionType.ORDERING))
                        .shortAnswer(questionRepository.countByQuestionType(QuestionType.SHORT_ANSWER))
                        .essay(questionRepository.countByQuestionType(QuestionType.ESSAY))
                        .build())
                .notificationStats(DashboardStatsDto.NotificationStatsDto.builder()
                        .totalSent(notificationHistoryRepository.countByStatus(NotificationStatus.SENT))
                        .totalFailed(notificationHistoryRepository.countByStatus(NotificationStatus.FAILED))
                        .totalPending(notificationHistoryRepository.countByStatus(NotificationStatus.PENDING))
                        .totalRetrying(notificationHistoryRepository.countByStatus(NotificationStatus.RETRYING))
                        .bySms(notificationHistoryRepository.countByChannel(NotificationChannel.SMS))
                        .byEmail(notificationHistoryRepository.countByChannel(NotificationChannel.EMAIL))
                        .build())
                .recentActivity(getRecentActivity())
                .build();
    }

    private List<DashboardStatsDto.RecentActivityDto> getRecentActivity() {
        Page<AuditLog> page = auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 10));
        return page.getContent().stream()
                .map(log -> DashboardStatsDto.RecentActivityDto.builder()
                        .id(log.getId())
                        .userId(log.getUserId())
                        .action(log.getAction())
                        .actionCategory(log.getActionCategory())
                        .entityType(log.getEntityType())
                        .createdAt(log.getCreatedAt())
                        .build())
                .toList();
    }
}
