package uz.eduplatform.modules.content.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.eduplatform.modules.content.domain.Topic;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TopicRepository extends JpaRepository<Topic, UUID> {

    List<Topic> findBySubjectIdAndParentIsNullOrderBySortOrderAsc(UUID subjectId);

    List<Topic> findBySubjectIdOrderBySortOrderAsc(UUID subjectId);

    List<Topic> findByParentIdOrderBySortOrderAsc(UUID parentId);

    Optional<Topic> findByIdAndSubjectId(UUID id, UUID subjectId);

    @Query("SELECT MAX(t.sortOrder) FROM Topic t WHERE t.subject.id = :subjectId AND t.parent.id = :parentId")
    Optional<Integer> findMaxSortOrderBySubjectAndParent(@Param("subjectId") UUID subjectId,
                                                         @Param("parentId") UUID parentId);

    @Query("SELECT MAX(t.sortOrder) FROM Topic t WHERE t.subject.id = :subjectId AND t.parent IS NULL")
    Optional<Integer> findMaxSortOrderBySubjectAndParentIsNull(@Param("subjectId") UUID subjectId);

    @Query("SELECT COUNT(t) FROM Topic t WHERE t.subject.id = :subjectId")
    long countBySubjectId(@Param("subjectId") UUID subjectId);

    @Query("SELECT SUM(t.questionCount) FROM Topic t WHERE t.subject.id = :subjectId")
    Optional<Integer> sumQuestionCountBySubjectId(@Param("subjectId") UUID subjectId);

    @Modifying
    @Query("DELETE FROM Topic t WHERE t.subject.id = :subjectId")
    void deleteAllBySubjectId(@Param("subjectId") UUID subjectId);

    @Query(value = "SELECT EXISTS(SELECT 1 FROM topics WHERE subject_id = :subjectId AND deleted_at IS NULL " +
            "AND (CAST(:parentId AS uuid) IS NULL AND parent_id IS NULL OR parent_id = CAST(:parentId AS uuid)) " +
            "AND name ->> 'uz_latn' = :name)",
            nativeQuery = true)
    boolean existsBySubjectIdAndParentIdAndDefaultName(@Param("subjectId") UUID subjectId,
                                                       @Param("parentId") UUID parentId,
                                                       @Param("name") String name);

    long count();
}
