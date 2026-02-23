package uz.eduplatform.modules.content.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.eduplatform.core.audit.AuditService;
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
public class TopicService {

    private final TopicRepository topicRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final SubjectService subjectService;
    private final AuditService auditService;
    private final MessageService messageService;

    @Transactional(readOnly = true)
    public List<TopicTreeDto> getTopicTree(UUID subjectId, UUID userId, Integer gradeLevel, AcceptLanguage language) {
        // Verify subject exists (no ownership check — anyone can access)
        subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", subjectId));

        String localeKey = language.toLocaleKey();
        List<Topic> rootTopics = topicRepository
                .findBySubjectIdAndGradeLevelAndUserIdAndParentIsNullOrderBySortOrderAsc(subjectId, gradeLevel, userId);
        return rootTopics.stream()
                .map(t -> buildTreeDto(t, localeKey))
                .toList();
    }

    @Transactional
    public TopicDto createTopic(UUID subjectId, UUID userId, CreateTopicRequest request, AcceptLanguage language) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", subjectId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Topic parent = null;
        int level = 1;
        Integer gradeLevel = request.getGradeLevel();

        if (request.getParentId() != null) {
            parent = topicRepository.findByIdAndSubjectId(request.getParentId(), subjectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Parent topic", "id", request.getParentId()));

            level = parent.getLevel() + 1;
            // Child topics inherit gradeLevel from parent
            gradeLevel = parent.getGradeLevel();

            if (level > Topic.MAX_DEPTH) {
                throw new BusinessException(messageService.get("topic.max.depth", language.toLocale()));
            }
        }

        int sortOrder;
        if (parent != null) {
            sortOrder = topicRepository.findMaxSortOrderBySubjectAndUserAndParent(subjectId, userId, parent.getId())
                    .orElse(-1) + 1;
        } else {
            sortOrder = topicRepository.findMaxSortOrderBySubjectAndUserAndGradeLevelAndParentIsNull(subjectId, userId, gradeLevel)
                    .orElse(-1) + 1;
        }

        // Clean locale keys (uzl -> uz_latn, uzc -> uz_cyrl)
        Map<String, String> cleanedName = TranslatedField.clean(request.getName());
        Map<String, String> cleanedDesc = TranslatedField.clean(request.getDescription());

        // Duplicate name check (scoped to user)
        String defaultName = TranslatedField.defaultValue(cleanedName);
        UUID parentId = parent != null ? parent.getId() : null;
        if (defaultName != null && topicRepository.existsBySubjectIdAndUserIdAndParentIdAndDefaultName(subjectId, userId, parentId, defaultName)) {
            throw new BusinessException(messageService.get("topic.name.exists", language.toLocale()));
        }

        Topic topic = Topic.builder()
                .subject(subject)
                .user(user)
                .gradeLevel(gradeLevel)
                .parent(parent)
                .name(cleanedName)
                .description(cleanedDesc)
                .level(level)
                .sortOrder(sortOrder)
                .build();

        topic = topicRepository.save(topic);

        // Build materialized path
        String path = parent != null
                ? parent.getPath() + "." + topic.getId()
                : topic.getId().toString();
        topic.setPath(path);
        topic = topicRepository.save(topic);

        // Update subject counters
        subjectService.updateSubjectCounters(subjectId);

        auditService.log(userId, null, "TOPIC_CREATED", "CONTENT",
                "Topic", topic.getId());

        return mapToDto(topic, language.toLocaleKey());
    }

    @Transactional
    public BulkCreateResponse createTopicsBulk(UUID subjectId, UUID userId, BulkCreateTopicRequest request, AcceptLanguage language) {
        int created = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", subjectId));

        for (int i = 0; i < request.getItems().size(); i++) {
            CreateTopicRequest item = request.getItems().get(i);
            try {
                Map<String, String> cleanedName = TranslatedField.clean(item.getName());
                String defaultName = TranslatedField.defaultValue(cleanedName);
                UUID parentId = item.getParentId();
                if (defaultName != null && topicRepository.existsBySubjectIdAndUserIdAndParentIdAndDefaultName(subjectId, userId, parentId, defaultName)) {
                    if (request.isSkipDuplicates()) {
                        skipped++;
                        continue;
                    }
                    errors.add("[" + (i + 1) + "] " + defaultName + ": " + messageService.get("topic.name.exists", language.toLocale()));
                    continue;
                }
                createTopic(subjectId, userId, item, language);
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
    public TopicDto updateTopic(UUID topicId, UUID userId, UpdateTopicRequest request, AcceptLanguage language) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic", "id", topicId));

        // Verify ownership via topic's user
        if (!topic.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Topic", "id", topicId);
        }

        UUID subjectId = topic.getSubject().getId();

        if (request.getName() != null) {
            Map<String, String> cleanedName = TranslatedField.clean(request.getName());
            String newDefaultName = TranslatedField.defaultValue(cleanedName);
            String currentDefaultName = TranslatedField.defaultValue(topic.getName());
            UUID parentId = topic.getParent() != null ? topic.getParent().getId() : null;
            if (newDefaultName != null && !newDefaultName.equals(currentDefaultName) &&
                    topicRepository.existsBySubjectIdAndUserIdAndParentIdAndDefaultName(subjectId, userId, parentId, newDefaultName)) {
                throw new BusinessException(messageService.get("topic.name.exists", language.toLocale()));
            }
            topic.setName(TranslatedField.merge(topic.getName(), cleanedName));
        }
        if (request.getDescription() != null) {
            topic.setDescription(TranslatedField.merge(topic.getDescription(), TranslatedField.clean(request.getDescription())));
        }
        if (request.getIsActive() != null) {
            topic.setIsActive(request.getIsActive());
        }

        topic = topicRepository.save(topic);

        auditService.log(userId, null, "TOPIC_UPDATED", "CONTENT",
                "Topic", topicId);

        return mapToDto(topic, language.toLocaleKey());
    }

    @Transactional
    public void deleteTopic(UUID topicId, UUID userId, AcceptLanguage language) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic", "id", topicId));

        // Verify ownership via topic's user
        if (!topic.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Topic", "id", topicId);
        }

        // Check for questions in this topic and its children
        int totalQuestions = countQuestionsRecursive(topic);
        if (totalQuestions > 0) {
            throw new BusinessException(messageService.get("topic.has.questions", language.toLocale()));
        }

        UUID subjectId = topic.getSubject().getId();
        topicRepository.delete(topic); // Cascade deletes children

        // Update subject counters
        subjectService.updateSubjectCounters(subjectId);

        auditService.log(userId, null, "TOPIC_DELETED", "CONTENT",
                "Topic", topicId);
    }

    @Transactional
    public void reorderTopics(UUID userId, ReorderTopicsRequest request) {
        for (ReorderTopicsRequest.TopicOrderItem item : request.getItems()) {
            Topic topic = topicRepository.findById(item.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Topic", "id", item.getId()));

            // Verify ownership via topic's user
            if (!topic.getUser().getId().equals(userId)) {
                throw new ResourceNotFoundException("Topic", "id", item.getId());
            }

            topic.setSortOrder(item.getSortOrder());
            topicRepository.save(topic);
        }

        auditService.log(userId, null, "TOPICS_REORDERED", "CONTENT");
    }

    @Transactional
    public TopicDto moveTopic(UUID topicId, UUID userId, MoveTopicRequest request, AcceptLanguage language) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic", "id", topicId));

        // Verify ownership via topic's user
        if (!topic.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Topic", "id", topicId);
        }

        UUID oldSubjectId = topic.getSubject().getId();

        // If moving to different subject
        if (request.getNewSubjectId() != null && !request.getNewSubjectId().equals(oldSubjectId)) {
            Subject newSubject = subjectRepository.findById(request.getNewSubjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Target subject", "id", request.getNewSubjectId()));
            topic.setSubject(newSubject);
        }

        // If changing parent
        if (request.getNewParentId() != null) {
            Topic newParent = topicRepository.findById(request.getNewParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("New parent topic", "id", request.getNewParentId()));

            // Prevent circular reference
            if (isDescendant(topic, newParent)) {
                throw new BusinessException(messageService.get("topic.circular.reference", language.toLocale()));
            }

            int newLevel = newParent.getLevel() + 1;
            int depthBelow = getMaxDepthBelow(topic);

            if (newLevel + depthBelow - 1 > Topic.MAX_DEPTH) {
                throw new BusinessException(messageService.get("topic.max.depth", language.toLocale()));
            }

            topic.setParent(newParent);
            updateLevelsRecursive(topic, newLevel);
        } else {
            // Moving to root
            topic.setParent(null);
            updateLevelsRecursive(topic, 1);
        }

        // Rebuild path
        rebuildPathRecursive(topic);

        topic = topicRepository.save(topic);

        // Update counters for affected subjects
        subjectService.updateSubjectCounters(oldSubjectId);
        if (request.getNewSubjectId() != null && !request.getNewSubjectId().equals(oldSubjectId)) {
            subjectService.updateSubjectCounters(request.getNewSubjectId());
        }

        auditService.log(userId, null, "TOPIC_MOVED", "CONTENT",
                "Topic", topicId);

        return mapToDto(topic, language.toLocaleKey());
    }

    private boolean isDescendant(Topic ancestor, Topic potentialDescendant) {
        if (potentialDescendant.getId().equals(ancestor.getId())) {
            return true;
        }
        Topic current = potentialDescendant.getParent();
        while (current != null) {
            if (current.getId().equals(ancestor.getId())) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private int getMaxDepthBelow(Topic topic) {
        if (topic.getChildren() == null || topic.getChildren().isEmpty()) {
            return 1;
        }
        int max = 0;
        for (Topic child : topic.getChildren()) {
            max = Math.max(max, getMaxDepthBelow(child));
        }
        return max + 1;
    }

    private void updateLevelsRecursive(Topic topic, int newLevel) {
        topic.setLevel(newLevel);
        topicRepository.save(topic);
        if (topic.getChildren() != null) {
            for (Topic child : topic.getChildren()) {
                updateLevelsRecursive(child, newLevel + 1);
            }
        }
    }

    private void rebuildPathRecursive(Topic topic) {
        String path = topic.getParent() != null
                ? topic.getParent().getPath() + "." + topic.getId()
                : topic.getId().toString();
        topic.setPath(path);
        topicRepository.save(topic);

        if (topic.getChildren() != null) {
            for (Topic child : topic.getChildren()) {
                rebuildPathRecursive(child);
            }
        }
    }

    private int countQuestionsRecursive(Topic topic) {
        int count = topic.getQuestionCount() != null ? topic.getQuestionCount() : 0;
        if (topic.getChildren() != null) {
            for (Topic child : topic.getChildren()) {
                count += countQuestionsRecursive(child);
            }
        }
        return count;
    }

    private TopicTreeDto buildTreeDto(Topic topic, String localeKey) {
        TopicTreeDto dto = TopicTreeDto.builder()
                .id(topic.getId())
                .subjectId(topic.getSubject().getId())
                .parentId(topic.getParent() != null ? topic.getParent().getId() : null)
                .userId(topic.getUser().getId())
                .gradeLevel(topic.getGradeLevel())
                .name(TranslatedField.resolve(topic.getName(), localeKey))
                .description(TranslatedField.resolve(topic.getDescription(), localeKey))
                .nameTranslations(TranslatedField.clean(topic.getName()))
                .descriptionTranslations(TranslatedField.clean(topic.getDescription()))
                .level(topic.getLevel())
                .questionCount(topic.getQuestionCount())
                .sortOrder(topic.getSortOrder())
                .isActive(topic.getIsActive())
                .children(new ArrayList<>())
                .build();

        if (topic.getChildren() != null && !topic.getChildren().isEmpty()) {
            List<TopicTreeDto> children = topic.getChildren().stream()
                    .map(t -> buildTreeDto(t, localeKey))
                    .toList();
            dto.setChildren(new ArrayList<>(children));
        }

        return dto;
    }

    public TopicDto mapToDto(Topic topic, String localeKey) {
        return TopicDto.builder()
                .id(topic.getId())
                .subjectId(topic.getSubject().getId())
                .parentId(topic.getParent() != null ? topic.getParent().getId() : null)
                .userId(topic.getUser().getId())
                .gradeLevel(topic.getGradeLevel())
                .name(TranslatedField.resolve(topic.getName(), localeKey))
                .description(TranslatedField.resolve(topic.getDescription(), localeKey))
                .nameTranslations(TranslatedField.clean(topic.getName()))
                .descriptionTranslations(TranslatedField.clean(topic.getDescription()))
                .level(topic.getLevel())
                .path(topic.getPath())
                .isActive(topic.getIsActive())
                .questionCount(topic.getQuestionCount())
                .sortOrder(topic.getSortOrder())
                .createdAt(topic.getCreatedAt())
                .updatedAt(topic.getUpdatedAt())
                .build();
    }
}
