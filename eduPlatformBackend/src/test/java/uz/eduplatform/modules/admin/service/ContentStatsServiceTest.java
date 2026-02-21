package uz.eduplatform.modules.admin.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import uz.eduplatform.modules.admin.dto.ContentStatsDto;
import uz.eduplatform.modules.content.domain.Subject;
import uz.eduplatform.modules.content.repository.QuestionRepository;
import uz.eduplatform.modules.content.repository.SubjectRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentStatsServiceTest {

    @Mock private SubjectRepository subjectRepository;
    @Mock private QuestionRepository questionRepository;

    @InjectMocks
    private ContentStatsService contentStatsService;

    @Test
    void getContentStats_returnsSubjectStats() {
        UUID subjectId = UUID.randomUUID();
        Subject subject = Subject.builder()
                .id(subjectId)
                .name(Map.of("uz_latn", "Matematika"))
                .topicCount(5)
                .questionCount(100)
                .testCount(10)
                .build();

        Page<Subject> page = new PageImpl<>(List.of(subject));
        when(subjectRepository.findByIsArchivedFalseOrderByQuestionCountDesc(any(Pageable.class)))
                .thenReturn(page);
        when(questionRepository.findTopTeachersByContentCreated(anyInt()))
                .thenReturn(Collections.emptyList());

        ContentStatsDto result = contentStatsService.getContentStats();

        assertNotNull(result);
        assertEquals(1, result.getSubjectStats().size());

        ContentStatsDto.SubjectStatsDto subjectStats = result.getSubjectStats().get(0);
        assertEquals(subjectId, subjectStats.getSubjectId());
        assertEquals("Matematika", subjectStats.getSubjectName());
        assertEquals(5, subjectStats.getTopicCount());
        assertEquals(100, subjectStats.getQuestionCount());
        assertEquals(10, subjectStats.getTestCount());
    }

    @Test
    void getContentStats_returnsTopTeachers() {
        Page<Subject> emptyPage = new PageImpl<>(List.of());
        when(subjectRepository.findByIsArchivedFalseOrderByQuestionCountDesc(any(Pageable.class)))
                .thenReturn(emptyPage);

        UUID teacherId = UUID.randomUUID();
        Object[] teacherRow = new Object[]{teacherId, "Ali", "Valiev", "ali@test.com", 50L, 3L};

        List<Object[]> teachers = new ArrayList<>();
        teachers.add(teacherRow);
        when(questionRepository.findTopTeachersByContentCreated(anyInt()))
                .thenReturn(teachers);

        ContentStatsDto result = contentStatsService.getContentStats();

        assertNotNull(result);
        assertEquals(1, result.getTopTeachers().size());

        ContentStatsDto.TopTeacherDto teacher = result.getTopTeachers().get(0);
        assertEquals(teacherId, teacher.getUserId());
        assertEquals("Ali", teacher.getFirstName());
        assertEquals("Valiev", teacher.getLastName());
        assertEquals("ali@test.com", teacher.getEmail());
        assertEquals(50L, teacher.getQuestionCount());
        assertEquals(3L, teacher.getSubjectCount());
    }
}
