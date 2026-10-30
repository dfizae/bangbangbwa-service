package com.backend.bangbangbwa.domain.document.client;

import com.backend.bangbangbwa.common.exception.CustomException;
import com.backend.bangbangbwa.common.exception.ErrorCode;
import com.backend.bangbangbwa.domain.document.entity.DocumentType;
import com.backend.bangbangbwa.domain.document.entity.RiskLevel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GmsDocumentAnalysisClient implements DocumentAnalysisClient {

    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final int MAX_TOKENS = 2000;

    // 실패 로그에 남길 응답 본문 미리보기 최대 길이
    private static final int LOG_SNIPPET_LENGTH = 500;

    private static final String REGISTER_SYSTEM_PROMPT = """
            너는 부동산 권리관계 분석 전문가다. 첨부된 등기부등본 또는 건축물대장을 분석해
            아래 항목을 추출하고, 제공된 매물 정보와 대조해 위험도를 판정하라.

            [추출]
            - 소유자명, 부동산 표시(주소)
            - 근저당권: 채권자, 채권최고액, 설정일 (복수 가능)
            - 압류 / 가압류 / 가처분 / 경매개시 여부
            - 신탁 등기 여부
            - (건축물대장) 위반건축물 여부, 용도

            [대조]
            - 등기부 부동산 표시 주소와 제공된 매물 주소 일치 여부(addressMatches)
            - 소유자 일치 여부(ownerMatches) — 판단 불가 시 true 로 두지 말고 근거를 reasons 에 남겨라

            [위험도 판정]
            - 위험: 압류·가압류·경매·신탁 존재 / 주소 불일치 / 근저당 채권최고액 과다
            - 주의: 소액 근저당만 존재 / 경미한 불일치 / 위반건축물
            - 안전: 권리침해 없음, 정보 일치

            반드시 아래 JSON 스키마로만 응답하라. 코드블록·설명 없이 JSON 객체만 출력하라.
            {
              "documentType": "등기부등본 | 건축물대장",
              "owner": "소유자명",
              "ownerMatches": true,
              "addressMatches": true,
              "mortgages": [{ "creditor": "", "maxClaim": 0, "date": "" }],
              "seizures": [],
              "trust": false,
              "violationBuilding": false,
              "riskLevel": "안전 | 주의 | 위험",
              "summary": "한 줄 요약",
              "reasons": ["판정 근거"]
            }
            """;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public GmsDocumentAnalysisClient(
            @Value("${gms.base-url}") String baseUrl,
            @Value("${gms.api-key}") String apiKey,
            @Value("${gms.model}") String model
    ) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public AnalysisResult analyze(
            String documentUrl,
            DocumentType documentType,
            DocumentContext context
    ) {
        // base64 인라인 대신 문서 URL 을 전달해 요청 body 를 작게 유지한다.
        // (AI 서버가 URL 로 원본 PDF 를 직접 내려받으므로 GMS 요청 크기 제한을 우회)
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_tokens", MAX_TOKENS,
                "system", REGISTER_SYSTEM_PROMPT,
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", List.of(
                                Map.of(
                                        "type", "document",
                                        "source", Map.of(
                                                "type", "url",
                                                "url", documentUrl
                                        )
                                ),
                                Map.of("type", "text", "text",
                                        buildUserText(documentType, context))
                        )
                ))
        );

        log.info("서류 분석 요청: type={}, mode=DOCUMENT_URL", documentType.getValue());

        String responseBody;
        try {
            responseBody = restClient.post()
                    .uri("/v1/messages")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException exception) {
            // GMS/AI 서버가 HTTP 오류 응답을 준 경우 — 상태·본문을 남긴다.
            log.warn("서류 분석 실패(GMS 응답 오류): status={}, body={}",
                    exception.getStatusCode(),
                    snippet(exception.getResponseBodyAsString()));
            throw new CustomException(ErrorCode.DOCUMENT_ANALYSIS_FAILED);
        } catch (RuntimeException exception) {
            log.warn("서류 분석 실패(GMS 요청 중 예외, 네트워크/기타)", exception);
            throw new CustomException(ErrorCode.DOCUMENT_ANALYSIS_FAILED);
        }

        return parse(responseBody);
    }

    private String buildUserText(DocumentType documentType, DocumentContext context) {
        String address = context != null && context.address() != null
                ? context.address()
                : "(주소 정보 없음)";
        return "서류 종류: " + documentType.getValue()
                + "\n매물 등록 주소: " + address
                + "\n\n위 서류를 분석하고, JSON 스키마로만 응답하라.";
    }

    private AnalysisResult parse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String text = root.path("content").get(0).path("text").asText();
            String json = extractJson(text);

            JsonNode result = objectMapper.readTree(json);
            RiskLevel riskLevel = RiskLevel.from(result.path("riskLevel").asText());
            String summary = result.path("summary").asText("");

            return new AnalysisResult(riskLevel, summary, json);
        } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException exception) {
            // GMS 호출은 성공(200)했으나 AI 응답을 JSON 스키마로 해석하지 못한 경우.
            log.warn("서류 분석 실패(AI 응답 파싱 불가): response={}",
                    snippet(responseBody), exception);
            throw new CustomException(ErrorCode.DOCUMENT_ANALYSIS_FAILED);
        }
    }

    private String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end < 0 || end < start) {
            throw new CustomException(ErrorCode.DOCUMENT_ANALYSIS_FAILED);
        }
        return text.substring(start, end + 1);
    }

    private static String snippet(String text) {
        if (text == null || text.isBlank()) {
            return "(없음)";
        }
        String cleaned = text.replaceAll("\\s+", " ").strip();
        return cleaned.length() <= LOG_SNIPPET_LENGTH
                ? cleaned
                : cleaned.substring(0, LOG_SNIPPET_LENGTH) + "...(생략)";
    }
}
