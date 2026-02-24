package uz.eduplatform.modules.test.service.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STPageOrientation;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocxExportService implements TestExportService {

    private final ExportHelper exportHelper;
    private final QuestionRepository questionRepository;
    private final MessageService messageService;
    private final ObjectMapper objectMapper;

    @Override
    public ExportFormat getFormat() {
        return ExportFormat.DOCX;
    }

    @Override
    public byte[] exportTest(TestHistory test, ExportFormat format, Locale locale) {
        try (XWPFDocument document = new XWPFDocument()) {
            setA4PageSize(document);

            boolean firstVariant = true;
            for (Map<String, Object> variant : test.getVariants()) {
                if (!firstVariant) addPageBreak(document);
                firstVariant = false;

                String variantCode = (String) variant.get("code");
                List<UUID> questionIds = exportHelper.parseQuestionIds(variant.get("questionIds"));
                List<List<String>> optionsOrder = exportHelper.parseOptionsOrder(variant.get("optionsOrder"));

                writeTestHeader(document, test, variantCode, locale);

                // Batch-fetch all questions for this variant
                Map<UUID, Question> questionMap = questionRepository.findAllById(questionIds).stream()
                        .collect(Collectors.toMap(Question::getId, Function.identity(), (a, b) -> a));

                int questionNum = 1;
                for (int qi = 0; qi < questionIds.size(); qi++) {
                    Question q = questionMap.get(questionIds.get(qi));
                    if (q == null) continue;

                    List<String> optOrder = (optionsOrder != null && qi < optionsOrder.size())
                            ? optionsOrder.get(qi) : null;
                    writeQuestion(document, q, questionNum++, optOrder, locale);
                }
            }

            return toByteArray(document);

        } catch (IOException e) {
            throw new BusinessException(messageService.get("export.docx.fail", locale, e.getMessage()));
        }
    }

    @Override
    public void exportTestToStream(TestHistory test, ExportFormat format, Locale locale, OutputStream out) throws IOException {
        try (XWPFDocument document = new XWPFDocument()) {
            setA4PageSize(document);

            boolean firstVariant = true;
            for (Map<String, Object> variant : test.getVariants()) {
                if (!firstVariant) addPageBreak(document);
                firstVariant = false;

                String variantCode = (String) variant.get("code");
                List<UUID> questionIds = exportHelper.parseQuestionIds(variant.get("questionIds"));
                List<List<String>> optionsOrder = exportHelper.parseOptionsOrder(variant.get("optionsOrder"));

                writeTestHeader(document, test, variantCode, locale);

                Map<UUID, Question> questionMap = questionRepository.findAllById(questionIds).stream()
                        .collect(Collectors.toMap(Question::getId, Function.identity(), (a, b) -> a));

                int questionNum = 1;
                for (int qi = 0; qi < questionIds.size(); qi++) {
                    Question q = questionMap.get(questionIds.get(qi));
                    if (q == null) continue;

                    List<String> optOrder = (optionsOrder != null && qi < optionsOrder.size())
                            ? optionsOrder.get(qi) : null;
                    writeQuestion(document, q, questionNum++, optOrder, locale);
                }
            }

            document.write(out);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public byte[] exportAnswerKey(TestHistory test, ExportFormat format, Locale locale) {
        try (XWPFDocument document = new XWPFDocument()) {
            setA4PageSize(document);

            String localeKey = LocaleKeys.fromLocale(locale);
            addCenteredBoldParagraph(document, messageService.get("export.answer.key.title", locale), 18);
            addCenteredParagraph(document, resolveTitle(test, localeKey), 12);
            addCenteredParagraph(document, messageService.get("export.created.date", locale,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))), 10);
            addEmptyLine(document);

            for (Map<String, Object> variant : test.getVariants()) {
                String code = (String) variant.get("code");
                List<Map<String, Object>> answerKey = (List<Map<String, Object>>) variant.get("answerKey");

                addCenteredBoldParagraph(document, messageService.get("export.variant.header", locale, code), 14);
                addEmptyLine(document);
                writeAnswerKeyTable(document, answerKey, locale);
                addEmptyLine(document);
            }

            addBoldParagraph(document, messageService.get("export.grading.title", locale), 11);
            String[] gradeKeys = {"export.grade.excellent", "export.grade.good",
                    "export.grade.satisfactory", "export.grade.unsatisfactory"};
            for (String key : gradeKeys) {
                addParagraph(document, "- " + messageService.get(key, locale), 10);
            }

            return toByteArray(document);

        } catch (IOException e) {
            throw new BusinessException(messageService.get("export.answer.key.docx.fail", locale, e.getMessage()));
        }
    }

    @Override
    public byte[] exportCombined(TestHistory test, ExportFormat format, Locale locale) {
        try (XWPFDocument document = new XWPFDocument()) {
            setA4PageSize(document);

            boolean firstVariant = true;
            for (Map<String, Object> variant : test.getVariants()) {
                if (!firstVariant) addPageBreak(document);
                firstVariant = false;

                String variantCode = (String) variant.get("code");
                List<UUID> questionIds = exportHelper.parseQuestionIds(variant.get("questionIds"));
                List<List<String>> optionsOrder = exportHelper.parseOptionsOrder(variant.get("optionsOrder"));

                writeTestHeader(document, test, variantCode, locale);

                // Batch-fetch all questions for this variant
                Map<UUID, Question> qMap = questionRepository.findAllById(questionIds).stream()
                        .collect(Collectors.toMap(Question::getId, Function.identity(), (a, b) -> a));

                int questionNum = 1;
                for (int qi = 0; qi < questionIds.size(); qi++) {
                    Question q = qMap.get(questionIds.get(qi));
                    if (q == null) continue;

                    List<String> optOrder = (optionsOrder != null && qi < optionsOrder.size())
                            ? optionsOrder.get(qi) : null;
                    writeQuestion(document, q, questionNum++, optOrder, locale);
                }
            }

            addPageBreak(document);
            writeAnswerKeyContent(document, test, locale);

            return toByteArray(document);

        } catch (IOException e) {
            throw new BusinessException(messageService.get("export.docx.fail", locale, e.getMessage()));
        }
    }

    @Override
    public byte[] exportProofs(TestHistory test, ExportFormat format, Locale locale) {
        try (XWPFDocument document = new XWPFDocument()) {
            setA4PageSize(document);

            String localeKey = LocaleKeys.fromLocale(locale);
            addCenteredBoldParagraph(document, messageService.get("export.proofs.title", locale), 18);
            addCenteredParagraph(document, resolveTitle(test, localeKey), 12);
            addEmptyLine(document);

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

                addBoldParagraph(document, num + ". " + TranslatedField.resolve(q.getQuestionText()), 11);

                XWPFParagraph proofLabel = document.createParagraph();
                proofLabel.setIndentationLeft(400);
                XWPFRun proofLabelRun = proofLabel.createRun();
                proofLabelRun.setBold(true);
                proofLabelRun.setFontSize(10);
                proofLabelRun.setText(messageService.get("export.proof.label", locale));

                XWPFParagraph proofPara = document.createParagraph();
                proofPara.setIndentationLeft(600);
                XWPFRun proofRun = proofPara.createRun();
                proofRun.setFontSize(10);
                proofRun.setText(proofText);

                addEmptyLine(document);
                num++;
            }

            return toByteArray(document);

        } catch (IOException e) {
            throw new BusinessException(messageService.get("export.proofs.docx.fail", locale, e.getMessage()));
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

    // ===== Private helpers =====

    private void setA4PageSize(XWPFDocument document) {
        CTSectPr sectPr = document.getDocument().getBody().addNewSectPr();
        CTPageSz pageSz = sectPr.addNewPgSz();
        pageSz.setW(BigInteger.valueOf(11906));
        pageSz.setH(BigInteger.valueOf(16838));
        pageSz.setOrient(STPageOrientation.PORTRAIT);
    }

    private void writeTestHeader(XWPFDocument document, TestHistory test,
                                  String variantCode, Locale locale) {
        String localeKey = LocaleKeys.fromLocale(locale);
        Map<String, Object> header = test.getHeaderConfig();
        String schoolName = resolveHeaderField(header, "schoolName", localeKey);
        String className = header != null && header.get("className") != null
                ? (String) header.get("className") : "";

        if (!schoolName.isEmpty()) addCenteredBoldParagraph(document, schoolName, 14);
        addCenteredBoldParagraph(document, resolveTitle(test, localeKey), 13);
        addCenteredBoldParagraph(document, messageService.get("export.variant", locale, variantCode), 16);
        addEmptyLine(document);
        addParagraph(document, messageService.get("export.student.name", locale), 11);
        addParagraph(document, messageService.get("export.class.date", locale, className), 11);
        addParagraph(document, messageService.get("export.question.count", locale, test.getQuestionCount()), 10);

        XWPFParagraph line = document.createParagraph();
        line.setBorderBottom(Borders.SINGLE);
        addEmptyLine(document);
    }

    @SuppressWarnings("unchecked")
    private void writeQuestion(XWPFDocument document, Question q, int num,
                                List<String> optionsOrder, Locale locale) {
        XWPFParagraph qPara = document.createParagraph();
        XWPFRun numRun = qPara.createRun();
        numRun.setBold(true);
        numRun.setFontSize(11);
        numRun.setText(num + ". ");
        XWPFRun textRun = qPara.createRun();
        textRun.setFontSize(11);
        textRun.setText(TranslatedField.resolve(q.getQuestionText()));

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
                XWPFParagraph optPara = document.createParagraph();
                optPara.setIndentationLeft(600);
                XWPFRun optRun = optPara.createRun();
                optRun.setFontSize(10);
                optRun.setText(label + ") " + text);
            }
        } else if (q.getQuestionType() == QuestionType.TRUE_FALSE) {
            XWPFParagraph tfPara = document.createParagraph();
            tfPara.setIndentationLeft(600);
            XWPFRun tfRun = tfPara.createRun();
            tfRun.setFontSize(10);
            tfRun.setText("A) " + messageService.get("export.true.option", locale)
                    + "     B) " + messageService.get("export.false.option", locale));
        }
        addEmptyLine(document);
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

    @SuppressWarnings("unchecked")
    private void writeAnswerKeyContent(XWPFDocument document, TestHistory test, Locale locale) {
        String localeKey = LocaleKeys.fromLocale(locale);
        addCenteredBoldParagraph(document, messageService.get("export.answer.key.title", locale), 18);
        addCenteredParagraph(document, resolveTitle(test, localeKey), 12);
        addCenteredParagraph(document, messageService.get("export.created.date", locale,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))), 10);
        addEmptyLine(document);

        for (Map<String, Object> variant : test.getVariants()) {
            String code = (String) variant.get("code");
            List<Map<String, Object>> answerKey = (List<Map<String, Object>>) variant.get("answerKey");
            addCenteredBoldParagraph(document, messageService.get("export.variant.header", locale, code), 14);
            addEmptyLine(document);
            writeAnswerKeyTable(document, answerKey, locale);
            addEmptyLine(document);
        }

        addBoldParagraph(document, messageService.get("export.grading.title", locale), 11);
        String[] gradeKeys = {"export.grade.excellent", "export.grade.good",
                "export.grade.satisfactory", "export.grade.unsatisfactory"};
        for (String key : gradeKeys) {
            addParagraph(document, "- " + messageService.get(key, locale), 10);
        }
    }

    private void writeAnswerKeyTable(XWPFDocument document, List<Map<String, Object>> answerKey, Locale locale) {
        int groupCount = 4;
        int cols = groupCount * 2;
        int totalQuestions = answerKey.size();
        int rows = (int) Math.ceil((double) totalQuestions / groupCount);

        XWPFTable table = document.createTable(rows + 1, cols);
        table.setWidth("100%");

        XWPFTableRow headerRow = table.getRow(0);
        for (int c = 0; c < groupCount; c++) {
            setCellText(headerRow.getCell(c * 2), "#", true);
            setCellText(headerRow.getCell(c * 2 + 1), messageService.get("export.answer.column.short", locale), true);
        }

        for (int r = 0; r < rows; r++) {
            XWPFTableRow row = table.getRow(r + 1);
            for (int c = 0; c < groupCount; c++) {
                int idx = r + c * rows;
                if (idx < totalQuestions) {
                    Map<String, Object> entry = answerKey.get(idx);
                    String numStr = String.valueOf(entry.get("questionNumber"));
                    String answer = String.valueOf(entry.get("answer"));
                    setCellText(row.getCell(c * 2), numStr, false);
                    setCellText(row.getCell(c * 2 + 1), answer, true);
                }
            }
        }
    }

    private void setCellText(XWPFTableCell cell, String text, boolean bold) {
        XWPFParagraph para = cell.getParagraphArray(0);
        if (para == null) para = cell.addParagraph();
        para.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = para.createRun();
        run.setText(text);
        run.setBold(bold);
        run.setFontSize(9);
    }

    private void addPageBreak(XWPFDocument document) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setPageBreak(true);
    }

    private void addCenteredBoldParagraph(XWPFDocument document, String text, int fontSize) {
        XWPFParagraph para = document.createParagraph();
        para.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = para.createRun();
        run.setBold(true);
        run.setFontSize(fontSize);
        run.setText(text);
    }

    private void addCenteredParagraph(XWPFDocument document, String text, int fontSize) {
        XWPFParagraph para = document.createParagraph();
        para.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = para.createRun();
        run.setFontSize(fontSize);
        run.setText(text);
    }

    private void addBoldParagraph(XWPFDocument document, String text, int fontSize) {
        XWPFParagraph para = document.createParagraph();
        XWPFRun run = para.createRun();
        run.setBold(true);
        run.setFontSize(fontSize);
        run.setText(text);
    }

    private void addParagraph(XWPFDocument document, String text, int fontSize) {
        XWPFParagraph para = document.createParagraph();
        XWPFRun run = para.createRun();
        run.setFontSize(fontSize);
        run.setText(text);
    }

    private void addEmptyLine(XWPFDocument document) {
        document.createParagraph();
    }

    private byte[] toByteArray(XWPFDocument document) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        document.write(baos);
        return baos.toByteArray();
    }
}
