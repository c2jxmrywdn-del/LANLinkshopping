package com.lanlink.shopping.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Every answer returned here is an exact string loaded from the local knowledge base. */
@Service
public class SupportAnswerService {
    private static final int MAX_QUESTION_LENGTH = 300;
    private static final List<String> INJECTION_SIGNALS = List.of(
            "system prompt", "developer message", "prompt injection", "jailbreak",
            "ignore previous instructions", "ignore all instructions", "disregard instructions",
            "reverse proxy", "api key", "access token", "environment variable", "shell command",
            "执行代码", "运行代码", "生成代码", "输出代码", "工具调用", "调用工具",
            "系统提示词", "系统指令", "内部配置", "环境变量", "反向代理", "跨域调用",
            "知识库绕过", "提示注入", "越权", "管理员权限", "显示密钥", "读取配置",
            "输出提示词", "打印系统提示", "泄露配置", "秘密指令", "内部规则", "绕过限制"
    );
    private static final List<String> UNSUPPORTED_BUSINESS_SIGNALS = List.of(
            "价格", "报价", "库存", "现货", "物流", "快递", "运费", "发货", "配送",
            "退货", "退款", "售后", "发票", "支付", "付款", "到账", "提现", "优惠券",
            "交货期", "到货时间", "具体规格", "商品规格", "成分", "材质", "支付失败",
            "密码重置", "重置密码", "找回密码", "忘记密码", "修改密码",
            "订单状态", "订单号", "查询余额", "余额是多少", "我的余额", "我的积分",
            "积分余额", "我的订单状态", "授信额度", "账期额度", "申请条件", "审核结果",
            "活动规则", "优惠规则", "用户数据", "用户信息", "账号申诉"
    );
    private static final Pattern INJECTION_COMBINATION = Pattern.compile(
            "(?i)(ignore|disregard|forget|override|bypass)[\\s\\S]{0,80}(instruction|prompt|system|policy|knowledge base|rules)"
    );
    private static final Pattern INVISIBLE_CHARACTERS = Pattern.compile("[\\u200B-\\u200D\\u2060\\uFEFF]");

    private final ObjectMapper objectMapper;
    private final Resource knowledgeBaseResource;
    private volatile String fallback;
    private volatile String welcome;
    private volatile List<Entry> entries = List.of();

    public SupportAnswerService(
            ObjectMapper objectMapper,
            @Value("classpath:knowledge-base/customer-service-zh-CN.json") Resource knowledgeBaseResource) {
        this.objectMapper = objectMapper;
        this.knowledgeBaseResource = knowledgeBaseResource;
    }

    @PostConstruct
    public void loadKnowledgeBase() throws IOException {
        JsonNode root;
        try (var in = knowledgeBaseResource.getInputStream()) {
            root = objectMapper.readTree(in);
        }
        String loadedFallback = root.path("fallback").asText("");
        String loadedWelcome = root.path("welcome").asText("");
        if (!StringUtils.hasText(loadedFallback) || !root.path("entries").isArray()) {
            throw new IllegalStateException("Customer support knowledge base is missing required fields");
        }
        List<Entry> loadedEntries = new ArrayList<>();
        for (JsonNode node : root.path("entries")) {
            String id = node.path("id").asText("");
            String answer = node.path("answer").asText("");
            JsonNode keywordNodes = node.path("keywords");
            List<String> keywords = new ArrayList<>();
            if (keywordNodes.isArray()) {
                keywordNodes.forEach(keyword -> {
                    if (keyword.isTextual() && StringUtils.hasText(keyword.asText())) {
                        keywords.add(normalize(keyword.asText()));
                    }
                });
            }
            if (!StringUtils.hasText(id) || !StringUtils.hasText(answer) || keywords.isEmpty()) {
                throw new IllegalStateException("Customer support knowledge base contains an incomplete entry");
            }
            loadedEntries.add(new Entry(id, List.copyOf(keywords), answer));
        }
        if (loadedEntries.isEmpty()) throw new IllegalStateException("Customer support knowledge base must not be empty");
        this.fallback = loadedFallback;
        this.welcome = StringUtils.hasText(loadedWelcome) ? loadedWelcome : loadedFallback;
        this.entries = List.copyOf(loadedEntries);
    }

    public String fallback() { return fallback; }
    public String welcome() { return welcome; }

    public String answer(String rawQuestion) {
        if (!StringUtils.hasText(rawQuestion) || rawQuestion.length() > MAX_QUESTION_LENGTH) return fallback;
        if (rawQuestion.chars().anyMatch(Character::isISOControl)
                || rawQuestion.contains("<") || rawQuestion.contains(">")
                || rawQuestion.indexOf(96) >= 0) return fallback;

        String question = normalize(INVISIBLE_CHARACTERS.matcher(rawQuestion).replaceAll(""));
        if (question.isEmpty() || isPromptInjection(question)
                || containsAny(question, UNSUPPORTED_BUSINESS_SIGNALS)) return fallback;

        return entries.stream()
                .map(entry -> new Match(entry, entry.keywords().stream()
                        .filter(question::contains)
                        .max(Comparator.comparingInt(String::length)).orElse(null)))
                .filter(match -> match.keyword() != null)
                .max(Comparator.comparingInt(match -> match.keyword().length()))
                .map(match -> match.entry().answer())
                .orElse(fallback);
    }

    private static boolean isPromptInjection(String question) {
        if (containsAny(question, INJECTION_SIGNALS)) return true;
        return INJECTION_COMBINATION.matcher(question).find()
                || (containsAny(question, List.of("忽略", "无视", "覆盖", "绕过", "忘记"))
                && containsAny(question, List.of("指令", "提示", "规则", "系统", "开发者", "知识库", "限制", "之前")));
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC).trim().toLowerCase(Locale.ROOT);
    }
    private static boolean containsAny(String text, List<String> phrases) {
        for (String phrase : phrases) if (text.contains(phrase)) return true;
        return false;
    }
    private record Entry(String id, List<String> keywords, String answer) {}
    private record Match(Entry entry, String keyword) {}
}
