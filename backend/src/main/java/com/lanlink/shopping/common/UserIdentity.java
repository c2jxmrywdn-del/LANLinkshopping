package com.lanlink.shopping.common;

import java.util.List;

/**
 * 用户身份类型（分类标准与识别口径）。
 *
 * 识别优先级：ADMIN > MERCHANT > VIP > BUYER > GUEST。
 *  - GUEST   访客：未登录，仅可浏览/搜索商品
 *  - BUYER   普通用户：已登录（采购方角色）
 *  - VIP     VIP 用户：采购方且累计已支付金额 ≥ 阈值（IdentityService.VIP_THRESHOLD）
 *  - MERCHANT 商户：商户角色
 *  - ADMIN   管理员：平台运营角色
 *
 * 每种身份的服务范围、功能权限由 IdentityService.permissionsOf(identity) 的权限矩阵统一描述，
 * 服务端据此做越权校验，前端据此做差异化渲染。
 */
public enum UserIdentity {

    GUEST("guest", "访客", "浏览商品、搜索", List.of("product:view", "product:search")),
    BUYER("buyer", "普通用户", "购物车、下单、订单管理、收货地址、个人资料",
            List.of("product:view", "product:search", "cart:manage", "order:create", "order:view",
                    "address:manage", "profile:manage", "credit:view", "credit:apply", "credit:repay")),
    VIP("vip", "VIP用户", "普通用户全部服务 + VIP 折扣价、专属服务",
            List.of("product:view", "product:search", "cart:manage", "order:create", "order:view",
                    "address:manage", "profile:manage", "vip:discount", "vip:service", "credit:view", "credit:apply", "credit:repay")),
    MERCHANT("merchant", "商户", "商品发布与管理、商户中心、订单履约",
            List.of("product:view", "product:search", "cart:manage", "order:create", "order:view",
                    "address:manage", "profile:manage", "merchant:manage", "product:publish")),
    ADMIN("admin", "管理员", "全部前台服务 + 平台运营后台（数据概览、商户管理、商品管理、交易管理、审计日志）",
            List.of("admin:all"));

    /**
     * 商户档「可选权限」白名单：运营端可对单个商户授予/回收，商户档案 perm_codes 仅允许配置该集合内的权限点。
     * 判定规则（见 integration.security.PermInterceptor）：
     *  - 商户身份下这些权限不再由角色矩阵直接决定，而以商户档案授权为准
     *    （perm_codes 为 null=未配置 → 默认全量授予，保证存量商户不降权；空串 → 显式不授予任何可选权限）；
     *  - 账户状态为冻结(2)/注销(3) 时可选项权限全部失效，实现「冻结即时收回经营权限」；
     *  - 其余权限仍走角色矩阵，保证按商户差异化授权的同时不越权。
     */
    public static final List<String> MERCHANT_OPTIONAL_PERMS =
            List.of("merchant:manage", "product:publish");

    /** 稳定标识（前端/日志使用） */
    private final String code;
    /** 展示名称 */
    private final String name;
    /** 服务范围说明（展示给用户） */
    private final String serviceScope;
    /** 功能权限集合 */
    private final List<String> permissions;

    UserIdentity(String code, String name, String serviceScope, List<String> permissions) {
        this.code = code;
        this.name = name;
        this.serviceScope = serviceScope;
        this.permissions = permissions;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public String getServiceScope() { return serviceScope; }
    public List<String> getPermissions() { return permissions; }

    /** 是否包含指定权限；admin:all 视为通配符（管理员拥有全部权限） */
    public boolean has(String permission) {
        return permissions.contains("admin:all") || permissions.contains(permission);
    }
}
