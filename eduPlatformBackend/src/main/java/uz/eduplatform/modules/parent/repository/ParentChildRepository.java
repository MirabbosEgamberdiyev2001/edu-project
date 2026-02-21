package uz.eduplatform.modules.parent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.eduplatform.modules.parent.domain.PairingStatus;
import uz.eduplatform.modules.parent.domain.ParentChild;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParentChildRepository extends JpaRepository<ParentChild, UUID> {

    Optional<ParentChild> findByPairingCode(String pairingCode);

    Optional<ParentChild> findByParentIdAndChildId(UUID parentId, UUID childId);

    List<ParentChild> findByParentIdAndStatus(UUID parentId, PairingStatus status);

    List<ParentChild> findByChildIdAndStatus(UUID childId, PairingStatus status);

    List<ParentChild> findByParentId(UUID parentId);

    List<ParentChild> findByChildId(UUID childId);

    boolean existsByParentIdAndChildIdAndStatus(UUID parentId, UUID childId, PairingStatus status);
}
