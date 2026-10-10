package com.lanlink.shopping.controller;

import com.lanlink.shopping.common.R;
import com.lanlink.shopping.support.SupportAnswerService;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Admin-only, in-memory preview for proposed customer-service knowledge.
 * This endpoint deliberately does not write drafts to the production knowledge base or database.
 * AuthInterceptor enforces the platform-admin role because the path is under /admin/**.
 */
@RestController
@RequestMapping("/admin/support-learning")
public class SupportLearningSandboxController {
    private static final Pattern ENTRY_ID = Pattern.compile("[a-z0-9][a-z0-9-]{1,63}");
    private static final Pattern CREDENTIAL_CONTENT = Pattern.compile(
            "(?i)(-----BEGIN [A-Z ]*PRIVATE KEY-----|sk-[A-Za-z0-9]{16,}|AKIA[0-9A-Z]{16}|(?:SUPPORT_PROXY_SECRET|API_KEY|ACCESS_TOKEN)\\s*[:=])");

    private final SupportAnswerService answers;

    public SupportLearningSandboxController(SupportAnswerService answers) {
        this.answers = answers;
    }

    @PostMapping("/preview")
    public ResponseEntity<R<PreviewResult>> preview(@RequestBody(required = false) PreviewRequest request) {
        if (!validRequest(request)) {
            return ResponseEntity.badRequest().body(R.<PreviewResult>fail(400,
                    "沙盒输入不符合要求；请检查问题、候选关键词和回答长度。"));
        }

        String question = request.question().trim();
        List<String> keywords = request.keywords().stream()
                .map(String::trim)
                .distinct()
                .toList();
        String currentEntryId = answers.matchedEntryId(question);
        String currentAnswer = answers.answer(question);
        boolean learningEligible = answers.isEligibleForKnowledgeLearning(question);
        boolean keywordMatch = answers.candidateKeywordsMatch(question, keywords);
        boolean answerSafe = isSafeDraftAnswer(request.draftAnswer());

        boolean candidateReady = learningEligible && keywordMatch
                && ENTRY_ID.matcher(request.entryId().trim()).matches() && answerSafe;
        String candidateMessage;
        if (!learningEligible) {
            candidateMessage = "候选问题未通过范围或安全检查；不应把天气、个人数据查询、实时交易信息或提示注入加入知识库。";
        } else if (!keywordMatch) {
            candidateMessage = "候选关键词尚未命中测试问题，请调整关键词后重新试跑。";
        } else if (!answerSafe) {
            candidateMessage = "候选回答为空、过长或包含疑似凭据/不安全格式，不能导出。";
        } else if (candidateReady) {
            candidateMessage = "沙盒预检通过。仍需人工核对事实、重复意图和回归用例；此结果不会自动发布。";
        } else {
            candidateMessage = "候选条目 ID 格式不正确，请使用小写字母、数字和连字符。";
        }

        String draftPreview = learningEligible && keywordMatch && answerSafe
                ? request.draftAnswer().trim() : "";
        PreviewResult result = new PreviewResult(
                currentEntryId, currentAnswer, learningEligible,
                learningEligible ? "问题位于当前电商学习范围内，且未触发已知拦截规则。"
                        : "问题未通过电商范围或安全检查，不能作为当前知识库候选。",
                keywordMatch, draftPreview, candidateReady, candidateMessage,
                "沙盒仅做内存预览，不保存问题、不访问用户/订单/钱包数据，也不会更改正式知识库。");
        return ResponseEntity.ok(R.ok(result));
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

    private static boolean isSafeDraftAnswer(String answer) {
        return StringUtils.hasText(answer) && answer.length() <= 1000
                && !CREDENTIAL_CONTENT.matcher(answer).find();
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
