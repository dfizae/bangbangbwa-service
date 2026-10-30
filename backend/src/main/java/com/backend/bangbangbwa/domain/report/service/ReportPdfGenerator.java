package com.backend.bangbangbwa.domain.report.service;

import com.backend.bangbangbwa.common.exception.CustomException;
import com.backend.bangbangbwa.common.exception.ErrorCode;
import com.backend.bangbangbwa.domain.property.storage.StorageService;
import com.backend.bangbangbwa.domain.report.dto.CaptureResponse;
import com.backend.bangbangbwa.domain.report.dto.DefectResponse;
import com.backend.bangbangbwa.domain.report.dto.ReportDetailResponse;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 리포트 상세 데이터를 HTML로 렌더링한 뒤 PDF 바이트로 변환합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportPdfGenerator {

    private static final String FONT_PATH = "fonts/NanumGothic-Regular.ttf";
    private static final String FONT_FAMILY = "ReportFont";

    // 문서 브랜딩 이미지 (src/main/resources 하위 classpath 경로)
    private static final String LOGO_PATH = "images/report-logo.png";
    private static final String WATERMARK_PATH = "images/report-watermark.png";

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // 마크다운 블록 패턴
    private static final Pattern MD_HEADING = Pattern.compile("^(#{1,6})\\s+(.*)$");
    private static final Pattern MD_BULLET = Pattern.compile("^[-*+]\\s+(.*)$");
    private static final Pattern MD_NUMBERED = Pattern.compile("^\\d+[.)]\\s+(.*)$");
    private static final Pattern MD_HR = Pattern.compile("^(-{3,}|\\*{3,}|_{3,})$");
    // 마크다운 인라인 패턴
    private static final Pattern MD_BOLD_STAR = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern MD_BOLD_UNDERSCORE = Pattern.compile("__(.+?)__");
    private static final Pattern MD_CODE = Pattern.compile("`([^`]+?)`");

    private final StorageService storageService;

    public byte[] generate(
            ReportDetailResponse detail,
            String propertyTitle
    ) {
        String html = buildHtml(detail, propertyTitle);

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
                    "리포트 PDF 생성에 실패했습니다. reportId={}",
                    detail.getReportId(),
                    exception
            );
            throw new CustomException(ErrorCode.PDF_GENERATION_FAILED);
        }
    }

    private void registerKoreanFont(PdfRendererBuilder builder) {
        ClassPathResource fontResource = new ClassPathResource(FONT_PATH);

        if (!fontResource.exists()) {
            log.warn(
                    "한글 폰트({})를 찾을 수 없어 기본 폰트로 렌더링합니다.",
                    FONT_PATH
            );
            return;
        }

        builder.useFont(
                () -> {
                    try {
                        return fontResource.getInputStream();
                    } catch (Exception exception) {
                        throw new CustomException(
                                ErrorCode.PDF_GENERATION_FAILED
                        );
                    }
                },
                FONT_FAMILY
        );
    }

    private String buildHtml(
            ReportDetailResponse detail,
            String propertyTitle
    ) {
        String logo = imageDataUri(LOGO_PATH);
        String watermark = imageDataUri(WATERMARK_PATH);

        StringBuilder body = new StringBuilder();

        // 워터마크 (본문 뒤, position:fixed 로 모든 페이지에 반복 표시)
        if (watermark != null) {
            body.append("<div id=\"watermark\"><img src=\"")
                    .append(watermark)
                    .append("\"/></div>");
        }

        // 머리글: 좌측 로고 / 우측 문서번호·발행일
        body.append("<table class=\"masthead\"><tr>");
        body.append("<td class=\"mh-logo\">");
        if (logo != null) {
            body.append("<img src=\"").append(logo).append("\"/>");
        }
        body.append("</td>");
        body.append("<td class=\"mh-meta\">");
        body.append("<div><span class=\"k\">문서번호</span>&#160;&#160;")
                .append(escape(documentNo(detail.getReportId())))
                .append("</div>");
        body.append("<div><span class=\"k\">발행일</span>&#160;&#160;")
                .append(detail.getCreatedAt() == null
                        ? "-"
                        : escape(detail.getCreatedAt().format(DATE_FORMATTER)))
                .append("</div>");
        body.append("</td></tr></table>");
        body.append("<div class=\"mh-rule\"></div>");

        // 문서 제목
        body.append("<div class=\"doc-title\">매물 영상 점검 리포트</div>");

        // 기본 정보 표
        body.append("<table class=\"info\">");
        body.append("<tr><th>매물명</th><td>")
                .append(escape(nullToDash(propertyTitle)))
                .append("</td><th>매물번호</th><td>")
                .append(escape(String.valueOf(detail.getPropertyId())))
                .append("</td></tr>");
        body.append("<tr><th>리포트 번호</th><td>")
                .append(escape(String.valueOf(detail.getReportId())))
                .append("</td><th>생성 일시</th><td>")
                .append(detail.getCreatedAt() == null
                        ? "-"
                        : escape(detail.getCreatedAt().format(DATE_TIME_FORMATTER)))
                .append("</td></tr>");
        body.append("</table>");

        appendSummary(body, detail);
        appendChecklist(body, detail);
        appendCaptures(body, detail.getCaptures());

        return """
                <html>
                <head>
                <style>
                @page {
                  size: A4;
                  margin: 44px 44px 52px 44px;
                  @bottom-left { content: "방방봐 매물 영상 점검 리포트"; font-family: "%s", sans-serif; font-size: 7.5px; color: #b3b8bf; }
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
                .info th, .info td { border-top: 1px solid #eceef1; border-bottom: 1px solid #eceef1; padding: 6px 10px 6px 0; font-size: 10px; text-align: left; vertical-align: top; }
                .info th { width: 66px; color: #9aa0a8; font-weight: normal; }
                .info td { color: #1f2328; padding-right: 26px; }

                h2.sec { font-size: 11.5px; font-weight: bold; color: #1f2328; letter-spacing: 0.5px; margin: 22px 0 9px 0; padding-bottom: 6px; border-bottom: 1px solid #e3e6ea; }
                h2.sec .no { color: #c2c7cd; font-weight: normal; margin-right: 8px; }

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

                table.data { width: 100%%; border-collapse: collapse; margin-top: 8px; font-size: 10px; }
                table.data th { border-top: 1px solid #1f2328; border-bottom: 1px solid #1f2328; padding: 6px 8px; text-align: left; color: #6b7178; font-weight: bold; letter-spacing: 0.3px; }
                table.data td { border-bottom: 1px solid #eceef1; padding: 6px 8px; text-align: left; vertical-align: top; color: #1f2328; }

                .capture-block { page-break-inside: avoid; margin-top: 6px; }
                .cap-label { font-size: 10.5px; font-weight: bold; color: #1f2328; margin-top: 16px; }
                img.capture { max-width: 300px; max-height: 300px; margin-top: 8px; border: 1px solid #e3e6ea; }
                .url { font-size: 9px; color: #5b616a; word-break: break-all; }
                .empty { color: #aab0b8; font-style: italic; font-size: 10px; }
                </style>
                </head>
                <body>%s</body>
                </html>
                """.formatted(FONT_FAMILY, FONT_FAMILY, FONT_FAMILY, body);
    }

    private void appendSummary(
            StringBuilder body,
            ReportDetailResponse detail
    ) {
        body.append("<h2 class=\"sec\"><span class=\"no\">01</span>종합 점검 소견</h2>");

        if (!hasText(detail.getSummary())) {
            body.append("<p class=\"empty\">점검 소견이 아직 등록되지 않았습니다.</p>");
            return;
        }

        body.append("<div class=\"summary\">")
                .append(renderMarkdown(detail.getSummary()))
                .append("</div>");
    }

    private void appendChecklist(
            StringBuilder body,
            ReportDetailResponse detail
    ) {
        body.append("<h2 class=\"sec\"><span class=\"no\">02</span>점검 체크리스트</h2>");

        if (detail.getChecklist() == null
                || detail.getChecklist().getItems() == null
                || detail.getChecklist().getItems().isEmpty()) {
            body.append("<p class=\"empty\">체크리스트 항목이 없습니다.</p>");
            return;
        }

        body.append("<table class=\"data\">");
        body.append("<tr><th>내용</th><th>상태</th><th>메모</th></tr>");

        detail.getChecklist().getItems().forEach(item -> {
            body.append("<tr>");
            body.append("<td>").append(escape(item.getContent())).append("</td>");
            body.append("<td>")
                    .append(item.getStatus() == null
                            ? "-"
                            : escape(item.getStatus().name()))
                    .append("</td>");
            body.append("<td>").append(escape(nullToDash(item.getMemo()))).append("</td>");
            body.append("</tr>");
        });

        body.append("</table>");
    }

    private void appendCaptures(
            StringBuilder body,
            List<CaptureResponse> captures
    ) {
        body.append("<h2 class=\"sec\"><span class=\"no\">03</span>현장 캡쳐 및 하자 분석</h2>");

        if (captures == null || captures.isEmpty()) {
            body.append("<p class=\"empty\">캡쳐 이미지가 없습니다.</p>");
            return;
        }

        int index = 1;
        for (CaptureResponse capture : captures) {
            // 캡쳐 라벨 + 이미지 + 하자표를 한 블록으로 묶어 페이지 중간에서 잘리지 않게 함
            body.append("<div class=\"capture-block\">");
            body.append("<div class=\"cap-label\">캡쳐 #").append(index++).append("</div>");
            appendCaptureImage(
                    body,
                    capture.getImageUrl(),
                    capture.getDefects()
            );
            appendDefects(body, capture.getDefects());
            body.append("</div>");
        }
    }

    /**
     * 캡쳐 이미지를 private 스토리지에서 내려받아 base64 data URI로 임베딩합니다.
     * 불러오지 못하면 원본 URL 텍스트로 대체해 PDF 생성 자체는 계속 진행합니다.
     */
    private void appendCaptureImage(
            StringBuilder body,
            String imageUrl,
            List<DefectResponse> defects
    ) {
        if (!hasText(imageUrl)) {
            body.append("<p class=\"empty\">이미지가 없습니다.</p>");
            return;
        }

        try {
            StorageService.BinaryContent image = storageService.download(imageUrl);
            String dataUri = drawBoundingBoxes(image, defects);
            body.append("<img class=\"capture\" src=\"")
                    .append(dataUri)
                    .append("\"/>");
        } catch (Exception exception) {
            log.warn(
                    "PDF에 캡쳐 이미지를 넣지 못해 URL로 대체합니다. imageUrl={}",
                    imageUrl,
                    exception
            );
            body.append("<div class=\"url\">")
                    .append(escape(imageUrl))
                    .append("</div>");
        }
    }

    /**
     * YOLO 정규화 좌표를 원본 이미지 픽셀 좌표로 변환해 하자 영역을 표시합니다.
     * 유효한 bbox가 없으면 원본 이미지를 그대로 반환합니다.
     */
    private String drawBoundingBoxes(
            StorageService.BinaryContent original,
            List<DefectResponse> defects
    ) throws Exception {
        if (defects == null || defects.stream().noneMatch(this::hasBoundingBox)) {
            return original.toDataUri();
        }

        BufferedImage source = ImageIO.read(
                new ByteArrayInputStream(original.bytes())
        );
        if (source == null) {
            log.warn("bbox를 그릴 수 없는 이미지 형식이어서 원본을 사용합니다.");
            return original.toDataUri();
        }

        BufferedImage annotated = new BufferedImage(
                source.getWidth(),
                source.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );
        Graphics2D graphics = annotated.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, annotated.getWidth(), annotated.getHeight());
            graphics.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );
            graphics.drawImage(source, 0, 0, null);
            graphics.setColor(Color.RED);
            graphics.setStroke(new BasicStroke(
                    Math.max(2.0f, annotated.getWidth() / 300.0f)
            ));

            for (DefectResponse defect : defects) {
                if (hasBoundingBox(defect)) {
                    drawBoundingBox(graphics, annotated, defect);
                }
            }
        } finally {
            graphics.dispose();
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (!ImageIO.write(annotated, "png", output)) {
                return original.toDataUri();
            }
            return "data:image/png;base64,"
                    + Base64.getEncoder().encodeToString(output.toByteArray());
        }
    }

    private void drawBoundingBox(
            Graphics2D graphics,
            BufferedImage image,
            DefectResponse defect
    ) {
        double left = defect.getBboxXCenter() - defect.getBboxWidth() / 2.0;
        double top = defect.getBboxYCenter() - defect.getBboxHeight() / 2.0;
        double right = defect.getBboxXCenter() + defect.getBboxWidth() / 2.0;
        double bottom = defect.getBboxYCenter() + defect.getBboxHeight() / 2.0;

        int x1 = clamp((int) Math.round(left * image.getWidth()), 0, image.getWidth() - 1);
        int y1 = clamp((int) Math.round(top * image.getHeight()), 0, image.getHeight() - 1);
        int x2 = clamp((int) Math.round(right * image.getWidth()), x1 + 1, image.getWidth());
        int y2 = clamp((int) Math.round(bottom * image.getHeight()), y1 + 1, image.getHeight());

        graphics.drawRect(x1, y1, x2 - x1, y2 - y1);
    }

    private boolean hasBoundingBox(DefectResponse defect) {
        return defect != null
                && isNormalized(defect.getBboxXCenter())
                && isNormalized(defect.getBboxYCenter())
                && isPositiveNormalized(defect.getBboxWidth())
                && isPositiveNormalized(defect.getBboxHeight());
    }

    private boolean isNormalized(Double value) {
        return value != null && value >= 0.0 && value <= 1.0;
    }

    private boolean isPositiveNormalized(Double value) {
        return value != null && value > 0.0 && value <= 1.0;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private void appendDefects(
            StringBuilder body,
            List<DefectResponse> defects
    ) {
        if (defects == null || defects.isEmpty()) {
            body.append("<p class=\"empty\">사용자가 직접 캡쳐한 이미지 입니다.</p>");
            return;
        }

        body.append("<table class=\"data\">");
        body.append("<tr><th>유형</th><th>신뢰도</th><th>설명</th><th>상태</th></tr>");

        defects.forEach(defect -> {
            body.append("<tr>");
            body.append("<td>").append(escape(nullToDash(defect.getType()))).append("</td>");
            body.append("<td>")
                    .append(defect.getConfidence() == null
                            ? "-"
                            : String.format("%.2f", defect.getConfidence()))
                    .append("</td>");
            body.append("<td>").append(escape(nullToDash(defect.getDescription()))).append("</td>");
            body.append("<td>").append(escape(nullToDash(defect.getStatus()))).append("</td>");
            body.append("</tr>");
        });

        body.append("</table>");
    }

    /**
     * classpath 이미지 리소스를 base64 data URI로 변환합니다. 없으면 null 을 반환해 생략합니다.
     */
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

    private String documentNo(Object reportId) {
        if (reportId == null) {
            return "BBB-R-______";
        }
        try {
            return String.format("BBB-R-%06d", Long.parseLong(String.valueOf(reportId)));
        } catch (NumberFormatException exception) {
            return "BBB-R-" + reportId;
        }
    }

    /**
     * AI 총평 등 마크다운 텍스트를 openhtmltopdf 가 렌더링할 수 있는 XHTML 로 변환합니다.
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

            // 인용 블록 누적
            if (trimmed.startsWith(">")) {
                flushParagraph(out, paragraph);
                if (inUl) { out.append("</ul>"); inUl = false; }
                if (inOl) { out.append("</ol>"); inOl = false; }
                quote.add(trimmed.substring(1).strip());
                continue;
            }
            flushQuote(out, quote);

            // 빈 줄 -> 문단/목록 종료
            if (trimmed.isEmpty()) {
                flushParagraph(out, paragraph);
                if (inUl) { out.append("</ul>"); inUl = false; }
                if (inOl) { out.append("</ol>"); inOl = false; }
                continue;
            }

            // 구분선
            if (MD_HR.matcher(trimmed).matches()) {
                flushParagraph(out, paragraph);
                if (inUl) { out.append("</ul>"); inUl = false; }
                if (inOl) { out.append("</ol>"); inOl = false; }
                out.append("<hr class=\"md-hr\"/>");
                continue;
            }

            // 제목
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

            // 글머리표 목록
            Matcher bullet = MD_BULLET.matcher(trimmed);
            if (bullet.matches()) {
                flushParagraph(out, paragraph);
                if (inOl) { out.append("</ol>"); inOl = false; }
                if (!inUl) { out.append("<ul class=\"md-ul\">"); inUl = true; }
                out.append("<li>").append(inlineMarkdown(bullet.group(1))).append("</li>");
                continue;
            }

            // 번호 목록
            Matcher numbered = MD_NUMBERED.matcher(trimmed);
            if (numbered.matches()) {
                flushParagraph(out, paragraph);
                if (inUl) { out.append("</ul>"); inUl = false; }
                if (!inOl) { out.append("<ol class=\"md-ol\">"); inOl = true; }
                out.append("<li>").append(inlineMarkdown(numbered.group(1))).append("</li>");
                continue;
            }

            // 일반 문단 줄
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

    /**
     * 인라인 마크다운(굵게/코드)을 변환합니다. HTML 이스케이프를 먼저 적용해 안전하게 처리합니다.
     */
    private String inlineMarkdown(String raw) {
        String escaped = escape(raw);
        escaped = MD_BOLD_STAR.matcher(escaped).replaceAll("<strong>$1</strong>");
        escaped = MD_BOLD_UNDERSCORE.matcher(escaped).replaceAll("<strong>$1</strong>");
        escaped = MD_CODE.matcher(escaped).replaceAll("<code>$1</code>");
        return escaped;
    }

    private String nullToDash(String value) {
        return (value == null || value.isBlank()) ? "-" : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
