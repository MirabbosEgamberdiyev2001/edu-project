package uz.eduplatform.modules.admin.service;

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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminDashboardServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserSessionRepository userSessionRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private TestHistoryRepository testHistoryRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private NotificationHistoryRepository notificationHistoryRepository;
    @Mock private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AdminDashboardService dashboardService;

    @Test
    void getDashboardStats_returnsAllFields() {
        setupMinimalMocks();

        DashboardStatsDto stats = dashboardService.getDashboardStats();

        assertNotNull(stats);
        assertNotNull(stats.getUsersByRole());
        assertNotNull(stats.getUsersByStatus());
        assertNotNull(stats.getQuestionsByDifficulty());
        assertNotNull(stats.getQuestionsByType());
        assertNotNull(stats.getNotificationStats());
        assertNotNull(stats.getRecentActivity());

        verify(userRepository).count();
        verify(topicRepository).count();
        verify(questionRepository).count();
        verify(testHistoryRepository).sumDownloadCount();
    }

    @Test
    void getDashboardStats_includesQuestionDistributions() {
        setupMinimalMocks();

        when(questionRepository.countByDifficulty(Difficulty.EASY)).thenReturn(100L);
        when(questionRepository.countByDifficulty(Difficulty.MEDIUM)).thenReturn(200L);
        when(questionRepository.countByDifficulty(Difficulty.HARD)).thenReturn(50L);

        when(questionRepository.countByQuestionType(QuestionType.MCQ_SINGLE)).thenReturn(150L);
        when(questionRepository.countByQuestionType(QuestionType.MCQ_MULTI)).thenReturn(80L);
        when(questionRepository.countByQuestionType(QuestionType.TRUE_FALSE)).thenReturn(60L);
        when(questionRepository.countByQuestionType(QuestionType.FILL_BLANK)).thenReturn(30L);
        when(questionRepository.countByQuestionType(QuestionType.MATCHING)).thenReturn(20L);
        when(questionRepository.countByQuestionType(QuestionType.ORDERING)).thenReturn(10L);
        when(questionRepository.countByQuestionType(QuestionType.SHORT_ANSWER)).thenReturn(5L);
        when(questionRepository.countByQuestionType(QuestionType.ESSAY)).thenReturn(3L);

        DashboardStatsDto stats = dashboardService.getDashboardStats();

        assertEquals(100L, stats.getQuestionsByDifficulty().getEasy());
        assertEquals(200L, stats.getQuestionsByDifficulty().getMedium());
        assertEquals(50L, stats.getQuestionsByDifficulty().getHard());

        assertEquals(150L, stats.getQuestionsByType().getMcqSingle());
        assertEquals(80L, stats.getQuestionsByType().getMcqMulti());
        assertEquals(60L, stats.getQuestionsByType().getTrueFalse());
    }

    @Test
    void getDashboardStats_includesNotificationStats() {
        setupMinimalMocks();

        when(notificationHistoryRepository.countByStatus(NotificationStatus.SENT)).thenReturn(500L);
        when(notificationHistoryRepository.countByStatus(NotificationStatus.FAILED)).thenReturn(10L);
        when(notificationHistoryRepository.countByStatus(NotificationStatus.PENDING)).thenReturn(5L);
        when(notificationHistoryRepository.countByStatus(NotificationStatus.RETRYING)).thenReturn(2L);
        when(notificationHistoryRepository.countByChannel(NotificationChannel.SMS)).thenReturn(300L);
        when(notificationHistoryRepository.countByChannel(NotificationChannel.EMAIL)).thenReturn(217L);

        DashboardStatsDto stats = dashboardService.getDashboardStats();

        assertEquals(500L, stats.getNotificationStats().getTotalSent());
        assertEquals(10L, stats.getNotificationStats().getTotalFailed());
        assertEquals(5L, stats.getNotificationStats().getTotalPending());
        assertEquals(2L, stats.getNotificationStats().getTotalRetrying());
        assertEquals(300L, stats.getNotificationStats().getBySms());
        assertEquals(217L, stats.getNotificationStats().getByEmail());
    }

    @Test
    void getDashboardStats_includesRecentActivity() {
        setupMinimalMocks();

        AuditLog log = AuditLog.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .action("USER_LOGIN")
                .actionCategory("AUTH")
                .entityType("User")
                .createdAt(LocalDateTime.now())
                .build();

        Page<AuditLog> page = new PageImpl<>(List.of(log));
        when(auditLogRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(page);

        DashboardStatsDto stats = dashboardService.getDashboardStats();

        assertEquals(1, stats.getRecentActivity().size());
        assertEquals("USER_LOGIN", stats.getRecentActivity().get(0).getAction());
        assertEquals("AUTH", stats.getRecentActivity().get(0).getActionCategory());
    }

    private void setupMinimalMocks() {
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.countByStatus(any(UserStatus.class))).thenReturn(0L);
        when(userRepository.countByCreatedAtAfter(any(LocalDateTime.class))).thenReturn(0L);
        when(subjectRepository.countByIsArchivedFalse()).thenReturn(0L);
        when(topicRepository.count()).thenReturn(0L);
        when(questionRepository.count()).thenReturn(0L);
        when(questionRepository.countByStatus(any(QuestionStatus.class))).thenReturn(0L);
        when(testHistoryRepository.countByDeletedAtIsNull()).thenReturn(0L);
        when(testHistoryRepository.countByCreatedAtAfterAndDeletedAtIsNull(any(LocalDateTime.class))).thenReturn(0L);
        when(userSessionRepository.countDistinctActiveUsersSince(any(LocalDateTime.class))).thenReturn(0L);
        when(testHistoryRepository.sumDownloadCount()).thenReturn(0L);
        when(userRepository.countByRole(any(Role.class))).thenReturn(0L);
        when(questionRepository.countByDifficulty(any(Difficulty.class))).thenReturn(0L);
        when(questionRepository.countByQuestionType(any(QuestionType.class))).thenReturn(0L);
        when(notificationHistoryRepository.countByStatus(any(NotificationStatus.class))).thenReturn(0L);
        when(notificationHistoryRepository.countByChannel(any(NotificationChannel.class))).thenReturn(0L);

        Page<AuditLog> emptyPage = new PageImpl<>(List.of());
        when(auditLogRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(emptyPage);
    }
}
