package uz.eduplatform.modules.homework.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.eduplatform.modules.homework.domain.Homework;

import java.util.UUID;

public interface HomeworkRepository extends JpaRepository<Homework, UUID> {

    Page<Homework> findByTeacherIdOrderByCreatedAtDesc(UUID teacherId, Pageable pageable);

    Page<Homework> findByGroupIdAndStatusOrderByCreatedAtDesc(UUID groupId, String status, Pageable pageable);

    Page<Homework> findByGroupIdOrderByCreatedAtDesc(UUID groupId, Pageable pageable);
}
