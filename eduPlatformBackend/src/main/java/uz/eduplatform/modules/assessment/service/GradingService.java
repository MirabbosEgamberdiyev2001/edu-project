package uz.eduplatform.modules.assessment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.eduplatform.core.common.exception.BusinessException;
import uz.eduplatform.core.common.exception.ResourceNotFoundException;
import uz.eduplatform.modules.assessment.domain.Answer;
import uz.eduplatform.modules.assessment.domain.AttemptStatus;
import uz.eduplatform.modules.assessment.domain.TestAttempt;
import uz.eduplatform.modules.assessment.dto.GradeAnswerRequest;
import uz.eduplatform.modules.assessment.repository.AnswerRepository;
import uz.eduplatform.modules.assessment.repository.TestAttemptRepository;
import uz.eduplatform.modules.content.domain.Question;
import uz.eduplatform.modules.content.domain.QuestionType;
import uz.eduplatform.modules.content.repository.QuestionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GradingService {

    private final TestAttemptRepository attemptRepository;
    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;

    /**
     * Auto-grade an attempt based on PRD grading algorithm.
     * MCQ_SINGLE, MCQ_MULTI, TRUE_FALSE, MATCHING, ORDERING are auto-graded.
     * SHORT_ANSWER, ESSAY, FILL_BLANK need manual grading.
     */
    @Transactional
    public TestAttempt gradeAttempt(TestAttempt attempt) {
        List<Answer> answers = answerRepository.findByAttemptIdOrderByQuestionIndexAsc(attempt.getId());

        // Collect all question IDs and fetch in batch
        List<UUID> questionIds = answers.stream().map(Answer::getQuestionId).toList();
        Map<UUID, Question> questionMap = new HashMap<>();
        questionRepository.findAllById(questionIds).forEach(q -> questionMap.put(q.getId(), q));

        BigDecimal totalScore = BigDecimal.ZERO;
        BigDecimal maxScore = BigDecimal.ZERO;
        boolean needsManualReview = false;

        for (Answer answer : answers) {
            Question question = questionMap.get(answer.getQuestionId());
            if (question == null) {
                log.warn("Question {} not found during grading of attempt {}", answer.getQuestionId(), attempt.getId());
                continue;
            }

            BigDecimal questionPoints = question.getPoints() != null ? question.getPoints() : BigDecimal.ONE;
            answer.setMaxPoints(questionPoints);
            maxScore = maxScore.add(questionPoints);

            if (answer.getSelectedAnswer() == null || answer.getSelectedAnswer().isBlank()) {
                // Unanswered
                answer.setEarnedPoints(BigDecimal.ZERO);
                answer.setIsCorrect(false);
                continue;
            }

            QuestionType type = question.getQuestionType();
            switch (type) {
                case MCQ_SINGLE -> gradeMcqSingle(answer, question);
                case MCQ_MULTI -> gradeMcqMulti(answer, question);
                case TRUE_FALSE -> gradeTrueFalse(answer, question);
                case MATCHING -> gradeMatching(answer, question);
                case ORDERING -> gradeOrdering(answer, question);
                default -> {
                    // SHORT_ANSWER, ESSAY, FILL_BLANK - need manual grading
                    answer.setNeedsManualGrading(true);
                    answer.setEarnedPoints(BigDecimal.ZERO);
                    needsManualReview = true;
                }
            }

            if (answer.getEarnedPoints() != null) {
                totalScore = totalScore.add(answer.getEarnedPoints());
            }
        }

        answerRepository.saveAll(answers);

        // Update attempt scores
        attempt.setRawScore(totalScore);
        attempt.setMaxScore(maxScore);
        if (maxScore.compareTo(BigDecimal.ZERO) > 0) {
            attempt.setPercentage(totalScore.multiply(BigDecimal.valueOf(100))
                    .divide(maxScore, 2, RoundingMode.HALF_UP));
        } else {
            attempt.setPercentage(BigDecimal.ZERO);
        }

        attempt.setStatus(needsManualReview ? AttemptStatus.NEEDS_REVIEW : AttemptStatus.AUTO_GRADED);
        return attemptRepository.save(attempt);
    }

    /**
     * Manually grade a single answer (for SHORT_ANSWER, ESSAY, FILL_BLANK).
     */
    @Transactional
    public void gradeManually(UUID teacherId, GradeAnswerRequest request) {
        Answer answer = answerRepository.findById(request.getAnswerId())
                .orElseThrow(() -> new ResourceNotFoundException("Answer", "id", request.getAnswerId()));

        if (request.getScore().compareTo(answer.getMaxPoints()) > 0) {
            throw BusinessException.ofKey("grading.score.exceeds.max", answer.getMaxPoints());
        }

        answer.setManualScore(request.getScore());
        answer.setEarnedPoints(request.getScore());
        answer.setManualFeedback(request.getFeedback());
        answer.setGradedBy(teacherId);
        answer.setGradedAt(LocalDateTime.now());
        answer.setNeedsManualGrading(false);
        answer.setIsCorrect(request.getScore().compareTo(answer.getMaxPoints()) == 0);
        answer.setIsPartial(request.getScore().compareTo(BigDecimal.ZERO) > 0
                && request.getScore().compareTo(answer.getMaxPoints()) < 0);
        answerRepository.save(answer);

        // Recalculate attempt total
        recalculateAttemptScore(answer.getAttempt().getId());
    }

    @Transactional
    public void recalculateAttemptScore(UUID attemptId) {
        TestAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("TestAttempt", "id", attemptId));

        List<Answer> answers = answerRepository.findByAttemptIdOrderByQuestionIndexAsc(attemptId);

        BigDecimal totalScore = BigDecimal.ZERO;
        BigDecimal maxScore = BigDecimal.ZERO;
        boolean stillNeedsReview = false;

        for (Answer answer : answers) {
            if (answer.getMaxPoints() != null) {
                maxScore = maxScore.add(answer.getMaxPoints());
            }
            if (answer.getEarnedPoints() != null) {
                totalScore = totalScore.add(answer.getEarnedPoints());
            }
            if (Boolean.TRUE.equals(answer.getNeedsManualGrading())) {
                stillNeedsReview = true;
            }
        }

        attempt.setRawScore(totalScore);
        attempt.setMaxScore(maxScore);
        if (maxScore.compareTo(BigDecimal.ZERO) > 0) {
            attempt.setPercentage(totalScore.multiply(BigDecimal.valueOf(100))
                    .divide(maxScore, 2, RoundingMode.HALF_UP));
        }

        attempt.setStatus(stillNeedsReview ? AttemptStatus.NEEDS_REVIEW : AttemptStatus.GRADED);
        attemptRepository.save(attempt);
    }

    // ==================== Grading Methods ====================

    @SuppressWarnings("unchecked")
    private void gradeMcqSingle(Answer answer, Question question) {
        Object selected = parseJson(answer.getSelectedAnswer());
        if (selected == null) {
            answer.setIsCorrect(false);
            answer.setEarnedPoints(BigDecimal.ZERO);
            return;
        }
        // Compare by option id (isCorrect flag), NOT by letter-label correctAnswer
        String correctOptionId = findCorrectOptionId(question.getOptions());
        boolean isCorrect = correctOptionId != null && correctOptionId.equals(String.valueOf(selected));
        answer.setIsCorrect(isCorrect);
        answer.setEarnedPoints(isCorrect ? question.getPoints() : BigDecimal.ZERO);
    }

    @SuppressWarnings("unchecked")
    private void gradeMcqMulti(Answer answer, Question question) {
        Set<Object> selectedRaw = toSet(parseJson(answer.getSelectedAnswer()));
        Set<String> correctIds = findCorrectOptionIds(question.getOptions());
        Set<String> selectedIds = selectedRaw.stream().map(String::valueOf).collect(java.util.stream.Collectors.toSet());

        if (selectedIds.equals(correctIds)) {
            answer.setIsCorrect(true);
            answer.setEarnedPoints(question.getPoints());
        } else {
            // Partial credit
            Set<String> intersection = new HashSet<>(selectedIds);
            intersection.retainAll(correctIds);

            Set<String> wrongSelections = new HashSet<>(selectedIds);
            wrongSelections.removeAll(correctIds);

            int correctCount = intersection.size();
            int wrongCount = wrongSelections.size();
            int totalCorrect = Math.max(1, correctIds.size());

            double partialRatio = Math.max(0, (double) (correctCount - wrongCount) / totalCorrect);
            BigDecimal partialScore = question.getPoints()
                    .multiply(BigDecimal.valueOf(partialRatio))
                    .setScale(2, RoundingMode.HALF_UP);

            answer.setIsPartial(partialScore.compareTo(BigDecimal.ZERO) > 0);
            answer.setIsCorrect(false);
            answer.setEarnedPoints(partialScore);
        }
    }

    private void gradeTrueFalse(Answer answer, Question question) {
        Object selected = parseJson(answer.getSelectedAnswer());
        Object correct = parseJson(question.getCorrectAnswer());

        // Normalize to lowercase string to handle Boolean vs String mismatch
        String selectedStr = selected != null ? String.valueOf(selected).toLowerCase() : null;
        String correctStr = correct != null ? String.valueOf(correct).toLowerCase() : null;

        if (selectedStr != null && selectedStr.equals(correctStr)) {
            answer.setIsCorrect(true);
            answer.setEarnedPoints(question.getPoints());
        } else {
            answer.setIsCorrect(false);
            answer.setEarnedPoints(BigDecimal.ZERO);
        }
    }

    @SuppressWarnings("unchecked")
    private String findCorrectOptionId(String optionsJson) {
        Object parsed = parseJson(optionsJson);
        if (!(parsed instanceof List<?> optionsList)) return null;
        for (Object opt : optionsList) {
            if (opt instanceof Map<?, ?> optMap && Boolean.TRUE.equals(optMap.get("isCorrect"))) {
                Object id = optMap.get("id");
                return id != null ? String.valueOf(id) : null;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Set<String> findCorrectOptionIds(String optionsJson) {
        Object parsed = parseJson(optionsJson);
        if (!(parsed instanceof List<?> optionsList)) return Set.of();
        Set<String> correctIds = new HashSet<>();
        for (Object opt : optionsList) {
            if (opt instanceof Map<?, ?> optMap && Boolean.TRUE.equals(optMap.get("isCorrect"))) {
                Object id = optMap.get("id");
                if (id != null) correctIds.add(String.valueOf(id));
            }
        }
        return correctIds;
    }

    private void gradeMatching(Answer answer, Question question) {
        Map<String, Object> selectedPairs = toMap(parseJson(answer.getSelectedAnswer()));
        Map<String, Object> correctPairs = toMap(parseJson(question.getCorrectAnswer()));

        if (correctPairs.isEmpty()) {
            answer.setIsCorrect(false);
            answer.setEarnedPoints(BigDecimal.ZERO);
            return;
        }

        int correctCount = 0;
        for (Map.Entry<String, Object> entry : selectedPairs.entrySet()) {
            if (Objects.equals(entry.getValue(), correctPairs.get(entry.getKey()))) {
                correctCount++;
            }
        }

        double ratio = (double) correctCount / correctPairs.size();
        BigDecimal earned = question.getPoints()
                .multiply(BigDecimal.valueOf(ratio))
                .setScale(2, RoundingMode.HALF_UP);

        answer.setIsCorrect(correctCount == correctPairs.size());
        answer.setIsPartial(correctCount > 0 && correctCount < correctPairs.size());
        answer.setEarnedPoints(earned);
    }

    private void gradeOrdering(Answer answer, Question question) {
        List<?> selectedOrder = toList(parseJson(answer.getSelectedAnswer()));
        List<?> correctOrder = toList(parseJson(question.getCorrectAnswer()));

        if (selectedOrder.equals(correctOrder)) {
            answer.setIsCorrect(true);
            answer.setEarnedPoints(question.getPoints());
        } else {
            answer.setIsCorrect(false);
            answer.setEarnedPoints(BigDecimal.ZERO);
        }
    }

    // ==================== Helpers ====================

    private Object parseJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException e) {
            return json;
        }
    }

    @SuppressWarnings("unchecked")
    private Set<Object> toSet(Object obj) {
        if (obj instanceof Collection<?> collection) {
            return new HashSet<>(collection);
        }
        return Set.of(obj);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object obj) {
        if (obj instanceof Map<?, ?> map) {
            Map<String, Object> result = new HashMap<>();
            map.forEach((k, v) -> result.put(String.valueOf(k), v));
            return result;
        }
        return Map.of();
    }

    private List<?> toList(Object obj) {
        if (obj instanceof List<?> list) {
            return list;
        }
        return List.of(obj);
    }
}
