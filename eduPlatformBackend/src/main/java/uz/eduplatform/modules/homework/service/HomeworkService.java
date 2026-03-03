package uz.eduplatform.modules.homework.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import uz.eduplatform.core.common.dto.PagedResponse;
import uz.eduplatform.core.common.exception.BusinessException;
import uz.eduplatform.core.common.exception.ResourceNotFoundException;
import uz.eduplatform.core.storage.FileStorageService;
import uz.eduplatform.modules.auth.domain.User;
import uz.eduplatform.modules.auth.repository.UserRepository;
import uz.eduplatform.modules.group.repository.GroupMemberRepository;
import uz.eduplatform.modules.homework.domain.Homework;
import uz.eduplatform.modules.homework.domain.HomeworkSubmission;
import uz.eduplatform.modules.homework.dto.*;
import uz.eduplatform.modules.homework.repository.HomeworkRepository;
import uz.eduplatform.modules.homework.repository.HomeworkSubmissionRepository;
import uz.eduplatform.modules.inappnotification.service.InAppNotificationService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeworkService {

    private final HomeworkRepository homeworkRepository;
    private final HomeworkSubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final FileStorageService fileStorageService;
    private final InAppNotificationService notificationService;

    // ---- Teacher: manage homeworks ----

    @Transactional
    public HomeworkDto createHomework(UUID teacherId, CreateHomeworkRequest request) {
        Homework hw = Homework.builder()
                .teacherId(teacherId)
                .groupId(request.getGroupId())
                .title(request.getTitle())
                .description(request.getDescription())
                .deadline(request.getDeadline())
                .allowResubmission(request.getAllowResubmission() != null ? request.getAllowResubmission() : true)
                .fileRequired(request.getFileRequired() != null ? request.getFileRequired() : false)
                .maxFileSizeMb(request.getMaxFileSizeMb() != null ? request.getMaxFileSizeMb() : 10)
                .status("ACTIVE")
                .build();
        hw = homeworkRepository.save(hw);
        return toDto(hw, null);
    }

    @Transactional(readOnly = true)
    public PagedResponse<HomeworkDto> getTeacherHomeworks(UUID teacherId, Pageable pageable) {
        Page<Homework> page = homeworkRepository.findByTeacherIdOrderByCreatedAtDesc(teacherId, pageable);
        List<HomeworkDto> dtos = page.getContent().stream().map(hw -> {
            long total = submissionRepository.countByHomeworkId(hw.getId());
            long graded = submissionRepository.countByHomeworkIdAndStatus(hw.getId(), "GRADED");
            HomeworkDto dto = toDto(hw, null);
            dto.setTotalSubmissions(total);
            dto.setGradedSubmissions(graded);
            return dto;
        }).toList();
        return PagedResponse.of(dtos, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public PagedResponse<HomeworkSubmissionDto> getSubmissions(UUID homeworkId, UUID teacherId, Pageable pageable) {
        Homework hw = homeworkRepository.findById(homeworkId)
                .orElseThrow(() -> new ResourceNotFoundException("Homework", "id", homeworkId));
        if (!hw.getTeacherId().equals(teacherId)) {
            throw new BusinessException("Access denied");
        }
        Page<HomeworkSubmission> page = submissionRepository
                .findByHomeworkIdOrderBySubmittedAtDesc(homeworkId, pageable);
        List<HomeworkSubmissionDto> dtos = page.getContent().stream()
                .map(s -> toSubmissionDto(s, true)).toList();
        return PagedResponse.of(dtos, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Transactional
    public HomeworkSubmissionDto gradeSubmission(UUID submissionId, UUID teacherId,
                                                  GradeSubmissionRequest request) {
        HomeworkSubmission sub = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("HomeworkSubmission", "id", submissionId));

        HomeworkSubmission finalSub = sub;
        Homework hw = homeworkRepository.findById(sub.getHomeworkId())
                .orElseThrow(() -> new ResourceNotFoundException("Homework", "id", finalSub.getHomeworkId()));
        if (!hw.getTeacherId().equals(teacherId)) {
            throw new BusinessException("Access denied");
        }

        sub.setGrade(request.getGrade());
        sub.setTeacherComment(request.getTeacherComment());
        sub.setGradedBy(teacherId);
        sub.setGradedAt(LocalDateTime.now());
        sub.setStatus("GRADED");
        sub = submissionRepository.save(sub);

        // Notify student
        notificationService.createAsync(sub.getStudentId(), "HOMEWORK_GRADED",
                "Uy vazifasi baholandi: " + hw.getTitle(),
                "Baho: " + request.getGrade() + "/100"
                        + (request.getTeacherComment() != null ? ". " + request.getTeacherComment() : ""),
                sub.getId(), "HOMEWORK_SUBMISSION");

        return toSubmissionDto(sub, false);
    }

    // ---- Student: view & submit homeworks ----

    @Transactional(readOnly = true)
    public PagedResponse<HomeworkDto> getStudentHomeworks(UUID studentId, UUID groupId, Pageable pageable) {
        Page<Homework> page;
        if (groupId != null) {
            // Filter to a specific group (used from GroupDetailPage)
            page = homeworkRepository.findByGroupIdAndStatusOrderByCreatedAtDesc(groupId, "ACTIVE", pageable);
        } else {
            // Fetch homeworks for ALL groups the student belongs to
            Set<UUID> studentGroupIds = new HashSet<>(
                    groupMemberRepository.findGroupIdsByStudentId(studentId));

            if (studentGroupIds.isEmpty()) {
                return PagedResponse.of(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0, 0);
            }
            page = homeworkRepository.findByGroupIdInAndStatus(studentGroupIds, "ACTIVE", pageable);
        }

        List<HomeworkDto> dtos = page.getContent().stream().map(hw -> {
            HomeworkSubmission mySubmission = submissionRepository
                    .findTopByHomeworkIdAndStudentIdOrderByAttemptNumberDesc(hw.getId(), studentId)
                    .orElse(null);
            HomeworkDto dto = toDto(hw, null);
            dto.setMySubmission(mySubmission != null ? toSubmissionDto(mySubmission, false) : null);
            return dto;
        }).toList();

        return PagedResponse.of(dtos, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Transactional
    public HomeworkSubmissionDto submitHomework(UUID homeworkId, UUID studentId,
                                                String textAnswer, MultipartFile file) {
        Homework hw = homeworkRepository.findById(homeworkId)
                .orElseThrow(() -> new ResourceNotFoundException("Homework", "id", homeworkId));

        if ("CLOSED".equals(hw.getStatus())) {
            throw new BusinessException("Homework is closed");
        }
        if (hw.getDeadline() != null && LocalDateTime.now().isAfter(hw.getDeadline())) {
            throw new BusinessException("Deadline has passed");
        }

        // Check if already submitted and resubmission
        HomeworkSubmission existing = submissionRepository
                .findTopByHomeworkIdAndStudentIdOrderByAttemptNumberDesc(homeworkId, studentId)
                .orElse(null);

        if (existing != null && !hw.getAllowResubmission()) {
            throw new BusinessException("Resubmission is not allowed for this homework");
        }

        int attemptNumber = existing != null ? existing.getAttemptNumber() + 1 : 1;

        if (hw.getFileRequired() && file == null) {
            throw new BusinessException("A file attachment is required");
        }

        String fileUrl = null;
        String fileName = null;
        if (file != null && !file.isEmpty()) {
            FileStorageService.FileInfo info = fileStorageService.store(file);
            fileUrl = "/api/v1/files/" + info.storedFilename();
            fileName = info.originalFilename();
        }

        HomeworkSubmission submission = HomeworkSubmission.builder()
                .homeworkId(homeworkId)
                .studentId(studentId)
                .textAnswer(textAnswer)
                .fileUrl(fileUrl)
                .fileName(fileName)
                .submittedAt(LocalDateTime.now())
                .attemptNumber(attemptNumber)
                .status("SUBMITTED")
                .build();

        submission = submissionRepository.save(submission);

        // Notify teacher
        String studentName = userRepository.findById(studentId)
                .map(u -> u.getFirstName() + " " + u.getLastName()).orElse("O'quvchi");
        notificationService.createAsync(hw.getTeacherId(), "HOMEWORK_SUBMITTED",
                "Uy vazifasi topshirildi: " + hw.getTitle(),
                studentName + " uy vazifasini topshirdi (urinish #" + attemptNumber + ")",
                submission.getId(), "HOMEWORK_SUBMISSION");

        return toSubmissionDto(submission, false);
    }

    // ---- Export ----

    @Transactional(readOnly = true)
    public byte[] exportSubmissionsExcel(UUID homeworkId, UUID teacherId) {
        Homework hw = homeworkRepository.findById(homeworkId)
                .orElseThrow(() -> new ResourceNotFoundException("Homework", "id", homeworkId));
        if (!hw.getTeacherId().equals(teacherId)) {
            throw new BusinessException("Access denied");
        }

        List<HomeworkSubmission> submissions = submissionRepository
                .findByHomeworkIdOrderBySubmittedAtDesc(homeworkId, Pageable.unpaged())
                .getContent();

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Submissions");

            // Header style
            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font bold = wb.createFont();
            bold.setBold(true);
            headerStyle.setFont(bold);

            // Header row
            String[] headers = {"#", "Student ID", "Student Name", "Text Answer",
                    "File", "Submitted At", "Attempt", "Status", "Grade", "Comment", "Graded At"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            int rowNum = 1;
            for (HomeworkSubmission s : submissions) {
                String studentName = userRepository.findById(s.getStudentId())
                        .map(u -> u.getFirstName() + " " + u.getLastName()).orElse("");
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(rowNum - 1);
                row.createCell(1).setCellValue(s.getStudentId().toString());
                row.createCell(2).setCellValue(studentName);
                row.createCell(3).setCellValue(s.getTextAnswer() != null ? s.getTextAnswer() : "");
                row.createCell(4).setCellValue(s.getFileName() != null ? s.getFileName() : "");
                row.createCell(5).setCellValue(s.getSubmittedAt() != null ? s.getSubmittedAt().format(fmt) : "");
                row.createCell(6).setCellValue(s.getAttemptNumber());
                row.createCell(7).setCellValue(s.getStatus() != null ? s.getStatus() : "");
                row.createCell(8).setCellValue(s.getGrade() != null ? s.getGrade().toString() : "");
                row.createCell(9).setCellValue(s.getTeacherComment() != null ? s.getTeacherComment() : "");
                row.createCell(10).setCellValue(s.getGradedAt() != null ? s.getGradedAt().format(fmt) : "");
            }
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel export", e);
        }
    }

    // ---- Mapping ----

    private HomeworkDto toDto(Homework hw, HomeworkSubmissionDto mySubmission) {
        return HomeworkDto.builder()
                .id(hw.getId())
                .teacherId(hw.getTeacherId())
                .groupId(hw.getGroupId())
                .title(hw.getTitle())
                .description(hw.getDescription())
                .deadline(hw.getDeadline())
                .allowResubmission(hw.getAllowResubmission())
                .fileRequired(hw.getFileRequired())
                .maxFileSizeMb(hw.getMaxFileSizeMb())
                .status(hw.getStatus())
                .createdAt(hw.getCreatedAt())
                .mySubmission(mySubmission)
                .build();
    }

    private HomeworkSubmissionDto toSubmissionDto(HomeworkSubmission s, boolean includeStudentName) {
        String studentName = null;
        if (includeStudentName) {
            studentName = userRepository.findById(s.getStudentId())
                    .map(u -> u.getFirstName() + " " + u.getLastName()).orElse(null);
        }
        return HomeworkSubmissionDto.builder()
                .id(s.getId())
                .homeworkId(s.getHomeworkId())
                .studentId(s.getStudentId())
                .studentName(studentName)
                .textAnswer(s.getTextAnswer())
                .fileUrl(s.getFileUrl())
                .fileName(s.getFileName())
                .submittedAt(s.getSubmittedAt())
                .grade(s.getGrade())
                .teacherComment(s.getTeacherComment())
                .gradedAt(s.getGradedAt())
                .attemptNumber(s.getAttemptNumber())
                .status(s.getStatus())
                .build();
    }
}
