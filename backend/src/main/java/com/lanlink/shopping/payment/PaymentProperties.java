package com.lanlink.shopping.payment;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 支付配置（前缀 lanlink.payment）。
 * 安全约定：所有密钥/证书路径仅从环境变量或 gitignore 的本地配置文件读取，
 * 模拟支付默认关闭；仅本地 dev profile 显式开启，生产环境必须使用真实支付渠道。
 */
@Data
@ConfigurationProperties(prefix = "lanlink.payment")
public class PaymentProperties {

    /** 全局模拟开关：true 时不请求真实渠道，本地直接生成支付参数并支持手动确认回调 */
    private boolean mock = false;

    /** 回调/跳转的公网基础地址（真实联调需部署到 HTTPS 域名，例：https://shop.example.com/api） */
    private String notifyBaseUrl = "http://localhost:8080/api";

    private Wechat wechat = new Wechat();
    private Alipay alipay = new Alipay();

    @Data
    public static class Wechat {
        /** 公众号/小程序/开放平台 AppID */
        private String appId = "";
        /** 商户号 */
        private String mchId = "";
        /** APIv2 商户密钥（32 位，商户平台设置；用于签名，切勿入库） */
        private String apiKey = "";
        /** 商户 API 证书 apiclient_cert.p12 绝对路径 */
        private String certPath = "";
        /** 签名类型 MD5 | HMAC-SHA256 */
        private String signType = "MD5";
        /** 交易类型：NATIVE(扫码) / JSAPI���q�^