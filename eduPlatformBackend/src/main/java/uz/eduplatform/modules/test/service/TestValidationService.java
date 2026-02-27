package uz.eduplatform.modules.test.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import uz.eduplatform.core.common.dto.PagedResponse;
import uz.eduplatform.core.common.exception.BusinessException;
import uz.eduplatform.core.common.exception.ResourceNotFoundException;
import uz.eduplatform.core.i18n.AcceptLanguage;
import uz.eduplatform.modules.content.domain.Difficulty;
import uz.eduplatform.modules.content.domain.Question;
import uz.eduplatform.modules.content.domain.QuestionStatus;
import uz.eduplatform.modules.content.domain.QuestionType;
import uz.eduplatform.modules.content.dto.QuestionDto;
import uz.eduplatform.modules.content.repository.QuestionRepository;
import uz.eduplatform.modules.content.repository.SubjectRepository;
import uz.eduplatform.modules.content.service.QuestionService;
import uz.eduplatform.modules.test.dto.AvailableQuestionsResponse;
import uz.eduplatform.modules.test.dto.DifficultyDistribution;
import uz.eduplatform.modules.test.dto.GenerateTestRequest;
import uz.eduplatform.modules.test.exception.InsufficientQuestionsException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestValidationService {

    private final SubjectRepository subjectRepository;
    private final QuestionRepository questionRepository;
    private final QuestionService questionService;
    private final ObjectMapper objectMapper;

    public void validateRequest(GenerateTestRequest request) {
        // Validate subject exists
        subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", request.getSubjectId()));

        boolean isManualMode = request.getQuestionIds() != null && !request.getQuestionIds().isEmpty();

        if (isManualMode) {
            // Manual mode: questionCount and difficultyDistribution are optional
            if (request.getQuestionIds().size() > 100) {
                throw BusinessException.ofKey("test.manual.too.many.questions", 100);
            }
        } else {
            // Auto mode: questionCount is required
            if (request.getQuestionCount() == null || request.getQuestionCount() < 1) {
                throw BusinessException.ofKey("test.question.count.required");
            }

            // Validate difficulty distribution sums to 100
            DifficultyDistribution dist = request.getDifficultyDistribution();
            if (dist != null) {
                int total = dist.getEasy() + dist.getMedium() + dist.getHard();
                if (total != 100) {
                    throw BusinessException.ofKey("test.difficulty.distribution.invalid", total);
                }
            }
        }
    }

    public void checkAvailability(GenerateTestRequest request, Map<Difficulty, List<Question>> byDifficulty) {
        DifficultyDistribution dist = request.getDifficultyDistribution() != null
                ? request.getDifficultyDistribution()
                : DifficultyDistribution.builder().build();

        int easyCount = (int) Math.ceil(request.getQuestionCount() * dist.getEasy() / 100.0);
        int mediumCount = (int) Math.ceil(request.getQuestionCount() * dist.getMedium() / 100.0);
        int hardCount = request.getQuestionCount() - easyCount - mediumCount;

        int availEasy = byDifficulty.getOrDefault(Difficulty.EASY, List.of()).size();
        int availMedium = byDifficulty.getOrDefault(Difficulty.MEDIUM, List.of()).size();
        int availHard = byDifficulty.getOrDefault(Difficulty.HARD, List.of()).size();

        if (availEasy < easyCount || availMedium < mediumCount || availHard < hardCount) {
            throw new InsufficientQuestionsException(
                    Map.of("easy", availEasy, "medium", availMedium, "hard", availHard),
                    Map.of("easy", easyCount, "medium", mediumCount, "hard", hardCount)
            );
        }
    }

    public AvailableQuestionsResponse getAvailableQuestions(List<UUID> topicIds, UUID userId) {
        return getAvailableQuestions(topicIds, null, userId);
    }

    public AvailableQuestionsResponse getAvailableQuestions(List<UUID> topicIds, UUID subjectId, UUID userId) {
        boolean hasTopics = topicIds != null && !topicIds.isEmpty();
        List<Question> questions;
        if (hasTopics) {
            questions = questionRepository.findByTopicIdsForTeacher(topicIds, userId);
        } else if (subjectId != null) {
            questions = questionRepository.findBySubjectIdForTeacher(subjectId, userId);
        } else {
            return AvailableQuestionsResponse.builder()
                    .totalAvailable(0).easyCount(0).mediumCount(0).hardCount(0).maxPossibleQuestions(0)
                    .build();
        }

        // Filter only valid MCQ questions with options for test generation
        List<Question> validMcq = questions.stream()
                .filter(this::isValidMcqForTest)
                .toList();

        if (validMcq.size() < questions.size()) {
            log.debug("Available questions: {} total, {} valid MCQ with options",
                    questions.size(), validMcq.size());
        }

        Map<Difficulty, Long> counts = validMcq.stream()
                .collect(Collectors.groupingBy(Question::getDifficulty, Collectors.counting()));

        int easy = counts.getOrDefault(Difficulty.EASY, 0L).intValue();
        int medium = counts.getOrDefault(Difficulty.MEDIUM, 0L).intValue();
        int hard = counts.getOrDefault(Difficulty.HARD, 0L).intValue();

        return AvailableQuestionsResponse.builder()
                .totalAvailable(validMcq.size())
                .easyCount(easy)
                .mediumCount(medium)
                .hardCount(hard)
                .maxPossibleQuestions(validMcq.size())
                .build();
    }

    public PagedResponse<QuestionDto> getQuestionsForSelection(
            List<UUID> topicIds, UUID subjectId, Difficulty difficulty, QuestionStatus status, String search,
            Pageable pageable, AcceptLanguage language, UUID userId) {
        String localeKey = language.toLocaleKey();
        String searchTerm = (search != null && !search.isBlank()) ? search.trim() : null;

        boolean useTopics = topicIds != null && !topicIds.isEmpty();

        if (!useTopics && subjectId == null) {
            return PagedResponse.of(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0L, 0);
        }

        Page<Question> page;
        if (!useTopics && subjectId != null) {
            // Subject-wide mode: single native query handles search/difficulty/status
            Pageable nativePageable = PageRequest.of(
                    pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "created_at"));
            page = questionRepository.searchBySubjectIdForTeacher(subjectId, userId, searchTerm,
                    difficulty != null ? difficulty.name() : null,
                    status != null ? status.name() : null, nativePageable);
        } else if (searchTerm != null) {
            // Native query requires actual DB column names for sorting (created_at, not createdAt)
            Pageable nativePageable = PageRequest.of(
                    pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "created_at"));
            // Text search across all accessible questions in given topics
            page = questionRepository.searchByTopicIdsForTeacher(topicIds, userId, searchTerm,
                    difficulty != null ? difficulty.name() : null,
                    status != null ? status.name() : null, nativePageable);
        } else if (status == null) {
            // No status filter: ACTIVE/APPROVED + teacher's own DRAFT/PENDING
            page = difficulty != null
                    ? questionRepository.findByTopicIdsFilteredForTeacher(topicIds, userId, difficulty, pageable)
                    : questionRepository.findByTopicIdsForTeacherPaged(topicIds, userId, pageable);
        } else if (status == QuestionStatus.ACTIVE || status == QuestionStatus.APPROVED) {
            // ACTIVE or APPROVED from everyone
            page = difficulty != null
                    ? questionRepository.findByTopicIdsFiltered(topicIds, difficulty, status, pageable)
                    : questionRepository.findByTopicIdsFilteredNoDifficulty(topicIds, status, pageable);
        } else {
            // DRAFT or PENDING: only teacher's own
            page = difficulty != null
                    ? questionRepository.findByTopicIdsFilteredOwned(topicIds, userId, status, difficulty, pageable)
                    : questionRepository.findByTopicIdsOwnedNoDifficulty(topicIds, userId, status, pageable);
        }

        List<QuestionDto> dtos = page.getContent().stream()
                .map(q -> questionService.mapToDto(q, localeKey))
                .toList();

        return PagedResponse.of(dtos, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    /**
     * Checks if a question is a valid MCQ with options for test generation.
     * Requires: MCQ_SINGLE or MCQ_MULTI type, at least 2 options, at least 1 correct answer.
     */
    @SuppressWarnings("unchecked")
    private boolean isValidMcqForTest(Question q) {
        if (q.getQuestionType() != QuestionType.MCQ_SINGLE
                && q.getQuestionType() != QuestionType.MCQ_MULTI) {
            return false;
        }

        Object parsed = parseJson(q.getOptions());
        if (!(parsed instanceof List<?> optionsList) || optionsList.size() < 2) {
            return false;
        }

        boolean hasCorrect = false;
        for (Object opt : optionsList) {
            if (!(opt instanceof Map<?, ?> optMap)) {
                return false;
            }
            if (optMap.get("text") == null) {
                return false;
            }
            if (Boolean.TRUE.equals(optMap.get("isCorrect"))) {
                hasCorrect = true;
            }
        }

        return hasCorrect;
    }

    private Object parseJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return null;
        }
    }
}
