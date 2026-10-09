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
 * 未配置 SMTP 时回退演示模式（接口回显 devCode），便于离线演示。
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
        int failedAttempts;
    }

    private final Map<String, CodeBox> store = new ConcurrentHashMap<>();

    private final JavaMailSender mailSender;
    private final Stri���q�^