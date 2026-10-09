package com.lanlink.shopping.config;

import com.lanlink.shopping.common.UserIdentity;
import com.lanlink.shopping.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * UserContext.identity() 兜底逻辑单测：
 * 保证任何场景下都不返回 null（修复 NPE 缺陷）。
 */
class UserContextTest {

    private HttpServletRequest request;
    private HttpSession session;

    private void mockRequest() {
        request = mock(HttpServletRequest.class);
        session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);
    }

    private User loggedUser() {
        User u = new User();
        u.setUserId(2L);
        u.setRoleId(1L);
        return u;
    }

    @Test
    void notLoggedInReturnsGuest() {
        mockRequest();
        // 未登录：无身份属性、session 无登录用户
        when(request.getAttribute(UserContext.IDENTITY_KEY)).thenReturn(null);
        when(session.getAttribute(UserContext.SESSION_KEY)).thenReturn(null);
        assertEquals(UserIdentity.GUEST, UserContext.identity(request));
    }

    @Test
    void loggedInWithoutIdentityAttributeReturnsBuyer() {
        mockRequest();
        // 已登录（session 有用户）但身份属性缺失 → 兜底 BUYER，绝���q�^