package uz.eduplatform.modules.test.service.export;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uz.eduplatform.core.common.exception.BusinessException;
import uz.eduplatform.modules.test.domain.TestHistory;
import uz.eduplatform.modules.test.service.ExportHelper;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestExportFacadeTest {

    @Mock private ExportHelper exportHelper;
    @Mock private TestExportService pdfService;
    @Mock private TestExportService docxService;

    private TestExportFacade facade;

    @BeforeEach
    void setUp() {
        when(pdfService.getFormat()).thenReturn(ExportFormat.PDF);
        when(docxService.getFormat()).thenReturn(ExportFormat.DOCX);
        facade = new TestExportFacade(List.of(pdfService, docxService), exportHelper);
    }

    @Test
    void exportTest_routesToPdfService() {
        UUID testId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TestHistory test = TestHistory.builder().id(testId).userId(userId).downloadCount(0).build();
        byte[] expectedBytes = new byte[]{1, 2, 3};

        when(exportHelper.getTestHistory(testId, userId)).thenReturn(test);
        when(pdfService.exportTest(test, ExportFormat.PDF, Locale.ENGLISH)).thenReturn(expectedBytes);

        byte[] result = facade.exportTest(testId, userId, ExportFormat.PDF, Locale.ENGLISH);

        assertArrayEquals(expectedBytes, result);
        verify(pdfService).exportTest(test, ExportFormat.PDF, Locale.ENGLISH);
        verify(docxService, never()).exportTest(any(), any(), any());
    }

    @Test
    void exportTest_routesToDocxService() {
        UUID testId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TestHistory test = TestHistory.builder().id(testId).userId(userId).downloadCount(0).build();
        byte[] expectedBytes = new byte[]{4, 5, 6};

        when(exportHelper.getTestHistory(testId, userId)).thenReturn(test);
        when(docxService.exportTest(test, ExportFormat.DOCX, Locale.ENGLISH)).thenReturn(expectedBytes);

        byte[] result = facade.exportTest(testId, userId, ExportFormat.DOCX, Locale.ENGLISH);

        assertArrayEquals(expectedBytes, result);
        verify(docxService).exportTest(test, ExportFormat.DOCX, Locale.ENGLISH);
        verify(pdfService, never()).exportTest(any(), any(), any());
    }

    @Test
    void exportTest_updatesDownloadCount() {
        UUID testId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TestHistory test = TestHistory.builder().id(testId).userId(userId).downloadCount(5).build();

        when(exportHelper.getTestHistory(testId, userId)).thenReturn(test);
        when(pdfService.exportTest(any(), any(), any())).thenReturn(new byte[]{1});

        facade.exportTest(testId, userId, ExportFormat.PDF, Locale.ENGLISH);

        verify(exportHelper).updateDownloadCount(test);
    }

    @Test
    void exportAnswerKey_doesNotUpdateDownloadCount() {
        UUID testId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TestHistory test = TestHistory.builder().id(testId).userId(userId).build();

        when(exportHelper.getTestHistory(testId, userId)).thenReturn(test);
        when(pdfService.exportAnswerKey(any(), any(), any())).thenReturn(new byte[]{1});

        facade.exportAnswerKey(testId, userId, ExportFormat.PDF, Locale.ENGLISH);

        verify(exportHelper, never()).updateDownloadCount(any());
    }

    @Test
    void exportCombined_updatesDownloadCount() {
        UUID testId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TestHistory test = TestHistory.builder().id(testId).userId(userId).downloadCount(0).build();

        when(exportHelper.getTestHistory(testId, userId)).thenReturn(test);
        when(docxService.exportCombined(any(), any(), any())).thenReturn(new byte[]{1});

        facade.exportCombined(testId, userId, ExportFormat.DOCX, Locale.ENGLISH);

        verify(exportHelper).updateDownloadCount(test);
    }

    @Test
    void exportProofs_doesNotUpdateDownloadCount() {
        UUID testId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TestHistory test = TestHistory.builder().id(testId).userId(userId).build();

        when(exportHelper.getTestHistory(testId, userId)).thenReturn(test);
        when(docxService.exportProofs(any(), any(), any())).thenReturn(new byte[]{1});

        facade.exportProofs(testId, userId, ExportFormat.DOCX, Locale.ENGLISH);

        verify(exportHelper, never()).updateDownloadCount(any());
    }

    @Test
    void exportTest_loadsTestViaExportHelper() {
        UUID testId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TestHistory test = TestHistory.builder().id(testId).userId(userId).downloadCount(0).build();

        when(exportHelper.getTestHistory(testId, userId)).thenReturn(test);
        when(pdfService.exportTest(any(), any(), any())).thenReturn(new byte[]{1});

        facade.exportTest(testId, userId, ExportFormat.PDF, Locale.ENGLISH);

        verify(exportHelper).getTestHistory(testId, userId);
    }
}
