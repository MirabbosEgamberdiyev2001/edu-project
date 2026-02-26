package uz.eduplatform.modules.assessment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uz.eduplatform.core.common.exception.BusinessException;
import uz.eduplatform.modules.assessment.domain.AttemptStatus;
import uz.eduplatform.modules.assessment.dto.AssignmentResultDto;
import uz.eduplatform.modules.assessment.dto.StudentResultDto;
import uz.eduplatform.modules.assessment.service.export.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ResultExportFacadeTest {

    @Mock private ResultService resultService;

    private ResultExportFacade exportFacade;
    private CsvResultExportService csvService;
    private ExcelResultExportService excelService;

    private UUID assignmentId;
    private UUID teacherId;
    private AssignmentResultDto resultDto;

    @BeforeEach
    void setUp() {
        csvService = new CsvResultExportService();
        excelService = new ExcelResultExportService();
        exportFacade = new ResultExportFacade(List.of(csvService, excelService), resultService);

        assignmentId = UUID.randomUUID();
        teacherId = UUID.randomUUID();

        StudentResultDto student1 = StudentResultDto.builder()
                .studentId(UUID.randomUUID())
                .firstName("Jasur")
                .lastName("Toshmatov")
                .score(BigDecimal.valueOf(85))
                .maxScore(BigDecimal.valueOf(100))
                .percentage(BigDecimal.valueOf(85))
                .attemptCount(1)
                .tabSwitches(2)
                .status(AttemptStatus.AUTO_GRADED.name())
                .submittedAt(LocalDateTime.now())
                .build();

        StudentResultDto student2 = StudentResultDto.builder()
                .studentId(UUID.randomUUID())
                .firstName("Ali")
                .lastName("Valiyev")
                .score(BigDecimal.valueOf(70))
                .maxScore(BigDecimal.valueOf(100))
                .percentage(BigDecimal.valueOf(70))
                .attemptCount(1)
                .tabSwitches(0)
                .status(AttemptStatus.AUTO_GRADED.name())
                .submittedAt(LocalDateTime.now())
                .build();

        resultDto = AssignmentResultDto.builder()
                .assignmentId(assignmentId)
                .assignmentTitle("Math Test")
                .totalStudents(2)
                .completedStudents(2)
                .averageScore(BigDecimal.valueOf(77.5))
                .highestScore(BigDecimal.valueOf(85))
                .lowestScore(BigDecimal.valueOf(70))
                .students(List.of(student1, student2))
                .build();

        when(resultService.getAssignmentResults(any(), any())).thenReturn(resultDto);
    }

    @Test
    void exportCsv_returnsValidCsv() {
        byte[] data = exportFacade.exportAssignmentResults(assignmentId, teacherId,
                ResultExportFormat.CSV, Locale.ENGLISH);

        assertNotNull(data);
        assertTrue(data.length > 0);

        String csv = new String(data);
        assertTrue(csv.contains("Student Name"));
        assertTrue(csv.contains("Jasur Toshmatov"));
        assertTrue(csv.contains("Ali Valiyev"));
        assertTrue(csv.contains("85"));
    }

    @Test
    void exportExcel_returnsValidExcel() {
        byte[] data = exportFacade.exportAssignmentResults(assignmentId, teacherId,
                ResultExportFormat.EXCEL, Locale.ENGLISH);

        assertNotNull(data);
        assertTrue(data.length > 0);
        // XLSX magic bytes: PK (ZIP format)
        assertEquals(0x50, data[0] & 0xFF);
        assertEquals(0x4B, data[1] & 0xFF);
    }

    @Test
    void exportCsv_withBOM_forExcelCompatibility() {
        byte[] data = csvService.exportResults(resultDto, Locale.ENGLISH);

        String csv = new String(data);
        assertTrue(csv.startsWith("\uFEFF"), "CSV should start with BOM for Excel compatibility");
    }

    @Test
    void exportCsv_escapesCommasInNames() {
        StudentResultDto studentWithComma = StudentResultDto.builder()
                .studentId(UUID.randomUUID())
                .firstName("Last, First")
                .lastName("")
                .status(AttemptStatus.AUTO_GRADED.name())
                .tabSwitches(0)
                .build();

        AssignmentResultDto commaResult = AssignmentResultDto.builder()
                .assignmentId(assignmentId)
                .assignmentTitle("Test")
                .students(List.of(studentWithComma))
                .build();

        byte[] data = csvService.exportResults(commaResult, Locale.ENGLISH);
        String csv = new String(data);
        assertTrue(csv.contains("\"Last, First\""), "Name with comma should be quoted");
    }
}
