package com.lanlink.shopping.config;

import com.lanlink.shopping.entity.User;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 当前登录用户上下文（基于 HttpSession）
 */
public class UserContext {
    public static final String SESSION_KEY = "LOGIN_USER";

    public static User current(HttpServletRequest request) {
        if (request == null) return null;
        Object obj = request.getSession().getAttribute(SESSION_KEY);
        return obj instanceof User ? (User) obj : null;
    }

    public static Long currentUserId(HttpServletRequest request) {
        User u = current(request);
        return u == null ? null : u.getUserId();
    }
}
