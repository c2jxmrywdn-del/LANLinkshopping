package com.lanlink.shopping.integration.security;

import com.lanlink.shopping.common.R;
import com.lanlink.shopping.common.UserIdentity;
import com.lanlink.shopping.config.UserContext;
import com.lanlink.shopping.entity.User;
import com.lanlink.shopping.service.AuditService;
import com.lanlink.shopping.service.MerchantService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * 权限校验拦截器（后端权威访问控制）。
 *
 * 执行顺序：AuthInterceptor（登录 + 身份实时识别 + 后台 RBAC）之后运行。
 * 读取 Handler 方法上的 @RequirePerm，按当前请求身份权限矩阵校验：
 *  - 身份为 GUEST（未登录）→ 401
 *  - 身份已登录但缺少所需权限 → 403 + ACCESS_DENIED 审计日志
 *
 * 商户级权限（可选权限白名单，见 UserIdentity.MERCHANT_OPTIONAL_PERMS）：
 *  - 商户身份下 merchant:manage / product:publish 等可选权限以商户档案授权为准，
 *    支持运营端按商户授予/回收，且账户冻结/注销即时失效；
 *  - 授权集合按请求缓存（request attribute），单次请求最多查询一次商户档案（idx_merchant_user 索引点查）。
 *
 * 权限标识与身份权限矩阵的映射见 common.UserIdentity。
 */
@Component
public class PermInterceptor implements HandlerInterceptor {

    /** 商户授权权限的请求级缓存键 */
    private static final String MERCHANT_PERMS_KEY = "LL_MERCHANT_PERMS";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AuditService auditService;
    private final MerchantService merchantService;

    public PermInterceptor(AuditService auditService, MerchantService merchantService) {
        this.auditService = auditService;
        this.merchantService = merchantService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod hm)) return true;

        RequirePerm need = AnnotatedElementUtils.findMergedAnnotation(hm.getMethod(), RequirePerm.class);
        // 无注解 / 权限值为 null 或空白：视为无需额外权限，直接放行（避免冗余检查与误拦截）
        if (need == null || need.value() == null || need.value().isBlank()) return true;

        UserIdentity identity = UserContext.identity(request);
        if (identity == null || identity == UserIdentity.GUEST) {
            write(response, R.fail(401, "请先登录"));
            return false;
        }
        if (!hasPermission(identity, UserContext.current(request), need.value(), request)) {
            User u = UserContext.current(request);
            auditService.record(u == null ? null : u.getUserId(), "ACCESS_DENIED",
                    "权限不足: 需要[" + need.value() + "]，当前身份[" + identity.getCode() + "] " + request.getRequestURI(),
                    request);
            write(response, R.fail(403, "无权限执行该操作"));
            return false;
        }
        return true;
    }

    /**
     * 权限判定：
     *  - 非商户身份，或所需权限不属于商户可选权限白名单 → 走角色权限矩阵；
     *  - 商户身份 + 商户可选权限 → 以该商户档案的授权集合为准（未配置=默认全量；冻结/注销=空集）。
     */
    private boolean hasPermission(UserIdentity identity, User user, String perm, HttpServletRequest request) {
        if (identity != UserIdentity.MERCHANT || !UserIdentity.MERCHANT_OPTIONAL_PERMS.contains(perm)) {
            return identity.has(perm);
        }
        return merchantOptionalPerms(user, request).contains(perm);
    }

    /** 商户有效可选权限（请求级缓存，避免同一请求重复查询商户档案） */
    private Set<String> merchantOptionalPerms(User user, HttpServletRequest request) {
        Object cached = request.getAttribute(MERCHANT_PERMS_KEY);
        if (cached instanceof Set<?> set) {
            @SuppressWarnings("unchecked")
            Set<String> cast = (Set<String>) set;
            return cast;
        }
        Set<String> perms = user == null ? Set.of() : merchantService.effectiveOptionalPerms(user.getUserId());
        request.setAttribute(MERCHANT_PERMS_KEY, perms);
        return perms;
    }

    private void write(HttpServletResponse response, R<?> body) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
