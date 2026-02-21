package uz.eduplatform.modules.content.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.eduplatform.core.audit.AuditService;
import uz.eduplatform.core.common.dto.PagedResponse;
import uz.eduplatform.core.common.exception.BusinessException;
import uz.eduplatform.core.common.exception.ResourceNotFoundException;
import uz.eduplatform.core.common.utils.MessageService;
import uz.eduplatform.core.i18n.AcceptLanguage;
import uz.eduplatform.modules.content.domain.Question;
import uz.eduplatform.modules.content.domain.QuestionStatus;
import uz.eduplatform.modules.content.dto.QuestionDto;
import uz.eduplatform.modules.content.repository.QuestionRepository;

import uz.eduplatform.modules.content.dto.BulkModerationResponse;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionModerationService {

    private final QuestionRepository questionRepository;
    private final QuestionService questionService;
    private final AuditService auditService;
    private final MessageService messageService;

    @Transactional(readOnly = true)
    public PagedResponse<QuestionDto> getPendingQuestions(Pageable pageable, AcceptLanguage language) {
        String localeKey = language.toLocaleKey();
        Page<Question> page = questionRepository.findByStatus(QuestionStatus.PENDING, pageable);

        List<QuestionDto> dtos = page.getContent().stream()
                .map(q -> questionService.mapToDto(q, localeKey))
                .toList();

        return PagedResponse.of(dtos, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Transactional
    public QuestionDto approveQuestion(UUID questionId, UUID moderatorId, AcceptLanguage language) {
        String localeKey = language.toLocaleKey();
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", "id", questionId));

        if (question.getStatus() != QuestionStatus.PENDING) {
            throw new BusinessException(messageService.get("question.approve.invalid.status", language.toLocale()));
        }

        question.setStatus(QuestionStatus.ACTIVE);
        question.setModeratedBy(moderatorId);
        question.setModeratedAt(LocalDateTime.now());
        question.setPublishedAt(LocalDateTime.now());

        question = questionRepository.save(question);

        auditService.log(moderatorId, "MODERATOR", "QUESTION_APPROVED", "CONTENT",
                "Question", questionId);

        return questionService.mapToDto(question, localeKey);
    }

    @Transactional
    public QuestionDto rejectQuestion(UUID questionId, UUID moderatorId, String reason, AcceptLanguage language) {
        String localeKey = language.toLocaleKey();
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", "id", questionId));

        if (question.getStatus() != QuestionStatus.PENDING) {
            throw new BusinessException(messageService.get("question.reject.invalid.status", language.toLocale()));
        }

        if (reason == null || reason.isBlank()) {
            throw new BusinessException(messageService.get("question.reject.reason.required", language.toLocale()));
        }

        question.setStatus(QuestionStatus.REJECTED);
        question.setRejectionReason(reason);
        question.setModeratedBy(moderatorId);
        question.setModeratedAt(LocalDateTime.now());

        question = questionRepository.save(question);

        auditService.log(moderatorId, "MODERATOR", "QUESTION_REJECTED", "CONTENT",
                "Question", questionId);

        return questionService.mapToDto(question, localeKey);
    }

    @Transactional
    public BulkModerationResponse bulkApprove(List<UUID> questionIds, UUID moderatorId, AcceptLanguage language) {
        List<Question> pendingQuestions = questionRepository.findByIdInAndStatus(questionIds, QuestionStatus.PENDING);
        Set<UUID> pendingIds = pendingQuestions.stream().map(Question::getId).collect(Collectors.toSet());

        List<UUID> failedIds = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (UUID id : questionIds) {
            if (!pendingIds.contains(id)) {
                failedIds.add(id);
                errors.add(messageService.get("question.bulk.not.pending", language.toLocale(), id));
            }
        }

        for (Question question : pendingQuestions) {
            question.setStatus(QuestionStatus.ACTIVE);
            question.setModeratedBy(moderatorId);
            question.setModeratedAt(LocalDateTime.now());
            question.setPublishedAt(LocalDateTime.now());
            auditService.log(moderatorId, "MODERATOR", "QUESTION_APPROVED", "CONTENT",
                    "Question", question.getId());
        }

        questionRepository.saveAll(pendingQuestions);

        return BulkModerationResponse.builder()
                .totalRequested(questionIds.size())
                .successCount(pendingQuestions.size())
                .failedCount(failedIds.size())
                .failedIds(failedIds)
                .errors(errors)
                .build();
    }

    @Transactional
    public BulkModerationResponse bulkReject(List<UUID> questionIds, UUID moderatorId, String reason, AcceptLanguage language) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(messageService.get("question.reject.reason.required", language.toLocale()));
        }

        List<Question> pendingQuestions = questionRepository.findByIdInAndStatus(questionIds, QuestionStatus.PENDING);
        Set<UUID> pendingIds = pendingQuestions.stream().map(Question::getId).collect(Collectors.toSet());

        List<UUID> failedIds = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (UUID id : questionIds) {
            if (!pendingIds.contains(id)) {
                failedIds.add(id);
                errors.add(messageService.get("question.bulk.not.pending", language.toLocale(), id));
            }
        }

        for (Question question : pendingQuestions) {
            question.setStatus(QuestionStatus.REJECTED);
            question.setRejectionReason(reason);
            question.setModeratedBy(moderatorId);
            question.setModeratedAt(LocalDateTime.now());
            auditService.log(moderatorId, "MODERATOR", "QUESTION_REJECTED", "CONTENT",
                    "Question", question.getId());
        }

        questionRepository.saveAll(pendingQuestions);

        return BulkModerationResponse.builder()
                .totalRequested(questionIds.size())
                .successCount(pendingQuestions.size())
                .failedCount(failedIds.size())
                .failedIds(failedIds)
                .errors(errors)
                .build();
    }
}
