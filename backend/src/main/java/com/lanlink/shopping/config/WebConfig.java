package com.lanlink.shopping.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置：注册拦截器 + 允许前端跨域 + 上传文件静态映射
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final com.lanlink.shopping.integration.security.PermInterceptor permInterceptor;
    private final com.lanlink.shopping.service.SysConfigService sysConfigService;

    @Value("${app.cors.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*}")
    private String allowedOriginPatterns;

    public WebConfig(AuthInterceptor authInterceptor,
                     com.lanlink.shopping.integration.security.PermInterceptor permInterceptor,
                     com.lanlink.shopping.service.SysConfigService sysConfigService) {
        this.authInterceptor = authInterceptor;
        this.permInterceptor = permInterceptor;
        this.sysConfigService = sysConfigService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RateLimitInterce���q�^