package uz.eduplatform.modules.assessment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.eduplatform.modules.assessment.domain.ExportJob;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExportJobRepository extends JpaRepository<ExportJob, UUID> {

    Optional<ExportJob> findByIdAndTeacherId(UUID id, UUID teacherId);

    @Modifying
    @Query("DELETE FROM ExportJob e WHERE e.expiresAt < :now")
    int deleteExpiredJobs(@Param("now") LocalDateTime now);
}
