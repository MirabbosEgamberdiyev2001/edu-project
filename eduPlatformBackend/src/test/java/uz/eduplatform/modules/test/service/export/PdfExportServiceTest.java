package uz.eduplatform.modules.test.service.export;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdfExportServiceTest {

    @Mock private QuestionRepository questionRepository;
    @Mock private MessageService messageService;
    @Mock private ExportHelper exportHelper;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PdfExportService pdfExportService;

    private static final ObjectMapper mapper = new ObjectMapper();

    private static String toJson(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test
    void getFormat_returnsPdf() {
        assertEquals(ExportFormat.PDF, pdfExportService.getFormat());
    }

    @Test
    void exportTest_generatesNonEmptyPdfBytes() {
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
        when(exportHelper.wrapText(anyString(), anyInt())).thenAnswer(i -> List.of((String) i.getArgument(0)));
        when(exportHelper.truncateText(anyString(), anyInt())).thenAnswer(i -> i.getArgument(0));
        when(questionRepository.findAllById(List.of(qId))).thenReturn(List.of(question));
        when(messageService.get(anyString(), any(Locale.class), any())).thenReturn("Translated text");
        when(messageService.get(anyString(), any(Locale.class))).thenReturn("Translated text");

        byte[] result = pdfExportService.exportTest(test, ExportFormat.PDF, Locale.ENGLISH);

        assertNotNull(result);
        assertTrue(result.length > 0);
        // PDF files start with %PDF
        assertEquals('%', (char) result[0]);
        assertEquals('P', (char) result[1]);
        assertEquals('D', (char) result[2]);
        assertEquals('F', (char) result[3]);
    }

    @Test
    void exportAnswerKey_generatesNonEmptyPdfBytes() {
        Map<String, Object> variant = new HashMap<>();
        variant.put("code", "A");
        variant.put("answerKey", List.of(
                Map.of("questionNumber", 1, "answer", "B"),
                Map.of("questionNumber", 2, "answer", "A")
        ));

        TestHistory test = TestHistory.builder()
                .id(UUID.randomUUID())
                .title("Science Test")
                .questionCount(2)
                .variants(List.of(variant))
                .build();

        when(messageService.get(anyString(), any(Locale.class), any())).thenReturn("Label");
        when(messageService.get(anyString(), any(Locale.class))).thenReturn("Label");

        byte[] result = pdfExportService.exportAnswerKey(test, ExportFormat.PDF, Locale.ENGLISH);

        assertNotNull(result);
        assertTrue(result.length > 0);
        assertEquals('%', (char) result[0]);
    }

    @Test
    void exportProofs_generatesNonEmptyPdfBytes() {
        UUID qId = UUID.randomUUID();
        Question question = Question.builder()
                .id(qId)
                .questionText(Map.of("uz_latn", "Explain gravity"))
                .questionType(QuestionType.MCQ_SINGLE)
                .proof(Map.of("uz_latn", "Gravity is a fundamental force that attracts objects with mass."))
                .build();

        Map<String, Object> variant = new HashMap<>();
        variant.put("code", "A");
        variant.put("questionIds", List.of(qId.toString()));

        TestHistory test = TestHistory.builder()
                .id(UUID.randomUUID())
                .title("Physics Test")
                .questionCount(1)
                .variants(List.of(variant))
                .build();

        when(exportHelper.parseQuestionIds(any())).thenReturn(List.of(qId));
        when(exportHelper.wrapText(anyString(), anyInt())).thenAnswer(i -> List.of((String) i.getArgument(0)));
        when(exportHelper.truncateText(anyString(), anyInt())).thenAnswer(i -> i.getArgument(0));
        when(questionRepository.findAllById(List.of(qId))).thenReturn(List.of(question));
        when(messageService.get(anyString(), any(Locale.class))).thenReturn("Label");

        byte[] result = pdfExportService.exportProofs(test, ExportFormat.PDF, Locale.ENGLISH);

        assertNotNull(result);
        assertTrue(result.length > 0);
        assertEquals('%', (char) result[0]);
    }

    @Test
    void exportCombined_generatesNonEmptyPdfBytes() {
        UUID qId = UUID.randomUUID();
        Question question = Question.builder()
                .id(qId)
                .questionText(Map.of("uz_latn", "What is H2O?"))
                .questionType(QuestionType.MCQ_SINGLE)
                .options(toJson(List.of(
                        Map.of("id", "1", "text", Map.of("uz_latn", "Water")),
                        Map.of("id", "2", "text", Map.of("uz_latn", "Oxygen"))
                )))
                .build();

        Map<String, Object> variant = new HashMap<>();
        variant.put("code", "A");
        variant.put("questionIds", List.of(qId.toString()));
        variant.put("optionsOrder", List.of(List.of("1", "2")));
        variant.put("answerKey", List.of(Map.of("questionNumber", 1, "answer", "A")));

        TestHistory test = TestHistory.builder()
                .id(UUID.randomUUID())
                .title("Chemistry Test")
                .questionCount(1)
                .headerConfig(Map.of("schoolName", "Test School"))
                .variants(List.of(variant))
                .build();

        when(exportHelper.parseQuestionIds(any())).thenReturn(List.of(qId));
        when(exportHelper.parseOptionsOrder(any())).thenReturn(List.of(List.of("1", "2")));
        when(exportHelper.wrapText(anyString(), anyInt())).thenAnswer(i -> List.of((String) i.getArgument(0)));
        when(exportHelper.truncateText(anyString(), anyInt())).thenAnswer(i -> i.getArgument(0));
        when(questionRepository.findAllById(List.of(qId))).thenReturn(List.of(question));
        when(messageService.get(anyString(), any(Locale.class), any())).thenReturn("Label");
        when(messageService.get(anyString(), any(Locale.class))).thenReturn("Label");

        byte[] result = pdfExportService.exportCombined(test, ExportFormat.PDF, Locale.ENGLISH);

        assertNotNull(result);
        assertTrue(result.length > 0);
        // Combined should be larger than either individual export
        byte[] testOnly = pdfExportService.exportTest(test, ExportFormat.PDF, Locale.ENGLISH);
        assertTrue(result.length > testOnly.length);
    }
}
