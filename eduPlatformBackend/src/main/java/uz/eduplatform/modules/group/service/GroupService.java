package uz.eduplatform.modules.group.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.eduplatform.core.common.dto.PagedResponse;
import uz.eduplatform.core.common.exception.BusinessException;
import uz.eduplatform.core.common.exception.ResourceNotFoundException;
import uz.eduplatform.core.i18n.TranslatedField;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupService {

    private final StudentGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;

    @Transactional
    public GroupDto createGroup(UUID teacherId, CreateGroupRequest request) {
        if (request.getSubjectId() != null) {
            subjectRepository.findById(request.getSubjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", request.getSubjectId()));
        }

        StudentGroup group = StudentGroup.builder()
                .teacherId(teacherId)
                .name(request.getName())
                .description(request.getDescription())
                .subjectId(request.getSubjectId())
                .build();

        group = groupRepository.save(group);

        if (request.getStudentIds() != null && !request.getStudentIds().isEmpty()) {
            addMembersInternal(group, request.getStudentIds());
        }

        log.info("Created group '{}' by teacher {}", group.getName(), teacherId);
        return mapToDto(group);
    }

    @Transactional(readOnly = true)
    public PagedResponse<GroupDto> getTeacherGroups(UUID teacherId, GroupStatus status, Pageable pageable) {
        Page<StudentGroup> page;
        if (status != null) {
            page = groupRepository.findByTeacherIdAndStatusOrderByCreatedAtDesc(teacherId, status, pageable);
        } else {
            page = groupRepository.findByTeacherIdOrderByCreatedAtDesc(teacherId, pageable);
        }

        List<GroupDto> dtos = page.getContent().stream()
                .map(this::mapToDto)
                .toList();

        return PagedResponse.of(dtos, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public PagedResponse<GroupDto> getStudentGroups(UUID studentId, Pageable pageable) {
        Page<StudentGroup> page = groupRepository.findGroupsByStudentId(studentId, pageable);

        List<GroupDto> dtos = page.getContent().stream()
                .map(this::mapToDto)
                .toList();

        return PagedResponse.of(dtos, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public GroupDto getGroup(UUID groupId, UUID userId) {
        StudentGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentGroup", "id", groupId));

        // Access: teacher owns it OR student is a member
        if (!group.getTeacherId().equals(userId)) {
            boolean isMember = memberRepository.existsByGroupIdAndStudentId(groupId, userId);
            if (!isMember) {
                throw new BusinessException("error.access.denied", null, HttpStatus.FORBIDDEN);
            }
        }

        return mapToDto(group);
    }

    @Transactional
    public GroupDto updateGroup(UUID groupId, UUID teacherId, UpdateGroupRequest request) {
        StudentGroup group = groupRepository.findByIdAndTeacherId(groupId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentGroup", "id", groupId));

        if (request.getName() != null) {
            group.setName(request.getName());
        }
        if (request.getDescription() != null) {
            group.setDescription(request.getDescription());
        }
        if (request.getSubjectId() != null) {
            subjectRepository.findById(request.getSubjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", request.getSubjectId()));
            group.setSubjectId(request.getSubjectId());
        }

        group = groupRepository.save(group);
        log.info("Updated group '{}' by teacher {}", group.getName(), teacherId);
        return mapToDto(group);
    }

    @Transactional
    public GroupDto archiveGroup(UUID groupId, UUID teacherId) {
        StudentGroup group = groupRepository.findByIdAndTeacherId(groupId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentGroup", "id", groupId));

        group.setStatus(GroupStatus.ARCHIVED);
        group = groupRepository.save(group);
        log.info("Archived group '{}' by teacher {}", group.getName(), teacherId);
        return mapToDto(group);
    }

    @Transactional
    public void deleteGroup(UUID groupId, UUID teacherId) {
        StudentGroup group = groupRepository.findByIdAndTeacherId(groupId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentGroup", "id", groupId));

        group.setDeletedAt(LocalDateTime.now());
        groupRepository.save(group);
        log.info("Soft-deleted group '{}' by teacher {}", group.getName(), teacherId);
    }

    // ── Member Management ──

    @Transactional
    public List<GroupMemberDto> addMembers(UUID groupId, UUID teacherId, AddMembersRequest request) {
        StudentGroup group = groupRepository.findByIdAndTeacherId(groupId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentGroup", "id", groupId));

        if (group.getStatus() == GroupStatus.ARCHIVED) {
            throw BusinessException.ofKey("group.add.members.archived");
        }

        return addMembersInternal(group, request.getStudentIds());
    }

    @Transactional
    public void removeMember(UUID groupId, UUID teacherId, UUID studentId) {
        groupRepository.findByIdAndTeacherId(groupId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentGroup", "id", groupId));

        GroupMember member = memberRepository.findByGroupIdAndStudentId(groupId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("GroupMember", "studentId", studentId));

        memberRepository.delete(member);
        log.info("Removed student {} from group {} by teacher {}", studentId, groupId, teacherId);
    }

    @Transactional(readOnly = true)
    public List<GroupMemberDto> getMembers(UUID groupId, UUID userId) {
        StudentGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentGroup", "id", groupId));

        // Access: teacher or member
        if (!group.getTeacherId().equals(userId)) {
            boolean isMember = memberRepository.existsByGroupIdAndStudentId(groupId, userId);
            if (!isMember) {
                throw new BusinessException("error.access.denied", null, HttpStatus.FORBIDDEN);
            }
        }

        return group.getMembers().stream()
                .map(this::mapMemberToDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UUID> getGroupStudentIds(UUID groupId) {
        return groupRepository.findStudentIdsByGroupId(groupId);
    }

    // ── Internal Helpers ──

    private List<GroupMemberDto> addMembersInternal(StudentGroup group, List<UUID> studentIds) {
        List<GroupMemberDto> added = new ArrayList<>();

        for (UUID studentId : studentIds) {
            // Verify user exists and is a student
            User student = userRepository.findById(studentId).orElse(null);
            if (student == null || student.getRole() != Role.STUDENT) {
                log.warn("Skipping invalid student ID: {}", studentId);
                continue;
            }

            // Skip if already a member
            if (memberRepository.existsByGroupIdAndStudentId(group.getId(), studentId)) {
                continue;
            }

            GroupMember member = GroupMember.builder()
                    .group(group)
                    .studentId(studentId)
                    .build();

            member = memberRepository.save(member);
            group.getMembers().add(member);
            added.add(mapMemberToDto(member, student));
        }

        log.info("Added {} members to group '{}'", added.size(), group.getName());
        return added;
    }

    private GroupDto mapToDto(StudentGroup group) {
        String teacherName = userRepository.findById(group.getTeacherId())
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .orElse(null);

        String subjectName = null;
        if (group.getSubjectId() != null) {
            subjectName = subjectRepository.findById(group.getSubjectId())
                    .map(s -> TranslatedField.resolve(s.getName()))
                    .orElse(null);
        }

        return GroupDto.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .teacherId(group.getTeacherId())
                .teacherName(teacherName)
                .subjectId(group.getSubjectId())
                .subjectName(subjectName)
                .status(group.getStatus())
                .memberCount(group.getMembers() != null ? group.getMembers().size() : 0)
                .createdAt(group.getCreatedAt())
                .updatedAt(group.getUpdatedAt())
                .build();
    }

    private GroupMemberDto mapMemberToDto(GroupMember member) {
        User student = userRepository.findById(member.getStudentId()).orElse(null);
        return mapMemberToDto(member, student);
    }

    private GroupMemberDto mapMemberToDto(GroupMember member, User student) {
        return GroupMemberDto.builder()
                .id(member.getId())
                .studentId(member.getStudentId())
                .studentName(student != null ? student.getFirstName() + " " + student.getLastName() : null)
                .studentEmail(student != null ? student.getEmail() : null)
                .joinedAt(member.getJoinedAt())
                .build();
    }
}
