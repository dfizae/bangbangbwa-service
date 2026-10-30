package com.backend.bangbangbwa.domain.report.service;

import com.backend.bangbangbwa.domain.checklist.dto.ChecklistItemResponse;
import com.backend.bangbangbwa.domain.checklist.dto.ChecklistResponse;
import com.backend.bangbangbwa.domain.property.storage.StorageService;
import com.backend.bangbangbwa.domain.report.dto.CaptureResponse;
import com.backend.bangbangbwa.domain.report.dto.DefectResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GMS를 경유해 Anthropic Claude로 총평을 생성하는 구현체.
 *
 * <p>체크리스트와 하자 분석 결과, 캡쳐 이미지(S3 presigned URL)를 전달하여 임차인에게 도움이 되는
 * 한국어 총평을 생성합니다.
 * API 키가 없거나 호출에 실패하면 {@code null}을 반환합니다.
 */
@Slf4j
@Component
public class AnthropicReportReviewGenerator implements ReportReviewGenerator {

    private static final int MAX_USER_CAPTURES = 10;
    private static final int MAX_DEFECT_CAPTURES = 10;
    private static final Duration PRESIGNED_URL_TTL = Duration.ofMinutes(60);

    private static final String SYSTEM_PROMPT = """
            너는 부동산 매물 방문 점검 리포트를 작성하는 전문가야.
            아래에 제공되는 체크리스트 결과, 하자(defect) 분석 결과와 캡쳐 이미지를 종합해서
            임차인이 계약 여부를 판단하는 데 도움이 되는 총평을 한국어로 작성해줘.
            - 3~5문장 분량으로 간결하게 작성해.
            - 발견된 하자와 주의할 점을 우선적으로 언급해.
            - 근거 없는 추측은 피하고, 제공된 정보에 기반해서 서술해.
            """;

    private final RestClient restClient;
    private final StorageService storageService;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final int maxTokens;

    public AnthropicReportReviewGenerator(
            RestClient restClient,
            StorageService storageService,
            @Value("${gms.api-key}") String apiKey,
            @Value("${gms.base-url}") String baseUrl,
            @Value("${gms.model}") String model,
            @Value("${gms.max-tokens:1024}") int maxTokens
    ) {
        this.restClient = restClient;
        this.storageService = storageService;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.maxTokens = maxTokens;
    }

    @Override
    public String generateReview(
            String propertyTitle,
            ChecklistResponse checklist,
            List<CaptureResponse> captures
    ) {
        if (!StringUtils.hasText(apiKey)) {
            log.warn("GMS API 키가 설정되지 않아 AI 총평 생성을 건너뜁니다.");
            return null;
        }

        List<Map<String, Object>> userContent =
                buildUserContent(propertyTitle, checklist, captures);

        // GMS 프록시는 요청 앞부분에서 model 필드를 찾는다. Map.of는 필드 순서를
        // 보장하지 않아 이미지가 커지면 model이 뒤로 밀려 "Model not found"(400)가
        // 발생할 수 있어, LinkedHashMap으로 model을 항상 맨 앞에 둔다.
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("system", SYSTEM_PROMPT);
        requestBody.put("messages", List.of(
                Map.of("role", "user", "content", userContent)
        ));

        log.info(
                "AI 총평 요청 전송: url={}, model=[{}], images={}",
                baseUrl + "/v1/messages", model, userContent.size() - 1
        );

        AnthropicMessagesResponse response;
        try {
            response = restClient.post()
                    .uri(baseUrl + "/v1/messages")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(AnthropicMessagesResponse.class);
        } catch (org.springframework.web.client.RestClientResponseException e) {
            log.error("Anthropic 호출 실패: status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        }

        String content = extractContent(response);

        if (!StringUtils.hasText(content)) {
            log.warn("Anthropic 응답에서 총평 텍스트를 찾지 못했습니다.");
            return null;
        }

        return content.trim();
    }

    private List<Map<String, Object>> buildUserContent(
            String propertyTitle,
            ChecklistResponse checklist,
            List<CaptureResponse> captures
    ) {
        List<Map<String, Object>> content = new ArrayList<>();

        if (captures != null) {
            // 사용자 캡처(하자 없음)와 AI 검출(하자 있음)을 나눠 각각 최대 10장씩,
            // 총 20장을 넘지 않게 첨부한다. 한쪽이 10장보다 적으면 있는 만큼만 넣는다.
            List<CaptureResponse> userCaptures = new ArrayList<>();
            List<CaptureResponse> defectCaptures = new ArrayList<>();
            for (CaptureResponse capture : captures) {
                if (!StringUtils.hasText(capture.getImageUrl())) {
                    continue;
                }
                boolean hasDefect = capture.getDefects() != null
                        && !capture.getDefects().isEmpty();
                if (hasDefect) {
                    defectCaptures.add(capture);
                } else {
                    userCaptures.add(capture);
                }
            }

            addImageContents(content, userCaptures, MAX_USER_CAPTURES);
            addImageContents(content, defectCaptures, MAX_DEFECT_CAPTURES);
        }

        content.add(Map.of(
                "type", "text",
                "text", buildTextContext(propertyTitle, checklist, captures)
        ));
        return content;
    }

    /**
     * 캡처 목록에서 최대 {@code limit}장까지 S3 presigned URL 이미지 블록으로 추가합니다.
     * base64 인라인 대신 URL을 넘겨 요청 바디를 작게 유지합니다.
     */
    private void addImageContents(
            List<Map<String, Object>> content,
            List<CaptureResponse> captures,
            int limit
    ) {
        int count = 0;
        for (CaptureResponse capture : captures) {
            if (count >= limit) {
                break;
            }
            String presignedUrl = storageService.createPresignedGetUrl(
                    capture.getImageUrl(),
                    PRESIGNED_URL_TTL
            );
            content.add(Map.of(
                    "type", "image",
                    "source", Map.of(
                            "type", "url",
                            "url", presignedUrl
                    )
            ));
            count++;
        }
    }

    private String buildTextContext(
            String propertyTitle,
            ChecklistResponse checklist,
            List<CaptureResponse> captures
    ) {
        StringBuilder text = new StringBuilder();

        text.append("[매물] ")
                .append(StringUtils.hasText(propertyTitle) ? propertyTitle : "정보 없음")
                .append("\n\n");

        text.append("[체크리스트]\n");
        if (checklist == null
                || checklist.getItems() == null
                || checklist.getItems().isEmpty()) {
            text.append("- 항목 없음\n");
        } else {
            for (ChecklistItemResponse item : checklist.getItems()) {
                text.append("- ")
                        .append(item.getContent())
                        .append(" (상태: ")
                        .append(item.getStatus() == null ? "미정" : item.getStatus().name())
                        .append(")");
                if (StringUtils.hasText(item.getMemo())) {
                    text.append(" 메모: ").append(item.getMemo());
                }
                text.append("\n");
            }
        }

        text.append("\n[하자 분석 결과]\n");
        List<DefectResponse> defects = collectDefects(captures);
        if (defects.isEmpty()) {
            text.append("- 감지된 하자 없음\n");
        } else {
            for (DefectResponse defect : defects) {
                text.append("- 유형: ")
                        .append(defect.getType() == null ? "미상" : defect.getType());
                if (defect.getDescription() != null) {
                    text.append(", 설명: ").append(defect.getDescription());
                }
                if (defect.getConfidence() != null) {
                    text.append(String.format(", 신뢰도: %.2f", defect.getConfidence()));
                }
                text.append("\n");
            }
        }

        text.append("\n위 정보와 첨부된 캡쳐 이미지를 종합해서 총평을 작성해줘.");

        return text.toString();
    }

    private List<DefectResponse> collectDefects(List<CaptureResponse> captures) {
        List<DefectResponse> defects = new ArrayList<>();
        if (captures == null) {
            return defects;
        }
        for (CaptureResponse capture : captures) {
            if (capture.getDefects() != null) {
                defects.addAll(capture.getDefects());
            }
        }
        return defects;
    }

    private String extractContent(AnthropicMessagesResponse response) {
        if (response == null
                || response.content() == null
                || response.content().isEmpty()) {
            return null;
        }

        return response.content().stream()
                .filter(block -> block != null && "text".equals(block.type()))
                .map(ContentBlock::text)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AnthropicMessagesResponse(List<ContentBlock> content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ContentBlock(String type, String text) {
    }

}
