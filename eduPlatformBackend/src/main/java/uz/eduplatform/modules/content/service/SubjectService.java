package uz.eduplatform.modules.content.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.eduplatform.core.audit.AuditService;
import uz.eduplatform.core.common.dto.PagedResponse;
import org.springframework.http.HttpStatus;
import uz.eduplatform.core.common.exception.BusinessException;
import uz.eduplatform.core.common.exception.ResourceNotFoundException;
import uz.eduplatform.core.common.utils.MessageService;
import uz.eduplatform.core.i18n.AcceptLanguage;
import uz.eduplatform.core.i18n.TranslatedField;
import uz.eduplatform.modules.auth.domain.User;
import uz.eduplatform.modules.auth.repository.UserRepository;
import uz.eduplatform.modules.content.domain.Subject;
import uz.eduplatform.modules.content.domain.Topic;
import uz.eduplatform.modules.content.dto.*;
import uz.eduplatform.modules.content.repository.SubjectRepository;
import uz.eduplatform.modules.content.repository.TopicRepository;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final MessageService messageService;

    @Transactional(readOnly = true)
    public PagedResponse<SubjectDto> getSubjects(UUID userId, String search, Integer gradeLevel,
                                                  Pageable pageable, AcceptLanguage language) {
        Page<Subject> page;
        String localeKey = language.toLocaleKey();

        boolean hasSearch = search != null && !search.isBlank();
        boolean hasGrade = gradeLevel != null;

        if (hasSearch) {
            // Native queries require actual DB column names for sorting (sort_order, not sortOrder)
            Pageable nativePageable = PageRequest.of(
                    pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.ASC, "sort_order"));
            if (hasGrade) {
                page = subjectRepository.searchByUserAndGradeLevel(userId, search.trim(), gradeLevel, nativePageable);
            } else {
                page = subjectRepository.searchByUser(userId, search.trim(), nativePageable);
            }
        } else if (hasGrade) {
            page = subjectRepository.findAllAccessibleByUserAndGradeLevel(userId, gradeLevel, pageable);
        } else {
            page = subjectRepository.findAllAccessibleByUser(userId, pageable);
        }

        List<SubjectDto> dtos = page.getContent().stream()
                .map(s -> mapToDto(s, localeKey))
                .toList();

        return PagedResponse.of(dtos, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public PagedResponse<SubjectDto> getArchivedSubjects(UUID userId, Pageable pageable,
                                                          AcceptLanguage language) {
        String localeKey = language.toLocaleKey();
        Page<Subject> page = subjectRepository.findByUserIdAndIsArchivedTrue(userId, pageable);

        List<SubjectDto> dtos = page.getContent().stream()
                .map(s -> mapToDto(s, localeKey))
                .toList();

        return PagedResponse.of(dtos, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public SubjectDto getSubjectById(UUID subjectId, UUID userId, AcceptLanguage language) {
        Subject subject = findSubjectForUser(subjectId, userId);
        return mapToDto(subject, language.toLocaleKey());
    }

    @Transactional
    public SubjectDto createSubject(UUID userId, CreateSubjectRequest request, AcceptLanguage language) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Clean locale keys (uzl -> uz_latn, uzc -> uz_cyrl)
        Map<String, String> cleanedName = TranslatedField.clean(request.getName());
        Map<String, String> cleanedDesc = TranslatedField.clean(request.getDescription());

        String defaultName = TranslatedField.defaultValue(cleanedName);
        if (defaultName != null && subjectRepository.existsByUserIdAndDefaultName(userId, defaultName)) {
            throw new BusinessException(messageService.get("subject.name.exists", language.toLocale()), HttpStatus.CONFLICT);
        }

        Subject subject = Subject.builder()
                .user(user)
                .name(cleanedName)
                .description(cleanedDesc)
                .icon(request.getIcon())
                .color(request.getColor())
                .gradeLevel(request.getGradeLevel())
                .build();

        subject = subjectRepository.save(subject);

        auditService.log(userId, null, "SUBJECT_CREATED", "CONTENT",
                "Subject", subject.getId());

        return mapToDto(subject, language.toLocaleKey());
    }

    @Transactional
    public BulkCreateResponse createSubjectsBulk(UUID userId, BulkCreateSubjectRequest request, AcceptLanguage language) {
        int created = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        for (int i = 0; i < request.getItems().size(); i++) {
            CreateSubjectRequest item = request.getItems().get(i);
            try {
                Map<String, String> cleanedName = TranslatedField.clean(item.getName());
                String defaultName = TranslatedField.defaultValue(cleanedName);
                if (defaultName != null && subjectRepository.existsByUserIdAndDefaultName(userId, defaultName)) {
                    if (request.isSkipDuplicates()) {
                        skipped++;
                        continue;
                    }
                    errors.add("[" + (i + 1) + "] " + defaultName + ": " + messageService.get("subject.name.exists", language.toLocale()));
                    continue;
                }

                Subject subject = Subject.builder()
                        .user(user)
                        .name(cleanedName)
                        .description(TranslatedField.clean(item.getDescription()))
                        .icon(item.getIcon())
                        .color(item.getColor())
                        .build();
                subjectRepository.save(subject);
                created++;
            } catch (Exception e) {
                errors.add("[" + (i + 1) + "] " + e.getMessage());
            }
        }

        return BulkCreateResponse.builder()
                .created(created)
                .skipped(skipped)
                .errors(errors)
                .build();
    }

    @Transactional
    public SubjectDto updateSubject(UUID subjectId, UUID userId, UpdateSubjectRequest request,
                                     AcceptLanguage language) {
        Subject subject = findSubjectForUser(subjectId, userId);

        if (request.getName() != null) {
            Map<String, String> cleanedName = TranslatedField.clean(request.getName());
            String newDefaultName = TranslatedField.defaultValue(cleanedName);
            String currentDefaultName = TranslatedField.defaultValue(subject.getName());
            if (newDefaultName != null && !newDefaultName.equalsIgnoreCase(currentDefaultName) &&
                    subjectRepository.existsByUserIdAndDefaultName(userId, newDefaultName)) {
                throw new BusinessException(messageService.get("subject.name.exists", language.toLocale()), HttpStatus.CONFLICT);
            }
            subject.setName(TranslatedField.merge(subject.getName(), cleanedName));
        }
        if (request.getDescription() != null) {
            subject.setDescription(TranslatedField.merge(subject.getDescription(), TranslatedField.clean(request.getDescription())));
        }
        if (request.getIcon() != null) {
            subject.setIcon(request.getIcon());
        }
        if (request.getColor() != null) {
            subject.setColor(request.getColor());
        }
        if (request.getIsActive() != null) {
            subject.setIsActive(request.getIsActive());
        }
        if (request.getGradeLevel() != null) {
            subject.setGradeLevel(request.getGradeLevel());
        }

        subject = subjectRepository.save(subject);

        auditService.log(userId, null, "SUBJECT_UPDATED", "CONTENT",
                "Subject", subject.getId());

        return mapToDto(subject, language.toLocaleKey());
    }

    @Transactional
    public void deleteSubject(UUID subjectId, UUID userId, AcceptLanguage language) {
        Subject subject = findSubjectForUser(subjectId, userId);

        if (subject.getQuestionCount() > 0) {
            throw new BusinessException(messageService.get("subject.has.questions", language.toLocale()));
        }

        topicRepository.deleteAllBySubjectId(subjectId);
        subjectRepository.delete(subject);

        auditService.log(userId, null, "SUBJECT_DELETED", "CONTENT",
                "Subject", subjectId);
    }

    @Transactional
    public SubjectDto archiveSubject(UUID subjectId, UUID userId, AcceptLanguage language) {
        Subject subject = findSubjectForUser(subjectId, userId);
        subject.setIsArchived(true);
        subject.setIsActive(false);
        subject = subjectRepository.save(subject);

        auditService.log(userId, null, "SUBJECT_ARCHIVED", "CONTENT",
                "Subject", subjectId);

        return mapToDto(subject, language.toLocaleKey());
    }

    @Transactional
    public SubjectDto restoreSubject(UUID subjectId, UUID userId, AcceptLanguage language) {
        Subject subject = subjectRepository.findByIdAndUserId(subjectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", subjectId));

        subject.setIsArchived(false);
        subject.setIsActive(true);
        subject = subjectRepository.save(subject);

        auditService.log(userId, null, "SUBJECT_RESTORED", "CONTENT",
                "Subject", subjectId);

        return mapToDto(subject, language.toLocaleKey());
    }

    @Transactional
    public void updateSubjectCounters(UUID subjectId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", subjectId));

        long topicCount = topicRepository.countBySubjectId(subjectId);
        subject.setTopicCount((int) topicCount);

        Integer questionCount = topicRepository.sumQuestionCountBySubjectId(subjectId).orElse(0);
        subject.setQuestionCount(questionCount);

        subjectRepository.save(subject);
    }

    private Subject findSubjectForUser(UUID subjectId, UUID userId) {
        return subjectRepository.findAccessibleByIdAndUserId(subjectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", subjectId));
    }

    public SubjectDto mapToDto(Subject subject, String localeKey) {
        return SubjectDto.builder()
                .id(subject.getId())
                .userId(subject.getUser() != null ? subject.getUser().getId() : null)
                .name(TranslatedField.resolve(subject.getName(), localeKey))
                .description(TranslatedField.resolve(subject.getDescription(), localeKey))
                .nameTranslations(TranslatedField.clean(subject.getName()))
                .descriptionTranslations(TranslatedField.clean(subject.getDescription()))
                .icon(subject.getIcon())
                .color(subject.getColor())
                .isTemplate(subject.getIsTemplate())
                .templateId(subject.getTemplateId())
                .isActive(subject.getIsActive())
                .isArchived(subject.getIsArchived())
                .gradeLevel(subject.getGradeLevel())
                .topicCount(subject.getTopicCount())
                .questionCount(subject.getQuestionCount())
                .testCount(subject.getTestCount())
                .sortOrder(subject.getSortOrder())
                .createdAt(subject.getCreatedAt())
                .updatedAt(subject.getUpdatedAt())
                .build();
    }
}
