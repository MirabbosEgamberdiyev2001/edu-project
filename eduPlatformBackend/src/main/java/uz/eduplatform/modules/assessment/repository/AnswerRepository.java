package uz.eduplatform.modules.assessment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
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
}
