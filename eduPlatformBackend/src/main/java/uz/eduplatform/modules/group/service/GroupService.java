package uz.eduplatform.modules.group.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.eduplatform.core.audit.AuditService;
import uz.eduplatform.core.common.dto.PagedResponse;
import uz.eduplatform.core.common.exception.BusinessException;
import uz.eduplatform.core.common.exception.ResourceNotFoundException;
import uz.eduplatform.core.i18n.TranslatedField;
import uz.eduplatform.modules.auth.domain.Role;
import uz.eduplatform.modules.auth.domain.User;
import uz.eduplatform.modules.auth.repository.UserRepository;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupService {

    private final StudentGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final AuditService auditService;

    @Transactional
    public GroupDto createGroup(UUID teacherId, CreateGroupRequest request) {
        if (request.getSubjectId() != null) {
            subjectRepository.findByIdAndUserId(request.getSubjectId(), teacherId)
                    .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", request.getSubjectId()));
        }

        Map<String, String> nameTranslations = TranslatedField.clean(request.getNameTranslations());
        Map<String, String> descTranslations = TranslatedField.clean(request.getDescriptionTranslations());
        String resolvedName = TranslatedField.resolve(nameTranslations);

        StudentGroup group = StudentGroup.builder()
                .teacherId(teacherId)
                .name(resolvedName != null ? resolvedName : "")
                .nameTranslations(nameTranslations)
                .descriptionTranslations(descTranslations)
                .subjectId(request.getSubjectId())
                .build();

        group = groupRepository.save(group);

        if (request.getStudentIds() != null && !request.getStudentIds().isEmpty()) {
            addMembersInternal(group, request.getStudentIds());
        }

        log.info("Created group by teacher {}", teacherId);
        return mapToDto(group);
    }

    @Transactional(readOnly = true)
    public PagedResponse<GroupDto> getTeacherGroups(UUID teacherId, GroupStatus status, String search, Pageable pageable) {
        Page<StudentGroup> page;
        boolean hasSearch = search != null && !search.isBlank();

        if (hasSearch && status != null) {
            page = groupRepository.searchByTeacherIdAndStatusAndName(teacherId, status, search.trim(), pageable);
        } else if (hasSearch) {
            page = groupRepository.searchByTeacherIdAndName(teacherId, search.trim(), pageable);
        } else if (status != null) {
            page = groupRepository.findByTeacherIdAndStatusOrderByCreatedAtDesc(teacherId, status, pageable);
        } else {
            page = groupRepository.findByTeacherIdOrderByCreatedAtDesc(teacherId, pageable);
        }

        List<GroupDto> dtos = page.getContent().stream().map(this::mapToDto).toList();
        return PagedResponse.of(dtos, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public PagedResponse<GroupDto> getStudentGroups(UUID studentId, Pageable pageable) {
        Page<StudentGroup> page = groupRepository.findGroupsByStudentId(studentId, pageable);
        List<GroupDto> dtos = page.getContent().stream().map(this::mapToDto).toList();
        return PagedResponse.of(dtos, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public GroupDto getGroup(UUID groupId, UUID userId) {
        StudentGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentGroup", "id", groupId));

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

        if (request.getNameTranslations() != null) {
            Map<String, String> cleaned = TranslatedField.clean(request.getNameTranslations());
            group.setNameTranslations(cleaned);
            String resolved = TranslatedField.resolve(cleaned);
            if (resolved != null) group.setName(resolved);
        }
        if (request.getDescriptionTranslations() != null) {
            group.setDescriptionTranslations(TranslatedField.clean(request.getDescriptionTranslations()));
        }
        if (request.getSubjectId() != null) {
            subjectRepository.findByIdAndUserId(request.getSubjectId(), teacherId)
                    .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", request.getSubjectId()));
            group.setSubjectId(request.getSubjectId());
        }

        group = groupRepository.save(group);
        log.info("Updated group {} by teacher {}", groupId, teacherId);
        return mapToDto(group);
    }

    @Transactional
    public GroupDto archiveGroup(UUID groupId, UUID teacherId) {
        StudentGroup group = groupRepository.findByIdAndTeacherId(groupId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentGroup", "id", groupId));
        group.setStatus(GroupStatus.ARCHIVED);
        group = groupRepository.save(group);
        log.info("Archived group {} by teacher {}", groupId, teacherId);
        return mapToDto(group);
    }

    @Transactional
    public void deleteGroup(UUID groupId, UUID teacherId) {
        StudentGroup group = groupRepository.findByIdAndTeacherId(groupId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentGroup", "id", groupId));
        group.setDeletedAt(LocalDateTime.now());
        groupRepository.save(group);
        log.info("Soft-deleted group {} by teacher {}", groupId, teacherId);
    }

    @Transactional
    public List<GroupMemberDto> addMembers(UUID groupId, UUID teacherId, AddMembersRequest request) {
        StudentGroup group = groupRepository.findByIdAndTeacherId(groupId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentGroup", "id", groupId));
        if (group.getStatus() == GroupStatus.ARCHIVED) {
            throw BusinessException.ofKey("group.add.members.archived");
        }
        List<GroupMemberDto> added = addMembersInternal(group, request.getStudentIds());
        auditService.log(teacherId, "TEACHER", "GROUP_MEMBERS_ADDED", "GROUP",
                "StudentGroup", groupId, null,
                Map.of("addedStudentIds", request.getStudentIds().toString(), "addedCount", added.size()));
        return added;
    }

    @Transactional
    public void removeMember(UUID groupId, UUID teacherId, UUID studentId) {
        groupRepository.findByIdAndTeacherId(groupId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentGroup", "id", groupId));
        GroupMember member = memberRepository.findByGroupIdAndStudentId(groupId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("GroupMember", "studentId", studentId));
        memberRepository.delete(member);
        auditService.log(teacherId, "TEACHER", "GROUP_MEMBER_REMOVED", "GROUP",
                "StudentGroup", groupId, Map.of("removedStudentId", studentId.toString()), null);
        log.info("Removed student {} from group {} by teacher {}", studentId, groupId, teacherId);
    }

    @Transactional
    public void removeMembersBatch(UUID groupId, UUID teacherId, List<UUID> studentIds) {
        groupRepository.findByIdAndTeacherId(groupId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentGroup", "id", groupId));
        int removedCount = 0;
        for (UUID studentId : studentIds) {
            Optional<GroupMember> memberOpt = memberRepository.findByGroupIdAndStudentId(groupId, studentId);
            if (memberOpt.isPresent()) {
                memberRepository.delete(memberOpt.get());
                removedCount++;
            }
        }
        auditService.log(teacherId, "TEACHER", "GROUP_MEMBERS_BATCH_REMOVED", "GROUP",
                "StudentGroup", groupId, Map.of("removedStudentIds", studentIds.toString()),
                Map.of("removedCount", removedCount));
        log.info("Teacher {} batch-removed {} members from group {}", teacherId, removedCount, groupId);
    }

    @Transactional(readOnly = true)
    public List<GroupMemberDto> getMembers(UUID groupId, UUID userId) {
        StudentGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentGroup", "id", groupId));
        if (!group.getTeacherId().equals(userId)) {
            boolean isMember = memberRepository.existsByGroupIdAndStudentId(groupId, userId);
            if (!isMember) throw new BusinessException("error.access.denied", null, HttpStatus.FORBIDDEN);
        }
        return group.getMembers().stream().map(this::mapMemberToDto).toList();
    }

    @Transactional(readOnly = true)
    public List<UUID> getGroupStudentIds(UUID groupId) {
        return groupRepository.findStudentIdsByGroupId(groupId);
    }

    @Transactional(readOnly = true)
    public PagedResponse<StudentSearchDto> searchStudents(String search, Pageable pageable) {
        Page<User> page = (search != null && !search.isBlank())
                ? userRepository.searchUsersByRole(search.trim(), Role.STUDENT, pageable)
                : userRepository.findByRole(Role.STUDENT, pageable);
        List<StudentSearchDto> dtos = page.getContent().stream()
                .map(u -> StudentSearchDto.builder()
                        .id(u.getId()).firstName(u.getFirstName())
                        .lastName(u.getLastName()).email(u.getEmail()).phone(u.getPhone()).build())
                .toList();
        return PagedResponse.of(dtos, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    // ── Internal Helpers ──

    private List<GroupMemberDto> addMembersInternal(StudentGroup group, List<UUID> studentIds) {
        List<GroupMemberDto> added = new ArrayList<>();
        for (UUID studentId : studentIds) {
            User student = userRepository.findById(studentId).orElse(null);
            if (student == null || student.getRole() != Role.STUDENT) {
                log.warn("Skipping invalid student ID: {}", studentId);
                continue;
            }
            if (memberRepository.existsByGroupIdAndStudentId(group.getId(), studentId)) continue;
            GroupMember member = GroupMember.builder().group(group).studentId(studentId).build();
            member = memberRepository.save(member);
            group.getMembers().add(member);
            added.add(mapMemberToDto(member, student));
        }
        log.info("Added {} members to group", added.size());
        return added;
    }

    private GroupDto mapToDto(StudentGroup group) {
        String teacherName = userRepository.findById(group.getTeacherId())
                .map(u -> u.getFirstName() + " " + u.getLastName()).orElse(null);
        String subjectName = null;
        if (group.getSubjectId() != null) {
            subjectName = subjectRepository.findById(group.getSubjectId())
                    .map(s -> TranslatedField.resolve(s.getName())).orElse(null);
        }
        String resolvedName = TranslatedField.resolve(group.getNameTranslations());
        if (resolvedName == null) resolvedName = group.getName();
        String resolvedDesc = TranslatedField.resolve(group.getDescriptionTranslations());
        if (resolvedDesc == null) resolvedDesc = group.getDescription();

        return GroupDto.builder()
                .id(group.getId())
                .name(resolvedName)
                .nameTranslations(group.getNameTranslations())
                .description(resolvedDesc)
                .descriptionTranslations(group.getDescriptionTranslations())
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
        return mapMemberToDto(member, userRepository.findById(member.getStudentId()).orElse(null));
    }

    private GroupMemberDto mapMemberToDto(GroupMember member, User student) {
        return GroupMemberDto.builder()
                .id(member.getId())
                .studentId(member.getStudentId())
                .firstName(student != null ? student.getFirstName() : null)
                .lastName(student != null ? student.getLastName() : null)
                .email(student != null ? student.getEmail() : null)
                .phone(student != null ? student.getPhone() : null)
                .joinedAt(member.getJoinedAt())
                .build();
    }
}
