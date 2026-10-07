package com.lanlink.shopping.integration.security;

import com.lanlink.shopping.common.UserIdentity;
import com.lanlink.shopping.config.UserContext;
import com.lanlink.shopping.entity.User;
import com.lanlink.shopping.service.AuditService;
import com.lanlink.shopping.service.MerchantService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * PermInterceptor 单元测试：注解驱动的后端权限校验。
 */
class PermInterceptorTest {

    private AuditService auditService;
    private MerchantService merchantService;
    private PermInterceptor interceptor;
    private HttpServletRequest request;
    private HttpServletResponse response;

    static class Annotated {
        @RequirePerm("product:publish")
        public void publish() {}

        @RequirePerm("merchant:manage")
        public void merchantManage() {}

        @RequirePerm("vip:discount")
        public void vipOnly() {}

        @RequirePerm("   ")
        public void blankPerm() {}

        @RequirePerm("")
        public void emptyPerm() {}

        public void open() {}
    }

    @BeforeEach
    void setUp() throws Exception {
        auditService = mock(AuditService.class);
        merchantService = mock(MerchantService.class);
        interceptor = new PermInterceptor(auditService, merchantService);
        // 默认商户档可选权限为空集（回收/冻结语义），各用例按需覆盖
        when(merchantService.effectiveOptionalPerms(any())).thenReturn(Set.of());
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new java.io.PrintWriter(new java.io.StringWriter()));
        // 403 分支需要读取当前登录用户（此处均视为匿名，userId=null）
        var session = mock(jakarta.servlet.http.HttpSession.class);
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute(UserContext.SESSION_KEY)).thenReturn(null);
    }

    /** 模拟已登录用户（权限判定链路需要 userId 读取商户档案授权） */
    private void loginAs(Long userId) {
        User u = new User();
        u.setUserId(userId);
        var session = mock(jakarta.servlet.http.HttpSession.class);
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute(UserContext.SESSION_KEY)).thenReturn(u);
    }

    private HandlerMethod method(String name) throws Exception {
        return new HandlerMethod(new Annotated(), Annotated.class.getMethod(name));
    }

    @Test
    void noAnnotationPasses() throws Exception {
        assertTrue(interceptor.preHandle(request, response, method("open")));
    }

    @Test
    void emptyOrNullPermValueSkipsCheck() throws Exception {
        // 空字符串 / 空白字符串权限值：直接放行，不进入身份/权限判断与审计
        assertTrue(interceptor.preHandle(request, response, method("emptyPerm")));
        assertTrue(interceptor.preHandle(request, response, method("blankPerm")));
        // 未登录（GUEST）也不应触发 401 —— 校验流程已跳过
        when(request.getAttribute(UserContext.IDENTITY_KEY)).thenReturn(UserIdentity.GUEST);
        assertTrue(interceptor.preHandle(request, response, method("blankPerm")));
        verify(auditService, never()).record(any(), any(), any(), any());
    }

    @Test
    void guestDenied401() throws Exception {
        // 未登录：identity = GUEST
        when(request.getAttribute(UserContext.IDENTITY_KEY)).thenReturn(UserIdentity.GUEST);
        assertFalse(interceptor.preHandle(request, response, method("publish")));
    }

    @Test
    void buyerWithoutPermDenied403AndAudited() throws Exception {
        when(request.getAttribute(UserContext.IDENTITY_KEY)).thenReturn(UserIdentity.BUYER);
        var session = mock(jakarta.servlet.http.HttpSession.class);
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute(UserContext.SESSION_KEY)).thenReturn(null);

        assertFalse(interceptor.preHandle(request, response, method("publish")));
        verify(auditService, times(1)).record(isNull(), eq("ACCESS_DENIED"), contains("product:publish"), eq(request));
    }

    @Test
    void merchantWithGrantedPermPasses() throws Exception {
        // 商户身份 + 档案已授予 product:publish → 放行（授权来自商户档案而非角色矩阵）
        when(request.getAttribute(UserContext.IDENTITY_KEY)).thenReturn(UserIdentity.MERCHANT);
        loginAs(7L);
        when(merchantService.effectiveOptionalPerms(7L)).thenReturn(Set.of("product:publish"));

        assertTrue(interceptor.preHandle(request, response, method("publish")));
        verify(merchantService).effectiveOptionalPerms(7L);
    }

    @Test
    void merchantWithRevokedPermDenied403AndAudited() throws Exception {
        // 商户身份但档案已回收该权限（默认桩返回空集）→ 403 + 审计
        when(request.getAttribute(UserContext.IDENTITY_KEY)).thenReturn(UserIdentity.MERCHANT);
        loginAs(7L);

        assertFalse(interceptor.preHandle(request, response, method("publish")));
        verify(auditService).record(eq(7L), eq("ACCESS_DENIED"), contains("product:publish"), eq(request));
    }

    @Test
    void frozenMerchantDeniedMerchantManage() throws Exception {
        // 账户冻结时 effectiveOptionalPerms 返回空集 → merchant:manage 即时失效
        when(request.getAttribute(UserContext.IDENTITY_KEY)).thenReturn(UserIdentity.MERCHANT);
        loginAs(8L);

        assertFalse(interceptor.preHandle(request, response, method("merchantManage")));
    }

    @Test
    void adminBypassesMerchantGrantCheck() throws Exception {
        // 管理员 admin:all 通配，不受商户级授权限制，也不查询商户档案
        when(request.getAttribute(UserContext.IDENTITY_KEY)).thenReturn(UserIdentity.ADMIN);
        assertTrue(interceptor.preHandle(request, response, method("publish")));
        verify(merchantService, never()).effectiveOptionalPerms(any());
    }

    @Test
    void nonOptionalPermDoesNotTouchMerchantProfile() throws Exception {
        // 非商户档可选权限（VIP 专属）直接走角色矩阵，不产生商户档案查询
        when(request.getAttribute(UserContext.IDENTITY_KEY)).thenReturn(UserIdentity.VIP);
        assertTrue(interceptor.preHandle(request, response, method("vipOnly")));
        verify(merchantService, never()).effectiveOptionalPerms(any());
    }

    @Test
    void buyerVipDenied403VipOnly() throws Exception {
        when(request.getAttribute(UserContext.IDENTITY_KEY)).thenReturn(UserIdentity.BUYER);
        assertFalse(interceptor.preHandle(request, response, method("vipOnly")));
    }

    @Test
    void vipWithPermPasses() throws Exception {
        when(request.getAttribute(UserContext.IDENTITY_KEY)).thenReturn(UserIdentity.VIP);
        assertTrue(interceptor.preHandle(request, response, method("vipOnly")));
    }
}
