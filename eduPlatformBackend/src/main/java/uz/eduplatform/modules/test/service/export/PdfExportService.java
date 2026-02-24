package uz.eduplatform.modules.test.service.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;
import uz.eduplatform.core.common.exception.BusinessException;
import uz.eduplatform.core.common.utils.MessageService;
import uz.eduplatform.core.i18n.LocaleKeys;
import uz.eduplatform.core.i18n.TranslatedField;
import uz.eduplatform.modules.content.domain.Question;
import uz.eduplatform.modules.content.domain.QuestionType;
import uz.eduplatform.modules.content.repository.QuestionRepository;
import uz.eduplatform.modules.test.domain.TestHistory;
import uz.eduplatform.modules.test.service.ExportHelper;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfExportService implements TestExportService {

    private final QuestionRepository questionRepository;
    private final MessageService messageService;
    private final ExportHelper exportHelper;
    private final ObjectMapper objectMapper;

    @PostConstruct
    void validateFontsAvailable() {
        String[] fontPaths = {"/fonts/NotoSans-Regular.ttf", "/fonts/NotoSans-Bold.ttf"};
        for (String path : fontPaths) {
            if (getClass().getResourceAsStream(path) == null) {
                log.error("CRITICAL: Required font missing from classpath: {}", path);
                throw new IllegalStateException("Required font missing: " + path
                        + ". PDF export will not work without Unicode fonts.");
            }
        }
        log.info("PDF export fonts validated successfully");
    }

    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final float MARGIN = 50;
    private static final float CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN;
    private static final float LINE_HEIGHT = 16;
    private static final float QUESTION_SPACING = 12;

    @Override
    public ExportFormat getFormat() {
        return ExportFormat.PDF;
    }

    @Override
    public byte[] exportTest(TestHistory test, ExportFormat format, Locale locale) {
        try (PDDocument document = new PDDocument()) {
            PDFont fontBold = loadFont(document, true);
            PDFont fontRegular = loadFont(document, false);

            for (Map<String, Object> variant : test.getVariants()) {
                String variantCode = (String) variant.get("code");
                List<UUID> questionIds = exportHelper.parseQuestionIds(variant.get("questionIds"));
                List<List<String>> optionsOrder = exportHelper.parseOptionsOrder(variant.get("optionsOrder"));

                // Batch-fetch all questions for this variant
                Map<UUID, Question> questionMap = questionRepository.findAllById(questionIds).stream()
                        .collect(Collectors.toMap(Question::getId, Function.identity(), (a, b) -> a));

                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                PDPageContentStream cs = new PDPageContentStream(document, page);

                float y = PAGE_HEIGHT - MARGIN;
                y = drawHeader(cs, fontBold, fontRegular, test, variantCode, y, locale);
                y -= 20;

                int questionNum = 1;
                for (int qi = 0; qi < questionIds.size(); qi++) {
                    Question q = questionMap.get(questionIds.get(qi));
                    if (q == null) continue;

                    float estimatedHeight = estimateQuestionHeight(q);
                    if (y - estimatedHeight < MARGIN + 30) {
                        cs.close();
                        page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        cs = new PDPageContentStream(document, page);
                        y = PAGE_HEIGHT - MARGIN;
                    }

                    List<String> optOrder = (optionsOrder != null && qi < optionsOrder.size())
                            ? optionsOrder.get(qi) : null;
                    y = drawQuestion(cs, fontBold, fontRegular, q, questionNum++, y, optOrder, locale);
                    y -= QUESTION_SPACING;
                }
                cs.close();
            }

            addPageNumbers(document, fontRegular);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();

        } catch (IOException e) {
            log.error("Failed to generate PDF", e);
            throw new BusinessException(messageService.get("export.pdf.fail", locale, e.getMessage()));
        }
    }

    @Override
    public void exportTestToStream(TestHistory test, ExportFormat format, Locale locale, OutputStream out) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDFont fontBold = loadFont(document, true);
            PDFont fontRegular = loadFont(document, false);

            for (Map<String, Object> variant : test.getVariants()) {
                String variantCode = (String) variant.get("code");
                List<UUID> questionIds = exportHelper.parseQuestionIds(variant.get("questionIds"));
                List<List<String>> optionsOrder = exportHelper.parseOptionsOrder(variant.get("optionsOrder"));

                Map<UUID, Question> questionMap = questionRepository.findAllById(questionIds).stream()
                        .collect(Collectors.toMap(Question::getId, Function.identity(), (a, b) -> a));

                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                PDPageContentStream cs = new PDPageContentStream(document, page);

                float y = PAGE_HEIGHT - MARGIN;
                y = drawHeader(cs, fontBold, fontRegular, test, variantCode, y, locale);
                y -= 20;

                int questionNum = 1;
                for (int qi = 0; qi < questionIds.size(); qi++) {
                    Question q = questionMap.get(questionIds.get(qi));
                    if (q == null) continue;

                    float estimatedHeight = estimateQuestionHeight(q);
                    if (y - estimatedHeight < MARGIN + 30) {
                        cs.close();
                        page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        cs = new PDPageContentStream(document, page);
                        y = PAGE_HEIGHT - MARGIN;
                    }

                    List<String> optOrder = (optionsOrder != null && qi < optionsOrder.size())
                            ? optionsOrder.get(qi) : null;
                    y = drawQuestion(cs, fontBold, fontRegular, q, questionNum++, y, optOrder, locale);
                    y -= QUESTION_SPACING;
                }
                cs.close();
            }

            addPageNumbers(document, fontRegular);
            document.save(out);
        }
    }

    @Override
    public void exportAnswerKeyToStream(TestHistory test, ExportFormat format, Locale locale, OutputStream out) throws IOException {
        byte[] data = exportAnswerKey(test, format, locale);
        out.write(data);
    }

    @Override
    public void exportCombinedToStream(TestHistory test, ExportFormat format, Locale locale, OutputStream out) throws IOException {
        // Combined still uses byte[] merge — streaming not possible with PDF merge
        byte[] data = exportCombined(test, format, locale);
        out.write(data);
    }

    @Override
    public void exportProofsToStream(TestHistory test, ExportFormat format, Locale locale, OutputStream out) throws IOException {
        byte[] data = exportProofs(test, format, locale);
        out.write(data);
    }

    @Override
    @SuppressWarnings("unchecked")
    public byte[] exportAnswerKey(TestHistory test, ExportFormat format, Locale locale) {
        try (PDDocument document = new PDDocument()) {
            PDFont fontBold = loadFont(document, true);
            PDFont fontRegular = loadFont(document, false);

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(document, page);

            String localeKey = LocaleKeys.fromLocale(locale);
            float y = PAGE_HEIGHT - MARGIN;
            y = drawCenteredText(cs, fontBold, 18, messageService.get("export.answer.key.title", locale), y);
            y -= 10;
            y = drawCenteredText(cs, fontRegular, 12, resolveTitle(test, localeKey), y);
            y -= 5;
            y = drawCenteredText(cs, fontRegular, 10,
                    messageService.get("export.created.date", locale,
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))), y);
            y -= 25;

            for (Map<String, Object> variant : test.getVariants()) {
                String code = (String) variant.get("code");
                List<Map<String, Object>> answerKey = (List<Map<String, Object>>) variant.get("answerKey");

                if (y < MARGIN + 150) {
                    cs.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    cs = new PDPageContentStream(document, page);
                    y = PAGE_HEIGHT - MARGIN;
                }

                y = drawCenteredText(cs, fontBold, 14, messageService.get("export.variant.header", locale, code), y);
                y -= 15;
                y = drawAnswerKeyTable(cs, fontBold, fontRegular, answerKey, y, locale);
                y -= 25;
            }

            y -= 10;
            cs.beginText();
            cs.setFont(fontBold, 11);
            cs.newLineAtOffset(MARGIN, y);
            cs.showText(sanitizeForPdf(messageService.get("export.grading.title", locale), fontBold));
            cs.endText();
            y -= LINE_HEIGHT;

            String[] gradeKeys = {"export.grade.excellent", "export.grade.good",
                    "export.grade.satisfactory", "export.grade.unsatisfactory"};
            for (String key : gradeKeys) {
                cs.beginText();
                cs.setFont(fontRegular, 10);
                cs.newLineAtOffset(MARGIN + 10, y);
                cs.showText(sanitizeForPdf("- " + messageService.get(key, locale), fontRegular));
                cs.endText();
                y -= LINE_HEIGHT;
            }

            cs.close();
            addPageNumbers(document, fontRegular);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();

        } catch (IOException e) {
            log.error("Failed to generate answer key PDF", e);
            throw new BusinessException(messageService.get("export.answer.key.pdf.fail", locale, e.getMessage()));
        }
    }

    @Override
    public byte[] exportCombined(TestHistory test, ExportFormat format, Locale locale) {
        byte[] testPdf = exportTest(test, format, locale);
        byte[] answerKeyPdf = exportAnswerKey(test, format, locale);
        return mergePdfs(testPdf, answerKeyPdf, locale);
    }

    @Override
    public byte[] exportProofs(TestHistory test, ExportFormat format, Locale locale) {
        try (PDDocument document = new PDDocument()) {
            PDFont fontBold = loadFont(document, true);
            PDFont fontRegular = loadFont(document, false);

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(document, page);

            String localeKey = LocaleKeys.fromLocale(locale);
            float y = PAGE_HEIGHT - MARGIN;
            y = drawCenteredText(cs, fontBold, 18, messageService.get("export.proofs.title", locale), y);
            y -= 10;
            y = drawCenteredText(cs, fontRegular, 12, resolveTitle(test, localeKey), y);
            y -= 25;

            Map<String, Object> firstVariant = test.getVariants().get(0);
            List<UUID> questionIds = exportHelper.parseQuestionIds(firstVariant.get("questionIds"));

            // Batch-fetch all questions
            Map<UUID, Question> questionMap = questionRepository.findAllById(questionIds).stream()
                    .collect(Collectors.toMap(Question::getId, Function.identity(), (a, b) -> a));

            int num = 1;
            for (UUID qId : questionIds) {
                Question q = questionMap.get(qId);
                String proofText = q != null ? TranslatedField.resolve(q.getProof()) : null;
                if (q == null || proofText == null || proofText.isBlank()) continue;

                String qText = TranslatedField.resolve(q.getQuestionText());
                float estimatedHeight = 80 + (proofText.length() / 60) * LINE_HEIGHT;
                if (y - estimatedHeight < MARGIN + 30) {
                    cs.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    cs = new PDPageContentStream(document, page);
                    y = PAGE_HEIGHT - MARGIN;
                }

                cs.beginText();
                cs.setFont(fontBold, 11);
                cs.newLineAtOffset(MARGIN, y);
                cs.showText(sanitizeForPdf(num + ". " + exportHelper.truncateText(qText, 80), fontBold));
                cs.endText();
                y -= LINE_HEIGHT + 5;

                cs.beginText();
                cs.setFont(fontRegular, 10);
                cs.newLineAtOffset(MARGIN + 15, y);
                cs.showText(sanitizeForPdf(messageService.get("export.proof.label", locale), fontRegular));
                cs.endText();
                y -= LINE_HEIGHT;

                for (String line : exportHelper.wrapText(proofText, 85)) {
                    if (y < MARGIN + 30) {
                        cs.close();
                        page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        cs = new PDPageContentStream(document, page);
                        y = PAGE_HEIGHT - MARGIN;
                    }
                    cs.beginText();
                    cs.setFont(fontRegular, 9);
                    cs.newLineAtOffset(MARGIN + 20, y);
                    cs.showText(sanitizeForPdf(line, fontRegular));
                    cs.endText();
                    y -= LINE_HEIGHT - 2;
                }
                y -= QUESTION_SPACING;
                num++;
            }

            cs.close();
            addPageNumbers(document, fontRegular);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();

        } catch (IOException e) {
            log.error("Failed to generate proofs PDF", e);
            throw new BusinessException(messageService.get("export.proofs.pdf.fail", locale, e.getMessage()));
        }
    }

    // ===== Translation helpers =====

    private String resolveTitle(TestHistory test, String localeKey) {
        if (test.getTitleTranslations() != null && !test.getTitleTranslations().isEmpty()) {
            String resolved = TranslatedField.resolve(test.getTitleTranslations(), localeKey);
            if (resolved != null) return resolved;
        }
        return test.getTitle();
    }

    @SuppressWarnings("unchecked")
    private String resolveHeaderField(Map<String, Object> header, String field, String localeKey) {
        if (header == null) return "";
        Object translations = header.get(field + "Translations");
        if (translations instanceof Map) {
            String resolved = TranslatedField.resolve((Map<String, String>) translations, localeKey);
            if (resolved != null) return resolved;
        }
        Object plain = header.get(field);
        return plain != null ? String.valueOf(plain) : "";
    }

    // ===== Font loading =====

    private PDFont loadFont(PDDocument document, boolean bold) throws IOException {
        String path = bold ? "/fonts/NotoSans-Bold.ttf" : "/fonts/NotoSans-Regular.ttf";
        InputStream is = getClass().getResourceAsStream(path);
        if (is != null) {
            try {
                return PDType0Font.load(document, is);
            } finally {
                is.close();
            }
        }
        log.error("NotoSans font not found at {}. PDF export requires embedded Unicode fonts for Cyrillic/Uzbek text.", path);
        throw new IOException("Required font not found: " + path
                + ". Ensure NotoSans-Regular.ttf and NotoSans-Bold.ttf are in src/main/resources/fonts/");
    }

    // ===== Private helpers =====

    private float drawHeader(PDPageContentStream cs, PDFont fontBold,
                             PDFont fontRegular, TestHistory test,
                             String variantCode, float y, Locale locale) throws IOException {
        String localeKey = LocaleKeys.fromLocale(locale);
        Map<String, Object> header = test.getHeaderConfig();
        String schoolName = resolveHeaderField(header, "schoolName", localeKey);
        String className = header != null && header.get("className") != null
                ? (String) header.get("className") : "";

        if (!schoolName.isEmpty()) {
            y = drawCenteredText(cs, fontBold, 14, schoolName, y);
            y -= 5;
        }

        String title = resolveTitle(test, localeKey);
        y = drawCenteredText(cs, fontBold, 13, title, y);
        y -= 5;
        y = drawCenteredText(cs, fontBold, 16, messageService.get("export.variant", locale, variantCode), y);
        y -= 15;

        cs.beginText();
        cs.setFont(fontRegular, 11);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText(sanitizeForPdf(messageService.get("export.student.name", locale), fontRegular));
        cs.endText();
        y -= LINE_HEIGHT + 3;

        cs.beginText();
        cs.setFont(fontRegular, 11);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText(sanitizeForPdf(messageService.get("export.class.date", locale, className), fontRegular));
        cs.endText();
        y -= LINE_HEIGHT + 3;

        cs.beginText();
        cs.setFont(fontRegular, 10);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText(sanitizeForPdf(messageService.get("export.question.count", locale, test.getQuestionCount()), fontRegular));
        cs.endText();
        y -= LINE_HEIGHT;

        cs.moveTo(MARGIN, y);
        cs.lineTo(PAGE_WIDTH - MARGIN, y);
        cs.stroke();
        y -= 10;

        return y;
    }

    @SuppressWarnings("unchecked")
    private float drawQuestion(PDPageContentStream cs, PDFont fontBold,
                                PDFont fontRegular, Question q,
                                int num, float y, List<String> optionsOrder, Locale locale) throws IOException {
        String questionText = num + ". " + TranslatedField.resolve(q.getQuestionText());
        // Sanitize before wrapping to avoid glyph errors for unsupported Unicode chars
        questionText = sanitizeForPdf(questionText, fontRegular);
        List<String> lines = exportHelper.wrapText(questionText, 85);
        for (int i = 0; i < lines.size(); i++) {
            cs.beginText();
            cs.setFont(i == 0 ? fontBold : fontRegular, 11);
            cs.newLineAtOffset(MARGIN, y);
            cs.showText(lines.get(i));
            cs.endText();
            y -= LINE_HEIGHT;
        }
        y -= 3;

        Object optionsObj = parseJson(q.getOptions());
        boolean isMcq = q.getQuestionType() == QuestionType.MCQ_SINGLE
                || q.getQuestionType() == QuestionType.MCQ_MULTI;

        if (isMcq && optionsObj instanceof List<?> optionsList && !optionsList.isEmpty()) {
            String[] labels = {"A", "B", "C", "D", "E", "F", "G", "H"};
            List<Map<String, Object>> options = new ArrayList<>();
            for (Object o : optionsList) {
                if (o instanceof Map) options.add((Map<String, Object>) o);
            }

            if (optionsOrder != null && !optionsOrder.isEmpty()) {
                List<Map<String, Object>> reordered = new ArrayList<>();
                for (String orderId : optionsOrder) {
                    for (Map<String, Object> opt : options) {
                        if (orderId.equals(String.valueOf(opt.get("id")))) {
                            reordered.add(opt);
                            break;
                        }
                    }
                }
                if (reordered.size() == options.size()) options = reordered;
            }

            for (int i = 0; i < options.size(); i++) {
                String text = resolveExportOptionText(options.get(i).get("text"));
                String label = i < labels.length ? labels[i] : String.valueOf(i + 1);
                cs.beginText();
                cs.setFont(fontRegular, 10);
                cs.newLineAtOffset(MARGIN + 20, y);
                cs.showText(sanitizeForPdf(label + ") " + exportHelper.truncateText(text, 75), fontRegular));
                cs.endText();
                y -= LINE_HEIGHT;
            }
        } else if (q.getQuestionType() == QuestionType.TRUE_FALSE) {
            cs.beginText();
            cs.setFont(fontRegular, 10);
            cs.newLineAtOffset(MARGIN + 20, y);
            cs.showText(sanitizeForPdf("A) " + messageService.get("export.true.option", locale)
                    + "     B) " + messageService.get("export.false.option", locale), fontRegular));
            cs.endText();
            y -= LINE_HEIGHT;
        }
        // No placeholder for missing options - question text is still shown

        return y;
    }

    @SuppressWarnings("unchecked")
    private float drawAnswerKeyTable(PDPageContentStream cs, PDFont fontBold,
                                      PDFont fontRegular,
                                      List<Map<String, Object>> answerKey, float y,
                                      Locale locale) throws IOException {
        // Table with 4 question-answer pairs per row (8 visual columns: #, Ans, #, Ans, ...)
        int groupCount = 4;
        int totalQuestions = answerKey.size();
        int rows = (int) Math.ceil((double) totalQuestions / groupCount);

        float numColWidth = 30;
        float ansColWidth = (CONTENT_WIDTH - numColWidth * groupCount) / groupCount;
        float rowHeight = LINE_HEIGHT + 4;

        // Draw header row
        float headerY = y;
        for (int c = 0; c < groupCount; c++) {
            float xNum = MARGIN + c * (numColWidth + ansColWidth);
            float xAns = xNum + numColWidth;

            // Header text
            cs.beginText();
            cs.setFont(fontBold, 9);
            cs.newLineAtOffset(xNum + 4, headerY - 12);
            cs.showText("#");
            cs.endText();

            cs.beginText();
            cs.setFont(fontBold, 9);
            cs.newLineAtOffset(xAns + 4, headerY - 12);
            cs.showText(sanitizeForPdf(messageService.get("export.answer.column.short", locale), fontBold));
            cs.endText();
        }

        // Draw header row border
        float tableWidth = groupCount * (numColWidth + ansColWidth);
        drawRect(cs, MARGIN, headerY - rowHeight, tableWidth, rowHeight);

        // Draw vertical lines in header
        for (int c = 0; c < groupCount; c++) {
            float xNum = MARGIN + c * (numColWidth + ansColWidth);
            float xAns = xNum + numColWidth;
            if (c > 0) {
                cs.moveTo(xNum, headerY);
                cs.lineTo(xNum, headerY - rowHeight);
                cs.stroke();
            }
            cs.moveTo(xAns, headerY);
            cs.lineTo(xAns, headerY - rowHeight);
            cs.stroke();
        }

        y = headerY - rowHeight;

        // Draw data rows
        for (int r = 0; r < rows; r++) {
            float rowTop = y;
            for (int c = 0; c < groupCount; c++) {
                int idx = r + c * rows;
                if (idx >= totalQuestions) continue;

                Map<String, Object> entry = answerKey.get(idx);
                String numStr = String.valueOf(entry.get("questionNumber"));
                String answer = String.valueOf(entry.get("answer"));

                float xNum = MARGIN + c * (numColWidth + ansColWidth);
                float xAns = xNum + numColWidth;

                cs.beginText();
                cs.setFont(fontRegular, 10);
                cs.newLineAtOffset(xNum + 4, rowTop - 12);
                cs.showText(sanitizeForPdf(numStr, fontRegular));
                cs.endText();

                cs.beginText();
                cs.setFont(fontBold, 10);
                cs.newLineAtOffset(xAns + 4, rowTop - 12);
                cs.showText(sanitizeForPdf(answer, fontBold));
                cs.endText();
            }

            // Row border
            drawRect(cs, MARGIN, rowTop - rowHeight, tableWidth, rowHeight);

            // Vertical lines
            for (int c = 0; c < groupCount; c++) {
                float xNum = MARGIN + c * (numColWidth + ansColWidth);
                float xAns = xNum + numColWidth;
                if (c > 0) {
                    cs.moveTo(xNum, rowTop);
                    cs.lineTo(xNum, rowTop - rowHeight);
                    cs.stroke();
                }
                cs.moveTo(xAns, rowTop);
                cs.lineTo(xAns, rowTop - rowHeight);
                cs.stroke();
            }

            y = rowTop - rowHeight;
        }

        return y;
    }

    private void drawRect(PDPageContentStream cs, float x, float y,
                           float width, float height) throws IOException {
        cs.moveTo(x, y);
        cs.lineTo(x + width, y);
        cs.lineTo(x + width, y + height);
        cs.lineTo(x, y + height);
        cs.closePath();
        cs.stroke();
    }

    private float drawCenteredText(PDPageContentStream cs, PDFont font,
                                    float fontSize, String text, float y) throws IOException {
        text = sanitizeForPdf(text, font);
        float textWidth = font.getStringWidth(text) / 1000 * fontSize;
        float x = (PAGE_WIDTH - textWidth) / 2;
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
        return y - fontSize - 3;
    }

    private void addPageNumbers(PDDocument document, PDFont font) throws IOException {
        int totalPages = document.getNumberOfPages();
        for (int i = 0; i < totalPages; i++) {
            PDPage page = document.getPage(i);
            PDPageContentStream cs = new PDPageContentStream(document, page,
                    PDPageContentStream.AppendMode.APPEND, true, true);
            String text = sanitizeForPdf((i + 1) + " / " + totalPages, font);
            float textWidth = font.getStringWidth(text) / 1000 * 9;
            cs.beginText();
            cs.setFont(font, 9);
            cs.newLineAtOffset((PAGE_WIDTH - textWidth) / 2, 25);
            cs.showText(text);
            cs.endText();
            cs.close();
        }
    }

    private float estimateQuestionHeight(Question q) {
        float height = LINE_HEIGHT * 2;
        Object optionsObj = parseJson(q.getOptions());
        if (optionsObj instanceof List<?> opts) {
            height += opts.size() * LINE_HEIGHT;
        }
        height += QUESTION_SPACING;
        return height;
    }

    @SuppressWarnings("unchecked")
    private String resolveExportOptionText(Object text) {
        if (text instanceof Map) {
            return TranslatedField.resolve((Map<String, String>) text);
        }
        return text != null ? String.valueOf(text) : "";
    }

    private Object parseJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Sanitizes text for PDF rendering by replacing characters unsupported by the font.
     * NotoSans lacks many math/special Unicode glyphs — this prevents IllegalArgumentException.
     */
    private String sanitizeForPdf(String text, PDFont font) {
        if (text == null || text.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            String ch = new String(Character.toChars(cp));
            try {
                font.encode(ch);
                sb.append(ch);
            } catch (Exception e) {
                sb.append(mapUnsupportedChar(cp));
            }
            i += Character.charCount(cp);
        }
        return sb.toString();
    }

    private String mapUnsupportedChar(int codePoint) {
        return switch (codePoint) {
            case 0x221A -> "sqrt";   // √
            case 0x03C0 -> "pi";     // π
            case 0x2211 -> "sum";    // ∑
            case 0x222B -> "int";    // ∫
            case 0x2260 -> "!=";     // ≠
            case 0x2264 -> "<=";     // ≤
            case 0x2265 -> ">=";     // ≥
            case 0x00B2 -> "^2";     // ²
            case 0x00B3 -> "^3";     // ³
            case 0x2248 -> "~=";     // ≈
            case 0x221E -> "inf";    // ∞
            case 0x00B1 -> "+/-";    // ±
            case 0x00D7 -> "*";      // ×
            case 0x00F7 -> "/";      // ÷
            case 0x2013 -> "-";      // –
            case 0x2014 -> "--";     // —
            case 0x2018, 0x2019 -> "'";  // ' '
            case 0x201C, 0x201D -> "\""; // " "
            case 0x2026 -> "...";    // …
            case 0x0394 -> "delta";  // Δ
            case 0x03B1 -> "alpha";  // α
            case 0x03B2 -> "beta";   // β
            case 0x03B3 -> "gamma";  // γ
            default -> "?";
        };
    }

    private byte[] mergePdfs(byte[] pdf1, byte[] pdf2, Locale locale) {
        try (PDDocument doc1 = PDDocument.load(pdf1);
             PDDocument doc2 = PDDocument.load(pdf2)) {

            for (int i = 0; i < doc2.getNumberOfPages(); i++) {
                doc1.addPage(doc2.getPage(i));
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc1.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new BusinessException(messageService.get("export.merge.pdf.fail", locale, e.getMessage()));
        }
    }
}
