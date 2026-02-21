package uz.eduplatform.modules.test.service.export;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.eduplatform.core.common.exception.BusinessException;
import uz.eduplatform.modules.test.domain.TestHistory;
import uz.eduplatform.modules.test.service.ExportHelper;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TestExportFacade {

    private final Map<ExportFormat, TestExportService> exportServices;
    private final ExportHelper exportHelper;

    public TestExportFacade(List<TestExportService> services, ExportHelper exportHelper) {
        this.exportServices = services.stream()
                .collect(Collectors.toMap(TestExportService::getFormat, Function.identity()));
        this.exportHelper = exportHelper;
    }

    public byte[] exportTest(UUID testId, UUID userId, ExportFormat format, Locale locale) {
        TestHistory test = exportHelper.getTestHistory(testId, userId);
        byte[] result = getService(format).exportTest(test, format, locale);
        exportHelper.updateDownloadCount(test);
        return result;
    }

    public byte[] exportAnswerKey(UUID testId, UUID userId, ExportFormat format, Locale locale) {
        TestHistory test = exportHelper.getTestHistory(testId, userId);
        return getService(format).exportAnswerKey(test, format, locale);
    }

    public byte[] exportCombined(UUID testId, UUID userId, ExportFormat format, Locale locale) {
        TestHistory test = exportHelper.getTestHistory(testId, userId);
        byte[] result = getService(format).exportCombined(test, format, locale);
        exportHelper.updateDownloadCount(test);
        return result;
    }

    public byte[] exportProofs(UUID testId, UUID userId, ExportFormat format, Locale locale) {
        TestHistory test = exportHelper.getTestHistory(testId, userId);
        return getService(format).exportProofs(test, format, locale);
    }

    private TestExportService getService(ExportFormat format) {
        TestExportService service = exportServices.get(format);
        if (service == null) {
            throw BusinessException.ofKey("test.export.unsupported.format", format);
        }
        return service;
    }
}
