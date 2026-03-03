package uz.eduplatform.modules.homework.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.eduplatform.modules.homework.domain.HomeworkSubmission;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HomeworkSubmissionRepository extends JpaRepository<HomeworkSubmission, UUID> {

    Page<HomeworkSubmission> findByHomeworkIdOrderBySubmittedAtDesc(UUID homeworkId, Pageable pageable);

    Optional<HomeworkSubmission> findTopByHomeworkIdAndStudentIdOrderByAttemptNumberDesc(
            UUID homeworkId, UUID studentId);

    long countByHomeworkId(UUID homeworkId);

    long countByHomeworkIdAndStatus(UUID homeworkId, String status);

    List<HomeworkSubmission> findByHomeworkIdAndStudentIdOrderByAttemptNumberDesc(
            UUID homeworkId, UUID studentId);
}
