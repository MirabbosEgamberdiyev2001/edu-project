package uz.eduplatform.modules.assessment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.eduplatform.modules.assessment.domain.AssignmentStatus;
import uz.eduplatform.modules.assessment.domain.TestAssignment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestAssignmentRepository extends JpaRepository<TestAssignment, UUID> {

    Page<TestAssignment> findByTeacherIdOrderByCreatedAtDesc(UUID teacherId, Pageable pageable);

    Page<TestAssignment> findByTeacherIdAndStatusOrderByCreatedAtDesc(
            UUID teacherId, AssignmentStatus status, Pageable pageable);

    Optional<TestAssignment> findByIdAndTeacherId(UUID id, UUID teacherId);

    // Find assignments for a specific student (JSONB contains)
    @Query(value = "SELECT * FROM test_assignments " +
            "WHERE assigned_student_ids @> :studentIdJson::jsonb " +
            "AND status IN ('ACTIVE', 'SCHEDULED') " +
            "AND deleted_at IS NULL " +
            "ORDER BY created_at DESC",
            countQuery = "SELECT COUNT(*) FROM test_assignments " +
                    "WHERE assigned_student_ids @> :studentIdJson::jsonb " +
                    "AND status IN ('ACTIVE', 'SCHEDULED') " +
                    "AND deleted_at IS NULL",
            nativeQuery = true)
    Page<TestAssignment> findAssignmentsForStudent(
            @Param("studentIdJson") String studentIdJson, Pageable pageable);

    // Find assignments that need status updates (scheduling)
    @Query("SELECT a FROM TestAssignment a WHERE a.status = 'SCHEDULED' " +
            "AND a.startTime <= :now")
    List<TestAssignment> findScheduledAssignmentsToActivate(@Param("now") LocalDateTime now);

    @Query("SELECT a FROM TestAssignment a WHERE a.status = 'ACTIVE' " +
            "AND a.endTime IS NOT NULL AND a.endTime <= :now")
    List<TestAssignment> findActiveAssignmentsToComplete(@Param("now") LocalDateTime now);

    long countByTeacherId(UUID teacherId);

    long countByTeacherIdAndStatus(UUID teacherId, AssignmentStatus status);

    long countByStatus(AssignmentStatus status);
}
