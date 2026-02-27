package uz.eduplatform.modules.test.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.eduplatform.modules.test.domain.GlobalStatus;
import uz.eduplatform.modules.test.domain.TestCategory;
import uz.eduplatform.modules.test.domain.TestHistory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestHistoryRepository extends JpaRepository<TestHistory, UUID> {

    Page<TestHistory> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Optional<TestHistory> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    Optional<TestHistory> findByIdAndDeletedAtIsNull(UUID id);

    Optional<TestHistory> findByPublicSlugAndIsPublicTrueAndDeletedAtIsNull(String publicSlug);

    long countByDeletedAtIsNull();

    long countByCreatedAtAfterAndDeletedAtIsNull(LocalDateTime date);

    @Query("SELECT COUNT(t) FROM TestHistory t WHERE t.userId = :userId AND t.deletedAt IS NULL")
    long countByUserId(@Param("userId") UUID userId);

    @Query("SELECT COALESCE(SUM(t.downloadCount), 0) FROM TestHistory t WHERE t.deletedAt IS NULL")
    long sumDownloadCount();

    @Query(value = "SELECT DATE_TRUNC('week', created_at)::date AS week_start, COUNT(*) " +
            "FROM test_history WHERE created_at >= :since AND deleted_at IS NULL " +
            "GROUP BY week_start ORDER BY week_start", nativeQuery = true)
    List<Object[]> countWeeklyTestCreations(@Param("since") LocalDateTime since);

    // ===== Global Test Queries =====

    Page<TestHistory> findByGlobalStatusAndDeletedAtIsNull(GlobalStatus globalStatus, Pageable pageable);

    Page<TestHistory> findByGlobalStatusAndCategoryAndDeletedAtIsNull(
            GlobalStatus globalStatus, TestCategory category, Pageable pageable);

    Page<TestHistory> findByGlobalStatusAndSubjectIdAndDeletedAtIsNull(
            GlobalStatus globalStatus, UUID subjectId, Pageable pageable);

    Page<TestHistory> findByGlobalStatusAndCategoryAndSubjectIdAndDeletedAtIsNull(
            GlobalStatus globalStatus, TestCategory category, UUID subjectId, Pageable pageable);

    Page<TestHistory> findByGlobalStatusAndGradeLevelAndDeletedAtIsNull(
            GlobalStatus globalStatus, Integer gradeLevel, Pageable pageable);

    // Native query: avoids Hibernate 6 JPQL null-enum binding issue.
    // Params are plain Strings/Integer so Spring Data does not attempt enum-type inference.
    // CAST(:param AS text) IS NULL pattern solves PostgreSQL type inference error with null JDBC params.
    // ORDER BY is hardcoded to avoid Hibernate 6 Sort+native-query incompatibility (camelCase vs snake_case).
    @Query(value = "SELECT * FROM test_history " +
           "WHERE global_status = :status " +
           "AND (CAST(:category AS text) IS NULL OR category::text = CAST(:category AS text)) " +
           "AND (CAST(:subjectId AS text) IS NULL OR subject_id::text = CAST(:subjectId AS text)) " +
           "AND (:gradeLevel IS NULL OR grade_level = :gradeLevel) " +
           "AND deleted_at IS NULL " +
           "ORDER BY created_at DESC",
           countQuery = "SELECT COUNT(*) FROM test_history " +
           "WHERE global_status = :status " +
           "AND (CAST(:category AS text) IS NULL OR category::text = CAST(:category AS text)) " +
           "AND (CAST(:subjectId AS text) IS NULL OR subject_id::text = CAST(:subjectId AS text)) " +
           "AND (:gradeLevel IS NULL OR grade_level = :gradeLevel) " +
           "AND deleted_at IS NULL",
           nativeQuery = true)
    Page<TestHistory> findGlobalTests(
            @Param("status") String status,
            @Param("category") String category,
            @Param("subjectId") String subjectId,
            @Param("gradeLevel") Integer gradeLevel,
            Pageable pageable);
}
