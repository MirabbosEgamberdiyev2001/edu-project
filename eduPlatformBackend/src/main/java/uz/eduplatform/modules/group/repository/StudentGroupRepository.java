package uz.eduplatform.modules.group.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.eduplatform.modules.group.domain.GroupStatus;
import uz.eduplatform.modules.group.domain.StudentGroup;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentGroupRepository extends JpaRepository<StudentGroup, UUID> {

    Page<StudentGroup> findByTeacherIdOrderByCreatedAtDesc(UUID teacherId, Pageable pageable);

    Page<StudentGroup> findByTeacherIdAndStatusOrderByCreatedAtDesc(
            UUID teacherId, GroupStatus status, Pageable pageable);

    Optional<StudentGroup> findByIdAndTeacherId(UUID id, UUID teacherId);

    @Query("SELECT g FROM StudentGroup g JOIN g.members m WHERE m.studentId = :studentId ORDER BY g.createdAt DESC")
    Page<StudentGroup> findGroupsByStudentId(@Param("studentId") UUID studentId, Pageable pageable);

    @Query("SELECT g FROM StudentGroup g JOIN g.members m WHERE m.studentId = :studentId AND g.status = :status ORDER BY g.createdAt DESC")
    Page<StudentGroup> findGroupsByStudentIdAndStatus(
            @Param("studentId") UUID studentId, @Param("status") GroupStatus status, Pageable pageable);

    long countByTeacherId(UUID teacherId);

    long countByTeacherIdAndStatus(UUID teacherId, GroupStatus status);

    @Query("SELECT m.studentId FROM GroupMember m WHERE m.group.id = :groupId")
    List<UUID> findStudentIdsByGroupId(@Param("groupId") UUID groupId);
}
