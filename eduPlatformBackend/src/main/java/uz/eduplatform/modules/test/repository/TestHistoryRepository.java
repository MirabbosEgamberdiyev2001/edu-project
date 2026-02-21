package uz.eduplatform.modules.test.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.eduplatform.modules.test.domain.TestHistory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestHistoryRepository extends JpaRepository<TestHistory, UUID> {

    Page<TestHistory> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Optional<TestHistory> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

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
}
