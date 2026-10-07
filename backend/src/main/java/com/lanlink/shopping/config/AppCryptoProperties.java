package com.lanlink.shopping.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 敏感字段加密配置（前缀 lanlink.crypto）。
 * 安全约定：生产环境必须通过环境变量 MERCHANT_CRYPTO_KEY 注入（支持 Base64(32字节) 密钥，
 * 或任意口令串——内部经 SHA-256 派生为 256 位密钥）；未配置时回退内置演示密钥并打印告警，
 * 与支付模块 PaymentProperties 的演示凭证约定一致，保证本地开箱可运行。
 */
@Data
@ConfigurationProperties(prefix = "lanlink.crypto")
public class AppCryptoProperties {

    /** AES-256-GCM 密钥：Base64(32字节) 或任意口令串；留空回退演示密钥 */
    private String key = "";
}
