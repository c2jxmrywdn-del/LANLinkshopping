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
        String uri = request.getRequestURI().replace(request.getContextPath(), "");

        // 实时身份识别（每次请求现场计算，保证准确性与实时性；身份变更无需重新登录即平滑切换）
        UserIdentity identity = identityService.identify(request);
        request.setAttribute(UserContext.IDENTITY_KEY, identity);

        if (isWhiteList(uri)) {
            return true;
        }
        if (UserContext.current(request) == null) {
            write(response, R.fail(401, "请先登录"));
            return false;
        }
        // 运营后台鉴权（越权访问记录审计日志）
        if (uri.startsWith("/admin/") || uri.contains("/admin/")) {
            User u = UserContext.current(request);
            if (u.getRoleId() == null || u.getRoleId() != 3L) {
                auditService.record(u.getUserId(), "ACCESS_DENIED", "越权访问后台: " + uri, request);
                write(response, R.fail(403, "无权限：仅平台运营可操作"));
                return false;
            }
        }
        return true;
    }

    private boolean isWhiteList(String uri) {
        return uri.startsWith("/auth/login")
                || uri.startsWith("/auth/register")
                || uri.startsWith("/home/")
                || uri.startsWith("/category/")
                // 商品仅公开浏览(分页/详情)；发布/图片上传/我的列表/审核均需登录
                || uri.equals("/product/page")
                || uri.startsWith("/product/detail/");
    }

    private void write(HttpServletResponse response, R<?> body) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
