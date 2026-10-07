package com.lanlink.shopping.integration.security;

import com.lanlink.shopping.controller.MerchantController;
import com.lanlink.shopping.dto.MerchantStatusDTO;
import com.lanlink.shopping.dto.MerchantUpdateDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 运营端商户管理接口契约回归测试（无容器、纯反射）。
 * 锁定三件事，防止后续重构破坏安全边界：
 *  1. 维护类接口必须落在 /admin/ 路径下 —— AuthInterceptor 正是按 URI 含 "/admin/" 限定「仅平台运营」；
 *  2. HTTP 方法与路径稳定 —— 前端与接口文档依赖该契约；
 *  3. 高危入参（状态/原因/注册资本）保留 Bean Validation 约束，且写操作保留 HttpServletRequest 以便审计记录来源 IP。
 */
class MerchantAdminEndpointTest {

    private Method method(String name, Class<?>... types) throws Exception {
        return MerchantController.class.getMethod(name, types);
    }

    /** 断言方法映射到期望路径，并返回 HTTP 方法名 */
    private String assertMapped(Method m, String expectedPath) {
        String[] paths = null;
        String http = null;
        PutMapping put = m.getAnnotation(PutMapping.class);
        GetMapping get = m.getAnnotation(GetMapping.class);
        PostMapping post = m.getAnnotation(PostMapping.class);
        DeleteMapping del = m.getAnnotation(DeleteMapping.class);
        if (put != null) { paths = put.value(); http = "PUT"; }
        else if (get != null) { paths = get.value(); http = "GET"; }
        else if (post != null) { paths = post.value(); http = "POST"; }
        else if (del != null) { paths = del.value(); http = "DELETE"; }

        assertNotNull(paths, m.getName() + " 缺少 HTTP 映射注解");
        assertEquals(1, paths.length, m.getName() + " 应显式声明单一映射路径");
        assertEquals(expectedPath, paths[0], m.getName() + " 映射路径与接口文档不一致");
        assertTrue(paths[0].startsWith("/admin/"),
                m.getName() + " 必须位于 /admin/ 下，否则不会被 AuthInterceptor 限定为平台运营");
        return http;
    }

    private void assertHasRequestParam(Method m) {
        assertTrue(Arrays.stream(m.getParameterTypes()).anyMatch(t -> t == HttpServletRequest.class),
                m.getName() + " 应接收 HttpServletRequest，否则审计日志无法记录来源 IP");
    }

    @Test
    void classBasePathIsMerchant() {
        RequestMapping rm = MerchantController.class.getAnnotation(RequestMapping.class);
        assertNotNull(rm);
        assertArrayEquals(new String[]{"/merchant"}, rm.value());
    }

    @Test
    void adminCrudEndpointsKeepDocumentedContracts() throws Exception {
        Method update = method("adminUpdate", Long.class, MerchantUpdateDTO.class, HttpServletRequest.class);
        assertEquals("PUT", assertMapped(update, "/admin/{merId}"));
        assertHasRequestParam(update);

        Method delete = method("adminDelete", Long.class, String.class, HttpServletRequest.class);
        assertEquals("DELETE", assertMapped(delete, "/admin/{merId}"));
        assertHasRequestParam(delete);
    }

    @Test
    void adminStatusEndpointKeepsContract() throws Exception {
        Method status = method("adminStatus", Long.class, MerchantStatusDTO.class, HttpServletRequest.class);
        assertEquals("POST", assertMapped(status, "/admin/status/{merId}"));
        assertHasRequestParam(status);
    }

    @Test
    void adminPermissionEndpointsKeepContract() throws Exception {
        Method read = method("adminPermConfig", Long.class);
        assertEquals("GET", assertMapped(read, "/admin/perms/{merId}"));

        Method write = method("adminPermUpdate", Long.class, List.class, HttpServletRequest.class);
        assertEquals("PUT", assertMapped(write, "/admin/perms/{merId}"));
        assertHasRequestParam(write);
    }

    @Test
    void adminTaxRegNoEndpointKeepsContract() throws Exception {
        Method taxNo = method("adminTaxRegNo", Long.class, HttpServletRequest.class);
        assertEquals("GET", assertMapped(taxNo, "/admin/{merId}/tax-reg-no"));
        assertHasRequestParam(taxNo);
    }

    @Test
    void statusDtoRequiresReasonAndBoundedStatus() throws Exception {
        Field status = MerchantStatusDTO.class.getDeclaredField("status");
        assertNotNull(status.getAnnotation(NotNull.class), "账户状态为必填");
        assertEquals(1, status.getAnnotation(Min.class).value());
        assertEquals(3, status.getAnnotation(Max.class).value(), "状态取值仅 1正常/2冻结/3注销");

        Field reason = MerchantStatusDTO.class.getDeclaredField("reason");
        assertNotNull(reason.getAnnotation(NotBlank.class), "状态变更必须填写原因，供审计与流程跟踪");
    }

    @Test
    void updateDtoBoundsCapitalAndAllowsPartialSubmit() throws Exception {
        Field regCapital = MerchantUpdateDTO.class.getDeclaredField("regCapital");
        assertEquals("0", regCapital.getAnnotation(DecimalMin.class).value(), "注册资本不可为负");

        // 资料修改为「按提交字段增量更新」：除校验约束外不应有 @NotBlank 之类的必填限制
        for (String name : List.of("entName", "creditCode", "regType", "joinType", "taxRegNo")) {
            Field f = MerchantUpdateDTO.class.getDeclaredField(name);
            assertNull(f.getAnnotation(NotNull.class), name + " 不应强制必填，否则无法增量更新");
            assertNull(f.getAnnotation(NotBlank.class), name + " 不应强制必填，否则无法增量更新");
        }
    }

    @Test
    void adminListAndDetailEndpointsExist() throws Exception {
        assertNotNull(method("adminList", Integer.class));
        // 分页接口支持 审核状态 / 账户状态 / 关键词 三维筛选
        assertNotNull(method("adminPage", long.class, long.class, Integer.class, Integer.class, String.class));
        assertNotNull(method("adminDetail", Long.class));
        assertNotNull(method("reviewLogs", Long.class));
    }
}
