package uz.eduplatform.core.common.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.eduplatform.core.common.exception.ResourceNotFoundException;
import uz.eduplatform.modules.content.domain.Subject;
import uz.eduplatform.modules.content.domain.Topic;
import uz.eduplatform.modules.content.repository.QuestionRepository;
import uz.eduplatform.modules.content.repository.SubjectRepository;
import uz.eduplatform.modules.content.repository.TopicRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SoftDeleteCascadeService {

    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;

    @Transactional
    public void softDeleteSubject(UUID subjectId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", subjectId));

        LocalDateTime now = LocalDateTime.now();
        subject.setDeletedAt(now);
        subjectRepository.save(subject);

        List<Topic> topics = topicRepository.findBySubjectIdOrderBySortOrderAsc(subjectId);
        for (Topic topic : topics) {
            softDeleteTopicInternal(topic, now);
        }

        log.info("Soft-deleted subject {} with {} topics", subjectId, topics.size());
    }

    @Transactional
    public void softDeleteTopic(UUID topicId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic", "id", topicId));

        softDeleteTopicInternal(topic, LocalDateTime.now());
        log.info("Soft-deleted topic {}", topicId);
    }

    private void softDeleteTopicInternal(Topic topic, LocalDateTime now) {
        topic.setDeletedAt(now);
        topicRepository.save(topic);

        // Cascade to child topics
        List<Topic> children = topicRepository.findByParentIdOrderBySortOrderAsc(topic.getId());
        for (Topic child : children) {
            softDeleteTopicInternal(child, now);
        }

        // Cascade to questions via native query
        questionRepository.softDeleteByTopicId(topic.getId(), now);
    }
}
