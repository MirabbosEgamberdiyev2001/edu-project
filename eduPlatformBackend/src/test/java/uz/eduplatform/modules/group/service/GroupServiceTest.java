package uz.eduplatform.modules.group.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import uz.eduplatform.core.common.exception.BusinessException;
import uz.eduplatform.core.common.exception.ResourceNotFoundException;
import uz.eduplatform.modules.auth.domain.Role;
import uz.eduplatform.modules.auth.domain.User;
import uz.eduplatform.modules.auth.repository.UserRepository;
import uz.eduplatform.modules.content.domain.Subject;
import uz.eduplatform.modules.content.repository.SubjectRepository;
import uz.eduplatform.modules.group.domain.GroupMember;
import uz.eduplatform.modules.group.domain.GroupStatus;
import uz.eduplatform.modules.group.domain.StudentGroup;
import uz.eduplatform.modules.group.dto.*;
import uz.eduplatform.modules.group.repository.GroupMemberRepository;
import uz.eduplatform.modules.group.repository.StudentGroupRepository;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GroupServiceTest {

    @Mock private StudentGroupRepository groupRepository;
    @Mock private GroupMemberRepository memberRepository;
    @Mock private UserRepository userRepository;
    @Mock private SubjectRepository subjectRepository;

    @InjectMocks private GroupService groupService;

    private UUID teacherId;
    private UUID studentId;
    private UUID groupId;
    private UUID subjectId;
    private User teacher;
    private User student;
    private Subject subject;
    private StudentGroup group;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        groupId = UUID.randomUUID();
        subjectId = UUID.randomUUID();

        teacher = User.builder()
                .id(teacherId).firstName("John").lastName("Teacher")
                .role(Role.TEACHER).email("teacher@test.com").build();

        student = User.builder()
                .id(studentId).firstName("Jane").lastName("Student")
                .role(Role.STUDENT).email("student@test.com").build();

        subject = Subject.builder().id(subjectId)
                .name(Map.of("uz_latn", "Matematika")).build();

        group = StudentGroup.builder()
                .id(groupId).teacherId(teacherId).name("Math Group 1")
                .description("Group for math").subjectId(subjectId)
                .status(GroupStatus.ACTIVE).members(new ArrayList<>())
                .createdAt(LocalDateTime.now()).build();

        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(subject));
    }

    // ── Create Group ──

    @Test
    void createGroup_success() {
        CreateGroupRequest request = CreateGroupRequest.builder()
                .name("Math Group 1").description("Group for math")
                .subjectId(subjectId).build();

        when(groupRepository.save(any(StudentGroup.class))).thenAnswer(inv -> {
            StudentGroup g = inv.getArgument(0);
            g.setId(groupId);
            return g;
        });

        GroupDto result = groupService.createGroup(teacherId, request);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Math Group 1");
        assertThat(result.getTeacherName()).isEqualTo("John Teacher");
        verify(groupRepository).save(any(StudentGroup.class));
    }

    @Test
    void createGroup_withStudents_addsMembers() {
        CreateGroupRequest request = CreateGroupRequest.builder()
                .name("Group").studentIds(List.of(studentId)).build();

        when(groupRepository.save(any(StudentGroup.class))).thenAnswer(inv -> {
            StudentGroup g = inv.getArgument(0);
            g.setId(groupId);
            g.setMembers(new ArrayList<>());
            return g;
        });
        when(memberRepository.existsByGroupIdAndStudentId(groupId, studentId)).thenReturn(false);
        when(memberRepository.save(any(GroupMember.class))).thenAnswer(inv -> {
            GroupMember m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });

        GroupDto result = groupService.createGroup(teacherId, request);

        assertThat(result).isNotNull();
        verify(memberRepository).save(any(GroupMember.class));
    }

    @Test
    void createGroup_invalidSubject_throws() {
        CreateGroupRequest request = CreateGroupRequest.builder()
                .name("Group").subjectId(UUID.randomUUID()).build();

        when(subjectRepository.findById(request.getSubjectId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.createGroup(teacherId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── Get Groups ──

    @Test
    void getTeacherGroups_returnsPagedResponse() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<StudentGroup> page = new PageImpl<>(List.of(group), pageable, 1);

        when(groupRepository.findByTeacherIdOrderByCreatedAtDesc(teacherId, pageable)).thenReturn(page);

        var result = groupService.getTeacherGroups(teacherId, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getTeacherGroups_withStatusFilter() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<StudentGroup> page = new PageImpl<>(List.of(group), pageable, 1);

        when(groupRepository.findByTeacherIdAndStatusOrderByCreatedAtDesc(
                teacherId, GroupStatus.ACTIVE, pageable)).thenReturn(page);

        var result = groupService.getTeacherGroups(teacherId, GroupStatus.ACTIVE, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getStudentGroups_returnsStudentGroups() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<StudentGroup> page = new PageImpl<>(List.of(group), pageable, 1);

        when(groupRepository.findGroupsByStudentId(studentId, pageable)).thenReturn(page);

        var result = groupService.getStudentGroups(studentId, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getGroup_byTeacher_success() {
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));

        GroupDto result = groupService.getGroup(groupId, teacherId);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Math Group 1");
    }

    @Test
    void getGroup_byStudent_member_success() {
        UUID otherUserId = UUID.randomUUID();
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(memberRepository.existsByGroupIdAndStudentId(groupId, otherUserId)).thenReturn(true);

        GroupDto result = groupService.getGroup(groupId, otherUserId);

        assertThat(result).isNotNull();
    }

    @Test
    void getGroup_nonMember_throws() {
        UUID otherUserId = UUID.randomUUID();
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(memberRepository.existsByGroupIdAndStudentId(groupId, otherUserId)).thenReturn(false);

        assertThatThrownBy(() -> groupService.getGroup(groupId, otherUserId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("error.access.denied");
    }

    // ── Update Group ──

    @Test
    void updateGroup_success() {
        UpdateGroupRequest request = UpdateGroupRequest.builder()
                .name("Updated Group").build();

        when(groupRepository.findByIdAndTeacherId(groupId, teacherId)).thenReturn(Optional.of(group));
        when(groupRepository.save(any(StudentGroup.class))).thenReturn(group);

        GroupDto result = groupService.updateGroup(groupId, teacherId, request);

        assertThat(result).isNotNull();
        verify(groupRepository).save(any(StudentGroup.class));
    }

    @Test
    void updateGroup_notFound_throws() {
        when(groupRepository.findByIdAndTeacherId(groupId, teacherId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.updateGroup(groupId, teacherId,
                UpdateGroupRequest.builder().name("x").build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── Archive / Delete ──

    @Test
    void archiveGroup_success() {
        when(groupRepository.findByIdAndTeacherId(groupId, teacherId)).thenReturn(Optional.of(group));
        when(groupRepository.save(any(StudentGroup.class))).thenReturn(group);

        GroupDto result = groupService.archiveGroup(groupId, teacherId);

        assertThat(group.getStatus()).isEqualTo(GroupStatus.ARCHIVED);
    }

    @Test
    void deleteGroup_softDeletes() {
        when(groupRepository.findByIdAndTeacherId(groupId, teacherId)).thenReturn(Optional.of(group));
        when(groupRepository.save(any(StudentGroup.class))).thenReturn(group);

        groupService.deleteGroup(groupId, teacherId);

        assertThat(group.getDeletedAt()).isNotNull();
        verify(groupRepository).save(group);
    }

    // ── Member Management ──

    @Test
    void addMembers_success() {
        AddMembersRequest request = AddMembersRequest.builder()
                .studentIds(List.of(studentId)).build();

        when(groupRepository.findByIdAndTeacherId(groupId, teacherId)).thenReturn(Optional.of(group));
        when(memberRepository.existsByGroupIdAndStudentId(groupId, studentId)).thenReturn(false);
        when(memberRepository.save(any(GroupMember.class))).thenAnswer(inv -> {
            GroupMember m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });

        List<GroupMemberDto> result = groupService.addMembers(groupId, teacherId, request);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStudentName()).isEqualTo("Jane Student");
    }

    @Test
    void addMembers_toArchivedGroup_throws() {
        group.setStatus(GroupStatus.ARCHIVED);
        AddMembersRequest request = AddMembersRequest.builder()
                .studentIds(List.of(studentId)).build();

        when(groupRepository.findByIdAndTeacherId(groupId, teacherId)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> groupService.addMembers(groupId, teacherId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("archived");
    }

    @Test
    void addMembers_duplicateSkipped() {
        AddMembersRequest request = AddMembersRequest.builder()
                .studentIds(List.of(studentId)).build();

        when(groupRepository.findByIdAndTeacherId(groupId, teacherId)).thenReturn(Optional.of(group));
        when(memberRepository.existsByGroupIdAndStudentId(groupId, studentId)).thenReturn(true);

        List<GroupMemberDto> result = groupService.addMembers(groupId, teacherId, request);

        assertThat(result).isEmpty();
        verify(memberRepository, never()).save(any());
    }

    @Test
    void removeMember_success() {
        GroupMember member = GroupMember.builder().id(UUID.randomUUID())
                .group(group).studentId(studentId).build();

        when(groupRepository.findByIdAndTeacherId(groupId, teacherId)).thenReturn(Optional.of(group));
        when(memberRepository.findByGroupIdAndStudentId(groupId, studentId)).thenReturn(Optional.of(member));

        groupService.removeMember(groupId, teacherId, studentId);

        verify(memberRepository).delete(member);
    }

    @Test
    void getMembers_byTeacher_success() {
        GroupMember member = GroupMember.builder().id(UUID.randomUUID())
                .group(group).studentId(studentId).build();
        group.setMembers(List.of(member));

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));

        List<GroupMemberDto> result = groupService.getMembers(groupId, teacherId);

        assertThat(result).hasSize(1);
    }

    @Test
    void getGroupStudentIds_returnsList() {
        when(groupRepository.findStudentIdsByGroupId(groupId)).thenReturn(List.of(studentId));

        List<UUID> result = groupService.getGroupStudentIds(groupId);

        assertThat(result).containsExactly(studentId);
    }
}
