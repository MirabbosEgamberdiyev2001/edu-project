package uz.eduplatform.modules.assessment.service.export;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.eduplatform.core.common.exception.ResourceNotFoundException;
import uz.eduplatform.modules.assessment.domain.ExportJob;
import uz.eduplatform.modules.assessment.domain.ExportJobStatus;
import uz.eduplatform.modules.assessment.dto.ExportJobDto;
import uz.eduplatform.modules.assessment.repository.ExportJobRepository;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncExportService {

    private static final int EXPORT_EXPIRY_HOURS = 1;

    private final ExportJobRepository exportJobRepository;
    private final ResultExportFacade resultExportFacade;

    @Transactional
    public ExportJobDto startExport(UUID assignmentId, UUID teacherId,
                                    ResultExportFormat format, Locale locale) {
        ExportJob job = ExportJob.builder()
                .assignmentId(assignmentId)
                .teacherId(teacherId)
                .format(format)
                .locale(locale != null ? locale.toLanguageTag() : "en")
                .status(ExportJobStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusHours(EXPORT_EXPIRY_HOURS))
                .build();

        job = exportJobRepository.save(job);
        log.info("Export job created: id={}, assignment={}, format={}", job.getId(), assignmentId, format);

        processExportAsync(job.getId());

        return toDto(job);
    }

    @Async("exportExecutor")
    public void processExportAsync(UUID jobId) {
        ExportJob job = exportJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            log.warn("Export job not found for async processing: {}", jobId);
            return;
        }

        job.setStatus(ExportJobStatus.PROCESSING);
        exportJobRepository.save(job);

        try {
            Locale locale = Locale.forLanguageTag(job.getLocale() != null ? job.getLocale() : "en");
            byte[] data = resultExportFacade.exportAssignmentResults(
                    job.getAssignmentId(), job.getTeacherId(), job.getFormat(), locale);

            String fileName = buildFileName(job);
            String contentType = resolveContentType(job.getFormat());

            job.setFileData(data);
            job.setFileName(fileName);
            job.setContentType(contentType);
            job.setStatus(ExportJobStatus.COMPLETED);
            job.setCompletedAt(LocalDateTime.now());

            exportJobRepository.save(job);
            log.info("Export job completed: id={}, size={} bytes", jobId, data.length);

        } catch (Exception e) {
            log.error("Export job failed: id={}", jobId, e);
            job.setStatus(ExportJobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setCompletedAt(LocalDateTime.now());
            exportJobRepository.save(job);
        }
    }

    @Transactional(readOnly = true)
    public ExportJobDto getJobStatus(UUID jobId, UUID teacherId) {
        ExportJob job = exportJobRepository.findByIdAndTeacherId(jobId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("ExportJob", "id", jobId));
        return toDto(job);
    }

    @Transactional(readOnly = true)
    public ExportJob getJobForDownload(UUID jobId, UUID teacherId) {
        ExportJob job = exportJobRepository.findByIdAndTeacherId(jobId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("ExportJob", "id", jobId));
        if (job.getStatus() != ExportJobStatus.COMPLETED) {
            throw new IllegalStateException("Export job is not completed yet. Current status: " + job.getStatus());
        }
        return job;
    }

    @Scheduled(fixedRate = 3600000) // every hour
    @Transactional
    public void cleanupExpiredJobs() {
        int deleted = exportJobRepository.deleteExpiredJobs(LocalDateTime.now());
        if (deleted > 0) {
            log.info("Cleaned up {} expired export jobs", deleted);
        }
    }

    private String buildFileName(ExportJob job) {
        String ext = job.getFormat() == ResultExportFormat.EXCEL ? ".xlsx" : ".csv";
        return "results-" + job.getAssignmentId() + ext;
    }

    private String resolveContentType(ResultExportFormat format) {
        return format == ResultExportFormat.EXCEL
                ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                : "text/csv; charset=UTF-8";
    }

    private ExportJobDto toDto(ExportJob job) {
        return ExportJobDto.builder()
                .id(job.getId())
                .assignmentId(job.getAssignmentId())
                .status(job.getStatus())
                .format(job.getFormat())
                .fileName(job.getFileName())
                .errorMessage(job.getErrorMessage())
                .createdAt(job.getCreatedAt())
                .completedAt(job.getCompletedAt())
                .build();
    }
}
