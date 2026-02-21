package uz.eduplatform.modules.test.service.export;

import uz.eduplatform.modules.test.domain.TestHistory;

import java.util.Locale;

public interface TestExportService {

    byte[] exportTest(TestHistory test, ExportFormat format, Locale locale);

    byte[] exportAnswerKey(TestHistory test, ExportFormat format, Locale locale);

    byte[] exportCombined(TestHistory test, ExportFormat format, Locale locale);

    byte[] exportProofs(TestHistory test, ExportFormat format, Locale locale);

    ExportFormat getFormat();
}
