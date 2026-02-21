package uz.eduplatform.modules.content.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uz.eduplatform.core.common.dto.ApiResponse;
import uz.eduplatform.core.common.utils.MessageService;
import uz.eduplatform.core.i18n.AcceptLanguage;
import uz.eduplatform.core.security.UserPrincipal;
import uz.eduplatform.modules.content.dto.*;
import uz.eduplatform.modules.content.service.TopicService;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Mavzular", description = "Mavzularni boshqarish API'lari — yaratish, tahrirlash, o'chirish, tartiblash")
@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
public class TopicController {

    private final TopicService topicService;
    private final MessageService messageService;

    @GetMapping("/api/v1/subjects/{subjectId}/topics")
    @Operation(summary = "Fan mavzulari daraxtini olish", description = "Berilgan fan bo'yicha barcha mavzularni ierarxik daraxt ko'rinishida olish. Ota-bola munosabatlari saqlanadi.")
    public ResponseEntity<ApiResponse<List<TopicTreeDto>>> getTopicTree(
            @PathVariable UUID subjectId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {

        List<TopicTreeDto> tree = topicService.getTopicTree(subjectId, principal.getId(), language);
        return ResponseEntity.ok(ApiResponse.success(tree));
    }

    @PostMapping("/api/v1/subjects/{subjectId}/topics")
    @Operation(summary = "Yangi mavzu yaratish", description = "Fan ichida yangi mavzu yaratish. parentId ko'rsatilsa, ichki mavzu (sub-topic) sifatida yaratiladi.")
    public ResponseEntity<ApiResponse<TopicDto>> createTopic(
            @PathVariable UUID subjectId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language,
            @Valid @RequestBody CreateTopicRequest request) {

        TopicDto topic = topicService.createTopic(subjectId, principal.getId(), request, language);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(topic, messageService.get("topic.created", language.toLocale())));
    }

    @PostMapping("/api/v1/subjects/{subjectId}/topics/bulk")
    @Operation(summary = "Bir nechta mavzu yaratish", description = "Bir so'rovda bir nechta mavzuni ommaviy yaratish. Mavjud nomlar avtomatik o'tkazib yuboriladi.")
    public ResponseEntity<ApiResponse<BulkCreateResponse>> createTopicsBulk(
            @PathVariable UUID subjectId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language,
            @Valid @RequestBody BulkCreateTopicRequest request) {

        BulkCreateResponse response = topicService.createTopicsBulk(subjectId, principal.getId(), request, language);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, messageService.get("topic.bulk.created", language.toLocale(),
                        response.getCreated(), response.getSkipped())));
    }

    @PutMapping("/api/v1/topics/{id}")
    @Operation(summary = "Mavzuni yangilash", description = "Mavzu ma'lumotlarini to'liq yangilash — nomi, tavsifi, tartib raqami.")
    public ResponseEntity<ApiResponse<TopicDto>> updateTopic(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language,
            @Valid @RequestBody UpdateTopicRequest request) {

        TopicDto topic = topicService.updateTopic(id, principal.getId(), request, language);
        return ResponseEntity.ok(ApiResponse.success(topic, messageService.get("topic.updated", language.toLocale())));
    }

    @PatchMapping("/api/v1/topics/{id}")
    @Operation(summary = "Mavzuni qisman yangilash", description = "Mavzuning faqat yuborilgan maydonlarini yangilash.")
    public ResponseEntity<ApiResponse<TopicDto>> patchTopic(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language,
            @Valid @RequestBody UpdateTopicRequest request) {

        TopicDto topic = topicService.updateTopic(id, principal.getId(), request, language);
        return ResponseEntity.ok(ApiResponse.success(topic, messageService.get("topic.updated", language.toLocale())));
    }

    @DeleteMapping("/api/v1/topics/{id}")
    @Operation(summary = "Mavzuni o'chirish", description = "Mavzuni barcha ichki mavzulari va savollari bilan birga yumshoq o'chirish.")
    public ResponseEntity<ApiResponse<Void>> deleteTopic(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language) {

        topicService.deleteTopic(id, principal.getId(), language);
        return ResponseEntity.ok(ApiResponse.success(null, messageService.get("topic.deleted", language.toLocale())));
    }

    @PutMapping("/api/v1/topics/reorder")
    @Operation(summary = "Mavzularni qayta tartiblash", description = "Mavzular tartibini drag-and-drop uslubida o'zgartirish. Yangi tartib raqamlar ro'yxati yuboriladi.")
    public ResponseEntity<ApiResponse<Void>> reorderTopics(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language,
            @Valid @RequestBody ReorderTopicsRequest request) {

        topicService.reorderTopics(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(null, messageService.get("topic.reordered", language.toLocale())));
    }

    @PostMapping("/api/v1/topics/{id}/move")
    @Operation(summary = "Mavzuni ko'chirish", description = "Mavzuni boshqa ota mavzuga yoki boshqa fanga ko'chirish.")
    public ResponseEntity<ApiResponse<TopicDto>> moveTopic(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Accept-Language", defaultValue = "uzl") AcceptLanguage language,
            @Valid @RequestBody MoveTopicRequest request) {

        TopicDto topic = topicService.moveTopic(id, principal.getId(), request, language);
        return ResponseEntity.ok(ApiResponse.success(topic, messageService.get("topic.moved", language.toLocale())));
    }
}
