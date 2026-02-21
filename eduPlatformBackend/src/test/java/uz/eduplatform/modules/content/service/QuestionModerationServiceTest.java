package uz.eduplatform.modules.content.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uz.eduplatform.core.audit.AuditService;
import uz.eduplatform.core.common.exception.BusinessException;
import uz.eduplatform.core.common.utils.MessageService;
import uz.eduplatform.core.i18n.AcceptLanguage;
import uz.eduplatform.modules.content.domain.Question;
import uz.eduplatform.modules.content.domain.QuestionStatus;
import uz.eduplatform.modules.content.dto.BulkModerationResponse;
import uz.eduplatform.modules.content.repository.QuestionRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionModerationServiceTest {

    @Mock private QuestionRepository questionRepository;
    @Mock private QuestionService questionService;
    @Mock private AuditService auditService;
    @Mock private MessageService messageService;

    @InjectMocks
    private QuestionModerationService moderationService;

    @Test
    void bulkApprove_approvesAllPendingQuestions() {
        UUID moderatorId = UUID.randomUUID();
        UUID q1Id = UUID.randomUUID();
        UUID q2Id = UUID.randomUUID();

        Question q1 = Question.builder()
                .id(q1Id)
                .questionText(Map.of("uz_latn", "Test 1"))
                .status(QuestionStatus.PENDING)
                .build();
        Question q2 = Question.builder()
                .id(q2Id)
                .questionText(Map.of("uz_latn", "Test 2"))
                .status(QuestionStatus.PENDING)
                .build();

        when(questionRepository.findByIdInAndStatus(any(), eq(QuestionStatus.PENDING)))
                .thenReturn(List.of(q1, q2));
        when(questionRepository.saveAll(any())).thenReturn(List.of(q1, q2));

        BulkModerationResponse response = moderationService.bulkApprove(
                List.of(q1Id, q2Id), moderatorId, AcceptLanguage.UZL);

        assertEquals(2, response.getTotalRequested());
        assertEquals(2, response.getSuccessCount());
        assertEquals(0, response.getFailedCount());
        assertTrue(response.getFailedIds().isEmpty());
        assertEquals(QuestionStatus.ACTIVE, q1.getStatus());
        assertEquals(QuestionStatus.ACTIVE, q2.getStatus());
        assertNotNull(q1.getModeratedAt());
        assertEquals(moderatorId, q1.getModeratedBy());

        verify(auditService, times(2)).log(eq(moderatorId), eq("MODERATOR"),
                eq("QUESTION_APPROVED"), eq("CONTENT"), eq("Question"), any(UUID.class));
    }

    @Test
    void bulkApprove_reportsFailedIdsForNonPendingQuestions() {
        UUID moderatorId = UUID.randomUUID();
        UUID pendingId = UUID.randomUUID();
        UUID nonPendingId = UUID.randomUUID();

        Question pendingQ = Question.builder()
                .id(pendingId)
                .questionText(Map.of("uz_latn", "Pending"))
                .status(QuestionStatus.PENDING)
                .build();

        when(questionRepository.findByIdInAndStatus(any(), eq(QuestionStatus.PENDING)))
                .thenReturn(List.of(pendingQ));
        when(questionRepository.saveAll(any())).thenReturn(List.of(pendingQ));

        BulkModerationResponse response = moderationService.bulkApprove(
                List.of(pendingId, nonPendingId), moderatorId, AcceptLanguage.UZL);

        assertEquals(2, response.getTotalRequested());
        assertEquals(1, response.getSuccessCount());
        assertEquals(1, response.getFailedCount());
        assertTrue(response.getFailedIds().contains(nonPendingId));
        assertEquals(1, response.getErrors().size());
    }

    @Test
    void bulkReject_requiresReason() {
        UUID moderatorId = UUID.randomUUID();
        List<UUID> ids = List.of(UUID.randomUUID());

        assertThrows(BusinessException.class,
                () -> moderationService.bulkReject(ids, moderatorId, null, AcceptLanguage.UZL));
        assertThrows(BusinessException.class,
                () -> moderationService.bulkReject(ids, moderatorId, "  ", AcceptLanguage.UZL));
    }

    @Test
    void bulkReject_rejectsAllPendingQuestions() {
        UUID moderatorId = UUID.randomUUID();
        UUID q1Id = UUID.randomUUID();

        Question q1 = Question.builder()
                .id(q1Id)
                .questionText(Map.of("uz_latn", "Test"))
                .status(QuestionStatus.PENDING)
                .build();

        when(questionRepository.findByIdInAndStatus(any(), eq(QuestionStatus.PENDING)))
                .thenReturn(List.of(q1));
        when(questionRepository.saveAll(any())).thenReturn(List.of(q1));

        String reason = "Poor quality content";
        BulkModerationResponse response = moderationService.bulkReject(
                List.of(q1Id), moderatorId, reason, AcceptLanguage.UZL);

        assertEquals(1, response.getTotalRequested());
        assertEquals(1, response.getSuccessCount());
        assertEquals(0, response.getFailedCount());
        assertEquals(QuestionStatus.REJECTED, q1.getStatus());
        assertEquals(reason, q1.getRejectionReason());
        assertEquals(moderatorId, q1.getModeratedBy());

        verify(auditService).log(eq(moderatorId), eq("MODERATOR"),
                eq("QUESTION_REJECTED"), eq("CONTENT"), eq("Question"), eq(q1Id));
    }
}
