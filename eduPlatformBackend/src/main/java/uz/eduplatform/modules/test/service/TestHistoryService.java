package uz.eduplatform.modules.test.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.eduplatform.core.audit.AuditService;
import uz.eduplatform.core.common.dto.PagedResponse;
import uz.eduplatform.core.common.exception.ResourceNotFoundException;
import uz.eduplatform.core.i18n.AcceptLanguage;
import uz.eduplatform.core.i18n.TranslatedField;
import uz.eduplatform.modules.content.domain.Subject;
import uz.eduplatform.modules.content.repository.SubjectRepository;
import uz.eduplatform.modules.test.domain.TestHistory;
import uz.eduplatform.modules.test.domain.TestStatus;
import uz.eduplatform.modules.test.dto.*;
import uz.eduplatform.modules.test.repository.TestHistoryRepository;
import uz.eduplatform.modules.test.repository.TestQuestionRepository;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TestHistoryService {

    private final TestHistoryRepository testHistoryRepository;
    private final TestQuestionRepository testQuestionRepository;
    private final TestGenerationService generationService;
    private final SubjectRepository subjectRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PagedResponse<TestHistoryDto> getTestHistory(UUID userId, Pageable pageable, AcceptLanguage language) {
        Page<TestHistory> page = testHistoryRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                userId, pageable);

        List<TestHistoryDto> dtos = page.getContent().stream()
                .map(h -> mapToDto(h, language))
                .toList();

        return PagedResponse.of(dtos, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public TestHistoryDto getTestById(UUID testId, UUID userId, AcceptLanguage language) {
        TestHistory history = testHistoryRepository.findByIdAndUserIdAndDeletedAtIsNull(testId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Test", "id", testId));
        return mapToDto(history, language);
    }

    @Transactional
    public void deleteTest(UUID testId, UUID userId) {
        TestHistory history = testHistoryRepository.findByIdAndUserIdAndDeletedAtIsNull(testId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Test", "id", testId));

        history.setDeletedAt(LocalDateTime.now());
        history.setStatus(TestStatus.DELETED);
        testHistoryRepository.save(history);

        auditService.log(userId, null, "TEST_DELETED", "TEST",
                "TestHistory", testId);
    }

    @Transactional
    public GenerateTestResponse duplicateTest(UUID testId, UUID userId) {
        TestHistory original = testHistoryRepository.findByIdAndUserIdAndDeletedAtIsNull(testId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Test", "id", testId));

        GenerateTestRequest request = GenerateTestRequest.builder()
                .title(original.getTitle() + " (Copy)")
                .subjectId(original.getSubjectId())
                .topicIds(original.getTopicIds())
                .questionCount(original.getQuestionCount())
                .variantCount(original.getVariantCount())
                .difficultyDistribution(toDifficultyDistribution(original.getDifficultyDistribution()))
                .shuffleQuestions(original.getShuffleQuestions())
                .shuffleOptions(original.getShuffleOptions())
                .headerConfig(toHeaderConfig(original.getHeaderConfig()))
                .randomSeed(null) // new random for duplicate
                .build();

        return generationService.generateTest(userId, request);
    }

    @Transactional
    public GenerateTestResponse regenerateTest(UUID testId, UUID userId) {
        TestHistory original = testHistoryRepository.findByIdAndUserIdAndDeletedAtIsNull(testId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Test", "id", testId));

        GenerateTestRequest request = GenerateTestRequest.builder()
                .title(original.getTitle())
                .subjectId(original.getSubjectId())
                .topicIds(original.getTopicIds())
                .questionCount(original.getQuestionCount())
                .variantCount(original.getVariantCount())
                .difficultyDistribution(toDifficultyDistribution(original.getDifficultyDistribution()))
                .shuffleQuestions(original.getShuffleQuestions())
                .shuffleOptions(original.getShuffleOptions())
                .headerConfig(toHeaderConfig(original.getHeaderConfig()))
                .randomSeed(original.getRandomSeed()) // same seed = same result
                .build();

        // Soft delete old test
        original.setDeletedAt(LocalDateTime.now());
        original.setStatus(TestStatus.DELETED);
        testHistoryRepository.save(original);

        return generationService.generateTest(userId, request);
    }

    private DifficultyDistribution toDifficultyDistribution(Map<String, Integer> map) {
        if (map == null) return null;
        return DifficultyDistribution.builder()
                .easy(map.getOrDefault("easy", 30))
                .medium(map.getOrDefault("medium", 50))
                .hard(map.getOrDefault("hard", 20))
                .build();
    }

    private HeaderConfig toHeaderConfig(Map<String, Object> map) {
        if (map == null) return null;
        return HeaderConfig.builder()
                .schoolName((String) map.get("schoolName"))
                .className((String) map.get("className"))
                .teacherName((String) map.get("teacherName"))
                .logoUrl((String) map.get("logoUrl"))
                .date((String) map.get("date"))
                .build();
    }

    @SuppressWarnings("unchecked")
    private TestHistoryDto mapToDto(TestHistory h, AcceptLanguage language) {
        String subjectName = null;
        try {
            Subject subject = subjectRepository.findById(h.getSubjectId()).orElse(null);
            if (subject != null) subjectName = TranslatedField.resolve(subject.getName(), language.toLocaleKey());
        } catch (Exception ignored) {
        }

        // Convert variants from stored JSON
        List<VariantDto> variantDtos = new ArrayList<>();
        if (h.getVariants() != null) {
            for (Map<String, Object> vm : h.getVariants()) {
                List<UUID> qIds = new ArrayList<>();
                Object qIdsObj = vm.get("questionIds");
                if (qIdsObj instanceof List<?> idList) {
                    for (Object id : idList) {
                        if (id instanceof String) {
                            qIds.add(UUID.fromString((String) id));
                        } else if (id instanceof UUID) {
                            qIds.add((UUID) id);
                        }
                    }
                }

                variantDtos.add(VariantDto.builder()
                        .code((String) vm.get("code"))
                        .questionIds(qIds)
                        .answerKey((List<Map<String, Object>>) vm.get("answerKey"))
                        .optionsOrder((List<List<String>>) vm.get("optionsOrder"))
                        .build());
            }
        }

        return TestHistoryDto.builder()
                .id(h.getId())
                .userId(h.getUserId())
                .title(h.getTitle())
                .subjectId(h.getSubjectId())
                .subjectName(subjectName)
                .topicIds(h.getTopicIds())
                .questionCount(h.getQuestionCount())
                .variantCount(h.getVariantCount())
                .difficultyDistribution(h.getDifficultyDistribution())
                .shuffleQuestions(h.getShuffleQuestions())
                .shuffleOptions(h.getShuffleOptions())
                .randomSeed(h.getRandomSeed())
                .headerConfig(h.getHeaderConfig())
                .variants(variantDtos)
                .testPdfUrl(h.getTestPdfUrl())
                .answerKeyPdfUrl(h.getAnswerKeyPdfUrl())
                .combinedPdfUrl(h.getCombinedPdfUrl())
                .proofsPdfUrl(h.getProofsPdfUrl())
                .downloadCount(h.getDownloadCount())
                .lastDownloadedAt(h.getLastDownloadedAt())
                .status(h.getStatus())
                .createdAt(h.getCreatedAt())
                .build();
    }
}
