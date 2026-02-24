package uz.eduplatform.modules.content.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.eduplatform.core.audit.AuditService;
import uz.eduplatform.core.common.dto.PagedResponse;
import uz.eduplatform.core.common.exception.BusinessException;
import uz.eduplatform.core.common.exception.ResourceNotFoundException;
import uz.eduplatform.core.common.utils.MessageService;
import uz.eduplatform.core.i18n.AcceptLanguage;
import uz.eduplatform.core.i18n.TranslatedField;
import uz.eduplatform.modules.auth.domain.User;
import uz.eduplatform.modules.auth.repository.UserRepository;
import uz.eduplatform.modules.content.domain.*;
import uz.eduplatform.modules.content.dto.*;
import uz.eduplatform.modules.content.repository.QuestionRepository;
import uz.eduplatform.modules.content.repository.QuestionVersionRepository;
import uz.eduplatform.modules.content.repository.TopicRepository;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final QuestionVersionRepository versionRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final MessageService messageService;
    private final ObjectMapper objectMapper;

    private static final Set<String> PROOF_REQUIRED_SUBJECTS = Set.of(
            "matematika", "fizika", "kimyo", "informatika"
    );

    @Transactional(readOnly = true)
    public PagedResponse<QuestionDto> getQuestions(UUID userId, QuestionFilterRequest filter, Pageable pageable, AcceptLanguage language) {
        String localeKey = language.toLocaleKey();
        Page<Question> page;

        if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
            // Native query requires actual DB column names for sorting
            Pageable nativePageable = PageRequest.of(
                    pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "created_at"));
            page = questionRepository.searchByUser(userId, filter.getSearch().trim(), nativePageable);
        } else {
            page = questionRepository.findAll(
                    buildFilterSpec(userId, filter), pageable
            );
        }

        List<QuestionDto> dtos = page.getContent().stream()
                .map(q -> mapToDto(q, localeKey))
                .toList();

        return PagedResponse.of(dtos, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public PagedResponse<QuestionDto> getQuestionsByTopic(UUID topicId, UUID userId, Pageable pageable, AcceptLanguage language) {
        String localeKey = language.toLocaleKey();

        // Validate that the topic belongs to a subject owned by this user
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic", "id", topicId));
        if (!topic.getSubject().getUser().getId().equals(userId)) {
            throw new BusinessException(messageService.get("error.access.denied", language.toLocale()));
        }

        Page<Question> page = questionRepository.findByTopicIdAndStatusNot(
                topicId, QuestionStatus.ARCHIVED, pageable);

        List<QuestionDto> dtos = page.getContent().stream()
                .map(q -> mapToDto(q, localeKey))
                .toList();

        return PagedResponse.of(dtos, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public List<QuestionDto> getQuestionsByIds(List<UUID> ids, UUID userId, AcceptLanguage language) {
        String localeKey = language.toLocaleKey();
        return questionRepository.findAllById(ids).stream()
                .filter(q -> q.getUser().getId().equals(userId))
                .map(q -> mapToDto(q, localeKey))
                .toList();
    }

    @Transactional(readOnly = true)
    public QuestionDto getQuestionById(UUID questionId, UUID userId, AcceptLanguage language) {
        String localeKey = language.toLocaleKey();
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", "id", questionId));

        if (!question.getUser().getId().equals(userId)) {
            throw new BusinessException(messageService.get("question.not.owner", language.toLocale()));
        }

        return mapToDto(question, localeKey);
    }

    @Transactional
    public QuestionDto createQuestion(UUID userId, CreateQuestionRequest request, AcceptLanguage language) {
        String localeKey = language.toLocaleKey();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Topic topic = topicRepository.findById(request.getTopicId())
                .orElseThrow(() -> new ResourceNotFoundException("Topic", "id", request.getTopicId()));

        // Clean locale keys (uzl -> uz_latn, uzc -> uz_cyrl)
        Map<String, String> cleanedText = TranslatedField.clean(request.getQuestionText());
        Map<String, String> cleanedProof = TranslatedField.clean(request.getProof());

        // Duplicate question text check
        String defaultText = TranslatedField.defaultValue(cleanedText);
        if (defaultText != null && questionRepository.existsByTopicIdAndDefaultQuestionText(request.getTopicId(), defaultText)) {
            throw new BusinessException(messageService.get("question.text.exists", language.toLocale()));
        }

        // Validate options based on question type
        validateQuestionOptions(request.getQuestionType(), request.getOptions(), request.getCorrectAnswer(), language.toLocale());

        // Check if proof is required
        boolean proofRequired = isProofRequired(topic.getSubject().getName());
        String resolvedProof = TranslatedField.resolve(cleanedProof);
        if (proofRequired && (resolvedProof == null || resolvedProof.isBlank())) {
            throw new BusinessException(messageService.get("question.proof.required.subject", language.toLocale(),
                    TranslatedField.resolve(topic.getSubject().getName(), localeKey)));
        }

        Question question = Question.builder()
                .topic(topic)
                .user(user)
                .questionText(cleanedText)
                .questionType(request.getQuestionType())
                .difficulty(request.getDifficulty() != null ? request.getDifficulty() : Difficulty.MEDIUM)
                .points(request.getPoints() != null ? request.getPoints() : java.math.BigDecimal.ONE)
                .timeLimitSeconds(request.getTimeLimitSeconds())
                .media(request.getMedia() != null ? request.getMedia() : Map.of())
                .options(request.getOptions() != null ? toJson(request.getOptions()) : "[]")
                .correctAnswer(request.getCorrectAnswer() != null ? toJson(request.getCorrectAnswer()) : "\"\"")

                .proof(cleanedProof)
                .proofRequired(proofRequired)
                .status(QuestionStatus.DRAFT)
                .build();

        question = questionRepository.save(question);

        // Update topic question count
        updateTopicQuestionCount(topic.getId());

        auditService.log(userId, null, "QUESTION_CREATED", "CONTENT",
                "Question", question.getId());

        return mapToDto(question, localeKey);
    }

    @Transactional
    public BulkCreateResponse createQuestionsBulk(UUID userId, BulkCreateQuestionRequest request, AcceptLanguage language) {
        int created = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < request.getItems().size(); i++) {
            CreateQuestionRequest item = request.getItems().get(i);
            try {
                Map<String, String> cleanedText = TranslatedField.clean(item.getQuestionText());
                String defaultText = TranslatedField.defaultValue(cleanedText);
                if (defaultText != null && questionRepository.existsByTopicIdAndDefaultQuestionText(item.getTopicId(), defaultText)) {
                    if (request.isSkipDuplicates()) {
                        skipped++;
                        continue;
                    }
                    errors.add("[" + (i + 1) + "]: " + messageService.get("question.text.exists", language.toLocale()));
                    continue;
                }
                createQuestion(userId, item, language);
                created++;
            } catch (Exception e) {
                errors.add("[" + (i + 1) + "] " + e.getMessage());
            }
        }

        return BulkCreateResponse.builder()
                .created(created)
                .skipped(skipped)
                .errors(errors)
                .build();
    }

    @Transactional
    public QuestionDto updateQuestion(UUID questionId, UUID userId, UpdateQuestionRequest request,
                                       AcceptLanguage language, boolean fullUpdate) {
        String localeKey = language.toLocaleKey();
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", "id", questionId));

        if (!question.getUser().getId().equals(userId)) {
            throw new BusinessException(messageService.get("question.not.owner", language.toLocale()));
        }

        if (question.getStatus() == QuestionStatus.ACTIVE) {
            throw new BusinessException(messageService.get("question.edit.active", language.toLocale()));
        }

        // Save current version to history
        saveVersion(question, userId, request.getChangeReason());

        // Update fields
        // PUT (fullUpdate=true): replace all fields, null clears the value
        // PATCH (fullUpdate=false): skip null fields (only update what was sent)
        if (request.getQuestionText() != null) {
            if (fullUpdate) {
                question.setQuestionText(TranslatedField.clean(request.getQuestionText()));
            } else {
                question.setQuestionText(TranslatedField.merge(question.getQuestionText(), TranslatedField.clean(request.getQuestionText())));
            }
        }
        if (request.getQuestionType() != null) {
            question.setQuestionType(request.getQuestionType());
        }
        if (request.getDifficulty() != null) {
            question.setDifficulty(request.getDifficulty());
        }
        if (request.getPoints() != null) {
            question.setPoints(request.getPoints());
        }
        if (fullUpdate || request.getTimeLimitSeconds() != null) {
            question.setTimeLimitSeconds(request.getTimeLimitSeconds());
        }
        if (request.getMedia() != null) {
            question.setMedia(request.getMedia());
        }
        if (request.getOptions() != null) {
            question.setOptions(toJson(request.getOptions()));
        }
        if (request.getCorrectAnswer() != null) {
            question.setCorrectAnswer(toJson(request.getCorrectAnswer()));
        }
        if (fullUpdate) {
            Map<String, String> cleanedProof = request.getProof() != null
                    ? TranslatedField.clean(request.getProof()) : Map.of();
            question.setProof(cleanedProof);
        } else if (request.getProof() != null) {
            question.setProof(TranslatedField.merge(question.getProof(), TranslatedField.clean(request.getProof())));
        }

        // Validate if options and correct answer were updated
        if (request.getOptions() != null || request.getCorrectAnswer() != null) {
            validateQuestionOptions(question.getQuestionType(), fromJson(question.getOptions()), fromJson(question.getCorrectAnswer()), language.toLocale());
        }

        question.setVersion(question.getVersion() + 1);

        // If was rejected, reset to draft
        if (question.getStatus() == QuestionStatus.REJECTED) {
            question.setStatus(QuestionStatus.DRAFT);
            question.setRejectionReason(null);
        }

        question = questionRepository.save(question);

        auditService.log(userId, null, "QUESTION_UPDATED", "CONTENT",
                "Question", questionId);

        return mapToDto(question, localeKey);
    }

    @Transactional
    public void deleteQuestion(UUID questionId, UUID userId, AcceptLanguage language) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", "id", questionId));

        if (!question.getUser().getId().equals(userId)) {
            throw new BusinessException(messageService.get("question.not.owner", language.toLocale()));
        }

        if (question.getTimesUsed() > 0) {
            throw new BusinessException(messageService.get("question.delete.used", language.toLocale()));
        }

        UUID topicId = question.getTopic().getId();
        question.setDeletedAt(LocalDateTime.now());
        questionRepository.save(question);

        // Update topic question count
        updateTopicQuestionCount(topicId);

        auditService.log(userId, null, "QUESTION_DELETED", "CONTENT",
                "Question", questionId);
    }

    @Transactional
    public QuestionDto submitForModeration(UUID questionId, UUID userId, AcceptLanguage language) {
        String localeKey = language.toLocaleKey();
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", "id", questionId));

        if (!question.getUser().getId().equals(userId)) {
            throw new BusinessException(messageService.get("question.not.owner", language.toLocale()));
        }

        if (question.getStatus() != QuestionStatus.DRAFT && question.getStatus() != QuestionStatus.REJECTED) {
            throw new BusinessException(messageService.get("question.submit.invalid.status", language.toLocale()));
        }

        // Validate proof requirement before submission
        String resolvedProof = TranslatedField.resolve(question.getProof());
        if (question.getProofRequired() && (resolvedProof == null || resolvedProof.isBlank())) {
            throw new BusinessException(messageService.get("question.proof.required", language.toLocale()));
        }

        question.setStatus(QuestionStatus.PENDING);
        question = questionRepository.save(question);

        auditService.log(userId, null, "QUESTION_SUBMITTED", "CONTENT",
                "Question", questionId);

        return mapToDto(question, localeKey);
    }

    @Transactional
    public BulkModerationResponse bulkSubmitForModeration(UUID userId, List<UUID> questionIds, AcceptLanguage language) {
        int successCount = 0;
        List<UUID> failedIds = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (UUID questionId : questionIds) {
            try {
                submitForModeration(questionId, userId, language);
                successCount++;
            } catch (Exception e) {
                failedIds.add(questionId);
                errors.add(questionId + ": " + e.getMessage());
            }
        }

        return BulkModerationResponse.builder()
                .totalRequested(questionIds.size())
                .successCount(successCount)
                .failedCount(failedIds.size())
                .failedIds(failedIds)
                .errors(errors)
                .build();
    }

    @Transactional(readOnly = true)
    public List<QuestionVersionDto> getVersionHistory(UUID questionId, UUID userId, AcceptLanguage language) {
        String localeKey = language.toLocaleKey();
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", "id", questionId));

        if (!question.getUser().getId().equals(userId)) {
            throw new BusinessException(messageService.get("question.not.owner", language.toLocale()));
        }

        return versionRepository.findByQuestionIdOrderByVersionDesc(questionId).stream()
                .map(v -> mapVersionToDto(v, localeKey))
                .toList();
    }

    @Transactional
    public QuestionDto rollbackToVersion(UUID questionId, Integer targetVersion, UUID userId, AcceptLanguage language) {
        String localeKey = language.toLocaleKey();
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", "id", questionId));

        if (!question.getUser().getId().equals(userId)) {
            throw new BusinessException(messageService.get("question.not.owner", language.toLocale()));
        }

        QuestionVersion version = versionRepository.findByQuestionIdAndVersion(questionId, targetVersion)
                .orElseThrow(() -> new ResourceNotFoundException("Version", "version", targetVersion));

        // Save current state before rollback
        saveVersion(question, userId, "Rollback to version " + targetVersion);

        // Apply version data
        question.setQuestionText(version.getQuestionText());
        question.setOptions(version.getOptions());
        question.setCorrectAnswer(version.getCorrectAnswer());
        question.setProof(version.getProof());
        question.setVersion(question.getVersion() + 1);
        question.setStatus(QuestionStatus.DRAFT);

        question = questionRepository.save(question);

        auditService.log(userId, null, "QUESTION_ROLLBACK", "CONTENT",
                "Question", questionId);

        return mapToDto(question, localeKey);
    }

    private void saveVersion(Question question, UUID changedBy, String changeReason) {
        QuestionVersion version = QuestionVersion.builder()
                .questionId(question.getId())
                .version(question.getVersion())
                .questionText(question.getQuestionText())
                .options(question.getOptions())
                .correctAnswer(question.getCorrectAnswer())
                .proof(question.getProof())
                .changedBy(changedBy)
                .changeReason(changeReason)
                .build();

        versionRepository.save(version);
    }

    @SuppressWarnings("unchecked")
    private void validateQuestionOptions(QuestionType type, Object options, Object correctAnswer, java.util.Locale locale) {
        // MCQ types MUST have options and correct answer - never skip validation
        switch (type) {
            case MCQ_SINGLE -> {
                if (options == null || !(options instanceof List<?> optList) || optList.isEmpty()) {
                    throw new BusinessException(messageService.get("question.mcq.single.options.required", locale));
                }
                if (optList.size() < 2 || optList.size() > 10) {
                    throw new BusinessException(messageService.get("question.mcq.single.options.count", locale));
                }
                // Validate each option has "text" field
                for (int i = 0; i < optList.size(); i++) {
                    Object opt = optList.get(i);
                    if (!(opt instanceof Map<?, ?> optMap) || optMap.get("text") == null) {
                        throw new BusinessException(messageService.get("question.mcq.option.text.required", locale));
                    }
                }
                long correctCount = optList.stream()
                        .filter(o -> o instanceof Map && Boolean.TRUE.equals(((Map<String, Object>) o).get("isCorrect")))
                        .count();
                if (correctCount != 1) {
                    throw new BusinessException(messageService.get("question.mcq.single.one.correct", locale));
                }
            }
            case MCQ_MULTI -> {
                if (options == null || !(options instanceof List<?> optList) || optList.isEmpty()) {
                    throw new BusinessException(messageService.get("question.mcq.multi.options.required", locale));
                }
                if (optList.size() < 2 || optList.size() > 10) {
                    throw new BusinessException(messageService.get("question.mcq.multi.options.count", locale));
                }
                // Validate each option has "text" field
                for (int i = 0; i < optList.size(); i++) {
                    Object opt = optList.get(i);
                    if (!(opt instanceof Map<?, ?> optMap) || optMap.get("text") == null) {
                        throw new BusinessException(messageService.get("question.mcq.option.text.required", locale));
                    }
                }
                long correctCount = optList.stream()
                        .filter(o -> o instanceof Map && Boolean.TRUE.equals(((Map<String, Object>) o).get("isCorrect")))
                        .count();
                if (correctCount < 2) {
                    throw new BusinessException(messageService.get("question.mcq.multi.min.correct", locale));
                }
            }
            case TRUE_FALSE -> {
                // correct answer should be boolean
                if (correctAnswer == null) {
                    throw new BusinessException(messageService.get("question.true.false.answer.required", locale));
                }
            }
            case MATCHING -> {
                if (options == null || correctAnswer == null) return;
                if (options instanceof Map<?, ?> matchMap) {
                    if (!matchMap.containsKey("left") || !matchMap.containsKey("right") || !matchMap.containsKey("correctPairs")) {
                        throw new BusinessException(messageService.get("question.matching.fields.required", locale));
                    }
                }
            }
            case ORDERING -> {
                if (options == null || correctAnswer == null) return;
                if (options instanceof Map<?, ?> orderMap) {
                    if (!orderMap.containsKey("items") || !orderMap.containsKey("correctOrder")) {
                        throw new BusinessException(messageService.get("question.ordering.fields.required", locale));
                    }
                }
            }
            case FILL_BLANK -> {
                if (options == null || correctAnswer == null) return;
                if (options instanceof Map<?, ?> fillMap) {
                    if (!fillMap.containsKey("answers")) {
                        throw new BusinessException(messageService.get("question.fill.blank.fields.required", locale));
                    }
                }
            }
            case SHORT_ANSWER, ESSAY -> {
                // No strict validation for these types
            }
        }
    }

    private boolean isProofRequired(Map<String, String> subjectName) {
        if (subjectName == null) return false;
        String defaultName = TranslatedField.defaultValue(subjectName);
        if (defaultName == null) return false;
        return PROOF_REQUIRED_SUBJECTS.contains(defaultName.toLowerCase().trim());
    }

    private void updateTopicQuestionCount(UUID topicId) {
        Topic topic = topicRepository.findById(topicId).orElse(null);
        if (topic != null) {
            long count = questionRepository.countByTopicId(topicId);
            topic.setQuestionCount((int) count);
            topicRepository.save(topic);
        }
    }

    private Specification<Question> buildFilterSpec(UUID userId, QuestionFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("user").get("id"), userId));
            if (filter.getSubjectId() != null) {
                predicates.add(cb.equal(root.get("topic").get("subject").get("id"), filter.getSubjectId()));
            }
            if (filter.getTopicId() != null) {
                predicates.add(cb.equal(root.get("topic").get("id"), filter.getTopicId()));
            }
            if (filter.getQuestionType() != null) {
                predicates.add(cb.equal(root.get("questionType"), filter.getQuestionType()));
            }
            if (filter.getDifficulty() != null) {
                predicates.add(cb.equal(root.get("difficulty"), filter.getDifficulty()));
            }
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public QuestionDto mapToDto(Question q, String localeKey) {
        return QuestionDto.builder()
                .id(q.getId())
                .topicId(q.getTopic().getId())
                .topicName(TranslatedField.resolve(q.getTopic().getName(), localeKey))
                .topicNameTranslations(TranslatedField.clean(q.getTopic().getName()))
                .subjectId(q.getTopic().getSubject().getId())
                .subjectName(TranslatedField.resolve(q.getTopic().getSubject().getName(), localeKey))
                .subjectNameTranslations(TranslatedField.clean(q.getTopic().getSubject().getName()))
                .userId(q.getUser().getId())
                .userName(q.getUser().getFirstName() + " " + q.getUser().getLastName())
                .questionText(TranslatedField.resolve(q.getQuestionText(), localeKey))
                .questionTextTranslations(TranslatedField.clean(q.getQuestionText()))
                .questionType(q.getQuestionType())
                .difficulty(q.getDifficulty())
                .points(q.getPoints())
                .timeLimitSeconds(q.getTimeLimitSeconds())
                .media(q.getMedia())
                .options(resolveOptions(fromJson(q.getOptions()), localeKey))
                .correctAnswer(fromJson(q.getCorrectAnswer()))
                .proof(TranslatedField.resolve(q.getProof(), localeKey))
                .proofTranslations(TranslatedField.clean(q.getProof()))
                .proofRequired(q.getProofRequired())
                .status(q.getStatus())
                .rejectionReason(q.getRejectionReason())
                .moderatedBy(q.getModeratedBy())
                .moderatedAt(q.getModeratedAt())
                .timesUsed(q.getTimesUsed())
                .correctRate(q.getCorrectRate())
                .avgTimeSeconds(q.getAvgTimeSeconds())
                .version(q.getVersion())
                .createdAt(q.getCreatedAt())
                .updatedAt(q.getUpdatedAt())
                .publishedAt(q.getPublishedAt())
                .build();
    }

    private QuestionVersionDto mapVersionToDto(QuestionVersion v, String localeKey) {
        return QuestionVersionDto.builder()
                .id(v.getId())
                .questionId(v.getQuestionId())
                .version(v.getVersion())
                .questionText(TranslatedField.resolve(v.getQuestionText(), localeKey))
                .questionTextTranslations(TranslatedField.clean(v.getQuestionText()))
                .options(resolveOptions(fromJson(v.getOptions()), localeKey))
                .correctAnswer(fromJson(v.getCorrectAnswer()))
                .proof(TranslatedField.resolve(v.getProof(), localeKey))
                .proofTranslations(TranslatedField.clean(v.getProof()))
                .changedBy(v.getChangedBy())
                .changeReason(v.getChangeReason())
                .createdAt(v.getCreatedAt())
                .build();
    }

    @SuppressWarnings("unchecked")
    private Object resolveOptions(Object options, String localeKey) {
        if (options instanceof List<?> optList) {
            return optList.stream()
                    .filter(o -> o instanceof Map)
                    .map(o -> {
                        Map<String, Object> opt = new LinkedHashMap<>((Map<String, Object>) o);
                        Object text = opt.get("text");
                        if (text instanceof Map) {
                            Map<String, String> textMap = (Map<String, String>) text;
                            Map<String, String> cleanedMap = TranslatedField.clean(textMap);
                            opt.put("text", TranslatedField.resolve(textMap, localeKey));
                            opt.put("textTranslations", cleanedMap != null ? cleanedMap : textMap);
                        }
                        return opt;
                    })
                    .toList();
        }

        // Handle object format: {"A": {"uz_latn": "...", "en": "..."}, "B": {...}}
        if (options instanceof Map<?, ?> optMap) {
            Map<String, Object> resolved = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : optMap.entrySet()) {
                String key = String.valueOf(entry.getKey());
                Object value = entry.getValue();
                if (value instanceof Map) {
                    Map<String, String> textMap = (Map<String, String>) value;
                    Map<String, String> cleanedMap = TranslatedField.clean(textMap);
                    Map<String, Object> optData = new LinkedHashMap<>();
                    optData.put("text", TranslatedField.resolve(textMap, localeKey));
                    optData.put("textTranslations", cleanedMap != null ? cleanedMap : textMap);
                    resolved.put(key, optData);
                } else {
                    resolved.put(key, value);
                }
            }
            return resolved;
        }

        return options;
    }

    @SuppressWarnings("unchecked")
    private String resolveOptionText(Object text, String localeKey) {
        if (text instanceof Map) {
            return TranslatedField.resolve((Map<String, String>) text, localeKey);
        }
        return text != null ? String.valueOf(text) : "";
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw BusinessException.ofKey("question.json.serialize.failed");
        }
    }

    private Object fromJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException e) {
            throw BusinessException.ofKey("question.json.parse.failed");
        }
    }
}
