package uz.eduplatform.modules.assessment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.eduplatform.modules.assessment.domain.AssignmentPromoCode;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PromoCodeRepository extends JpaRepository<AssignmentPromoCode, UUID> {

    Optional<AssignmentPromoCode> findByCodeAndIsActiveTrue(String code);

    Optional<AssignmentPromoCode> findByAssignmentIdAndIsActiveTrue(UUID assignmentId);

    List<AssignmentPromoCode> findByAssignmentId(UUID assignmentId);

    @Modifying
    @Query("UPDATE AssignmentPromoCode p SET p.currentUses = p.currentUses + 1, p.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE p.id = :id AND (p.maxUses IS NULL OR p.currentUses < p.maxUses)")
    int incrementCurrentUses(@Param("id") UUID id);
}
