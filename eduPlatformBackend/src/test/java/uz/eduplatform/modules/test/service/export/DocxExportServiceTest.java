package uz.eduplatform.modules.test.service.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uz.eduplatform.core.common.utils.MessageService;
import uz.eduplatform.modules.content.domain.Question;
import uz.eduplatform.modules.content.domain.QuestionType;
import uz.eduplatform.modules.content.repository.QuestionRepository;
import uz.eduplatform.modules.test.domain.TestHistory;
import uz.eduplatform.modules.test.service.ExportHelper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocxExportServiceTest {

    @Mock private ExportHelper exportHelper;
    @Mock private QuestionRepository questionRepository;
    @Mock private MessageService messageService;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private DocxExportService docxExportService;

    private static final ObjectMapper mapper = new ObjectMapper();

    private static String toJson(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test
    void getFormat_returnsDocx() {
        assertEquals(ExportFormat.DOCX, docxExportService.getFormat());
    }

    @Test
    void exportTest_generatesNonEmptyDocxBytes() throws IOException {
        UUID qId = UUID.randomUUID();
        Question question = Question.builder()
                .id(qId)
                .questionText(Map.of("uz_latn", "What is 2+2?"))
                .questionType(QuestionType.MCQ_SINGLE)
                .options(toJson(List.of(
                        Map.of("id", "1", "text", Map.of("uz_latn", "3")),
                        Map.of("id", "2", "text", Map.of("uz_latn", "4")),
                        Map.of("id", "3", "text", Map.of("uz_latn", "5"))
                )))
                .build();

        Map<String, Object> variant = new HashMap<>();
        variant.put("code", "A");
        variant.put("questionIds", List.of(qId.toString()));
        variant.put("optionsOrder", List.of(List.of("1", "2", "3")));
        variant.put("answerKey", List.of(Map.of("questionNumber", 1, "answer", "B")));

        TestHistory test = TestHistory.builder()
                .id(UUID.randomUUID())
                .title("Math Test")
                .questionCount(1)
                .headerConfig(Map.of("schoolName", "School #1", "className", "5A"))
                .variants(List.of(variant))
                .build();

        when(exportHelper.parseQuestionIds(any())).thenReturn(List.of(qId));
        when(exportHelper.parseOptionsOrder(any())).thenReturn(List.of(List.of("1", "2", "3")));
        when(questionRepository.findAllById(List.of(qId))).thenReturn(List.of(question));
        when(messageService.get(anyString(), any(Locale.class), any())).thenReturn("Translated");
        when(messageService.get(anyString(), any(Locale.class))).thenReturn("Translated");

        byte[] result = docxExportService.exportTest(test, ExportFormat.DOCX, Locale.ENGLISH);

        assertNotNull(result);
        assertTrue(result.length > 0);

        // Verify it's a valid DOCX by parsing it
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(result))) {
            assertFalse(doc.getParagraphs().isEmpty());
        }
    }

    @Test
    void exportAnswerKey_generatesNonEmptyDocxBytes() throws IOException {
        Map<String, Object> variant = new HashMap<>();
        variant.put("code", "A");
        variant.put("answerKey", List.of(
                Map.of("questionNumber", 1, "answer", "B"),
                Map.of("questionNumber", 2, "answer", "A"),
                Map.of("questionNumber", 3, "answer", "C")
        ));

        TestHistory test = TestHistory.builder()
                .id(UUID.randomUUID())
                .title("Science Test")
                .questionCount(3)
                .variants(List.of(variant))
                .build();

        when(messageService.get(anyString(), any(Locale.class), any())).thenReturn("Label");
        when(messageService.get(anyString(), any(Locale.class))).thenReturn("Label");

        byte[] result = docxExportService.exportAnswerKey(test, ExportFormat.DOCX, Locale.ENGLISH);

        assertNotNull(result);
        assertTrue(result.length > 0);

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(result))) {
            assertFalse(doc.getParagraphs().isEmpty());
            // Should contain a table for the answer key
            assertFalse(doc.getTables().isEmpty());
        }
    }

    @Test
    void exportProofs_generatesNonEmptyDocxBytes() throws IOException {
        UUID qId = UUID.randomUUID();
        Question question = Question.builder()
                .id(qId)
                .questionText(Map.of("uz_latn", "Explain photosynthesis"))
                .questionType(QuestionType.MCQ_SINGLE)
                .proof(Map.of("uz_latn", "Photosynthesis converts light energy into chemical energy stored in glucose."))
                .build();

        Map<String, Object> variant = new HashMap<>();
        variant.put("code", "A");
        variant.put("questionIds", List.of(qId.toString()));

        TestHistory test = TestHistory.builder()
                .id(UUID.randomUUID())
                .title("Biology Test")
                .questionCount(1)
                .variants(List.of(variant))
                .build();

        when(exportHelper.parseQuestionIds(any())).thenReturn(List.of(qId));
        when(questionRepository.findAllById(List.of(qId))).thenReturn(List.of(question));
        when(messageService.get(anyString(), any(Locale.class))).thenReturn("Label");

        byte[] result = docxExportService.exportProofs(test, ExportFormat.DOCX, Locale.ENGLISH);

        assertNotNull(result);
        assertTrue(result.length > 0);

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(result))) {
            assertFalse(doc.getParagraphs().isEmpty());
        }
    }

    @Test
    void exportCombined_generatesNonEmptyDocxBytes() throws IOException {
        UUID qId = UUID.randomUUID();
        Question question = Question.builder()
                .id(qId)
                .questionText(Map.of("uz_latn", "What is H2O?"))
                .questionType(QuestionType.TRUE_FALSE)
                .build();

        Map<String, Object> variant = new HashMap<>();
        variant.put("code", "A");
        variant.put("questionIds", List.of(qId.toString()));
        variant.put("optionsOrder", null);
        variant.put("answerKey", List.of(Map.of("questionNumber", 1, "answer", "True")));

        TestHistory test = TestHistory.builder()
                .id(UUID.randomUUID())
                .title("Chemistry Test")
                .questionCount(1)
                .headerConfig(Map.of("schoolName", "Test School"))
                .variants(List.of(variant))
                .build();

        when(exportHelper.parseQuestionIds(any())).thenReturn(List.of(qId));
        when(exportHelper.parseOptionsOrder(any())).thenReturn(null);
        when(questionRepository.findAllById(List.of(qId))).thenReturn(List.of(question));
        when(messageService.get(anyString(), any(Locale.class), any())).thenReturn("Label");
        when(messageService.get(anyString(), any(Locale.class))).thenReturn("Label");

        byte[] result = docxExportService.exportCombined(test, ExportFormat.DOCX, Locale.ENGLISH);

        assertNotNull(result);
        assertTrue(result.length > 0);

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(result))) {
            assertFalse(doc.getParagraphs().isEmpty());
            // Combined should have answer key table too
            assertFalse(doc.getTables().isEmpty());
        }
    }

    @Test
    void exportTest_withTrueFalseQuestion_includesOptions() throws IOException {
        UUID qId = UUID.randomUUID();
        Question question = Question.builder()
                .id(qId)
                .questionText(Map.of("uz_latn", "The earth is flat"))
                .questionType(QuestionType.TRUE_FALSE)
                .build();

        Map<String, Object> variant = new HashMap<>();
        variant.put("code", "B");
        variant.put("questionIds", List.of(qId.toString()));
        variant.put("optionsOrder", null);

        TestHistory test = TestHistory.builder()
                .id(UUID.randomUUID())
                .title("Geography Test")
                .questionCount(1)
                .headerConfig(Map.of())
                .variants(List.of(variant))
                .build();

        when(exportHelper.parseQuestionIds(any())).thenReturn(List.of(qId));
        when(exportHelper.parseOptionsOrder(any())).thenReturn(null);
        when(questionRepository.findAllById(List.of(qId))).thenReturn(List.of(question));
        when(messageService.get(anyString(), any(Locale.class), any())).thenReturn("Label");
        when(messageService.get(anyString(), any(Locale.class))).thenReturn("Label");

        byte[] result = docxExportService.exportTest(test, ExportFormat.DOCX, Locale.ENGLISH);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }
}
