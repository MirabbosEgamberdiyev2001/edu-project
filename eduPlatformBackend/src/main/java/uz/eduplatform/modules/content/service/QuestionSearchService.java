package uz.eduplatform.modules.content.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.eduplatform.core.common.dto.PagedResponse;
import uz.eduplatform.core.i18n.AcceptLanguage;
import uz.eduplatform.modules.content.domain.Question;
import uz.eduplatform.modules.content.dto.QuestionDto;
import uz.eduplatform.modules.content.repository.QuestionRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuestionSearchService {

    private final QuestionRepository questionRepository;
    private final QuestionService questionService;

    @Transactional(readOnly = true)
    public PagedResponse<QuestionDto> search(UUID userId, String query, Pageable pageable, AcceptLanguage language) {
        String localeKey = language.toLocaleKey();
        Page<Question> page = questionRepository.searchByUser(userId, query.trim(), pageable);

        List<QuestionDto> dtos = page.getContent().stream()
                .map(q -> questionService.mapToDto(q, localeKey))
                .toList();

        return PagedResponse.of(dtos, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
