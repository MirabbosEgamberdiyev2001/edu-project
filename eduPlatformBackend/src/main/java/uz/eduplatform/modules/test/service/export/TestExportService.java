package uz.eduplatform.modules.test.service.export;

import uz.eduplatform.modules.test.domain.TestHistory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

public interface TestExportService {

    byte[] exportTest(TestHistory test, ExportFormat format, Locale locale);

    byte[] exportAnswerKey(TestHistory test, ExportFormat format, Locale locale);

    byte[] exportCombined(TestHistory test, ExportFormat format, Locale locale);

    byte[] exportProofs(TestHistory test, ExportFormat format, Locale locale);

    /**
     * Stream export directly to the output stream, avoiding full byte[] buffering in memory.
     */
    default void exportTestToStream(TestHistory test, ExportFormat format, Locale locale, OutputStream out) throws IOException {
        out.write(exportTest(test, format, locale));
    }

    default void exportAnswerKeyToStream(TestHistory test, ExportFormat format, Locale locale, OutputStream out) throws IOException {
        out.write(exportAnswerKey(test, format, locale));
    }

    default void exportCombinedToStream(TestHistory test, ExportFormat format, Locale locale, OutputStream out) throws IOException {
        out.write(exportCombined(test, format, locale));
    }

    default void exportProofsToStream(TestHistory test, ExportFormat format, Locale locale, OutputStream out) throws IOException {
        out.write(exportProofs(test, format, locale));
    }

    ExportFormat getFormat();
}
