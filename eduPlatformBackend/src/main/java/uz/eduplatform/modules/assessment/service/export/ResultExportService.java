package uz.eduplatform.modules.assessment.service.export;

import uz.eduplatform.modules.assessment.dto.AssignmentResultDto;

import java.util.Locale;

public interface ResultExportService {

    byte[] exportResults(AssignmentResultDto results, Locale locale);

    ResultExportFormat getFormat();
}
