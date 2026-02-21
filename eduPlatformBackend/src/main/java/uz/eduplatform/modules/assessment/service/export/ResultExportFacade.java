package uz.eduplatform.modules.assessment.service.export;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.eduplatform.core.common.exception.BusinessException;
import uz.eduplatform.modules.assessment.dto.AssignmentResultDto;
import uz.eduplatform.modules.assessment.service.ResultService;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ResultExportFacade {

    private final Map<ResultExportFormat, ResultExportService> exportServices;
    private final ResultService resultService;

    public ResultExportFacade(List<ResultExportService> services, ResultService resultService) {
        this.exportServices = services.stream()
                .collect(Collectors.toMap(ResultExportService::getFormat, Function.identity()));
        this.resultService = resultService;
    }

    public byte[] exportAssignmentResults(UUID assignmentId, UUID teacherId,
                                          ResultExportFormat format, Locale locale) {
        AssignmentResultDto results = resultService.getAssignmentResults(assignmentId, teacherId);
        return getService(format).exportResults(results, locale);
    }

    private ResultExportService getService(ResultExportFormat format) {
        ResultExportService service = exportServices.get(format);
        if (service == null) {
            throw BusinessException.ofKey("result.export.unsupported.format", format);
        }
        return service;
    }
}
