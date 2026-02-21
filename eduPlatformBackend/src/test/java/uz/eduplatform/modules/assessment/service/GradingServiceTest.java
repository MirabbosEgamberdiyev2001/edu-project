package uz.eduplatform.modules.assessment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GradingServiceTest {

    @Mock
    private TestAttemptRepository attemptRepository;

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private GradingService gradingService;

    private UUID attemptId;
    private UUID questionId;
    private TestAttempt attempt;

    @BeforeEach
    void setUp() {
        attemptId = UUID.randomUUID();
        questionId = UUID.randomUUID();
        attempt = TestAttempt.builder()
                .id(attemptId)
                .studentId(UUID.randomUUID())
                .status(AttemptStatus.SUBMITTED)
                .build();
    }

    // ==================== MCQ_SINGLE ====================

    @Test
    void gradeAttempt_mcqSingleCorrect_fullPoints() {
        Question question = buildQuestion(questionId, QuestionType.MCQ_SINGLE, "\"A\"", new BigDecimal("2.00"));
        Answer answer = buildAnswer(attemptId, questionId, 0, "\"A\"");

        setupMocks(List.of(answer), List.of(question));

        when(attemptRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        TestAttempt result = gradingService.gradeAttempt(attempt);

        assertTrue(answer.getIsCorrect());
        assertEquals(new BigDecimal("2.00"), answer.getEarnedPoints());
        assertEquals(new BigDecimal("100.00"), result.getPercentage());
        assertEquals(AttemptStatus.AUTO_GRADED, result.getStatus());
    }

    @Test
    void gradeAttempt_mcqSingleWrong_zeroPoints() {
        Question question = buildQuestion(questionId, QuestionType.MCQ_SINGLE, "\"A\"", new BigDecimal("2.00"));
        Answer answer = buildAnswer(attemptId, questionId, 0, "\"B\"");

        setupMocks(List.of(answer), List.of(question));
        when(attemptRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        TestAttempt result = gradingService.gradeAttempt(attempt);

        assertFalse(answer.getIsCorrect());
        assertEquals(BigDecimal.ZERO, answer.getEarnedPoints());
        assertEquals(new BigDecimal("0.00"), result.getPercentage());
    }

    // ==================== MCQ_MULTI ====================

    @Test
    void gradeAttempt_mcqMultiExactMatch_fullPoints() {
        Question question = buildQuestion(questionId, QuestionType.MCQ_MULTI, "[\"A\",\"C\"]", new BigDecimal("4.00"));
        Answer answer = buildAnswer(attemptId, questionId, 0, "[\"A\",\"C\"]");

        setupMocks(List.of(answer), List.of(question));
        when(attemptRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        gradingService.gradeAttempt(attempt);

        assertTrue(answer.getIsCorrect());
        assertEquals(new BigDecimal("4.00"), answer.getEarnedPoints());
    }

    @Test
    void gradeAttempt_mcqMultiPartialCredit_partialPoints() {
        // Correct: A, C. Student selects: A, B (1 correct, 1 wrong out of 2)
        // partialRatio = max(0, (1 - 1) / 2) = 0
        Question question = buildQuestion(questionId, QuestionType.MCQ_MULTI, "[\"A\",\"C\"]", new BigDecimal("4.00"));
        Answer answer = buildAnswer(attemptId, questionId, 0, "[\"A\",\"B\"]");

        setupMocks(List.of(answer), List.of(question));
        when(attemptRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        gradingService.gradeAttempt(attempt);

        assertFalse(answer.getIsCorrect());
        assertEquals(new BigDecimal("0.00"), answer.getEarnedPoints());
    }

    @Test
    void gradeAttempt_mcqMultiPartialCredit_oneCorrectNoWrong() {
        // Correct: A, B, C. Student selects: A (1 correct, 0 wrong out of 3)
        // partialRatio = max(0, (1 - 0) / 3) = 0.333...
        Question question = buildQuestion(questionId, QuestionType.MCQ_MULTI, "[\"A\",\"B\",\"C\"]", new BigDecimal("3.00"));
        Answer answer = buildAnswer(attemptId, questionId, 0, "[\"A\"]");

        setupMocks(List.of(answer), List.of(question));
        when(attemptRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        gradingService.gradeAttempt(attempt);

        assertFalse(answer.getIsCorrect());
        assertTrue(answer.getIsPartial());
        assertEquals(new BigDecimal("1.00"), answer.getEarnedPoints());
    }

    // ==================== TRUE_FALSE ====================

    @Test
    void gradeAttempt_trueFalseCorrect_fullPoints() {
        Question question = buildQuestion(questionId, QuestionType.TRUE_FALSE, "true", BigDecimal.ONE);
        Answer answer = buildAnswer(attemptId, questionId, 0, "true");

        setupMocks(List.of(answer), List.of(question));
        when(attemptRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        gradingService.gradeAttempt(attempt);

        assertTrue(answer.getIsCorrect());
        assertEquals(BigDecimal.ONE, answer.getEarnedPoints());
    }

    @Test
    void gradeAttempt_trueFalseWrong_zeroPoints() {
        Question question = buildQuestion(questionId, QuestionType.TRUE_FALSE, "true", BigDecimal.ONE);
        Answer answer = buildAnswer(attemptId, questionId, 0, "false");

        setupMocks(List.of(answer), List.of(question));
        when(attemptRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        gradingService.gradeAttempt(attempt);

        assertFalse(answer.getIsCorrect());
        assertEquals(BigDecimal.ZERO, answer.getEarnedPoints());
    }

    // ==================== MATCHING ====================

    @Test
    void gradeAttempt_matchingAllCorrect_fullPoints() {
        Question question = buildQuestion(questionId, QuestionType.MATCHING,
                "{\"1\":\"A\",\"2\":\"B\",\"3\":\"C\"}", new BigDecimal("3.00"));
        Answer answer = buildAnswer(attemptId, questionId, 0,
                "{\"1\":\"A\",\"2\":\"B\",\"3\":\"C\"}");

        setupMocks(List.of(answer), List.of(question));
        when(attemptRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        gradingService.gradeAttempt(attempt);

        assertTrue(answer.getIsCorrect());
        assertEquals(new BigDecimal("3.00"), answer.getEarnedPoints());
    }

    @Test
    void gradeAttempt_matchingPartialCorrect_proportionalPoints() {
        // 2 out of 3 correct -> 2/3 * 3.00 = 2.00
        Question question = buildQuestion(questionId, QuestionType.MATCHING,
                "{\"1\":\"A\",\"2\":\"B\",\"3\":\"C\"}", new BigDecimal("3.00"));
        Answer answer = buildAnswer(attemptId, questionId, 0,
                "{\"1\":\"A\",\"2\":\"B\",\"3\":\"X\"}");

        setupMocks(List.of(answer), List.of(question));
        when(attemptRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        gradingService.gradeAttempt(attempt);

        assertFalse(answer.getIsCorrect());
        assertTrue(answer.getIsPartial());
        assertEquals(new BigDecimal("2.00"), answer.getEarnedPoints());
    }

    // ==================== ORDERING ====================

    @Test
    void gradeAttempt_orderingCorrect_fullPoints() {
        Question question = buildQuestion(questionId, QuestionType.ORDERING,
                "[\"A\",\"B\",\"C\",\"D\"]", new BigDecimal("2.00"));
        Answer answer = buildAnswer(attemptId, questionId, 0,
                "[\"A\",\"B\",\"C\",\"D\"]");

        setupMocks(List.of(answer), List.of(question));
        when(attemptRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        gradingService.gradeAttempt(attempt);

        assertTrue(answer.getIsCorrect());
        assertEquals(new BigDecimal("2.00"), answer.getEarnedPoints());
    }

    @Test
    void gradeAttempt_orderingWrong_zeroPoints() {
        Question question = buildQuestion(questionId, QuestionType.ORDERING,
                "[\"A\",\"B\",\"C\",\"D\"]", new BigDecimal("2.00"));
        Answer answer = buildAnswer(attemptId, questionId, 0,
                "[\"A\",\"C\",\"B\",\"D\"]");

        setupMocks(List.of(answer), List.of(question));
        when(attemptRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        gradingService.gradeAttempt(attempt);

        assertFalse(answer.getIsCorrect());
        assertEquals(BigDecimal.ZERO, answer.getEarnedPoints());
    }

    // ==================== ESSAY / SHORT_ANSWER ====================

    @Test
    void gradeAttempt_essay_needsManualGrading() {
        Question question = buildQuestion(questionId, QuestionType.ESSAY, "\"expected\"", new BigDecimal("5.00"));
        Answer answer = buildAnswer(attemptId, questionId, 0, "\"student essay text\"");

        setupMocks(List.of(answer), List.of(question));
        when(attemptRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        TestAttempt result = gradingService.gradeAttempt(attempt);

        assertTrue(answer.getNeedsManualGrading());
        assertEquals(BigDecimal.ZERO, answer.getEarnedPoints());
        assertEquals(AttemptStatus.NEEDS_REVIEW, result.getStatus());
    }

    @Test
    void gradeAttempt_shortAnswer_needsManualGrading() {
        Question question = buildQuestion(questionId, QuestionType.SHORT_ANSWER, "\"answer\"", BigDecimal.ONE);
        Answer answer = buildAnswer(attemptId, questionId, 0, "\"student answer\"");

        setupMocks(List.of(answer), List.of(question));
        when(attemptRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        TestAttempt result = gradingService.gradeAttempt(attempt);

        assertTrue(answer.getNeedsManualGrading());
        assertEquals(AttemptStatus.NEEDS_REVIEW, result.getStatus());
    }

    // ==================== Unanswered ====================

    @Test
    void gradeAttempt_unanswered_zeroPoints() {
        Question question = buildQuestion(questionId, QuestionType.MCQ_SINGLE, "\"A\"", new BigDecimal("2.00"));
        Answer answer = buildAnswer(attemptId, questionId, 0, null);

        setupMocks(List.of(answer), List.of(question));
        when(attemptRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        gradingService.gradeAttempt(attempt);

        assertFalse(answer.getIsCorrect());
        assertEquals(BigDecimal.ZERO, answer.getEarnedPoints());
    }

    // ==================== Mixed questions ====================

    @Test
    void gradeAttempt_mixedQuestions_correctTotalScore() {
        UUID q1Id = UUID.randomUUID();
        UUID q2Id = UUID.randomUUID();
        UUID q3Id = UUID.randomUUID();

        Question q1 = buildQuestion(q1Id, QuestionType.MCQ_SINGLE, "\"A\"", new BigDecimal("2.00"));
        Question q2 = buildQuestion(q2Id, QuestionType.TRUE_FALSE, "true", BigDecimal.ONE);
        Question q3 = buildQuestion(q3Id, QuestionType.ESSAY, "\"text\"", new BigDecimal("5.00"));

        Answer a1 = buildAnswer(attemptId, q1Id, 0, "\"A\"");  // correct: 2.00
        Answer a2 = buildAnswer(attemptId, q2Id, 1, "false");  // wrong: 0
        Answer a3 = buildAnswer(attemptId, q3Id, 2, "\"essay\""); // manual: 0

        setupMocks(List.of(a1, a2, a3), List.of(q1, q2, q3));
        when(attemptRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        TestAttempt result = gradingService.gradeAttempt(attempt);

        // Total earned: 2.00, max: 8.00, percentage: 25.00%
        assertEquals(new BigDecimal("2.00"), result.getRawScore());
        assertEquals(new BigDecimal("8.00"), result.getMaxScore());
        assertEquals(new BigDecimal("25.00"), result.getPercentage());
        assertEquals(AttemptStatus.NEEDS_REVIEW, result.getStatus()); // essay needs review
    }

    // ==================== Manual Grading ====================

    @Test
    void gradeManually_fullScore_marksCorrect() {
        UUID answerId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();

        Answer answer = Answer.builder()
                .id(answerId)
                .attempt(attempt)
                .questionId(questionId)
                .maxPoints(new BigDecimal("5.00"))
                .needsManualGrading(true)
                .build();

        when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));
        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(answerRepository.findByAttemptIdOrderByQuestionIndexAsc(attemptId)).thenReturn(List.of(answer));
        when(attemptRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        GradeAnswerRequest request = GradeAnswerRequest.builder()
                .answerId(answerId)
                .score(new BigDecimal("5.00"))
                .feedback("Excellent work!")
                .build();

        gradingService.gradeManually(teacherId, request);

        assertTrue(answer.getIsCorrect());
        assertFalse(answer.getIsPartial());
        assertEquals(new BigDecimal("5.00"), answer.getEarnedPoints());
        assertEquals("Excellent work!", answer.getManualFeedback());
        assertEquals(teacherId, answer.getGradedBy());
        assertFalse(answer.getNeedsManualGrading());
    }

    @Test
    void gradeManually_partialScore_marksPartial() {
        UUID answerId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();

        Answer answer = Answer.builder()
                .id(answerId)
                .attempt(attempt)
                .questionId(questionId)
                .maxPoints(new BigDecimal("5.00"))
                .needsManualGrading(true)
                .build();

        when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));
        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(answerRepository.findByAttemptIdOrderByQuestionIndexAsc(attemptId)).thenReturn(List.of(answer));
        when(attemptRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        GradeAnswerRequest request = GradeAnswerRequest.builder()
                .answerId(answerId)
                .score(new BigDecimal("3.00"))
                .build();

        gradingService.gradeManually(teacherId, request);

        assertFalse(answer.getIsCorrect());
        assertTrue(answer.getIsPartial());
        assertEquals(new BigDecimal("3.00"), answer.getEarnedPoints());
    }

    @Test
    void gradeManually_exceedsMaxPoints_throwsException() {
        UUID answerId = UUID.randomUUID();

        Answer answer = Answer.builder()
                .id(answerId)
                .maxPoints(new BigDecimal("5.00"))
                .build();

        when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));

        GradeAnswerRequest request = GradeAnswerRequest.builder()
                .answerId(answerId)
                .score(new BigDecimal("6.00"))
                .build();

        assertThrows(BusinessException.class, () ->
                gradingService.gradeManually(UUID.randomUUID(), request));
    }

    @Test
    void gradeManually_answerNotFound_throwsException() {
        UUID answerId = UUID.randomUUID();

        when(answerRepository.findById(answerId)).thenReturn(Optional.empty());

        GradeAnswerRequest request = GradeAnswerRequest.builder()
                .answerId(answerId)
                .score(BigDecimal.ONE)
                .build();

        assertThrows(ResourceNotFoundException.class, () ->
                gradingService.gradeManually(UUID.randomUUID(), request));
    }

    // ==================== Helpers ====================

    private void setupMocks(List<Answer> answers, List<Question> questions) {
        when(answerRepository.findByAttemptIdOrderByQuestionIndexAsc(attemptId)).thenReturn(answers);
        when(questionRepository.findAllById(any())).thenReturn(questions);
        when(answerRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));
    }

    private Question buildQuestion(UUID id, QuestionType type, String correctAnswer, BigDecimal points) {
        return Question.builder()
                .id(id)
                .questionText(Map.of("uz_latn", "Test savol"))
                .questionType(type)
                .correctAnswer(correctAnswer)
                .points(points)
                .build();
    }

    private Answer buildAnswer(UUID attemptId, UUID questionId, int index, String selectedAnswer) {
        return Answer.builder()
                .id(UUID.randomUUID())
                .attempt(attempt)
                .questionId(questionId)
                .questionIndex(index)
                .selectedAnswer(selectedAnswer)
                .build();
    }
}
