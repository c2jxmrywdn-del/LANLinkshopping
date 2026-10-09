package com.lanlink.shopping.config;

import com.lanlink.shopping.common.R;
import com.lanlink.shopping.common.UserIdentity;
import com.lanlink.shopping.entity.User;
import com.lanlink.shopping.service.AuditService;
import com.lanlink.shopping.service.IdentityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录 + RBAC 拦截器（同时承担用户身份识别与越权审计）。
 * 规则:
 *  - 白名单(注册/登录/公开浏览)直接放行
 *  - 每次请求实时识别用户身份（GUEST/BUYER/VIP/MERCHANT/ADMIN），写入 request attribute
 *  - /admin/** 需平台运营(role_id=3)，越权访问记录 ACCESS_DENIED 审计日志
 *  - 其余需登录
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final IdentityService identityService;
    private final AuditService auditService;

    public AuthInterceptor(IdentityService identityService, AuditService auditService) {
        this.identityService = identityService;
        this.auditService = auditService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestU���q�^