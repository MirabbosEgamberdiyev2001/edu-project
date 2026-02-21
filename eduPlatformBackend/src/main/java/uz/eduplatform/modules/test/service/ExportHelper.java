package uz.eduplatform.modules.test.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uz.eduplatform.core.common.exception.ResourceNotFoundException;
import uz.eduplatform.modules.test.domain.TestHistory;
import uz.eduplatform.modules.test.repository.TestHistoryRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ExportHelper {

    private final TestHistoryRepository testHistoryRepository;

    public TestHistory getTestHistory(UUID testId, UUID userId) {
        return testHistoryRepository.findByIdAndUserIdAndDeletedAtIsNull(testId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Test", "id", testId));
    }

    public void updateDownloadCount(TestHistory test) {
        test.setDownloadCount(test.getDownloadCount() + 1);
        test.setLastDownloadedAt(LocalDateTime.now());
        testHistoryRepository.save(test);
    }

    @SuppressWarnings("unchecked")
    public List<UUID> parseQuestionIds(Object qIdsObj) {
        List<UUID> ids = new ArrayList<>();
        if (qIdsObj instanceof List<?> list) {
            for (Object id : list) {
                if (id instanceof String) ids.add(UUID.fromString((String) id));
                else if (id instanceof UUID) ids.add((UUID) id);
            }
        }
        return ids;
    }

    @SuppressWarnings("unchecked")
    public List<List<String>> parseOptionsOrder(Object optOrderObj) {
        if (optOrderObj instanceof List<?> list) {
            List<List<String>> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof List<?> innerList) {
                    result.add(innerList.stream().map(String::valueOf).toList());
                } else {
                    result.add(null);
                }
            }
            return result;
        }
        return null;
    }

    public List<String> wrapText(String text, int maxChars) {
        if (text == null) return List.of();
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\n")) {
            while (paragraph.length() > maxChars) {
                int breakAt = paragraph.lastIndexOf(' ', maxChars);
                if (breakAt <= 0) breakAt = maxChars;
                lines.add(paragraph.substring(0, breakAt));
                paragraph = paragraph.substring(breakAt).trim();
            }
            if (!paragraph.isEmpty()) lines.add(paragraph);
        }
        return lines;
    }

    public String truncateText(String text, int maxLen) {
        if (text == null) return "";
        text = text.replace("\n", " ").replace("\r", "");
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }
}
