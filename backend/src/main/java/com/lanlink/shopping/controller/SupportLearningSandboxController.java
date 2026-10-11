package com.lanlink.shopping.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lanlink.shopping.common.R;
import com.lanlink.shopping.support.SupportAnswerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Admin-only gateway to the isolated support-learning Docker sandbox.
 * Admin authentication stays here; candidate evaluation runs in a separate,
 * token-protected container with no production database or external API access.
 */
@RestController
@RequestMapping("/admin/support-learning")
public class SupportLearningSandboxController {
    private static final Pattern ENTRY_ID = Pattern.compile("[a-z0-9][a-z0-9-]{1,63}");

    private final SupportAnswerService answers;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String sandboxUrl;
    private final String sandboxToken;

    public SupportLearningSandboxController(
            SupportAnswerService answers,
            ObjectMapper objectMapper,
            @Value("${SUPPORT_SANDBOX_URL:}") String sandboxUrl,
            @Value("${SUPPORT_SANDBOX_TOKEN:}") String sandboxToken) {
        this.answers = answers;
        this.objectMapper = objectMapper;
        this.sandboxUrl = sandboxUrl == null ? "" : sandboxUrl.trim().replaceAll("/+$", "");
        this.sandboxToken = sandboxToken == null ? "" : sandboxToken.trim();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @PostMapping("/preview")
    public ResponseEntity<R<PreviewResult>> preview(@RequestBody(required = false) PreviewRequest request) {
        if (!validRequest(request)) {
            return ResponseEntity.badRequest().body(R.<PreviewResult>fail(400,
                    "沙盒输入不符合要求；请检查问题、候选关键词和回答长度。"));
        }
        if (!StringUtils.hasText(sandboxUrl) || !StringUtils.hasText(sandboxToken)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(R.<PreviewResult>fail(503,
                    "隔离 Docker 学习沙盒尚未连接；请检查服务地址与容器认证配置。"));
        }

        String question = request.question().trim();
        List<String> keywords = request.keywords().stream()
                .map(String::trim)
                .distinct()
                .toList();

        // The established knowledge matcher remains the source of truth for scope and intent.
        // Only the candidate-validation step is delegated to the isolated container.
        String currentEntryId = answers.matchedEntryId(question);
        String currentAnswer = answers.answer(question);
        boolean learningEligible = answers.isEligibleForKnowledgeLearning(question);
        boolean keywordMatch = answers.candidateKeywordsMatch(question, keywords);
        boolean keywordConflict = answers.hasKeywordConflict(keywords);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("question", question);
        payload.put("entryId", request.entryId().trim());
        payload.put("keywords", keywords);
        payload.put("draftAnswer", request.draftAnswer().trim());
        payload.put("currentEntryId", currentEntryId);
        payload.put("currentAnswer", currentAnswer);
        payload.put("learningEligible", learningEligible);
        payload.put("learningEligibilityMessage", learningEligible
                ? "问题位于当前电商学习范围内，且未触发已知拦截规则。"
                : "问题未通过电商范围或安全检查，不能作为当前知识库候选。");
        payload.put("draftKeywordsMatch", keywordMatch);
        payload.put("keywordConflict", keywordConflict);

        try {
            String endpoint = sandboxUrl + "/v1/evaluate";
            String json = objectMapper.writeValueAsString(payload);
            HttpRequest outbound = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(4))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("X-Sandbox-Token", sandboxToken)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = httpClient.send(outbound, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.println("[support-learning-sandbox] container returned HTTP " + response.statusCode());
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(R.<PreviewResult>fail(503,
                        "隔离 Docker 沙盒暂时不可用，请稍后重试。"));
            }
            PreviewResult result = objectMapper.readValue(response.body(), PreviewResult.class);
            if (result == null || !StringUtils.hasText(result.sandboxNotice())) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(R.<PreviewResult>fail(503,
                        "隔离 Docker 沙盒返回了无效结果，请检查容器版本。"));
            }
            return ResponseEntity.ok(R.ok(result));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(R.<PreviewResult>fail(503,
                    "隔离 Docker 沙盒请求已中断，请重试。"));
        } catch (IOException | IllegalArgumentException ex) {
            // Deliberately omit request/answer/token values from logs.
            System.err.println("[support-learning-sandbox] isolated evaluation failed: " + ex.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(R.<PreviewResult>fail(503,
                    "隔离 Docker 沙盒暂时不可用，请检查容器状态与服务地址。"));
        }
    }

    private static boolean validRequest(PreviewRequest request) {
        if (request == null || !StringUtils.hasText(request.question())
                || request.question().length() > 300
                || !StringUtils.hasText(request.entryId()) || request.entryId().length() > 64
                || request.keywords() == null || request.keywords().isEmpty() || request.keywords().size() > 16
                || !StringUtils.hasText(request.draftAnswer()) || request.draftAnswer().length() > 1000) {
            return false;
        }
        for (String keyword : request.keywords()) {
            if (!StringUtils.hasText(keyword) || keyword.trim().length() > 80
                    || keyword.chars().anyMatch(Character::isISOControl)
                    || keyword.contains("<") || keyword.contains(">")) return false;
        }
        return request.draftAnswer().chars().noneMatch(Character::isISOControl)
                && !request.draftAnswer().contains("<")
                && !request.draftAnswer().contains(">")
                && request.draftAnswer().indexOf(96) < 0;
    }

    public record PreviewRequest(String question, String entryId, List<String> keywords, String draftAnswer) {}
    public record PreviewResult(
            String currentEntryId,
            String currentAnswer,
            boolean learningEligible,
            String learningEligibilityMessage,
            boolean draftKeywordsMatch,
            String draftAnswerPreview,
            boolean candidateReadyToExport,
            String candidateMessage,
            String sandboxNotice) {}
}
