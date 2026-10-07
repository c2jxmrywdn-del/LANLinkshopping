package com.lanlink.shopping.common;

import com.lanlink.shopping.config.AppCryptoProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 敏感字段加密工具（AES-256-GCM）。
 * 存储约定：密文统一为 "enc:" 前缀 + Base64(12字节随机IV + 密文含16字节认证标签)；
 *           每次加密使用全新随机 IV，同一明文多次加密产生不同密文。
 * 存量兼容：读取时按前缀判断，无前缀视为历史明文原样返回（写入时自动升级为密文），
 *           单条记录读取路径可配合读侧迁移将历史明文补加密落库。
 * 展示约定：列表/详情出口统一 {@link #mask(String)} 脱敏（保留前3后4），
 *           完整明文仅在管理端授权接口按需解密返回。
 */
@Slf4j
@Component
@EnableConfigurationProperties(AppCryptoProperties.class)
public class CryptoUtil {

    private static final String CIPHER_PREFIX = "enc:";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    /** 未配置密钥时的演示回退密钥（仅本地演示可运行，切勿用于生产） */
    private static final String DEV_FALLBACK_KEY = "lanlink-shopping-demo-crypto-key";

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public CryptoUtil(AppCryptoProperties props) {
        this.key = deriveKey(props == null ? null : props.getKey());
    }

    /** 测试/兼容路径：使用演示密钥构建（不参与 Spring 装配） */
    public static CryptoUtil devDefault() {
        return new CryptoUtil(new AppCryptoProperties());
    }

    /** 加密：null/空串原样返回；已带 enc: 前缀的幂等返回（避免二次加密） */
    public String encrypt(String plain) {
        if (plain == null || plain.isEmpty() || isEncrypted(plain)) {
            return plain;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(cipherText, 0, out, iv.length, cipherText.length);
            return CIPHER_PREFIX + Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("敏感字段加密失败", e);
        }
    }

    /** 解密：无 enc: 前缀的历史明文原样返回（存量兼容）；密文损坏或密钥不匹配抛出明确异常 */
    public String decrypt(String stored) {
        if (!isEncrypted(stored)) {
            return stored;
        }
        try {
            byte[] all = Base64.getDecoder().decode(stored.substring(CIPHER_PREFIX.length()));
            if (all.length <= IV_BYTES) {
                throw new IllegalArgumentException("密文长度非法");
            }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, all, 0, IV_BYTES));
            byte[] plain = cipher.doFinal(all, IV_BYTES, all.length - IV_BYTES);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("敏感字段解密失败（密钥不匹配或密文损坏）", e);
        }
    }

    /** 是否已加密（带 enc: 前缀） */
    public boolean isEncrypted(String s) {
        return s != null && s.startsWith(CIPHER_PREFIX);
    }

    /** 脱敏展示：保留前3后4，中间以 **** 占位；长度不足时整体掩码 */
    public static String mask(String plain) {
        if (plain == null || plain.isEmpty()) {
            return plain;
        }
        if (plain.length() <= 8) {
            return "****";
        }
        return plain.substring(0, 3) + "****" + plain.substring(plain.length() - 4);
    }

    /**
     * 是否为脱敏占位值（含 **** 掩码符）。
     * 写入前用它拦截「前端把脱敏值原样回填提交」的情况，避免掩码被当作真实值加密落库。
     */
    public static boolean isMasked(String value) {
        return value != null && value.contains("****");
    }

    /** 密钥派生：Base64(32字节) 直接使用；否则 SHA-256(口令串) 派生 256 位密钥 */
    private static SecretKey deriveKey(String configured) {
        String material = configured;
        if (material == null || material.isBlank()) {
            log.warn("未配置 lanlink.crypto.key（建议设置环境变量 MERCHANT_CRYPTO_KEY），"
                    + "敏感字段加密回退内置演示密钥，切勿用于生产环境");
            material = DEV_FALLBACK_KEY;
        }
        byte[] bytes = tryDecodeBase64(material);
        if (bytes == null || bytes.length != 32) {
            bytes = sha256(material);
        }
        return new SecretKeySpec(bytes, "AES");
    }

    private static byte[] tryDecodeBase64(String s) {
        try {
            return Base64.getDecoder().decode(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static byte[] sha256(String s) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
