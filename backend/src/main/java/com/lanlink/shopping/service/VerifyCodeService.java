package com.lanlink.shopping.service;

import com.lanlink.shopping.common.BusinessException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 验证码服务：内存存储，5 分钟过期 + 60 秒冷却 + 单次使用。
 * 邮件通道：接入 JavaMailSender（SMTP），真实发送验证码邮件；
 * 仅开发环境显式开启 demo-mode 时允许回显验证码；生产环境无可用通道时拒绝发送。
 */
@Service
public class VerifyCodeService {

    private static final Logger log = LoggerFactory.getLogger(VerifyCodeService.class);

    private static final long TTL_MS = 5 * 60 * 1000L;
    private static final long COOLDOWN_MS = 60 * 1000L;
    /** 验证码邮件标题 */
    private static final String MAIL_SUBJECT = "【LANLink 商城】邮箱验证码";

    private static class CodeBox {
        String code;
        long exp;
        long lastSendAt;
    }

    private final Map<String, CodeBox> store = new ConcurrentHashMap<>();

    private final JavaMailSender mailSender;
    private final String mailHost;
    private final String mailFrom;
    private final boolean demoMode;

    public VerifyCodeService(
            @Autowired(required = false) JavaMailSender mailSender,
            @Value("${spring.mail.host:}") String mailHost,
            @Value("${spring.mail.username:}") String mailFrom,
            @Value("${lanlink.verify-code.demo-mode:false}") boolean demoMode) {
        this.mailSender = mailSender;
        this.mailHost = mailHost;
        this.mailFrom = mailFrom;
        this.demoMode = demoMode;
    }

    /**
     * 发送验证码。开发演示回显必须显式开启 demo-mode。
     * 发送失败时移除本次验证码，避免无效验证码占用冷却期。
     */
    public String send(Long userId, String type, String target) {
        if (target == null || target.isBlank()) {
            throw new BusinessException("验证码接收方不能为空");
        }
        if (!"email".equals(type) && !"phone".equals(type)) {
            throw new BusinessException("不支持的验证码类型");
        }

        String key = key(userId, type, target);
        long now = System.currentTimeMillis();
        // 清理过期项，避免长时间运行后验证码缓存无限增长。
        store.entrySet().removeIf(entry -> entry.getValue().exp < now);

        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        CodeBox nb = new CodeBox();
        nb.code = code;
        nb.exp = now + TTL_MS;
        nb.lastSendAt = now;

        // 对单个 key 原子预留冷却窗口，避免并发请求同时通过限流检查。
        store.compute(key, (k, current) -> {
            if (current != null && now - current.lastSendAt < COOLDOWN_MS
                    && current.exp >= now) {
                throw new BusinessException("验证码发送过于频繁，请稍后再试");
            }
            return nb;
        });

        if ("email".equals(type)) {
            try {
                if (sendEmailCode(target, code)) {
                    return null; // 已真实发送，不回显验证码
                }
                if (demoMode) {
                    log.warn("SMTP 未配置（host={}），当前为开发验证码演示模式", mailHost);
                    return code;
                }
                store.remove(key, nb);
                throw new BusinessException("邮箱验证码服务暂未配置");
            } catch (BusinessException ex) {
                store.remove(key, nb);
                throw ex;
            }
        }

        if (!demoMode) {
            store.remove(key, nb);
            throw new BusinessException("短信验证码服务暂未配置");
        }
        return code; // 仅开发演示模式回显
    }

    /** 校验验证码；成功后立即失效（单次使用） */
    public void verify(Long userId, String type, String target, String code) {
        String key = key(userId, type, target);
        CodeBox box = store.get(key);
        if (box == null) {
            throw new BusinessException("验证码已过期，请重新获取");
        }
        if (box.exp < System.currentTimeMillis()) {
            store.remove(key, box);
            throw new BusinessException("验证码已过期，请重新获取");
        }
        if (!box.code.equals(code)) {
            throw new BusinessException("验证码错误");
        }
        // compare-and-remove 保证并发校验时只有一个请求能成功消费验证码。
        if (!store.remove(key, box)) {
            throw new BusinessException("验证码已使用，请重新获取");
        }
    }

    /** 通过 SMTP 发送验证码邮件；返回 false 表示 SMTP 未配置。 */
    private boolean sendEmailCode(String to, String code) throws BusinessException {
        if (mailSender == null || mailHost == null || mailHost.isBlank()) {
            return false;
        }
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            if (mailFrom != null && !mailFrom.isBlank()) {
                helper.setFrom(mailFrom);
            }
            helper.setTo(to);
            helper.setSubject(MAIL_SUBJECT);
            helper.setText(buildHtmlBody(code), true);
            mailSender.send(msg);
            log.info("验证码邮件已发送至 {}（5 分钟内有效）", to);
            return true;
        } catch (jakarta.mail.MessagingException | org.springframework.mail.MailException ex) {
            log.error("验证码邮件发送失败 to={}", to, ex);
            throw new BusinessException("邮件发送失败，请稍后重试");
        }
    }

    private String buildHtmlBody(String code) {
        return "<div style=\"font-family:'Microsoft YaHei',Arial,sans-serif;max-width:520px;margin:0 auto;"
                + "padding:28px;border:1px solid #e5e7eb;border-radius:12px\">"
                + "<h2 style=\"margin:0 0 16px;color:#1f3864\">LANLink 商城</h2>"
                + "<p style=\"color:#333;font-size:14px\">您的邮箱验证码为：</p>"
                + "<p style=\"font-size:32px;font-weight:700;letter-spacing:6px;color:#1e6eb8;margin:8px 0 20px\">"
                + code + "</p>"
                + "<p style=\"color:#666;font-size:13px\">验证码 5 分钟内有效，请勿泄露给他人。"
                + "若非本人操作，请忽略此邮件。</p>"
                + "</div>";
    }

    private String key(Long userId, String type, String target) {
        return userId + ":" + type + ":" + target;
    }
}
