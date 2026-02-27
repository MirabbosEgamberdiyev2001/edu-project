package uz.eduplatform.modules.content.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.eduplatform.modules.content.domain.*;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuestionRepository extends JpaRepository<Question, UUID>, JpaSpecificationExecutor<Question> {

    Page<Question> findByTopicIdAndStatusNot(UUID topicId, QuestionStatus status, Pageable pageable);

    Page<Question> findByUserIdAndStatusNot(UUID userId, QuestionStatus status, Pageable pageable);

    Page<Question> findByStatus(QuestionStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"topic", "topic.subject", "user"})
    @Query("SELECT q FROM Question q WHERE q.topic.id IN :topicIds AND q.status = :status")
    List<Question> findByTopicIdsAndStatus(@Param("topicIds") List<UUID> topicIds,
                                           @Param("status") QuestionStatus status);

    @Query("SELECT q FROM Question q WHERE q.topic.id IN :topicIds AND q.status = :status AND q.difficulty = :difficulty")
    List<Question> findByTopicIdsAndStatusAndDifficulty(@Param("topicIds") List<UUID> topicIds,
                                                        @Param("status") QuestionStatus status,
                                                        @Param("difficulty") Difficulty difficulty);

    @Query("SELECT q FROM Question q WHERE q.topic.subject.id = :subjectId AND q.status = :status")
    List<Question> findBySubjectIdAndStatus(@Param("subjectId") UUID subjectId,
                                            @Param("status") QuestionStatus status);

    @Query("SELECT COUNT(q) FROM Question q WHERE q.topic.id = :topicId")
    long countByTopicId(@Param("topicId") UUID topicId);

    @Query("SELECT COUNT(q) FROM Question q WHERE q.status = :status")
    long countByStatus(@Param("status") QuestionStatus status);

    long count();

    @Query("SELECT q FROM Question q WHERE q.user.id = :userId " +
            "AND (:topicId IS NULL OR q.topic.id = :topicId) " +
            "AND (:type IS NULL OR q.questionType = :type) " +
            "AND (:difficulty IS NULL OR q.difficulty = :difficulty) " +
            "AND (:status IS NULL OR q.status = :status)")
    Page<Question> findByFilters(@Param("userId") UUID userId,
                                  @Param("topicId") UUID topicId,
                                  @Param("type") QuestionType type,
                                  @Param("difficulty") Difficulty difficulty,
                                  @Param("status") QuestionStatus status,
                                  Pageable pageable);

    @Query(value = "SELECT q.* FROM questions q WHERE q.deleted_at IS NULL AND " +
            "q.user_id = :userId AND " +
            "(LOWER(q.question_text ->> 'uz_latn') LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(q.question_text ->> 'uz_cyrl') LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(q.question_text ->> 'en') LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(q.question_text ->> 'ru') LIKE LOWER(CONCAT('%', :search, '%')))",
            countQuery = "SELECT COUNT(*) FROM questions q WHERE q.deleted_at IS NULL AND " +
                    "q.user_id = :userId AND " +
                    "(LOWER(q.question_text ->> 'uz_latn') LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    " LOWER(q.question_text ->> 'uz_cyrl') LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    " LOWER(q.question_text ->> 'en') LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    " LOWER(q.question_text ->> 'ru') LIKE LOWER(CONCAT('%', :search, '%')))",
            nativeQuery = true)
    Page<Question> searchByUser(@Param("userId") UUID userId,
                                @Param("search") String search,
                                Pageable pageable);

    @EntityGraph(attributePaths = {"topic", "topic.subject"})
    @Query("SELECT q FROM Question q WHERE q.id IN :ids AND q.status = :status")
    List<Question> findByIdInAndStatus(@Param("ids") List<UUID> ids, @Param("status") QuestionStatus status);

    @EntityGraph(attributePaths = {"topic", "topic.subject", "user"})
    @Query("SELECT q FROM Question q WHERE q.topic.id IN :topicIds " +
           "AND (q.status IN ('ACTIVE', 'APPROVED') " +
           "OR (q.user.id = :userId AND q.status IN ('DRAFT', 'PENDING', 'APPROVED')))")
    List<Question> findByTopicIdsForTeacher(@Param("topicIds") List<UUID> topicIds,
                                            @Param("userId") UUID userId);

    // --- With difficulty filter ---

    @EntityGraph(attributePaths = {"topic", "topic.subject", "user"})
    @Query("SELECT q FROM Question q WHERE q.topic.id IN :topicIds " +
           "AND q.user.id = :userId AND q.status = :status " +
           "AND q.difficulty = :difficulty")
    Page<Question> findByTopicIdsFilteredOwned(@Param("topicIds") List<UUID> topicIds,
                                                @Param("userId") UUID userId,
                                                @Param("status") QuestionStatus status,
                                                @Param("difficulty") Difficulty difficulty,
                                                Pageable pageable);

    @EntityGraph(attributePaths = {"topic", "topic.subject", "user"})
    @Query("SELECT q FROM Question q WHERE q.topic.id IN :topicIds " +
           "AND q.difficulty = :difficulty " +
           "AND (q.status IN ('ACTIVE', 'APPROVED') OR (q.user.id = :userId AND q.status IN ('DRAFT', 'PENDING', 'APPROVED')))")
    Page<Question> findByTopicIdsFilteredForTeacher(@Param("topicIds") List<UUID> topicIds,
                                                     @Param("userId") UUID userId,
                                                     @Param("difficulty") Difficulty difficulty,
                                                     Pageable pageable);

    @EntityGraph(attributePaths = {"topic", "topic.subject", "user"})
    @Query("SELECT q FROM Question q WHERE q.topic.id IN :topicIds " +
           "AND q.difficulty = :difficulty " +
           "AND q.status = :status")
    Page<Question> findByTopicIdsFiltered(@Param("topicIds") List<UUID> topicIds,
                                           @Param("difficulty") Difficulty difficulty,
                                           @Param("status") QuestionStatus status,
                                           Pageable pageable);

    // --- Without difficulty filter (avoids Hibernate null enum bug with PostgreSQL) ---

    @EntityGraph(attributePaths = {"topic", "topic.subject", "user"})
    @Query("SELECT q FROM Question q WHERE q.topic.id IN :topicIds " +
           "AND q.user.id = :userId AND q.status = :status")
    Page<Question> findByTopicIdsOwnedNoDifficulty(@Param("topicIds") List<UUID> topicIds,
                                                    @Param("userId") UUID userId,
                                                    @Param("status") QuestionStatus status,
                                                    Pageable pageable);

    @EntityGraph(attributePaths = {"topic", "topic.subject", "user"})
    @Query("SELECT q FROM Question q WHERE q.topic.id IN :topicIds " +
           "AND (q.status IN ('ACTIVE', 'APPROVED') OR (q.user.id = :userId AND q.status IN ('DRAFT', 'PENDING', 'APPROVED')))")
    Page<Question> findByTopicIdsForTeacherPaged(@Param("topicIds") List<UUID> topicIds,
                                                  @Param("userId") UUID userId,
                                                  Pageable pageable);

    @EntityGraph(attributePaths = {"topic", "topic.subject", "user"})
    @Query("SELECT q FROM Question q WHERE q.topic.id IN :topicIds " +
           "AND q.status = :status")
    Page<Question> findByTopicIdsFilteredNoDifficulty(@Param("topicIds") List<UUID> topicIds,
                                                      @Param("status") QuestionStatus status,
                                                      Pageable pageable);

    @Query(value = "SELECT q.* FROM questions q WHERE q.deleted_at IS NULL " +
            "AND q.topic_id IN :topicIds " +
            "AND (q.status IN ('ACTIVE', 'APPROVED') OR (q.user_id = :userId AND q.status IN ('DRAFT', 'PENDING', 'APPROVED'))) " +
            "AND (:difficulty IS NULL OR q.difficulty = CAST(:difficulty AS VARCHAR)) " +
            "AND (:status IS NULL OR q.status = CAST(:status AS VARCHAR)) " +
            "AND (LOWER(q.question_text ->> 'uz_latn') LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(q.question_text ->> 'uz_cyrl') LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(q.question_text ->> 'en') LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(q.question_text ->> 'ru') LIKE LOWER(CONCAT('%', :search, '%')))",
            countQuery = "SELECT COUNT(*) FROM questions q WHERE q.deleted_at IS NULL " +
                    "AND q.topic_id IN :topicIds " +
                    "AND (q.status IN ('ACTIVE', 'APPROVED') OR (q.user_id = :userId AND q.status IN ('DRAFT', 'PENDING', 'APPROVED'))) " +
                    "AND (:difficulty IS NULL OR q.difficulty = CAST(:difficulty AS VARCHAR)) " +
                    "AND (:status IS NULL OR q.status = CAST(:status AS VARCHAR)) " +
                    "AND (LOWER(q.question_text ->> 'uz_latn') LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    " LOWER(q.question_text ->> 'uz_cyrl') LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    " LOWER(q.question_text ->> 'en') LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    " LOWER(q.question_text ->> 'ru') LIKE LOWER(CONCAT('%', :search, '%')))",
            nativeQuery = true)
    Page<Question> searchByTopicIdsForTeacher(@Param("topicIds") List<UUID> topicIds,
                                               @Param("userId") UUID userId,
                                               @Param("search") String search,
                                               @Param("difficulty") String difficulty,
                                               @Param("status") String status,
                                               Pageable pageable);

    @EntityGraph(attributePaths = {"topic", "topic.subject", "user"})
    @Query("SELECT q FROM Question q WHERE q.topic.subject.id = :subjectId " +
           "AND (q.status IN ('ACTIVE', 'APPROVED') OR (q.user.id = :userId AND q.status IN ('DRAFT', 'PENDING', 'APPROVED')))")
    Page<Question> findBySubjectIdForTeacherPaged(@Param("subjectId") UUID subjectId,
                                                   @Param("userId") UUID userId,
                                                   Pageable pageable);

    @EntityGraph(attributePaths = {"topic", "topic.subject", "user"})
    @Query("SELECT q FROM Question q WHERE q.topic.subject.id = :subjectId " +
           "AND (q.status IN ('ACTIVE', 'APPROVED') OR (q.user.id = :userId AND q.status IN ('DRAFT', 'PENDING', 'APPROVED')))")
    List<Question> findBySubjectIdForTeacher(@Param("subjectId") UUID subjectId,
                                              @Param("userId") UUID userId);

    @Query(value = "SELECT q.* FROM questions q " +
            "JOIN topics t ON q.topic_id = t.id " +
            "WHERE q.deleted_at IS NULL AND t.subject_id = :subjectId AND t.deleted_at IS NULL " +
            "AND (q.status IN ('ACTIVE', 'APPROVED') OR (q.user_id = :userId AND q.status IN ('DRAFT', 'PENDING', 'APPROVED'))) " +
            "AND (CAST(:difficulty AS TEXT) IS NULL OR q.difficulty = CAST(:difficulty AS VARCHAR)) " +
            "AND (CAST(:status AS TEXT) IS NULL OR q.status = CAST(:status AS VARCHAR)) " +
            "AND (CAST(:search AS TEXT) IS NULL OR " +
            " LOWER(q.question_text ->> 'uz_latn') LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(q.question_text ->> 'uz_cyrl') LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(q.question_text ->> 'en') LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(q.question_text ->> 'ru') LIKE LOWER(CONCAT('%', :search, '%')))",
            countQuery = "SELECT COUNT(*) FROM questions q " +
                    "JOIN topics t ON q.topic_id = t.id " +
                    "WHERE q.deleted_at IS NULL AND t.subject_id = :subjectId AND t.deleted_at IS NULL " +
                    "AND (q.status IN ('ACTIVE', 'APPROVED') OR (q.user_id = :userId AND q.status IN ('DRAFT', 'PENDING', 'APPROVED'))) " +
                    "AND (CAST(:difficulty AS TEXT) IS NULL OR q.difficulty = CAST(:difficulty AS VARCHAR)) " +
                    "AND (CAST(:status AS TEXT) IS NULL OR q.status = CAST(:status AS VARCHAR)) " +
                    "AND (CAST(:search AS TEXT) IS NULL OR " +
                    " LOWER(q.question_text ->> 'uz_latn') LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    " LOWER(q.question_text ->> 'uz_cyrl') LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    " LOWER(q.question_text ->> 'en') LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    " LOWER(q.question_text ->> 'ru') LIKE LOWER(CONCAT('%', :search, '%')))",
            nativeQuery = true)
    Page<Question> searchBySubjectIdForTeacher(@Param("subjectId") UUID subjectId,
                                                @Param("userId") UUID userId,
                                                @Param("search") String search,
                                                @Param("difficulty") String difficulty,
                                                @Param("status") String status,
                                                Pageable pageable);

    long countByDifficulty(Difficulty difficulty);

    long countByQuestionType(QuestionType questionType);

    @Query(value = "SELECT u.id, u.first_name, u.last_name, u.email, COUNT(q.id) as question_count, " +
            "COUNT(DISTINCT s.id) as subject_count " +
            "FROM questions q " +
            "JOIN users u ON q.user_id = u.id " +
            "JOIN topics t ON q.topic_id = t.id " +
            "JOIN subjects s ON t.subject_id = s.id " +
            "WHERE q.deleted_at IS NULL AND u.deleted_at IS NULL " +
            "GROUP BY u.id, u.first_name, u.last_name, u.email " +
            "ORDER BY question_count DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<Object[]> findTopTeachersByContentCreated(@Param("limit") int limit);

    @Modifying
    @Query("UPDATE Question q SET q.timesUsed = q.timesUsed + 1 WHERE q.id IN :ids")
    void incrementTimesUsed(@Param("ids") List<UUID> ids);

    @Query(value = "SELECT EXISTS(SELECT 1 FROM questions WHERE topic_id = :topicId AND deleted_at IS NULL " +
            "AND question_text ->> 'uz_latn' = :text)",
            nativeQuery = true)
    boolean existsByTopicIdAndDefaultQuestionText(@Param("topicId") UUID topicId, @Param("text") String text);

    @Modifying
    @Query(value = "UPDATE questions SET deleted_at = :now WHERE topic_id = :topicId AND deleted_at IS NULL", nativeQuery = true)
    void softDeleteByTopicId(@Param("topicId") UUID topicId, @Param("now") java.time.LocalDateTime now);
}
