package com.lanlink.shopping.service;

import com.lanlink.shopping.common.BusinessException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 验证码服务：内存存储，5 分钟过期 + 60 秒冷却。
 * 演示环境：验证码由接口直接返回（devCode），未接真实短信/邮件通道。
 */
@Service
public class VerifyCodeService {

    private static final long TTL_MS = 5 * 60 * 1000L;
    private static final long COOLDOWN_MS = 60 * 1000L;

    private static class CodeBox {
        String code;
        long exp;
        long lastSendAt;
    }

    private final Map<String, CodeBox> store = new ConcurrentHashMap<>();

    /** 发送验证码，返回明文（演示环境供 devCode 回显） */
    public String send(Long userId, String type, String target) {
        String key = key(userId, type, target);
        CodeBox box = store.get(key);
        long now = System.currentTimeMillis();
        if (box != null && now - box.lastSendAt < COOLDOWN_MS) {
            throw new BusinessException("验证码发送过于频繁，请稍后再试");
        }
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        CodeBox nb = new CodeBox();
        nb.code = code;
        nb.exp = now + TTL_MS;
        nb.lastSendAt = now;
        store.put(key, nb);
        return code;
    }

    /** 校验验证码；成功后立即失效（单次使用） */
    public void verify(Long userId, String type, String target, String code) {
        String key = key(userId, type, target);
        CodeBox box = store.get(key);
        if (box == null || box.exp < System.currentTimeMillis()) {
            store.remove(key);
            throw new BusinessException("验证码已过期，请重新获取");
        }
        if (!box.code.equals(code)) {
            throw new BusinessException("验证码错误");
        }
        store.remove(key);
    }

    private String key(Long userId, String type, String target) {
        return userId + ":" + type + ":" + target;
    }
}