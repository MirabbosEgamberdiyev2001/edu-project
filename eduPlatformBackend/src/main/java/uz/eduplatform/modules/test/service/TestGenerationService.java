package uz.eduplatform.modules.test.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.eduplatform.core.audit.AuditService;
import uz.eduplatform.core.common.exception.BusinessException;
import uz.eduplatform.core.i18n.TranslatedField;
import uz.eduplatform.modules.content.domain.Difficulty;
import uz.eduplatform.modules.content.domain.Question;
import uz.eduplatform.modules.content.domain.QuestionStatus;
import uz.eduplatform.modules.content.domain.QuestionType;
import uz.eduplatform.modules.content.domain.Subject;
import uz.eduplatform.modules.content.domain.Topic;
import uz.eduplatform.modules.content.repository.QuestionRepository;
import uz.eduplatform.modules.content.repository.SubjectRepository;
import uz.eduplatform.modules.content.repository.TopicRepository;
import uz.eduplatform.modules.test.domain.TestHistory;
import uz.eduplatform.modules.test.domain.TestQuestion;
import uz.eduplatform.modules.test.domain.TestStatus;
import uz.eduplatform.modules.test.dto.*;
import uz.eduplatform.modules.test.repository.TestHistoryRepository;
import uz.eduplatform.modules.test.repository.TestQuestionRepository;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestGenerationService {

    private final QuestionRepository questionRepository;
    private final TestHistoryRepository testHistoryRepository;
    private final TestQuestionRepository testQuestionRepository;
    private final TopicRepository topicRepository;
    private final SubjectRepository subjectRepository;
    private final TestValidationService validationService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    private static final String[] VARIANT_CODES = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J"};

    @Transactional
    public GenerateTestResponse generateTest(UUID userId, GenerateTestRequest request) {
        return doGenerateTest(userId, request, true);
    }

    @Transactional(readOnly = true)
    public GenerateTestResponse previewTest(UUID userId, GenerateTestRequest request) {
        return doGenerateTest(userId, request, false);
    }

    private GenerateTestResponse doGenerateTest(UUID userId, GenerateTestRequest request, boolean persist) {
        // 1. Validate input
        validationService.validateRequest(request);

        // 1.1 Validate topic ownership
        // - Subject owner: full access to all topics
        // - Template subject: teacher can use only their own topics
        if (request.getTopicIds() != null && !request.getTopicIds().isEmpty()) {
            for (UUID topicId : request.getTopicIds()) {
                Topic topic = topicRepository.findById(topicId)
                        .orElseThrow(() -> BusinessException.ofKey("test.topic.not.found"));
                Subject subject = topic.getSubject();
                boolean isSubjectOwner = subject.getUser().getId().equals(userId);
                boolean isTemplateWithOwnTopic = Boolean.TRUE.equals(subject.getIsTemplate())
                        && topic.getUser().getId().equals(userId);
                if (!isSubjectOwner && !isTemplateWithOwnTopic) {
                    throw BusinessException.ofKey("test.topic.not.owned");
                }
            }
        }

        boolean isManualMode = request.getQuestionIds() != null && !request.getQuestionIds().isEmpty();
        List<Question> selected;
        int questionCount;
        Map<String, Integer> actualDistribution;

        long seed = request.getRandomSeed() != null ? request.getRandomSeed() : System.currentTimeMillis();
        Random random = new Random(seed);

        if (isManualMode) {
            // Manual mode: fetch selected questions directly
            selected = questionRepository.findAllById(request.getQuestionIds());

            if (selected.isEmpty()) {
                throw BusinessException.ofKey("test.manual.questions.not.found");
            }

            // Validate ownership: user must own the question or its subject
            for (Question q : selected) {
                boolean isQuestionOwner = q.getUser().getId().equals(userId);
                boolean isSubjectOwner = q.getTopic() != null
                        && q.getTopic().getSubject() != null
                        && q.getTopic().getSubject().getUser().getId().equals(userId);
                boolean isTemplateQuestion = q.getTopic() != null
                        && q.getTopic().getSubject() != null
                        && Boolean.TRUE.equals(q.getTopic().getSubject().getIsTemplate());
                boolean isActiveOrApproved = q.getStatus() == QuestionStatus.ACTIVE
                        || q.getStatus() == QuestionStatus.APPROVED;

                if (!isQuestionOwner && !isSubjectOwner && !(isTemplateQuestion && isActiveOrApproved)) {
                    throw BusinessException.ofKey("test.manual.question.not.accessible");
                }
            }

            // Filter only valid MCQ questions with options (A, B, C, D)
            List<Question> invalidQuestions = selected.stream()
                    .filter(q -> !isValidForTest(q))
                    .toList();

            if (!invalidQuestions.isEmpty()) {
                log.warn("Manual mode: {} out of {} questions are not valid MCQ with options, filtering out",
                        invalidQuestions.size(), selected.size());
                selected = new ArrayList<>(selected.stream().filter(this::isValidForTest).toList());
                if (selected.isEmpty()) {
                    throw BusinessException.ofKey("test.manual.no.valid.mcq.questions");
                }
            }

            questionCount = selected.size();

            // Calculate actual difficulty distribution from selected questions
            Map<Difficulty, Long> counts = selected.stream()
                    .collect(Collectors.groupingBy(Question::getDifficulty, Collectors.counting()));
            actualDistribution = Map.of(
                    "easy", counts.getOrDefault(Difficulty.EASY, 0L).intValue(),
                    "medium", counts.getOrDefault(Difficulty.MEDIUM, 0L).intValue(),
                    "hard", counts.getOrDefault(Difficulty.HARD, 0L).intValue()
            );

            // Override questionCount in request for history saving
            request.setQuestionCount(questionCount);
        } else {
            // Auto mode: fetch questions from topics or subject-wide
            boolean hasTopics = request.getTopicIds() != null && !request.getTopicIds().isEmpty();
            List<Question> candidates;
            if (hasTopics) {
                candidates = questionRepository.findByTopicIdsForTeacher(request.getTopicIds(), userId);
            } else if (request.getSubjectId() != null) {
                candidates = questionRepository.findBySubjectIdForTeacher(request.getSubjectId(), userId);
            } else {
                throw BusinessException.ofKey("test.no.active.questions");
            }

            if (candidates.isEmpty()) {
                throw BusinessException.ofKey("test.no.active.questions");
            }

            // 2.1 Filter only valid MCQ questions with options (A, B, C, D)
            candidates = candidates.stream().filter(this::isValidForTest).toList();
            if (candidates.isEmpty()) {
                throw BusinessException.ofKey("test.no.valid.mcq.questions");
            }

            log.debug("Auto mode: {} valid MCQ questions found for test generation", candidates.size());

            // 3. Group by difficulty
            Map<Difficulty, List<Question>> byDifficulty = candidates.stream()
                    .collect(Collectors.groupingBy(Question::getDifficulty));

            // 4. Calculate counts based on distribution
            DifficultyDistribution dist = request.getDifficultyDistribution() != null
                    ? request.getDifficultyDistribution()
                    : DifficultyDistribution.builder().build();

            // Floor-based rounding: remainder goes to the largest category (no bias)
            int total = request.getQuestionCount();
            int easyCount = (int) Math.floor(total * dist.getEasy() / 100.0);
            int mediumCount = (int) Math.floor(total * dist.getMedium() / 100.0);
            int hardCount = (int) Math.floor(total * dist.getHard() / 100.0);

            // Distribute remainder to categories by largest fractional part
            int remainder = total - easyCount - mediumCount - hardCount;
            if (remainder > 0) {
                double easyFrac = (total * dist.getEasy() / 100.0) - easyCount;
                double medFrac = (total * dist.getMedium() / 100.0) - mediumCount;
                double hardFrac = (total * dist.getHard() / 100.0) - hardCount;

                for (int r = 0; r < remainder; r++) {
                    if (hardFrac >= easyFrac && hardFrac >= medFrac) { hardCount++; hardFrac = -1; }
                    else if (medFrac >= easyFrac) { mediumCount++; medFrac = -1; }
                    else { easyCount++; easyFrac = -1; }
                }
            }

            // 5. Check availability
            validationService.checkAvailability(request, byDifficulty);

            // 6. Random select with seed
            selected = new ArrayList<>();
            selected.addAll(randomSelect(byDifficulty.getOrDefault(Difficulty.EASY, List.of()), easyCount, random));
            selected.addAll(randomSelect(byDifficulty.getOrDefault(Difficulty.MEDIUM, List.of()), mediumCount, random));
            selected.addAll(randomSelect(byDifficulty.getOrDefault(Difficulty.HARD, List.of()), hardCount, random));

            questionCount = request.getQuestionCount();
            actualDistribution = Map.of(
                    "easy", easyCount,
                    "medium", mediumCount,
                    "hard", hardCount
            );
        }

        // Generate variants
        int variantCount = request.getVariantCount() != null ? request.getVariantCount() : 1;
        List<VariantDto> variantDtos = new ArrayList<>();
        List<Map<String, Object>> variantMaps = new ArrayList<>();

        for (int i = 0; i < variantCount; i++) {
            VariantDto variant = createVariant(
                    VARIANT_CODES[i],
                    selected,
                    Boolean.TRUE.equals(request.getShuffleQuestions()),
                    Boolean.TRUE.equals(request.getShuffleOptions()),
                    random
            );
            variantDtos.add(variant);

            // Convert to map for JSON storage
            Map<String, Object> variantMap = new LinkedHashMap<>();
            variantMap.put("code", variant.getCode());
            variantMap.put("questionIds", variant.getQuestionIds().stream().map(UUID::toString).toList());
            variantMap.put("answerKey", variant.getAnswerKey());
            variantMap.put("optionsOrder", variant.getOptionsOrder());
            variantMaps.add(variantMap);
        }

        // Build header config map
        Map<String, Object> headerConfigMap = null;
        if (request.getHeaderConfig() != null) {
            headerConfigMap = new LinkedHashMap<>();
            HeaderConfig hc = request.getHeaderConfig();
            if (hc.getSchoolName() != null) headerConfigMap.put("schoolName", hc.getSchoolName());
            if (hc.getSchoolNameTranslations() != null) headerConfigMap.put("schoolNameTranslations", hc.getSchoolNameTranslations());
            if (hc.getClassName() != null) headerConfigMap.put("className", hc.getClassName());
            if (hc.getTeacherName() != null) headerConfigMap.put("teacherName", hc.getTeacherName());
            if (hc.getTeacherNameTranslations() != null) headerConfigMap.put("teacherNameTranslations", hc.getTeacherNameTranslations());
            if (hc.getLogoUrl() != null) headerConfigMap.put("logoUrl", hc.getLogoUrl());
            if (hc.getDate() != null) headerConfigMap.put("date", hc.getDate());
        }

        // Resolve title from translations if available
        Map<String, String> titleTranslations = request.getTitleTranslations();
        String resolvedTitle = request.getTitle();
        if (titleTranslations != null && !titleTranslations.isEmpty()) {
            String fromTranslations = TranslatedField.resolve(titleTranslations);
            if (fromTranslations != null) resolvedTitle = fromTranslations;
        }
        if (resolvedTitle == null || resolvedTitle.isBlank()) {
            resolvedTitle = "Test";
        }

        if (persist) {
            // Save to history
            TestHistory history = TestHistory.builder()
                    .userId(userId)
                    .title(resolvedTitle)
                    .titleTranslations(titleTranslations)
                    .category(request.getCategory())
                    .subjectId(request.getSubjectId())
                    .topicIds(request.getTopicIds())
                    .questionCount(questionCount)
                    .variantCount(variantCount)
                    .difficultyDistribution(actualDistribution)
                    .shuffleQuestions(request.getShuffleQuestions())
                    .shuffleOptions(request.getShuffleOptions())
                    .randomSeed(seed)
                    .headerConfig(headerConfigMap)
                    .variants(variantMaps)
                    .status(TestStatus.READY)
                    .build();

            history = testHistoryRepository.save(history);

            // Save test questions (batch)
            Map<UUID, Question> selectedMap = selected.stream()
                    .collect(Collectors.toMap(Question::getId, q -> q, (a, b) -> a));
            List<TestQuestion> allTestQuestions = new ArrayList<>();
            for (VariantDto variant : variantDtos) {
                for (int qi = 0; qi < variant.getQuestionIds().size(); qi++) {
                    UUID qId = variant.getQuestionIds().get(qi);
                    Question q = selectedMap.get(qId);

                    allTestQuestions.add(TestQuestion.builder()
                            .testId(history.getId())
                            .questionId(qId)
                            .questionVersion(q != null ? q.getVersion() : 1)
                            .variantCode(variant.getCode())
                            .questionOrder(qi + 1)
                            .optionsOrder(variant.getOptionsOrder() != null && qi < variant.getOptionsOrder().size()
                                    ? variant.getOptionsOrder().get(qi) : null)
                            .build());
                }
            }
            testQuestionRepository.saveAll(allTestQuestions);

            // Update question stats (times_used)
            List<UUID> selectedIds = selected.stream().map(Question::getId).toList();
            questionRepository.incrementTimesUsed(selectedIds);

            auditService.log(userId, null, "TEST_GENERATED", "TEST",
                    "TestHistory", history.getId());

            return GenerateTestResponse.builder()
                    .testId(history.getId())
                    .title(history.getTitle())
                    .titleTranslations(history.getTitleTranslations())
                    .questionCount(history.getQuestionCount())
                    .variantCount(history.getVariantCount())
                    .difficultyDistribution(actualDistribution)
                    .randomSeed(seed)
                    .variants(variantDtos)
                    .createdAt(history.getCreatedAt())
                    .build();
        } else {
            // Preview mode: return response without persisting
            return GenerateTestResponse.builder()
                    .testId(null)
                    .title(resolvedTitle)
                    .titleTranslations(titleTranslations)
                    .questionCount(questionCount)
                    .variantCount(variantCount)
                    .difficultyDistribution(actualDistribution)
                    .randomSeed(seed)
                    .variants(variantDtos)
                    .createdAt(null)
                    .build();
        }
    }

    @SuppressWarnings("unchecked")
    private VariantDto createVariant(String code, List<Question> questions,
                                     boolean shuffleQ, boolean shuffleOpt, Random random) {
        List<Question> variantQuestions = new ArrayList<>(questions);

        // Fisher-Yates shuffle for questions
        if (shuffleQ) {
            for (int i = variantQuestions.size() - 1; i > 0; i--) {
                int j = random.nextInt(i + 1);
                Question temp = variantQuestions.get(i);
                variantQuestions.set(i, variantQuestions.get(j));
                variantQuestions.set(j, temp);
            }
        }

        List<Map<String, Object>> answerKey = new ArrayList<>();
        List<List<String>> allOptionsOrder = new ArrayList<>();

        for (int i = 0; i < variantQuestions.size(); i++) {
            Question q = variantQuestions.get(i);

            Object parsedOptions = parseJson(q.getOptions());

            // Always process MCQ questions with valid options (regardless of shuffleOpt flag)
            if ((q.getQuestionType() == QuestionType.MCQ_SINGLE
                    || q.getQuestionType() == QuestionType.MCQ_MULTI)
                    && parsedOptions instanceof List<?> optionsList
                    && !optionsList.isEmpty()) {

                List<Map<String, Object>> options = new ArrayList<>();
                for (Object opt : optionsList) {
                    if (opt instanceof Map) {
                        options.add(new LinkedHashMap<>((Map<String, Object>) opt));
                    }
                }

                // Fisher-Yates shuffle for options (only if shuffleOpt is enabled)
                if (shuffleOpt) {
                    for (int k = options.size() - 1; k > 0; k--) {
                        int j = random.nextInt(k + 1);
                        Map<String, Object> temp = options.get(k);
                        options.set(k, options.get(j));
                        options.set(j, temp);
                    }
                }

                // Find correct answer positions (after shuffle if applicable)
                List<String> correctIds = new ArrayList<>();
                List<String> optionOrder = new ArrayList<>();
                String[] labels = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J"};

                for (int k = 0; k < options.size(); k++) {
                    Map<String, Object> opt = options.get(k);
                    optionOrder.add(String.valueOf(opt.get("id")));
                    if (Boolean.TRUE.equals(opt.get("isCorrect"))) {
                        correctIds.add(labels[k]);
                    }
                }

                if (correctIds.isEmpty()) {
                    log.warn("Question {} has no correct answer marked in options", q.getId());
                }

                Map<String, Object> keyEntry = new LinkedHashMap<>();
                keyEntry.put("questionNumber", i + 1);
                keyEntry.put("answer", String.join(",", correctIds));
                answerKey.add(keyEntry);
                allOptionsOrder.add(optionOrder);

            } else if (q.getQuestionType() == QuestionType.TRUE_FALSE) {
                // TRUE_FALSE: answer key from correctAnswer field
                String correctAnswer = findCorrectAnswer(q);
                Map<String, Object> keyEntry = new LinkedHashMap<>();
                keyEntry.put("questionNumber", i + 1);
                keyEntry.put("answer", correctAnswer);
                answerKey.add(keyEntry);
                allOptionsOrder.add(null);
            } else {
                // Fallback for any other type - should not reach here after isValidForTest filter
                log.warn("Question {} (type={}) has no valid MCQ options, included with empty answer",
                        q.getId(), q.getQuestionType());
                String correctAnswer = findCorrectAnswer(q);
                Map<String, Object> keyEntry = new LinkedHashMap<>();
                keyEntry.put("questionNumber", i + 1);
                keyEntry.put("answer", correctAnswer);
                answerKey.add(keyEntry);
                allOptionsOrder.add(null);
            }
        }

        return VariantDto.builder()
                .code(code)
                .questionIds(variantQuestions.stream().map(Question::getId).toList())
                .answerKey(answerKey)
                .optionsOrder(allOptionsOrder)
                .build();
    }

    @SuppressWarnings("unchecked")
    private String findCorrectAnswer(Question q) {
        Object parsedOpts = parseJson(q.getOptions());
        if (parsedOpts instanceof List<?> optionsList) {
            String[] labels = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J"};
            List<String> correctLabels = new ArrayList<>();

            for (int i = 0; i < optionsList.size(); i++) {
                Object opt = optionsList.get(i);
                if (opt instanceof Map<?, ?> optMap && Boolean.TRUE.equals(optMap.get("isCorrect"))) {
                    if (i < labels.length) {
                        correctLabels.add(labels[i]);
                    }
                }
            }
            return String.join(",", correctLabels);
        }

        if (q.getCorrectAnswer() != null) {
            Object parsed = parseJson(q.getCorrectAnswer());
            return parsed != null ? parsed.toString() : q.getCorrectAnswer();
        }

        return "";
    }

    private List<Question> randomSelect(List<Question> source, int count, Random random) {
        if (count <= 0) return List.of();
        if (source.size() <= count) return new ArrayList<>(source);

        List<Question> shuffled = new ArrayList<>(source);
        // Fisher-Yates partial shuffle
        for (int i = shuffled.size() - 1; i > 0 && (shuffled.size() - i) <= count; i--) {
            int j = random.nextInt(i + 1);
            Question temp = shuffled.get(i);
            shuffled.set(i, shuffled.get(j));
            shuffled.set(j, temp);
        }

        return new ArrayList<>(shuffled.subList(shuffled.size() - count, shuffled.size()));
    }

    /**
     * Validates that a question is suitable for test generation:
     * - Must be MCQ_SINGLE or MCQ_MULTI type
     * - Must have a non-empty list of options (minimum 2)
     * - Each option must have an "id" and "text" field
     * - At least one option must be marked as correct (isCorrect=true)
     */
    @SuppressWarnings("unchecked")
    private boolean isValidForTest(Question q) {
        // Only MCQ types are valid for printed tests
        if (q.getQuestionType() != QuestionType.MCQ_SINGLE
                && q.getQuestionType() != QuestionType.MCQ_MULTI) {
            return false;
        }

        // Parse options JSON
        Object parsed = parseJson(q.getOptions());
        if (!(parsed instanceof List<?> optionsList) || optionsList.size() < 2) {
            return false;
        }

        // Verify options have required structure and at least one correct answer
        boolean hasCorrect = false;
        for (Object opt : optionsList) {
            if (!(opt instanceof Map<?, ?> optMap)) {
                return false;
            }
            // Each option must have text
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
            log.warn("Failed to parse JSON: {}", e.getMessage());
            return null;
        }
    }
}
