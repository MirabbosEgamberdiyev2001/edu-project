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
import uz.eduplatform.core.i18n.TranslatedField;
import uz.eduplatform.modules.content.domain.Question;
import uz.eduplatform.modules.content.domain.QuestionType;
import uz.eduplatform.modules.content.repository.QuestionRepository;
import uz.eduplatform.modules.test.domain.TestHistory;
import uz.eduplatform.modules.test.service.ExportHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
    @SuppressWarnings("unchecked")
    public byte[] exportAnswerKey(TestHistory test, ExportFormat format, Locale locale) {
        try (PDDocument document = new PDDocument()) {
            PDFont fontBold = loadFont(document, true);
            PDFont fontRegular = loadFont(document, false);

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(document, page);

            float y = PAGE_HEIGHT - MARGIN;
            y = drawCenteredText(cs, fontBold, 18, messageService.get("export.answer.key.title", locale), y);
            y -= 10;
            y = drawCenteredText(cs, fontRegular, 12, test.getTitle(), y);
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
            cs.showText(messageService.get("export.grading.title", locale));
            cs.endText();
            y -= LINE_HEIGHT;

            String[] gradeKeys = {"export.grade.excellent", "export.grade.good",
                    "export.grade.satisfactory", "export.grade.unsatisfactory"};
            for (String key : gradeKeys) {
                cs.beginText();
                cs.setFont(fontRegular, 10);
                cs.newLineAtOffset(MARGIN + 10, y);
                cs.showText("- " + messageService.get(key, locale));
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

            float y = PAGE_HEIGHT - MARGIN;
            y = drawCenteredText(cs, fontBold, 18, messageService.get("export.proofs.title", locale), y);
            y -= 10;
            y = drawCenteredText(cs, fontRegular, 12, test.getTitle(), y);
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
                cs.showText(num + ". " + exportHelper.truncateText(qText, 80));
                cs.endText();
                y -= LINE_HEIGHT + 5;

                cs.beginText();
                cs.setFont(fontRegular, 10);
                cs.newLineAtOffset(MARGIN + 15, y);
                cs.showText(messageService.get("export.proof.label", locale));
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
                    cs.showText(line);
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
        log.warn("NotoSans font not found at {}, falling back to Helvetica (Cyrillic will not render)", path);
        return bold ? PDType1Font.HELVETICA_BOLD
                    : PDType1Font.HELVETICA;
    }

    // ===== Private helpers =====

    private float drawHeader(PDPageContentStream cs, PDFont fontBold,
                             PDFont fontRegular, TestHistory test,
                             String variantCode, float y, Locale locale) throws IOException {
        Map<String, Object> header = test.getHeaderConfig();
        String schoolName = header != null && header.get("schoolName") != null
                ? (String) header.get("schoolName") : "";
        String className = header != null && header.get("className") != null
                ? (String) header.get("className") : "";

        if (!schoolName.isEmpty()) {
            y = drawCenteredText(cs, fontBold, 14, schoolName, y);
            y -= 5;
        }

        y = drawCenteredText(cs, fontBold, 13, test.getTitle(), y);
        y -= 5;
        y = drawCenteredText(cs, fontBold, 16, messageService.get("export.variant", locale, variantCode), y);
        y -= 15;

        cs.beginText();
        cs.setFont(fontRegular, 11);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText(messageService.get("export.student.name", locale));
        cs.endText();
        y -= LINE_HEIGHT + 3;

        cs.beginText();
        cs.setFont(fontRegular, 11);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText(messageService.get("export.class.date", locale, className));
        cs.endText();
        y -= LINE_HEIGHT + 3;

        cs.beginText();
        cs.setFont(fontRegular, 10);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText(messageService.get("export.question.count", locale, test.getQuestionCount()));
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
                cs.showText(label + ") " + exportHelper.truncateText(text, 75));
                cs.endText();
                y -= LINE_HEIGHT;
            }
        } else if (q.getQuestionType() == QuestionType.TRUE_FALSE) {
            cs.beginText();
            cs.setFont(fontRegular, 10);
            cs.newLineAtOffset(MARGIN + 20, y);
            cs.showText("A) " + messageService.get("export.true.option", locale)
                    + "     B) " + messageService.get("export.false.option", locale));
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
        int cols = 4;
        float colWidth = CONTENT_WIDTH / cols;
        int rows = (int) Math.ceil((double) answerKey.size() / cols);

        for (int c = 0; c < cols; c++) {
            float x = MARGIN + c * colWidth;
            cs.beginText();
            cs.setFont(fontBold, 9);
            cs.newLineAtOffset(x + 5, y);
            cs.showText("  " + messageService.get("export.answer.column", locale));
            cs.endText();
        }
        y -= LINE_HEIGHT;

        cs.moveTo(MARGIN, y + 3);
        cs.lineTo(PAGE_WIDTH - MARGIN, y + 3);
        cs.stroke();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int idx = r + c * rows;
                if (idx >= answerKey.size()) continue;

                Map<String, Object> entry = answerKey.get(idx);
                String answer = String.valueOf(entry.get("answer"));
                String numStr = String.valueOf(entry.get("questionNumber"));

                float x = MARGIN + c * colWidth;
                cs.beginText();
                cs.setFont(fontRegular, 10);
                cs.newLineAtOffset(x + 5, y);
                cs.showText(String.format("  %-4s  %s", numStr, answer));
                cs.endText();
            }
            y -= LINE_HEIGHT;
        }

        return y;
    }

    private float drawCenteredText(PDPageContentStream cs, PDFont font,
                                    float fontSize, String text, float y) throws IOException {
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
            String text = (i + 1) + " / " + totalPages;
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
