package uz.eduplatform.modules.assessment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.eduplatform.core.common.dto.PagedResponse;
import uz.eduplatform.core.common.exception.BusinessException;
import uz.eduplatform.core.common.exception.ResourceNotFoundException;
import uz.eduplatform.modules.assessment.domain.*;
import uz.eduplatform.modules.assessment.dto.*;
import uz.eduplatform.modules.assessment.repository.AnswerRepository;
import uz.eduplatform.modules.assessment.repository.TestAssignmentRepository;
import uz.eduplatform.modules.assessment.repository.TestAttemptRepository;
import uz.eduplatform.modules.auth.repository.UserRepository;
import uz.eduplatform.modules.content.domain.Question;
import uz.eduplatform.modules.content.repository.QuestionRepository;
import uz.eduplatform.modules.parent.service.ParentNotificationService;
import uz.eduplatform.modules.test.domain.TestQuestion;
import uz.eduplatform.modules.test.repository.TestHistoryRepository;
import uz.eduplatform.modules.test.repository.TestQuestionRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestTakingService {

    private final TestAssignmentRepository assignmentRepository;
    private final TestAttemptRepository attemptRepository;
    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final GradingService gradingService;
    private final LiveMonitoringService liveMonitoringService;
    private final ParentNotificationService parentNotificationService;
    private final TestHistoryRepository testHistoryRepository;
    private final TestQuestionRepository testQuestionRepository;
    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public AttemptDto startAttempt(UUID assignmentId, UUID studentId, StartAttemptRequest request, String ipAddress) {
        TestAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("TestAssignment", "id", assignmentId));

        // Validate assignment is active
        if (assignment.getStatus() != AssignmentStatus.ACTIVE) {
            throw BusinessException.ofKey("test.taking.assignment.not.active");
        }

        // Validate student is assigned
        if (assignment.getAssignedStudentIds() != null
                && !assignment.getAssignedStudentIds().contains(studentId)) {
            throw new BusinessException("test.taking.not.assigned", null, HttpStatus.FORBIDDEN);
        }

        // Validate time window
        LocalDateTime now = LocalDateTime.now();
        if (assignment.getStartTime() != null && now.isBefore(assignment.getStartTime())) {
            throw BusinessException.ofKey("test.taking.not.started.yet");
        }
        if (assignment.getEndTime() != null && now.isAfter(assignment.getEndTime())) {
            throw BusinessException.ofKey("test.taking.already.ended");
        }

        // Validate access code
        if (assignment.getAccessCode() != null && !assignment.getAccessCode().isBlank()) {
            if (request == null || request.getAccessCode() == null
                    || !assignment.getAccessCode().equals(request.getAccessCode())) {
                throw BusinessException.ofKey("test.taking.invalid.access.code");
            }
        }

        // Check for existing in-progress attempt
        var existingInProgress = attemptRepository.findByAssignmentIdAndStudentIdAndStatus(
                assignmentId, studentId, AttemptStatus.IN_PROGRESS);
        if (existingInProgress.isPresent()) {
            // Resume existing attempt
            return mapToDto(existingInProgress.get(), assignment);
        }

        // Check max attempts
        long attemptCount = attemptRepository.countByAssignmentIdAndStudentId(assignmentId, studentId);
        if (attemptCount >= assignment.getMaxAttempts()) {
            throw BusinessException.ofKey("test.taking.max.attempts.reached");
        }

        // Determine variant count from the linked test history
        int availableVariants = testHistoryRepository.findById(assignment.getTestHistoryId())
                .map(th -> th.getVariantCount() != null ? th.getVariantCount() : 1)
                .orElse(1);

        // Determine variant index: deterministic seed from student+assignment for reproducibility
        int variantIndex;
        if (assignment.getShufflePerStudent()) {
            long seed = studentId.getMostSignificantBits() ^ assignmentId.getLeastSignificantBits()
                    ^ (attemptCount * 31);
            variantIndex = Math.abs(Long.hashCode(seed)) % availableVariants;
        } else {
            variantIndex = (int) (attemptCount % availableVariants);
        }

        TestAttempt attempt = TestAttempt.builder()
                .assignment(assignment)
                .studentId(studentId)
                .attemptNumber((int) attemptCount + 1)
                .variantIndex(variantIndex)
                .startedAt(now)
                .status(AttemptStatus.IN_PROGRESS)
                .ipAddress(ipAddress)
                .build();

        attempt = attemptRepository.save(attempt);
        log.info("Student {} started attempt #{} for assignment {}", studentId, attempt.getAttemptNumber(), assignmentId);

        // Broadcast WebSocket event
        try {
            String studentName = userRepository.findById(studentId)
                    .map(u -> u.getFirstName() + " " + u.getLastName())
                    .orElse("Unknown");
            liveMonitoringService.broadcastEvent(LiveTestEvent.builder()
                    .eventType(LiveTestEvent.EventType.STUDENT_STARTED)
                    .assignmentId(assignmentId)
                    .studentId(studentId)
                    .studentName(studentName)
                    .answeredQuestions(0)
                    .totalQuestions(0)
                    .tabSwitchCount(0)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to broadcast start event: {}", e.getMessage());
        }

        return mapToDto(attempt, assignment);
    }

    @Transactional
    public BatchSaveAnswerResponse saveAnswersBatch(UUID attemptId, UUID studentId, BatchSaveAnswerRequest request) {
        TestAttempt attempt = attemptRepository.findByIdAndStudentId(attemptId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("TestAttempt", "id", attemptId));

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw BusinessException.ofKey("test.taking.attempt.not.in.progress");
        }

        checkTimeExpired(attempt);

        // Load all existing answers in one query
        List<Answer> existingAnswers = answerRepository.findByAttemptIdOrderByQuestionIndexAsc(attemptId);
        Map<UUID, Answer> answerMap = existingAnswers.stream()
                .collect(Collectors.toMap(Answer::getQuestionId, Function.identity()));

        List<AnswerDto> savedDtos = new ArrayList<>();
        int saved = 0;
        int failed = 0;

        for (SubmitAnswerRequest answerReq : request.getAnswers()) {
            try {
                Answer answer = answerMap.getOrDefault(answerReq.getQuestionId(),
                        Answer.builder()
                                .attempt(attempt)
                                .questionId(answerReq.getQuestionId())
                                .questionIndex(answerReq.getQuestionIndex())
                                .build());

                answer.setSelectedAnswer(toJson(answerReq.getSelectedAnswer()));
                if (answerReq.getTimeSpentSeconds() != null) {
                    answer.setTimeSpentSeconds(answerReq.getTimeSpentSeconds());
                }
                if (answerReq.getBookmarked() != null) {
                    answer.setBookmarked(answerReq.getBookmarked());
                }

                answerMap.put(answerReq.getQuestionId(), answer);
                saved++;
            } catch (Exception e) {
                failed++;
                log.warn("Failed to process answer for question {}: {}", answerReq.getQuestionId(), e.getMessage());
            }
        }

        // Save all at once
        List<Answer> toSave = new ArrayList<>(answerMap.values());
        answerRepository.saveAll(toSave);

        for (SubmitAnswerRequest answerReq : request.getAnswers()) {
            Answer savedAnswer = answerMap.get(answerReq.getQuestionId());
            if (savedAnswer != null) {
                savedDtos.add(AnswerDto.builder()
                        .id(savedAnswer.getId())
                        .questionId(savedAnswer.getQuestionId())
                        .questionIndex(savedAnswer.getQuestionIndex())
                        .selectedAnswer(parseJson(savedAnswer.getSelectedAnswer()))
                        .bookmarked(savedAnswer.getBookmarked())
                        .timeSpentSeconds(savedAnswer.getTimeSpentSeconds())
                        .build());
            }
        }

        return BatchSaveAnswerResponse.builder()
                .saved(saved)
                .failed(failed)
                .answers(savedDtos)
                .build();
    }

    @Transactional
    public AnswerDto saveAnswer(UUID attemptId, UUID studentId, SubmitAnswerRequest request) {
        TestAttempt attempt = attemptRepository.findByIdAndStudentId(attemptId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("TestAttempt", "id", attemptId));

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw BusinessException.ofKey("test.taking.attempt.not.in.progress");
        }

        // Check if time has expired
        checkTimeExpired(attempt);

        // Upsert answer
        Answer answer = answerRepository.findByAttemptIdAndQuestionId(attemptId, request.getQuestionId())
                .orElse(Answer.builder()
                        .attempt(attempt)
                        .questionId(request.getQuestionId())
                        .questionIndex(request.getQuestionIndex())
                        .build());

        answer.setSelectedAnswer(toJson(request.getSelectedAnswer()));
        if (request.getTimeSpentSeconds() != null) {
            answer.setTimeSpentSeconds(request.getTimeSpentSeconds());
        }
        if (request.getBookmarked() != null) {
            answer.setBookmarked(request.getBookmarked());
        }

        answer = answerRepository.save(answer);

        // Broadcast WebSocket event
        try {
            long answeredCount = answerRepository.countByAttemptIdAndSelectedAnswerIsNotNull(attemptId);
            long totalCount = answerRepository.countByAttemptId(attemptId);
            liveMonitoringService.broadcastEvent(LiveTestEvent.builder()
                    .eventType(LiveTestEvent.EventType.ANSWER_SAVED)
                    .assignmentId(attempt.getAssignment().getId())
                    .studentId(studentId)
                    .answeredQuestions((int) answeredCount)
                    .totalQuestions((int) totalCount)
                    .tabSwitchCount(attempt.getTabSwitchCount())
                    .build());
        } catch (Exception e) {
            log.warn("Failed to broadcast answer event: {}", e.getMessage());
        }

        return AnswerDto.builder()
                .id(answer.getId())
                .questionId(answer.getQuestionId())
                .questionIndex(answer.getQuestionIndex())
                .selectedAnswer(parseJson(answer.getSelectedAnswer()))
                .bookmarked(answer.getBookmarked())
                .timeSpentSeconds(answer.getTimeSpentSeconds())
                .build();
    }

    @Transactional
    public AttemptDto submitAttempt(UUID attemptId, UUID studentId) {
        TestAttempt attempt = attemptRepository.findByIdAndStudentId(attemptId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("TestAttempt", "id", attemptId));

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw BusinessException.ofKey("test.taking.attempt.not.in.progress");
        }

        // Guard: auto-submit and reject if time has already expired
        checkTimeExpired(attempt);

        attempt.setSubmittedAt(LocalDateTime.now());
        attempt.setStatus(AttemptStatus.SUBMITTED);
        attempt = attemptRepository.save(attempt);

        // Auto-grade
        attempt = gradingService.gradeAttempt(attempt);
        log.info("Student {} submitted attempt {} - score: {}%", studentId, attemptId, attempt.getPercentage());

        // Notify parents of test result
        try {
            String title = attempt.getAssignment() != null ? attempt.getAssignment().getTitle() : "Test";
            parentNotificationService.notifyParentsOfTestResult(studentId, title, attempt.getPercentage());
            parentNotificationService.notifyParentsOfLowScore(studentId, title,
                    attempt.getPercentage(), new java.math.BigDecimal("40.00"));
        } catch (Exception e) {
            log.warn("Failed to notify parents: {}", e.getMessage());
        }

        // Broadcast WebSocket event
        try {
            liveMonitoringService.broadcastEvent(LiveTestEvent.builder()
                    .eventType(LiveTestEvent.EventType.SUBMITTED)
                    .assignmentId(attempt.getAssignment().getId())
                    .studentId(studentId)
                    .percentage(attempt.getPercentage())
                    .tabSwitchCount(attempt.getTabSwitchCount())
                    .build());
        } catch (Exception e) {
            log.warn("Failed to broadcast submit event: {}", e.getMessage());
        }

        return mapToDto(attempt, attempt.getAssignment());
    }

    @Transactional
    public AttemptDto reportTabSwitch(UUID attemptId, UUID studentId) {
        TestAttempt attempt = attemptRepository.findByIdAndStudentId(attemptId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("TestAttempt", "id", attemptId));

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            return mapToDto(attempt, attempt.getAssignment());
        }

        attempt.setTabSwitchCount(attempt.getTabSwitchCount() + 1);
        log.warn("Tab switch detected for student {} on attempt {} (count: {})",
                studentId, attemptId, attempt.getTabSwitchCount());

        TestAssignment assignment = attempt.getAssignment();
        int threshold = assignment.getTabSwitchThreshold() != null ? assignment.getTabSwitchThreshold() : 0;
        String action = assignment.getTabSwitchAction() != null ? assignment.getTabSwitchAction() : "WARN";

        // Check threshold
        if (threshold > 0 && attempt.getTabSwitchCount() >= threshold) {
            switch (action) {
                case "FLAG" -> {
                    attempt.setFlagged(true);
                    attempt.setFlagReason("Tab switch threshold exceeded: " + attempt.getTabSwitchCount() + "/" + threshold);
                    log.warn("Attempt {} flagged for excessive tab switching", attemptId);
                }
                case "AUTO_SUBMIT" -> {
                    attempt.setFlagged(true);
                    attempt.setFlagReason("Auto-submitted: tab switch threshold exceeded");
                    attempt.setSubmittedAt(LocalDateTime.now());
                    attempt.setStatus(AttemptStatus.SUBMITTED);
                    attemptRepository.save(attempt);
                    gradingService.gradeAttempt(attempt);
                    log.warn("Attempt {} auto-submitted due to excessive tab switching", attemptId);

                    // Broadcast auto-submit event
                    try {
                        liveMonitoringService.broadcastEvent(LiveTestEvent.builder()
                                .eventType(LiveTestEvent.EventType.SUBMITTED)
                                .assignmentId(assignment.getId())
                                .studentId(studentId)
                                .tabSwitchCount(attempt.getTabSwitchCount())
                                .percentage(attempt.getPercentage())
                                .build());
                    } catch (Exception e) {
                        log.warn("Failed to broadcast auto-submit event: {}", e.getMessage());
                    }

                    return mapToDto(attempt, assignment);
                }
                default -> {
                    // WARN: just broadcast
                }
            }
        }

        attemptRepository.save(attempt);

        // Broadcast tab switch event
        try {
            liveMonitoringService.broadcastEvent(LiveTestEvent.builder()
                    .eventType(LiveTestEvent.EventType.TAB_SWITCH)
                    .assignmentId(assignment.getId())
                    .studentId(studentId)
                    .tabSwitchCount(attempt.getTabSwitchCount())
                    .build());
        } catch (Exception e) {
            log.warn("Failed to broadcast tab switch event: {}", e.getMessage());
        }

        return mapToDto(attempt, assignment);
    }

    @Transactional(readOnly = true)
    public AttemptDto getAttempt(UUID attemptId, UUID userId) {
        TestAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("TestAttempt", "id", attemptId));

        // Access check: student can view own attempts, teacher can view assignment attempts
        TestAssignment assignment = attempt.getAssignment();
        if (!attempt.getStudentId().equals(userId) && !assignment.getTeacherId().equals(userId)) {
            throw new BusinessException("error.access.denied", null, HttpStatus.FORBIDDEN);
        }

        return mapToDto(attempt, assignment);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AttemptDto> getStudentAttempts(UUID studentId, Pageable pageable) {
        Page<TestAttempt> page = attemptRepository.findByStudentIdOrderByCreatedAtDesc(studentId, pageable);

        // All attempts belong to the same student — fetch name once to avoid N+1
        String studentName = userRepository.findById(studentId)
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .orElse(null);

        List<AttemptDto> dtos = page.getContent().stream()
                .map(a -> mapToDto(a, a.getAssignment(), studentName))
                .toList();

        return PagedResponse.of(dtos, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    private void checkTimeExpired(TestAttempt attempt) {
        TestAssignment assignment = attempt.getAssignment();
        LocalDateTime deadline = attempt.getStartedAt().plusMinutes(assignment.getDurationMinutes());
        if (LocalDateTime.now().isAfter(deadline)) {
            // Auto-submit
            attempt.setSubmittedAt(deadline);
            attempt.setStatus(AttemptStatus.SUBMITTED);
            attemptRepository.save(attempt);
            gradingService.gradeAttempt(attempt);
            throw BusinessException.ofKey("test.taking.time.expired");
        }
    }

    private AttemptDto mapToDto(TestAttempt attempt, TestAssignment assignment) {
        String studentName = userRepository.findById(attempt.getStudentId())
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .orElse(null);
        return mapToDto(attempt, assignment, studentName);
    }

    private AttemptDto mapToDto(TestAttempt attempt, TestAssignment assignment, String studentName) {
        // Calculate remaining seconds
        Long remainingSeconds = null;
        if (attempt.getStatus() == AttemptStatus.IN_PROGRESS && assignment != null) {
            LocalDateTime deadline = attempt.getStartedAt().plusMinutes(assignment.getDurationMinutes());
            Duration remaining = Duration.between(LocalDateTime.now(), deadline);
            remainingSeconds = Math.max(0, remaining.getSeconds());
        }

        // Load questions for the exam UI and result page
        List<AttemptQuestionDto> questionDtos = null;
        if (assignment != null) {
            int variantIdx = attempt.getVariantIndex() != null ? attempt.getVariantIndex() : 0;
            if (attempt.getStatus() == AttemptStatus.IN_PROGRESS) {
                // During exam: load questions WITHOUT correct answers (security)
                questionDtos = loadQuestionsForAttempt(assignment.getTestHistoryId(), variantIdx, false, false);
            } else {
                // After submission: load questions WITH correct answers/proofs according to assignment settings
                boolean showAnswers = Boolean.TRUE.equals(assignment.getShowCorrectAnswers());
                boolean showProofs  = Boolean.TRUE.equals(assignment.getShowProofs());
                questionDtos = loadQuestionsForAttempt(assignment.getTestHistoryId(), variantIdx, showAnswers, showProofs);
            }
        }

        // Load answers as a map keyed by questionId (completed attempts only)
        Map<UUID, AnswerDto> answersMap = null;
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            List<Answer> answers = answerRepository.findByAttemptIdOrderByQuestionIndexAsc(attempt.getId());
            answersMap = answers.stream()
                    .collect(Collectors.toMap(Answer::getQuestionId, this::mapAnswerToDto));
        }

        // totalQuestions: prefer the loaded question list; fall back to answer-record count
        int totalQuestionsInt = (questionDtos != null && !questionDtos.isEmpty())
                ? questionDtos.size()
                : (answersMap != null ? answersMap.size() : (int) answerRepository.countByAttemptId(attempt.getId()));
        int answeredQuestionsInt = answersMap != null
                ? (int) answersMap.values().stream().filter(a -> a.getSelectedAnswer() != null).count()
                : (int) answerRepository.countByAttemptIdAndSelectedAnswerIsNotNull(attempt.getId());

        String assignmentTitle = assignment != null ? assignment.getTitle() : null;
        Integer durationMinutes = assignment != null ? assignment.getDurationMinutes() : null;

        return AttemptDto.builder()
                .id(attempt.getId())
                .assignmentId(assignment != null ? assignment.getId() : null)
                .assignmentTitle(assignmentTitle)
                .studentId(attempt.getStudentId())
                .studentName(studentName)
                .attemptNumber(attempt.getAttemptNumber())
                .variantIndex(attempt.getVariantIndex())
                .startedAt(attempt.getStartedAt())
                .submittedAt(attempt.getSubmittedAt())
                .remainingSeconds(remainingSeconds)
                .rawScore(attempt.getRawScore())
                .maxScore(attempt.getMaxScore())
                .percentage(attempt.getPercentage())
                .status(attempt.getStatus())
                .tabSwitchCount(attempt.getTabSwitchCount())
                .ipAddress(attempt.getIpAddress())
                .flagged(attempt.getFlagged())
                .flagReason(attempt.getFlagReason())
                .questions(questionDtos)
                .answers(answersMap)
                .totalQuestions(totalQuestionsInt)
                .answeredQuestions(answeredQuestionsInt)
                .createdAt(attempt.getCreatedAt())
                // Frontend-expected aliases
                .testTitle(assignmentTitle)
                .tabSwitches(attempt.getTabSwitchCount())
                .score(attempt.getRawScore() != null ? attempt.getRawScore().doubleValue() : null)
                .timeRemaining(remainingSeconds)
                .durationMinutes(durationMinutes)
                .build();
    }

    private List<AttemptQuestionDto> loadQuestionsForAttempt(
            UUID testHistoryId, int variantIndex, boolean includeCorrectAnswer, boolean includeProof) {
        if (testHistoryId == null) return List.of();
        try {
            // Convert numeric index to variant code: 0→A, 1→B, etc.
            String variantCode = String.valueOf((char) ('A' + variantIndex));
            List<TestQuestion> testQuestions = testQuestionRepository
                    .findByTestIdAndVariantCodeOrderByQuestionOrderAsc(testHistoryId, variantCode);

            if (testQuestions.isEmpty()) {
                log.debug("No questions found for testHistoryId={} variantCode={}", testHistoryId, variantCode);
                return List.of();
            }

            // Batch-load Question entities by their IDs
            List<UUID> questionIds = testQuestions.stream()
                    .map(TestQuestion::getQuestionId).toList();
            Map<UUID, Question> questionMap = new HashMap<>();
            questionRepository.findAllById(questionIds)
                    .forEach(q -> questionMap.put(q.getId(), q));

            return testQuestions.stream()
                    .map(tq -> {
                        Question q = questionMap.get(tq.getQuestionId());
                        if (q == null) return null;
                        AttemptQuestionDto.AttemptQuestionDtoBuilder builder = AttemptQuestionDto.builder()
                                .id(q.getId())
                                .questionText(extractText(q.getQuestionText()))
                                .questionType(q.getQuestionType() != null ? q.getQuestionType().name() : null)
                                .difficulty(q.getDifficulty() != null ? q.getDifficulty().name() : null)
                                .points(q.getPoints() != null ? q.getPoints().doubleValue() : 1.0)
                                .timeLimitSeconds(q.getTimeLimitSeconds())
                                .media(q.getMedia())
                                .options(parseJson(q.getOptions()))
                                .optionsOrder(tq.getOptionsOrder());
                        if (includeCorrectAnswer) {
                            builder.correctAnswer(parseJson(q.getCorrectAnswer()));
                        }
                        if (includeProof && q.getProof() != null && !q.getProof().isEmpty()) {
                            builder.proof(extractText(q.getProof()));
                        }
                        return builder.build();
                    })
                    .filter(java.util.Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to load questions for testHistoryId={}: {}", testHistoryId, e.getMessage());
            return List.of();
        }
    }

    private String extractText(Map<String, String> textMap) {
        if (textMap == null || textMap.isEmpty()) return "";
        for (String lang : new String[]{"uz", "en", "ru", "uz_Cyrl"}) {
            String val = textMap.get(lang);
            if (val != null && !val.isBlank()) return val;
        }
        return textMap.values().stream().findFirst().orElse("");
    }

    private AnswerDto mapAnswerToDto(Answer answer) {
        Object parsedAnswer = parseJson(answer.getSelectedAnswer());
        return AnswerDto.builder()
                .id(answer.getId())
                .questionId(answer.getQuestionId())
                .questionIndex(answer.getQuestionIndex())
                .selectedAnswer(parsedAnswer)
                .response(parsedAnswer)              // frontend alias for selectedAnswer
                .isCorrect(answer.getIsCorrect())
                .isPartial(answer.getIsPartial())
                .earnedPoints(answer.getEarnedPoints())
                .maxPoints(answer.getMaxPoints())
                .score(answer.getEarnedPoints() != null  // frontend alias for earnedPoints
                        ? answer.getEarnedPoints().doubleValue() : null)
                .needsManualGrading(answer.getNeedsManualGrading())
                .manualScore(answer.getManualScore())
                .manualFeedback(answer.getManualFeedback())
                .gradedAt(answer.getGradedAt())
                .timeSpentSeconds(answer.getTimeSpentSeconds())
                .bookmarked(answer.getBookmarked())
                .build();
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw BusinessException.ofKey("test.taking.serialize.answer.failed");
        }
    }

    private Object parseJson(String json) {
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException e) {
            return json;
        }
    }
}
