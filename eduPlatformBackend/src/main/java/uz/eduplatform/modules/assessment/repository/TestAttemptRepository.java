package uz.eduplatform.modules.assessment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.eduplatform.modules.assessment.domain.AttemptStatus;
import uz.eduplatform.modules.assessment.domain.TestAttempt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestAttemptRepository extends JpaRepository<TestAttempt, UUID> {

    List<TestAttempt> findByAssignmentIdOrderByCreatedAtDesc(UUID assignmentId);

    Page<TestAttempt> findByStudentIdOrderByCreatedAtDesc(UUID studentId, Pageable pageable);

    List<TestAttempt> findByStudentIdOrderByCreatedAtDesc(UUID studentId);

    List<TestAttempt> findByAssignmentIdAndStudentIdOrderByAttemptNumberAsc(
            UUID assignmentId, UUID studentId);

    Optional<TestAttempt> findByIdAndStudentId(UUID id, UUID studentId);

    Optional<TestAttempt> findByAssignmentIdAndStudentIdAndStatus(
            UUID assignmentId, UUID studentId, AttemptStatus status);

    long countByAssignmentIdAndStudentId(UUID assignmentId, UUID studentId);

    long countByAssignmentId(UUID assignmentId);

    @Query("SELECT COUNT(a) FROM TestAttempt a WHERE a.assignment.id = :assignmentId " +
            "AND a.status NOT IN ('IN_PROGRESS')")
    long countSubmittedByAssignmentId(@Param("assignmentId") UUID assignmentId);

    @Query("SELECT COUNT(DISTINCT a.studentId) FROM TestAttempt a " +
            "WHERE a.assignment.id = :assignmentId")
    long countDistinctStudentsByAssignmentId(@Param("assignmentId") UUID assignmentId);

    @Query("SELECT AVG(a.percentage) FROM TestAttempt a " +
            "WHERE a.assignment.id = :assignmentId AND a.percentage IS NOT NULL")
    Double averagePercentageByAssignmentId(@Param("assignmentId") UUID assignmentId);

    @Query("SELECT MAX(a.percentage) FROM TestAttempt a " +
            "WHERE a.assignment.id = :assignmentId AND a.percentage IS NOT NULL")
    Double maxPercentageByAssignmentId(@Param("assignmentId") UUID assignmentId);

    @Query("SELECT MIN(a.percentage) FROM TestAttempt a " +
            "WHERE a.assignment.id = :assignmentId AND a.percentage IS NOT NULL")
    Double minPercentageByAssignmentId(@Param("assignmentId") UUID assignmentId);

    // Auto-submit scheduler: find expired IN_PROGRESS attempts
    @Query(value = "SELECT ta.* FROM test_attempts ta " +
            "JOIN test_assignments tsg ON ta.assignment_id = tsg.id " +
            "WHERE ta.status = 'IN_PROGRESS' " +
            "AND ta.started_at + (tsg.duration_minutes * interval '1 minute') < :now",
            nativeQuery = true)
    List<TestAttempt> findExpiredInProgressAttempts(@Param("now") LocalDateTime now);

    // Student dashboard: find by student and status
    List<TestAttempt> findByStudentIdAndStatus(UUID studentId, AttemptStatus status);

    // Student dashboard: find attempts submitted after a date
    List<TestAttempt> findByStudentIdAndSubmittedAtAfter(UUID studentId, LocalDateTime after);
}
