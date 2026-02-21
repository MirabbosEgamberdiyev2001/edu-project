package uz.eduplatform.modules.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.eduplatform.core.i18n.TranslatedField;
import uz.eduplatform.modules.admin.dto.ContentStatsDto;
import uz.eduplatform.modules.content.domain.Subject;
import uz.eduplatform.modules.content.repository.QuestionRepository;
import uz.eduplatform.modules.content.repository.SubjectRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContentStatsService {

    private final SubjectRepository subjectRepository;
    private final QuestionRepository questionRepository;

    @Cacheable(value = "content_stats", key = "'global'")
    @Transactional(readOnly = true)
    public ContentStatsDto getContentStats() {
        List<Subject> topSubjects = subjectRepository.findByIsArchivedFalseOrderByQuestionCountDesc(
                PageRequest.of(0, 50)).getContent();

        List<ContentStatsDto.SubjectStatsDto> subjectStats = topSubjects.stream()
                .map(s -> ContentStatsDto.SubjectStatsDto.builder()
                        .subjectId(s.getId())
                        .subjectName(TranslatedField.resolve(s.getName()))
                        .topicCount(s.getTopicCount() != null ? s.getTopicCount() : 0)
                        .questionCount(s.getQuestionCount() != null ? s.getQuestionCount() : 0)
                        .testCount(s.getTestCount() != null ? s.getTestCount() : 0)
                        .build())
                .toList();

        List<Object[]> topTeachersRaw = questionRepository.findTopTeachersByContentCreated(10);
        List<ContentStatsDto.TopTeacherDto> topTeachers = topTeachersRaw.stream()
                .map(row -> ContentStatsDto.TopTeacherDto.builder()
                        .userId((UUID) row[0])
                        .firstName((String) row[1])
                        .lastName((String) row[2])
                        .email((String) row[3])
                        .questionCount(((Number) row[4]).longValue())
                        .subjectCount(((Number) row[5]).longValue())
                        .build())
                .toList();

        return ContentStatsDto.builder()
                .subjectStats(subjectStats)
                .topTeachers(topTeachers)
                .build();
    }
}
