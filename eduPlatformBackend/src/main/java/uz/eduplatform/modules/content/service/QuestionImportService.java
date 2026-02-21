package uz.eduplatform.modules.content.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import uz.eduplatform.core.audit.AuditService;
import uz.eduplatform.core.common.exception.BusinessException;
import uz.eduplatform.core.common.exception.ResourceNotFoundException;
import uz.eduplatform.core.i18n.TranslatedField;
import uz.eduplatform.modules.auth.domain.User;
import uz.eduplatform.modules.auth.repository.UserRepository;
import uz.eduplatform.modules.content.domain.*;
import uz.eduplatform.modules.content.dto.ImportResult;
import uz.eduplatform.modules.content.repository.QuestionRepository;
import uz.eduplatform.modules.content.repository.TopicRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionImportService {

    private final QuestionRepository questionRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Transactional
    public ImportResult importFromExcel(UUID userId, UUID topicId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic", "id", topicId));

        ImportResult result = ImportResult.builder()
                .errors(new ArrayList<>())
                .build();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int totalRows = sheet.getLastRowNum(); // excluding header
            result.setTotalRows(totalRows);

            int successCount = 0;

            for (int i = 1; i <= totalRows; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    Question question = parseRow(row, i, topic, user);
                    questionRepository.save(question);
                    successCount++;
                } catch (Exception e) {
                    result.getErrors().add(ImportResult.ImportError.builder()
                            .row(i + 1)
                            .message(e.getMessage())
                            .build());
                }
            }

            result.setSuccessCount(successCount);
            result.setErrorCount(result.getErrors().size());

            // Update topic question count
            long count = questionRepository.countByTopicId(topicId);
            topic.setQuestionCount((int) count);
            topicRepository.save(topic);

            auditService.log(userId, null, "QUESTIONS_IMPORTED", "CONTENT",
                    "Topic", topicId);

        } catch (IOException e) {
            throw BusinessException.ofKey("question.import.read.failed");
        }

        return result;
    }

    public byte[] generateExportTemplate() {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Questions");

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Question", "Type", "Difficulty", "OptionA", "OptionB",
                    "OptionC", "OptionD", "CorrectAnswer", "Points", "Proof"};

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 6000);
            }

            // Example row
            Row exampleRow = sheet.createRow(1);
            exampleRow.createCell(0).setCellValue("2 + 2 = ?");
            exampleRow.createCell(1).setCellValue("MCQ_SINGLE");
            exampleRow.createCell(2).setCellValue("EASY");
            exampleRow.createCell(3).setCellValue("3");
            exampleRow.createCell(4).setCellValue("4");
            exampleRow.createCell(5).setCellValue("5");
            exampleRow.createCell(6).setCellValue("6");
            exampleRow.createCell(7).setCellValue("B");
            exampleRow.createCell(8).setCellValue("1");
            exampleRow.createCell(9).setCellValue("2 + 2 = 4");

            // Instructions sheet
            Sheet instructionsSheet = workbook.createSheet("Instructions");
            instructionsSheet.createRow(0).createCell(0).setCellValue("Import Instructions");
            instructionsSheet.createRow(2).createCell(0).setCellValue("Type values: MCQ_SINGLE, MCQ_MULTI, TRUE_FALSE, FILL_BLANK, MATCHING, ORDERING, SHORT_ANSWER, ESSAY");
            instructionsSheet.createRow(3).createCell(0).setCellValue("Difficulty values: EASY, MEDIUM, HARD");
            instructionsSheet.createRow(4).createCell(0).setCellValue("CorrectAnswer: A, B, C, D (for MCQ_SINGLE), or A,B (comma-separated for MCQ_MULTI)");
            instructionsSheet.createRow(5).createCell(0).setCellValue("Points: numeric value (default 1)");
            instructionsSheet.setColumnWidth(0, 20000);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw BusinessException.ofKey("question.import.template.failed");
        }
    }

    private Question parseRow(Row row, int rowIndex, Topic topic, User user) {
        String questionText = getCellStringValue(row, 0);
        if (questionText == null || questionText.isBlank()) {
            throw BusinessException.ofKey("question.import.row.text.required", rowIndex + 1);
        }

        String typeStr = getCellStringValue(row, 1);
        QuestionType type;
        try {
            type = QuestionType.valueOf(typeStr != null ? typeStr.trim().toUpperCase() : "MCQ_SINGLE");
        } catch (IllegalArgumentException e) {
            throw BusinessException.ofKey("question.import.row.invalid.type", rowIndex + 1, typeStr);
        }

        String diffStr = getCellStringValue(row, 2);
        Difficulty difficulty;
        try {
            difficulty = Difficulty.valueOf(diffStr != null ? diffStr.trim().toUpperCase() : "MEDIUM");
        } catch (IllegalArgumentException e) {
            difficulty = Difficulty.MEDIUM;
        }

        String optA = getCellStringValue(row, 3);
        String optB = getCellStringValue(row, 4);
        String optC = getCellStringValue(row, 5);
        String optD = getCellStringValue(row, 6);
        String correctStr = getCellStringValue(row, 7);
        String pointsStr = getCellStringValue(row, 8);
        String proof = getCellStringValue(row, 9);

        // Build options
        List<Map<String, Object>> options = new ArrayList<>();
        Map<String, String> optionMap = new LinkedHashMap<>();
        optionMap.put("A", optA);
        optionMap.put("B", optB);
        optionMap.put("C", optC);
        optionMap.put("D", optD);

        Set<String> correctAnswers = new HashSet<>();
        if (correctStr != null) {
            for (String c : correctStr.split(",")) {
                correctAnswers.add(c.trim().toUpperCase());
            }
        }

        for (Map.Entry<String, String> entry : optionMap.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isBlank()) {
                Map<String, Object> opt = new LinkedHashMap<>();
                opt.put("id", entry.getKey());
                opt.put("text", TranslatedField.wrap(entry.getValue()));
                opt.put("isCorrect", correctAnswers.contains(entry.getKey()));
                options.add(opt);
            }
        }

        if (options.size() < 2) {
            throw BusinessException.ofKey("question.import.row.options.min", rowIndex + 1);
        }

        BigDecimal points = BigDecimal.ONE;
        if (pointsStr != null && !pointsStr.isBlank()) {
            try {
                points = new BigDecimal(pointsStr.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        return Question.builder()
                .topic(topic)
                .user(user)
                .questionText(TranslatedField.wrap(questionText))
                .questionType(type)
                .difficulty(difficulty)
                .points(points)
                .options(toJson(options))
                .correctAnswer(toJson(correctStr))
                .proof(TranslatedField.wrap(proof))
                .proofRequired(proof != null && !proof.isBlank())
                .status(QuestionStatus.DRAFT)
                .build();
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw BusinessException.ofKey("question.json.serialize.failed");
        }
    }

    private String getCellStringValue(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }
}
