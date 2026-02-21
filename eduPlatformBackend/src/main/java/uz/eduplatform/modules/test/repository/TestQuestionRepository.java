package uz.eduplatform.modules.test.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.eduplatform.modules.test.domain.TestQuestion;

import java.util.List;
import java.util.UUID;

@Repository
public interface TestQuestionRepository extends JpaRepository<TestQuestion, UUID> {

    List<TestQuestion> findByTestIdOrderByVariantCodeAscQuestionOrderAsc(UUID testId);

    List<TestQuestion> findByTestIdAndVariantCodeOrderByQuestionOrderAsc(UUID testId, String variantCode);

    void deleteByTestId(UUID testId);
}
