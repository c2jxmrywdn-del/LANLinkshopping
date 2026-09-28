package com.lanlink.shopping.config;

import com.lanlink.shopping.common.R;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lanlink.shopping.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录 + RBAC 拦截器
 * 规则:
 *  - 白名单(注册/登录/公开浏览)直接放行
 *  - /admin/** 需平台运营(role_id=3)
 *  - 其余需登录
 */
public class AuthInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI().replace(request.getContextPath(), "");

        if (isWhiteList(uri)) {
            return true;
        }
        if (UserContext.current(request) == null) {
            write(response, R.fail(401, "请先登录"));
            return false;
        }
        // 运营后台鉴权
        if (uri.startsWith("/admin/") || uri.contains("/admin/")) {
            User u = UserContext.current(request);
            if (u.getRoleId() == null || u.getRoleId() != 3L) {
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
            || (uri.startsWith("/product/") && !uri.contains("/admin/"));
    }

    private void write(HttpServletResponse response, R<?> body) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
