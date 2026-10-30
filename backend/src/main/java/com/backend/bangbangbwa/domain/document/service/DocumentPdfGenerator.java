package com.backend.bangbangbwa.domain.document.service;

import com.backend.bangbangbwa.common.exception.CustomException;
import com.backend.bangbangbwa.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 매물에 등록된 서류들의 AI 분석 결과(위험도·요약·근거)를 한 편의 HTML로 렌더링한 뒤
 * PDF 바이트로 변환합니다. 서류마다 챕터를 나눠 하나의 합본 리포트로 제공합니다.
 * 업로드된 원본 서류가 아니라, 우리가 분석해 정리한 리포트입니다.
 */
@Slf4j
@Component
public class DocumentPdfGenerator {

    private static final String FONT_PATH = "fonts/NanumGothic-Regular.ttf";
    private static final String FONT_FAMILY = "DocumentFont";

    // 문서 브랜딩 이미지 (리포트와 동일한 classpath 자산 재사용)
    private static final String LOGO_PATH = "images/report-logo.png";
    private static final String WATERMARK_PATH = "images/report-watermark.png";

    // 마크다운 블록 패턴
    private static final Pattern MD_HEADING = Pattern.compile("^(#{1,6})\\s+(.*)$");
    private static final Pattern MD_BULLET = Pattern.compile("^[-*+]\\s+(.*)$");
    private static final Pattern MD_NUMBERED = Pattern.compile("^\\d+[.)]\\s+(.*)$");
    private static final Pattern MD_HR = Pattern.compile("^(-{3,}|\\*{3,}|_{3,})$");
    // 마크다운 인라인 패턴
    private static final Pattern MD_BOLD_STAR = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern MD_BOLD_UNDERSCORE = Pattern.compile("__(.+?)__");
    private static final Pattern MD_CODE = Pattern.compile("`([^`]+?)`");

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 합본 리포트의 챕터 하나에 대응하는 서류별 분석 결과.
     */
    public record DocumentSection(
            String documentType,
            String riskLevel,
            String summary,
            String resultJson
    ) {
    }

    public byte[] generate(Long propertyId, List<DocumentSection> sections) {
        String html = buildHtml(propertyId, sections);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            registerKoreanFont(builder);
            builder.withHtmlContent(html, null);
            builder.toStream(outputStream);
            builder.run();

            return outputStream.toByteArray();
        } catch (Exception exception) {
            log.error(
                    "서류 분석 PDF 생성에 실패했습니다. propertyId={}",
                    propertyId,
                    exception
            );
            throw new CustomException(ErrorCode.PDF_GENERATION_FAILED);
        }
    }

    private void registerKoreanFont(PdfRendererBuilder builder) {
        ClassPathResource fontResource = new ClassPathResource(FONT_PATH);
        if (!fontResource.exists()) {
            log.warn("한글 폰트({})를 찾을 수 없어 기본 폰트로 렌더링합니다.", FONT_PATH);
            return;
        }
        builder.useFont(
                () -> {
                    try {
                        return fontResource.getInputStream();
                    } catch (Exception exception) {
                        throw new CustomException(ErrorCode.PDF_GENERATION_FAILED);
                    }
                },
                FONT_FAMILY
        );
    }

    private String buildHtml(Long propertyId, List<DocumentSection> sections) {
        String logo = imageDataUri(LOGO_PATH);
        String watermark = imageDataUri(WATERMARK_PATH);

        StringBuilder body = new StringBuilder();

        // 워터마크 (본문 뒤, 모든 페이지에 반복)
        if (watermark != null) {
            body.append("<div id=\"watermark\"><img src=\"")
                    .append(watermark).append("\"/></div>");
        }

        // 머리글: 좌측 로고 / 우측 문서번호
        body.append("<table class=\"masthead\"><tr>");
        body.append("<td class=\"mh-logo\">");
        if (logo != null) {
            body.append("<img src=\"").append(logo).append("\"/>");
        }
        body.append("</td>");
        body.append("<td class=\"mh-meta\">");
        body.append("<div><span class=\"k\">문서번호</span>&#160;&#160;")
                .append(escape(documentNo(propertyId))).append("</div>");
        body.append("</td></tr></table>");
        body.append("<div class=\"mh-rule\"></div>");

        // 제목
        body.append("<div class=\"doc-title\">서류 분석 리포트</div>");

        // 서류별 본문 (두 번째 서류부터는 새 페이지에서 시작)
        for (int index = 0; index < sections.size(); index++) {
            appendDocumentBody(body, sections.get(index), index > 0);
        }

        body.append("<p class=\"disclaimer\">본 리포트는 자동 분석 결과를 정리한 참고 자료이며, "
                + "법적 효력을 갖지 않습니다. 정확한 권리관계는 원본 서류와 전문가 확인이 필요합니다.</p>");

        return """
                <html>
                <head>
                <style>
                @page {
                  size: A4;
                  margin: 44px 44px 52px 44px;
                  @bottom-left { content: "방방봐 서류 분석 리포트"; font-family: "%s", sans-serif; font-size: 7.5px; color: #b3b8bf; }
                  @bottom-right { content: counter(page) " / " counter(pages); font-family: "%s", sans-serif; font-size: 7.5px; color: #b3b8bf; }
                }
                body { font-family: "%s", sans-serif; font-size: 11px; color: #1f2328; line-height: 1.55; }

                #watermark { position: fixed; top: 350px; left: 257px; width: 280px; z-index: -1; }
                #watermark img { width: 280px; }

                .masthead { width: 100%%; border-collapse: collapse; }
                .mh-logo img { height: 46px; }
                .mh-meta { text-align: right; vertical-align: middle; font-size: 9px; color: #9aa0a8; line-height: 1.7; }
                .mh-meta .k { color: #c2c7cd; letter-spacing: 0.3px; }
                .mh-rule { border-bottom: 1px solid #1f2328; margin-top: 12px; }

                .doc-title { font-size: 19px; font-weight: bold; color: #1f2328; letter-spacing: 1px; margin: 14px 0 12px 0; }

                .info { width: 100%%; border-collapse: collapse; margin: 4px 0 6px 0; }
                .info th, .info td { border-top: 1px solid #eceef1; border-bottom: 1px solid #eceef1; padding: 6px 10px 6px 0; font-size: 10px; text-align: left; vertical-align: middle; }
                .info th { width: 66px; color: #9aa0a8; font-weight: normal; }
                .info td { color: #1f2328; padding-right: 26px; }
                /* 합본 리포트에서 두 번째 서류부터 새 페이지에서 시작 */
                .page-break { page-break-before: always; }

                h2.sec { font-size: 11.5px; font-weight: bold; color: #1f2328; letter-spacing: 0.5px; margin: 22px 0 9px 0; padding-bottom: 6px; border-bottom: 1px solid #e3e6ea; }
                h2.sec .no { color: #c2c7cd; font-weight: normal; margin-right: 8px; }
                .subh { font-size: 10.5px; font-weight: bold; color: #3a3f46; margin: 12px 0 4px 0; }

                .summary { margin-top: 4px; }
                .md-h1, .md-h2 { font-size: 11px; font-weight: bold; color: #1f2328; margin: 12px 0 5px 0; }
                .md-h3 { font-size: 10.5px; font-weight: bold; color: #3a3f46; margin: 10px 0 4px 0; }
                .md-p { margin: 6px 0; }
                .md-ul, .md-ol { margin: 6px 0; padding-left: 18px; }
                .md-ul li, .md-ol li { margin: 3px 0; }
                .md-quote { margin: 10px 0; padding: 2px 0 2px 12px; border-left: 2px solid #d7dbe0; color: #5b616a; }
                .md-hr { border: none; border-top: 1px solid #eceef1; margin: 10px 0; }
                strong { color: #111418; }
                code { font-family: monospace; background: #f2f3f5; padding: 0 3px; font-size: 9.5px; }

                .meta { margin: 6px 0; }
                .meta .item { margin: 3px 0; font-size: 10px; }
                .meta .label { display: inline-block; width: 84px; color: #9aa0a8; }

                .doc-list { margin: 6px 0; padding-left: 18px; }
                .doc-list li { margin: 3px 0; font-size: 10.5px; }

                table.data { width: 100%%; border-collapse: collapse; margin-top: 8px; font-size: 10px; }
                table.data th { border-top: 1px solid #1f2328; border-bottom: 1px solid #1f2328; padding: 6px 8px; text-align: left; color: #6b7178; font-weight: bold; letter-spacing: 0.3px; }
                table.data td { border-bottom: 1px solid #eceef1; padding: 6px 8px; text-align: left; vertical-align: top; color: #1f2328; }

                .badge { font-size: 9px; font-weight: bold; padding: 1px 7px; border: 1px solid; border-radius: 2px; letter-spacing: 0.3px; }
                .risk-danger { color: #a23b39; border-color: #e2b5b3; background: #faf1f0; }
                .risk-caution { color: #9a6a1c; border-color: #e6d3ad; background: #faf5ea; }
                .risk-safe { color: #3a6b4c; border-color: #bcd6c4; background: #f1f7f3; }
                .risk-none { color: #7b8088; border-color: #dfe2e6; background: #f5f6f8; }

                .empty { color: #aab0b8; font-style: italic; font-size: 10px; }
                .disclaimer { margin-top: 26px; padding-top: 10px; border-top: 1px solid #eceef1; font-size: 8.5px; color: #a2a8af; line-height: 1.5; }
                </style>
                </head>
                <body>%s</body>
                </html>
                """.formatted(FONT_FAMILY, FONT_FAMILY, FONT_FAMILY, body);
    }

    /**
     * 서류 한 건의 본문(기본 정보 표 + 요약/조항별 검토/추출 정보/판정 근거)을 붙입니다.
     * 섹션 번호는 서류마다 01부터 다시 시작합니다.
     */
    private void appendDocumentBody(
            StringBuilder body,
            DocumentSection section,
            boolean pageBreak
    ) {
        // 기본 정보 표
        body.append("<table class=\"info")
                .append(pageBreak ? " page-break" : "")
                .append("\">");
        body.append("<tr><th>서류 종류</th><td>")
                .append(escape(nullToDash(section.documentType())))
                .append("</td><th>위험도</th><td>")
                .append(riskBadge(section.riskLevel()))
                .append("</td></tr>");
        body.append("</table>");

        int no = 0;

        // 요약 (마크다운 렌더링)
        body.append(secHeading(++no, "요약"));
        if (hasText(section.summary())) {
            body.append("<div class=\"summary\">")
                    .append(renderMarkdown(section.summary()))
                    .append("</div>");
        } else {
            body.append("<p class=\"empty\">요약이 없습니다.</p>");
        }

        JsonNode root = parse(section.resultJson());
        if (root != null) {
            if (isNonEmptyArray(root.get("findings"))) {
                body.append(secHeading(++no, "조항별 검토"));
                appendFindings(body, root.get("findings"));
            }
            if (hasRegisterFields(root)) {
                body.append(secHeading(++no, "추출 정보"));
                appendRegisterDetails(body, root);
            }
            if (isNonEmptyArray(root.get("reasons"))) {
                body.append(secHeading(++no, "판정 근거"));
                appendStringList(body, root.get("reasons"));
            }
        }
    }

    private String secHeading(int no, String title) {
        return "<h2 class=\"sec\"><span class=\"no\">"
                + String.format("%02d", no)
                + "</span>" + escape(title) + "</h2>";
    }

    private void appendFindings(StringBuilder body, JsonNode findings) {
        body.append("<table class=\"data\"><tr><th>조항</th><th>문제점</th><th>위험도</th><th>근거</th></tr>");
        for (JsonNode finding : findings) {
            body.append("<tr>")
                    .append("<td>").append(escape(text(finding, "clause"))).append("</td>")
                    .append("<td>").append(escape(text(finding, "issue"))).append("</td>")
                    .append("<td>").append(riskBadge(text(finding, "riskLevel"))).append("</td>")
                    .append("<td>").append(escape(text(finding, "reason"))).append("</td>")
                    .append("</tr>");
        }
        body.append("</table>");
    }

    private void appendRegisterDetails(StringBuilder body, JsonNode root) {
        body.append("<div class=\"meta\">");
        if (root.has("owner")) {
            body.append(row("소유자", escape(text(root, "owner"))));
        }
        if (root.has("ownerMatches")) {
            body.append(row("소유자 일치", yesNo(root.get("ownerMatches"))));
        }
        if (root.has("addressMatches")) {
            body.append(row("주소 일치", yesNo(root.get("addressMatches"))));
        }
        if (root.has("trust")) {
            body.append(row("신탁 등기", yesNo(root.get("trust"))));
        }
        if (root.has("violationBuilding")) {
            body.append(row("위반건축물", yesNo(root.get("violationBuilding"))));
        }
        body.append("</div>");

        appendMortgages(body, root.get("mortgages"));
        appendSeizures(body, root.get("seizures"));
    }

    private void appendMortgages(StringBuilder body, JsonNode mortgages) {
        if (!isNonEmptyArray(mortgages)) {
            return;
        }
        body.append("<div class=\"subh\">근저당권</div>");
        body.append("<table class=\"data\"><tr><th>채권자</th><th>채권최고액</th><th>설정일</th></tr>");
        for (JsonNode mortgage : mortgages) {
            body.append("<tr>")
                    .append("<td>").append(escape(text(mortgage, "creditor"))).append("</td>")
                    .append("<td>").append(escape(text(mortgage, "maxClaim"))).append("</td>")
                    .append("<td>").append(escape(text(mortgage, "date"))).append("</td>")
                    .append("</tr>");
        }
        body.append("</table>");
    }

    private void appendSeizures(StringBuilder body, JsonNode seizures) {
        if (!isNonEmptyArray(seizures)) {
            return;
        }
        body.append("<div class=\"subh\">압류·가압류 등</div>");
        appendStringList(body, seizures);
    }

    private void appendStringList(StringBuilder body, JsonNode array) {
        if (!isNonEmptyArray(array)) {
            return;
        }
        body.append("<ul class=\"doc-list\">");
        for (JsonNode item : array) {
            body.append("<li>").append(escape(item.asText(""))).append("</li>");
        }
        body.append("</ul>");
    }

    private boolean hasRegisterFields(JsonNode root) {
        return root.has("owner")
                || root.has("mortgages")
                || root.has("seizures")
                || root.has("violationBuilding");
    }

    private boolean isNonEmptyArray(JsonNode node) {
        return node != null && node.isArray() && !node.isEmpty();
    }

    private JsonNode parse(String resultJson) {
        if (resultJson == null || resultJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(resultJson);
        } catch (Exception exception) {
            log.warn("서류 분석 resultJson 파싱 실패 — 요약만 렌더링합니다.", exception);
            return null;
        }
    }

    private String riskBadge(String riskLevel) {
        String label = nullToDash(riskLevel);
        String cssClass = switch (label) {
            case "위험" -> "risk-danger";
            case "주의" -> "risk-caution";
            case "안전" -> "risk-safe";
            default -> "risk-none";
        };
        return "<span class=\"badge " + cssClass + "\">" + escape(label) + "</span>";
    }

    private String row(String label, String value) {
        return "<div class=\"item\"><span class=\"label\">" + escape(label) + "</span>"
                + value + "</div>";
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText("");
    }

    private String yesNo(JsonNode node) {
        if (node == null || node.isNull()) {
            return "-";
        }
        return node.asBoolean(false) ? "예" : "아니오";
    }

    private String documentNo(Long propertyId) {
        if (propertyId == null) {
            return "BBB-D-______";
        }
        return String.format("BBB-D-%06d", propertyId);
    }

    /**
     * 마크다운 텍스트를 openhtmltopdf 가 렌더링할 수 있는 XHTML 로 변환합니다.
     * 지원: #~###### 제목, - * + 글머리표, 1. 번호 목록, > 인용, --- 구분선, **굵게**, `코드`.
     */
    private String renderMarkdown(String markdown) {
        if (!hasText(markdown)) {
            return "";
        }

        String[] lines = markdown
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .split("\n", -1);

        StringBuilder out = new StringBuilder();
        StringBuilder paragraph = new StringBuilder();
        List<String> quote = new ArrayList<>();
        boolean inUl = false;
        boolean inOl = false;

        for (String line : lines) {
            String trimmed = line.strip();

            if (trimmed.startsWith(">")) {
                flushParagraph(out, paragraph);
                if (inUl) { out.append("</ul>"); inUl = false; }
                if (inOl) { out.append("</ol>"); inOl = false; }
                quote.add(trimmed.substring(1).strip());
                continue;
            }
            flushQuote(out, quote);

            if (trimmed.isEmpty()) {
                flushParagraph(out, paragraph);
                if (inUl) { out.append("</ul>"); inUl = false; }
                if (inOl) { out.append("</ol>"); inOl = false; }
                continue;
            }

            if (MD_HR.matcher(trimmed).matches()) {
                flushParagraph(out, paragraph);
                if (inUl) { out.append("</ul>"); inUl = false; }
                if (inOl) { out.append("</ol>"); inOl = false; }
                out.append("<hr class=\"md-hr\"/>");
                continue;
            }

            Matcher heading = MD_HEADING.matcher(trimmed);
            if (heading.matches()) {
                flushParagraph(out, paragraph);
                if (inUl) { out.append("</ul>"); inUl = false; }
                if (inOl) { out.append("</ol>"); inOl = false; }
                int level = heading.group(1).length();
                String cls = level <= 1 ? "md-h1" : (level == 2 ? "md-h2" : "md-h3");
                out.append("<div class=\"").append(cls).append("\">")
                        .append(inlineMarkdown(heading.group(2)))
                        .append("</div>");
                continue;
            }

            Matcher bullet = MD_BULLET.matcher(trimmed);
            if (bullet.matches()) {
                flushParagraph(out, paragraph);
                if (inOl) { out.append("</ol>"); inOl = false; }
                if (!inUl) { out.append("<ul class=\"md-ul\">"); inUl = true; }
                out.append("<li>").append(inlineMarkdown(bullet.group(1))).append("</li>");
                continue;
            }

            Matcher numbered = MD_NUMBERED.matcher(trimmed);
            if (numbered.matches()) {
                flushParagraph(out, paragraph);
                if (inUl) { out.append("</ul>"); inUl = false; }
                if (!inOl) { out.append("<ol class=\"md-ol\">"); inOl = true; }
                out.append("<li>").append(inlineMarkdown(numbered.group(1))).append("</li>");
                continue;
            }

            if (inUl) { out.append("</ul>"); inUl = false; }
            if (inOl) { out.append("</ol>"); inOl = false; }
            if (paragraph.length() > 0) {
                paragraph.append(" ");
            }
            paragraph.append(inlineMarkdown(trimmed));
        }

        flushQuote(out, quote);
        flushParagraph(out, paragraph);
        if (inUl) { out.append("</ul>"); }
        if (inOl) { out.append("</ol>"); }

        return out.toString();
    }

    private void flushParagraph(StringBuilder out, StringBuilder paragraph) {
        if (paragraph.length() > 0) {
            out.append("<p class=\"md-p\">").append(paragraph).append("</p>");
            paragraph.setLength(0);
        }
    }

    private void flushQuote(StringBuilder out, List<String> quote) {
        if (!quote.isEmpty()) {
            out.append("<blockquote class=\"md-quote\">")
                    .append(renderMarkdown(String.join("\n", quote)))
                    .append("</blockquote>");
            quote.clear();
        }
    }

    private String inlineMarkdown(String raw) {
        String escaped = escape(raw);
        escaped = MD_BOLD_STAR.matcher(escaped).replaceAll("<strong>$1</strong>");
        escaped = MD_BOLD_UNDERSCORE.matcher(escaped).replaceAll("<strong>$1</strong>");
        escaped = MD_CODE.matcher(escaped).replaceAll("<code>$1</code>");
        return escaped;
    }

    private String imageDataUri(String path) {
        ClassPathResource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            log.warn("리포트 이미지({})를 찾을 수 없어 생략합니다.", path);
            return null;
        }
        try (InputStream in = resource.getInputStream()) {
            byte[] bytes = in.readAllBytes();
            return "data:image/png;base64,"
                    + Base64.getEncoder().encodeToString(bytes);
        } catch (Exception exception) {
            log.warn("리포트 이미지({}) 로딩에 실패했습니다.", path, exception);
            return null;
        }
    }

    private String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
