package uz.eduplatform.modules.assessment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.eduplatform.modules.assessment.domain.Answer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, UUID> {

    List<Answer> findByAttemptIdOrderByQuestionIndexAsc(UUID attemptId);

    Optional<Answer> findByAttemptIdAndQuestionId(UUID attemptId, UUID questionId);

    long countByAttemptId(UUID attemptId);

    long countByAttemptIdAndSelectedAnswerIsNotNull(UUID attemptId);

    long countByAttemptIdAndNeedsManualGradingTrue(UUID attemptId);

    /**
     * Returns per-question aggregated stats for all submitted/graded attempts in an assignment.
     * Columns: question_id (UUID), question_index (Long), total_answered (Long), correct_count (Long).
     */
    @Query(value = """
            SELECT a.question_id,
                   MIN(a.question_index)                                             AS question_index,
                   COUNT(*)                                                           AS total_answered,
                   SUM(CASE WHEN a.is_correct = true THEN 1 ELSE 0 END)              AS correct_count
            FROM answers a
            JOIN test_attempts ta ON a.attempt_id = ta.id
            WHERE ta.assignment_id = :assignmentId
              AND a.is_correct IS NOT NULL
            GROUP BY a.question_id
            ORDER BY MIN(a.question_index)
            """, nativeQuery = true)
    List<Object[]> findQuestionStatsByAssignmentId(@Param("assignmentId") UUID assignmentId);
}
