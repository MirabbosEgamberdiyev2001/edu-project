package uz.eduplatform.modules.homework.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.eduplatform.modules.homework.domain.Homework;

import java.util.Collection;
import java.util.UUID;

public interface HomeworkRepository extends JpaRepository<Homework, UUID> {

    Page<Homework> findByTeacherIdOrderByCreatedAtDesc(UUID teacherId, Pageable pageable);

    Page<Homework> findByGroupIdAndStatusOrderByCreatedAtDesc(UUID groupId, String status, Pageable pageable);

    Page<Homework> findByGroupIdOrderByCreatedAtDesc(UUID groupId, Pageable pageable);

    /** Fetch all active homeworks assigned to any of the given groups */
    @Query("SELECT h FROM Homework h WHERE h.groupId IN :groupIds AND h.status = :status AND h.deletedAt IS NULL ORDER BY h.createdAt DESC")
    Page<Homework> findByGroupIdInAndStatus(
            @Param("groupIds") Collection<UUID> groupIds,
            @Param("status") String status,
            Pageable pageable);
}
