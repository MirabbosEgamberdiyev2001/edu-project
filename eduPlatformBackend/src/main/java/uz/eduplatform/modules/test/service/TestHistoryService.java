package uz.eduplatform.modules.test.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.eduplatform.core.audit.AuditService;
import uz.eduplatform.core.common.dto.PagedResponse;
import uz.eduplatform.core.common.exception.BusinessException;
import uz.eduplatform.core.common.exception.ResourceNotFoundException;
import uz.eduplatform.core.i18n.AcceptLanguage;
import uz.eduplatform.core.i18n.LocaleKeys;
import uz.eduplatform.core.i18n.TranslatedField;
import uz.eduplatform.modules.auth.domain.User;
import uz.eduplatform.modules.auth.repository.UserRepository;
import uz.eduplatform.modules.content.domain.Subject;
import uz.eduplatform.modules.content.repository.SubjectRepository;
import uz.eduplatform.modules.test.domain.GlobalStatus;
import uz.eduplatform.modules.test.domain.TestCategory;
import uz.eduplatform.modules.test.domain.TestHistory;
import uz.eduplatform.modules.test.domain.TestStatus;
import uz.eduplatform.modules.test.dto.*;
import uz.eduplatform.modules.test.repository.TestHistoryRepository;
import uz.eduplatform.modules.test.repository.TestQuestionRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TestHistoryService {

    private final TestHistoryRepository testHistoryRepository;
    private final TestQuestionRepository testQuestionRepository;
    private final TestGenerationService generationService;
    private final SubjectRepository subjectRepository;
    private final AuditService auditService;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PagedResponse<TestHistoryDto> getTestHistory(UUID userId, Pageable pageable, AcceptLanguage language) {
        Page<TestHistory> page = testHistoryRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                userId, pageable);

        // Batch-fetch all subjects to avoid N+1 queries
        Set<UUID> subjectIds = page.getContent().stream()
                .map(TestHistory::getSubjectId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, Subject> subjectMap = subjectIds.isEmpty()
                ? Map.of()
                : subjectRepository.findAllById(subjectIds).stream()
                        .collect(Collectors.toMap(Subject::getId, s -> s, (a, b) -> a));

        List<TestHistoryDto> dtos = page.getContent().stream()
                .map(h -> mapToDto(h, language, subjectMap.get(h.getSubjectId())))
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
                .titleTranslations(original.getTitleTranslations())
                .category(original.getCategory())
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
                .titleTranslations(original.getTitleTranslations())
                .category(original.getCategory())
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

    @SuppressWarnings("unchecked")
    private HeaderConfig toHeaderConfig(Map<String, Object> map) {
        if (map == null) return null;
        HeaderConfig.HeaderConfigBuilder builder = HeaderConfig.builder()
                .schoolName((String) map.get("schoolName"))
                .className((String) map.get("className"))
                .teacherName((String) map.get("teacherName"))
                .logoUrl((String) map.get("logoUrl"))
                .date((String) map.get("date"));

        Object snt = map.get("schoolNameTranslations");
        if (snt instanceof Map) builder.schoolNameTranslations((Map<String, String>) snt);

        Object tnt = map.get("teacherNameTranslations");
        if (tnt instanceof Map) builder.teacherNameTranslations((Map<String, String>) tnt);

        return builder.build();
    }

    private TestHistoryDto mapToDto(TestHistory h, AcceptLanguage language) {
        Subject subject = null;
        try {
            subject = subjectRepository.findById(h.getSubjectId()).orElse(null);
        } catch (Exception ignored) {
        }
        return mapToDto(h, language, subject);
    }

    @SuppressWarnings("unchecked")
    private TestHistoryDto mapToDto(TestHistory h, AcceptLanguage language, Subject subject) {
        String subjectName = null;
        if (subject != null) {
            subjectName = TranslatedField.resolve(subject.getName(), language.toLocaleKey());
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
                .titleTranslations(h.getTitleTranslations())
                .category(h.getCategory())
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
                .isPublic(h.getIsPublic())
                .publicSlug(h.getPublicSlug())
                .publicDurationMinutes(h.getPublicDurationMinutes())
                .status(h.getStatus())
                .createdAt(h.getCreatedAt())
                .globalStatus(h.getGlobalStatus())
                .globalRejectionReason(h.getGlobalRejectionReason())
                .globalSubmittedAt(h.getGlobalSubmittedAt())
                .globalReviewedAt(h.getGlobalReviewedAt())
                .gradeLevel(h.getGradeLevel())
                .build();
    }

    @Transactional
    public TestHistoryDto updateTest(UUID testId, UUID userId, UpdateTestRequest request, AcceptLanguage language) {
        TestHistory history = testHistoryRepository.findByIdAndUserIdAndDeletedAtIsNull(testId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Test", "id", testId));

        if (request.getTitleTranslations() != null && !request.getTitleTranslations().isEmpty()) {
            history.setTitleTranslations(request.getTitleTranslations());
            String resolved = TranslatedField.resolve(request.getTitleTranslations());
            if (resolved != null) history.setTitle(resolved);
        } else if (request.getTitle() != null && !request.getTitle().isBlank()) {
            history.setTitle(request.getTitle());
        }
        if (request.getCategory() != null) {
            history.setCategory(request.getCategory());
        }
        if (request.getHeaderConfig() != null) {
            HeaderConfig hc = request.getHeaderConfig();
            Map<String, Object> headerMap = new HashMap<>();
            if (hc.getSchoolName() != null) headerMap.put("schoolName", hc.getSchoolName());
            if (hc.getSchoolNameTranslations() != null) headerMap.put("schoolNameTranslations", hc.getSchoolNameTranslations());
            if (hc.getClassName() != null) headerMap.put("className", hc.getClassName());
            if (hc.getTeacherName() != null) headerMap.put("teacherName", hc.getTeacherName());
            if (hc.getTeacherNameTranslations() != null) headerMap.put("teacherNameTranslations", hc.getTeacherNameTranslations());
            if (hc.getLogoUrl() != null) headerMap.put("logoUrl", hc.getLogoUrl());
            if (hc.getDate() != null) headerMap.put("date", hc.getDate());
            history.setHeaderConfig(headerMap);
        }

        testHistoryRepository.save(history);

        auditService.log(userId, null, "TEST_UPDATED", "TEST",
                "TestHistory", testId);

        return mapToDto(history, language);
    }

    @Transactional
    public TestHistoryDto publishTest(UUID testId, UUID userId, Integer durationMinutes, AcceptLanguage language) {
        TestHistory history = testHistoryRepository.findByIdAndUserIdAndDeletedAtIsNull(testId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Test", "id", testId));

        history.setIsPublic(true);
        history.setPublicDurationMinutes(durationMinutes);
        if (history.getPublicSlug() == null) {
            history.setPublicSlug(generateSlug());
        }
        testHistoryRepository.save(history);
        return mapToDto(history, language);
    }

    // ===== Global Test Moderation Methods =====

    @Transactional
    public TestHistoryDto submitForGlobal(UUID testId, UUID userId, AcceptLanguage language) {
        TestHistory history = testHistoryRepository.findByIdAndUserIdAndDeletedAtIsNull(testId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Test", "id", testId));

        if (history.getGlobalStatus() == GlobalStatus.PENDING_MODERATION) {
            throw BusinessException.ofKey("test.global.already.pending");
        }
        if (history.getGlobalStatus() == GlobalStatus.APPROVED) {
            throw BusinessException.ofKey("test.global.already.approved");
        }

        history.setGlobalStatus(GlobalStatus.PENDING_MODERATION);
        history.setGlobalSubmittedAt(LocalDateTime.now());
        history.setGlobalRejectionReason(null);
        testHistoryRepository.save(history);

        auditService.log(userId, null, "TEST_SUBMITTED_FOR_GLOBAL", "TEST", "TestHistory", testId);
        return mapToDto(history, language);
    }

    @Transactional
    public TestHistoryDto approveGlobal(UUID testId, UUID moderatorId, AcceptLanguage language) {
        TestHistory history = testHistoryRepository.findByIdAndDeletedAtIsNull(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test", "id", testId));

        if (history.getGlobalStatus() != GlobalStatus.PENDING_MODERATION) {
            throw BusinessException.ofKey("test.global.not.pending");
        }

        history.setGlobalStatus(GlobalStatus.APPROVED);
        history.setGlobalReviewedAt(LocalDateTime.now());
        history.setGlobalReviewedBy(moderatorId);
        testHistoryRepository.save(history);

        auditService.log(moderatorId, null, "TEST_GLOBAL_APPROVED", "TEST", "TestHistory", testId);
        return mapToDto(history, language);
    }

    @Transactional
    public TestHistoryDto rejectGlobal(UUID testId, UUID moderatorId, String reason, AcceptLanguage language) {
        TestHistory history = testHistoryRepository.findByIdAndDeletedAtIsNull(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test", "id", testId));

        history.setGlobalStatus(GlobalStatus.REJECTED);
        history.setGlobalReviewedAt(LocalDateTime.now());
        history.setGlobalReviewedBy(moderatorId);
        history.setGlobalRejectionReason(reason);
        testHistoryRepository.save(history);

        auditService.log(moderatorId, null, "TEST_GLOBAL_REJECTED", "TEST", "TestHistory", testId);
        return mapToDto(history, language);
    }

    @Transactional(readOnly = true)
    public PagedResponse<TestHistoryDto> getPendingGlobalTests(Pageable pageable, AcceptLanguage language) {
        Page<TestHistory> page = testHistoryRepository.findByGlobalStatusAndDeletedAtIsNull(
                GlobalStatus.PENDING_MODERATION, pageable);
        return toPagedResponse(page, language);
    }

    @Transactional(readOnly = true)
    public PagedResponse<TestHistoryDto> getApprovedGlobalTests(
            TestCategory category, UUID subjectId, Integer gradeLevel,
            Pageable pageable, AcceptLanguage language) {
        Page<TestHistory> page = testHistoryRepository.findGlobalTests(
                GlobalStatus.APPROVED.name(),
                category != null ? category.name() : null,
                subjectId != null ? subjectId.toString() : null,
                gradeLevel, pageable);
        return toPagedResponse(page, language);
    }

    private PagedResponse<TestHistoryDto> toPagedResponse(Page<TestHistory> page, AcceptLanguage language) {
        Set<UUID> subjectIds = page.getContent().stream()
                .map(TestHistory::getSubjectId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, Subject> subjectMap = subjectIds.isEmpty()
                ? Map.of()
                : subjectRepository.findAllById(subjectIds).stream()
                        .collect(Collectors.toMap(Subject::getId, s -> s, (a, b) -> a));

        List<TestHistoryDto> dtos = page.getContent().stream()
                .map(h -> mapToDtoWithTeacher(h, language, subjectMap.get(h.getSubjectId())))
                .toList();

        return PagedResponse.of(dtos, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    private TestHistoryDto mapToDtoWithTeacher(TestHistory h, AcceptLanguage language, Subject subject) {
        TestHistoryDto dto = mapToDto(h, language, subject);
        // Add teacher name for moderators
        if (h.getUserId() != null) {
            userRepository.findById(h.getUserId()).ifPresent(user ->
                dto.setTeacherName(user.getFirstName() + " " + user.getLastName()));
        }
        return dto;
    }

    @Transactional
    public TestHistoryDto unpublishTest(UUID testId, UUID userId, AcceptLanguage language) {
        TestHistory history = testHistoryRepository.findByIdAndUserIdAndDeletedAtIsNull(testId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Test", "id", testId));

        history.setIsPublic(false);
        testHistoryRepository.save(history);
        return mapToDto(history, language);
    }

    @Transactional(readOnly = true)
    public TestHistoryDto getPublicTestBySlug(String slug) {
        TestHistory history = testHistoryRepository.findByPublicSlugAndIsPublicTrueAndDeletedAtIsNull(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Test", "slug", slug));
        return mapToDto(history, AcceptLanguage.UZL);
    }

    private String generateSlug() {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(8);
        java.util.Random rnd = new java.security.SecureRandom();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
