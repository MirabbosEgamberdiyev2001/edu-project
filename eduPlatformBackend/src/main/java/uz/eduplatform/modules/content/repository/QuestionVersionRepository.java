package uz.eduplatform.modules.content.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.eduplatform.modules.content.domain.QuestionVersion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuestionVersionRepository extends JpaRepository<QuestionVersion, UUID> {

    List<QuestionVersion> findByQuestionIdOrderByVersionDesc(UUID questionId);

    Optional<QuestionVersion> findByQuestionIdAndVersion(UUID questionId, Integer version);

    Optional<QuestionVersion> findTopByQuestionIdOrderByVersionDesc(UUID questionId);
}
