package uz.eduplatform.modules.group.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.eduplatform.modules.group.domain.GroupMember;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {

    List<GroupMember> findByGroupId(UUID groupId);

    Optional<GroupMember> findByGroupIdAndStudentId(UUID groupId, UUID studentId);

    boolean existsByGroupIdAndStudentId(UUID groupId, UUID studentId);

    void deleteByGroupIdAndStudentId(UUID groupId, UUID studentId);

    long countByGroupId(UUID groupId);

    List<GroupMember> findByStudentId(UUID studentId);
}
