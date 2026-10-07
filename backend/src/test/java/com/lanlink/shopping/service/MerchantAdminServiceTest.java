package com.lanlink.shopping.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lanlink.shopping.common.BusinessException;
import com.lanlink.shopping.common.CryptoUtil;
import com.lanlink.shopping.common.UserIdentity;
import com.lanlink.shopping.dto.MerchantUpdateDTO;
import com.lanlink.shopping.entity.Enterprise;
import com.lanlink.shopping.entity.Merchant;
import com.lanlink.shopping.entity.MerchantReviewLog;
import com.lanlink.shopping.entity.Qualification;
import com.lanlink.shopping.entity.User;
import com.lanlink.shopping.mapper.EnterpriseMapper;
import com.lanlink.shopping.mapper.MerchantMapper;
import com.lanlink.shopping.mapper.MerchantReviewLogMapper;
import com.lanlink.shopping.mapper.QualificationMapper;
import com.lanlink.shopping.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 运营端商户管理服务单元测试：资料增删改查、账户状态流转、权限配置、敏感字段加密与审计日志。
 * 覆盖 plan 的 admin-crud-api / crypto-security 交付项的验收点。
 */
class MerchantAdminServiceTest {

    private static final Long OPERATOR = 99L;
    private static final String CREDIT_CODE = "91500000MA5U0000XA";
    private static final String TAX_REG_NO = "91500000MA5U0000XA";

    private MerchantMapper merchantMapper;
    private EnterpriseMapper enterpriseMapper;
    private QualificationMapper qualificationMapper;
    private AuditService auditService;
    private UserMapper userMapper;
    private MerchantReviewLogMapper reviewLogMapper;
    private CryptoUtil cryptoUtil;
    private HttpServletRequest request;
    private MerchantService service;

    /**
     * 纯单元测试没有 SqlSessionFactory，MyBatis-Plus 的 lambda 列名缓存（TableInfo）不会自动安装，
     * 使用 `Wrappers.lambdaQuery()` 时会报 “can not find lambda cache for this entity”。
     * 这里显式注册本测试涉及的实体元数据，使查询条件构造可在无容器环境下执行。
     */
    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        for (Class<?> entity : List.of(Merchant.class, Enterprise.class, Qualification.class,
                User.class, MerchantReviewLog.class)) {
            TableInfoHelper.initTableInfo(assistant, entity);
        }
    }

    @BeforeEach
    void setUp() {
        merchantMapper = mock(MerchantMapper.class);
        enterpriseMapper = mock(EnterpriseMapper.class);
        qualificationMapper = mock(QualificationMapper.class);
        auditService = mock(AuditService.class);
        userMapper = mock(UserMapper.class);
        reviewLogMapper = mock(MerchantReviewLogMapper.class);
        cryptoUtil = CryptoUtil.devDefault();
        request = mock(HttpServletRequest.class);
        service = new MerchantService(merchantMapper, enterpriseMapper, qualificationMapper,
                auditService, userMapper, reviewLogMapper, cryptoUtil);
    }

    private Merchant merchant(Long merId, Integer status) {
        Merchant m = new Merchant();
        m.setMerId(merId);
        m.setEntId(50L);
        m.setUserId(7L);
        m.setRegType("公司");
        m.setRegCapital(new BigDecimal("500000"));
        m.setTaxStatus(1);
        m.setJoinType("入驻");
        m.setReviewStatus(1);
        m.setStatus(status);
        return m;
    }

    private MerchantUpdateDTO fullDto() {
        MerchantUpdateDTO dto = new MerchantUpdateDTO();
        dto.setEntName("重庆华晨建材集团有限公司");
        dto.setCreditCode(CREDIT_CODE);
        dto.setRegType("个体工商户");
        dto.setRegCapital(new BigDecimal("200000"));
        dto.setTaxStatus(1);
        dto.setJoinType("邀约");
        dto.setTaxRegNo(TAX_REG_NO);
        return dto;
    }

    // ===== 基本资料修改 =====

    @Test
    void updateByAdminEncryptsSensitiveFieldsAndSyncsEnterprise() {
        Merchant m = merchant(1L, 1);
        Enterprise ent = new Enterprise();
        ent.setEntId(50L);
        ent.setName("旧名称");
        when(merchantMapper.selectById(1L)).thenReturn(m);
        when(enterpriseMapper.selectById(50L)).thenReturn(ent);

        // 落库瞬间快照取值：服务返回前会对同一实体做出口脱敏，不能等到 verify 后再读取实体字段
        final String[] taxNoAtWrite = new String[1];
        final String[] regTypeAtWrite = new String[1];
        doAnswer(inv -> {
            Merchant arg = inv.getArgument(0);
            taxNoAtWrite[0] = arg.getTaxRegNo();
            regTypeAtWrite[0] = arg.getRegType();
            return 1;
        }).when(merchantMapper).updateById(any(Merchant.class));

        Merchant out = service.updateByAdmin(1L, fullDto(), OPERATOR, request);

        assertEquals("个体工商户", regTypeAtWrite[0]);
        assertTrue(cryptoUtil.isEncrypted(taxNoAtWrite[0]), "税务登记号必须加密落库");
        assertEquals(TAX_REG_NO, cryptoUtil.decrypt(taxNoAtWrite[0]));

        ArgumentCaptor<Enterprise> ec = ArgumentCaptor.forClass(Enterprise.class);
        verify(enterpriseMapper).updateById(ec.capture());
        Enterprise savedEnt = ec.getValue();
        assertEquals("重庆华晨建材集团有限公司", savedEnt.getName(), "企业名称应联动更新");
        assertTrue(cryptoUtil.isEncrypted(savedEnt.getCreditCode()), "统一社会信用代码必须加密落库");
        assertEquals(CREDIT_CODE, cryptoUtil.decrypt(savedEnt.getCreditCode()));

        verify(auditService).record(eq(OPERATOR), eq("MERCHANT_UPDATE"), contains("税务登记号"), eq(request));
        assertEquals("915****00XA", out.getTaxRegNo(), "返回给前端的税务登记号必须脱敏");
    }

    @Test
    void updateByAdminOnlyTouchesSubmittedFields() {
        Merchant m = merchant(2L, 1);
        when(merchantMapper.selectById(2L)).thenReturn(m);

        MerchantUpdateDTO dto = new MerchantUpdateDTO();
        dto.setJoinType("加盟");
        service.updateByAdmin(2L, dto, OPERATOR, request);

        ArgumentCaptor<Merchant> mc = ArgumentCaptor.forClass(Merchant.class);
        verify(merchantMapper).updateById(mc.capture());
        assertEquals("加盟", mc.getValue().getJoinType());
        assertEquals("公司", mc.getValue().getRegType(), "未提交的字段应保持原值");
        assertNull(mc.getValue().getTaxRegNo(), "未提交税务登记号时不应写入");
        verify(enterpriseMapper, never()).updateById(any(Enterprise.class));
    }

    @Test
    void updateByAdminRejectsInvalidRegType() {
        when(merchantMapper.selectById(3L)).thenReturn(merchant(3L, 1));
        MerchantUpdateDTO dto = new MerchantUpdateDTO();
        dto.setRegType("合伙企业");

        assertThrows(BusinessException.class, () -> service.updateByAdmin(3L, dto, OPERATOR, request));
        verify(merchantMapper, never()).updateById(any(Merchant.class));
    }

    @Test
    void updateByAdminRejectsMissingMerchant() {
        when(merchantMapper.selectById(404L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.updateByAdmin(404L, fullDto(), OPERATOR, request));
    }

    // ===== 账户状态维护 =====

    @Test
    void changeStatusFreezesMerchantAndWritesAuditAndReviewLog() {
        when(merchantMapper.selectById(4L)).thenReturn(merchant(4L, 1));

        Merchant out = service.changeStatus(4L, 2, "存在违规经营行为", OPERATOR, request);

        assertEquals(2, out.getStatus());
        ArgumentCaptor<Merchant> mc = ArgumentCaptor.forClass(Merchant.class);
        verify(merchantMapper).updateById(mc.capture());
        assertEquals(2, mc.getValue().getStatus());

        ArgumentCaptor<MerchantReviewLog> lc = ArgumentCaptor.forClass(MerchantReviewLog.class);
        verify(reviewLogMapper).insert(lc.capture());
        assertEquals("status", lc.getValue().getAction());
        assertTrue(lc.getValue().getReason().contains("冻结"));

        verify(auditService).record(eq(OPERATOR), eq("MERCHANT_STATUS_CHANGE"), contains("冻结"), eq(request));
        verify(userMapper, never()).updateById(any(User.class)); // 冻结不改角色，仅权限链路失效
        verify(merchantMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void changeStatusRequiresReason() {
        when(merchantMapper.selectById(5L)).thenReturn(merchant(5L, 1));
        assertThrows(BusinessException.class, () -> service.changeStatus(5L, 2, "  ", OPERATOR, request));
        assertThrows(BusinessException.class, () -> service.changeStatus(5L, 2, null, OPERATOR, request));
    }

    @Test
    void changeStatusRejectsSameStatusAndDeletedMerchant() {
        when(merchantMapper.selectById(6L)).thenReturn(merchant(6L, 2));
        assertThrows(BusinessException.class, () -> service.changeStatus(6L, 2, "重复冻结", OPERATOR, request));

        when(merchantMapper.selectById(7L)).thenReturn(merchant(7L, 3));
        assertThrows(BusinessException.class, () -> service.changeStatus(7L, 1, "尝试恢复", OPERATOR, request));
    }

    @Test
    void changeStatusToCancelledDelegatesToDelete() {
        Merchant m = merchant(8L, 1);
        when(merchantMapper.selectById(8L)).thenReturn(m);
        User u = new User();
        u.setUserId(7L);
        u.setRoleId(2L);
        when(userMapper.selectById(7L)).thenReturn(u);

        Merchant out = service.changeStatus(8L, 3, "商户主动退出", OPERATOR, request);

        assertEquals(3, out.getStatus());
        verify(merchantMapper).deleteById(8L);
        assertEquals(1L, u.getRoleId(), "注销后商户负责人身份应降级为买家");
    }

    // ===== 注销（逻辑删除 + 身份降级）=====

    @Test
    void deleteByAdminLogicallyDeletesAndDowngradesMerchantRole() {
        when(merchantMapper.selectById(9L)).thenReturn(merchant(9L, 1));
        User u = new User();
        u.setUserId(7L);
        u.setRoleId(2L);
        when(userMapper.selectById(7L)).thenReturn(u);

        service.deleteByAdmin(9L, "长期未经营", OPERATOR, request);

        ArgumentCaptor<Merchant> mc = ArgumentCaptor.forClass(Merchant.class);
        verify(merchantMapper).updateById(mc.capture());
        assertEquals(3, mc.getValue().getStatus(), "注销应同时把账户状态置为已注销");
        verify(merchantMapper).deleteById(9L); // 逻辑删除（@TableLogic）
        verify(userMapper).updateById(u);
        assertEquals(1L, u.getRoleId());
        verify(auditService).record(eq(OPERATOR), eq("MERCHANT_DELETE"), contains("长期未经营"), eq(request));
        verify(auditService).record(eq(7L), eq("IDENTITY_CHANGE"), contains("merchant -> buyer"), isNull());
    }

    @Test
    void deleteByAdminRequiresReasonAndRejectsRepeatedDelete() {
        when(merchantMapper.selectById(10L)).thenReturn(merchant(10L, 1));
        assertThrows(BusinessException.class, () -> service.deleteByAdmin(10L, "", OPERATOR, request));

        when(merchantMapper.selectById(11L)).thenReturn(merchant(11L, 3));
        assertThrows(BusinessException.class, () -> service.deleteByAdmin(11L, "重复注销", OPERATOR, request));
        verify(merchantMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void deleteByAdminKeepsBuyerRoleUntouched() {
        when(merchantMapper.selectById(12L)).thenReturn(merchant(12L, 1));
        User u = new User();
        u.setUserId(7L);
        u.setRoleId(1L); // 已是买家（如审核未通过）
        when(userMapper.selectById(7L)).thenReturn(u);

        service.deleteByAdmin(12L, "清理无效档案", OPERATOR, request);

        verify(userMapper, never()).updateById(any(User.class));
        verify(merchantMapper).deleteById(12L);
    }

    // ===== 权限配置 =====

    @Test
    void configPermsPersistsWhitelistAndAudits() {
        when(merchantMapper.selectById(13L)).thenReturn(merchant(13L, 1));

        service.configPerms(13L, List.of("product:publish"), OPERATOR, request);

        ArgumentCaptor<Merchant> mc = ArgumentCaptor.forClass(Merchant.class);
        verify(merchantMapper).updateById(mc.capture());
        assertEquals("product:publish", mc.getValue().getPermCodes());
        verify(auditService).record(eq(OPERATOR), eq("MERCHANT_PERM_CHANGE"), contains("product:publish"), eq(request));
        verify(reviewLogMapper).insert(any(MerchantReviewLog.class));
    }

    @Test
    void configPermsAllowsRevokingEverythingWithEmptyList() {
        when(merchantMapper.selectById(14L)).thenReturn(merchant(14L, 1));

        service.configPerms(14L, List.of(), OPERATOR, request);

        ArgumentCaptor<Merchant> mc = ArgumentCaptor.forClass(Merchant.class);
        verify(merchantMapper).updateById(mc.capture());
        assertEquals("", mc.getValue().getPermCodes(), "空列表应落库为空串（显式回收全部，区别于 null 未配置）");
    }

    @Test
    void configPermsRejectsPermissionOutsideWhitelist() {
        when(merchantMapper.selectById(15L)).thenReturn(merchant(15L, 1));

        assertThrows(BusinessException.class,
                () -> service.configPerms(15L, List.of("admin:all"), OPERATOR, request));
        assertThrows(BusinessException.class,
                () -> service.configPerms(15L, List.of("credit:repay"), OPERATOR, request));
        verify(merchantMapper, never()).updateById(any(Merchant.class));
    }

    @Test
    void permConfigReturnsWhitelistAndDefaultGrant() {
        when(merchantMapper.selectById(16L)).thenReturn(merchant(16L, 1));

        Map<String, Object> cfg = service.permConfig(16L);

        assertEquals(false, cfg.get("configured"));
        assertEquals(1, cfg.get("status"));
        assertNotNull(cfg.get("optionalPerms"));
        assertEquals(UserIdentity.MERCHANT_OPTIONAL_PERMS, cfg.get("grantedPerms"),
                "未配置时应默认授予全部可选权限（兼容存量商户）");
    }

    @Test
    void permConfigReflectsExplicitConfiguration() {
        Merchant m = merchant(17L, 1);
        m.setPermCodes("product:publish");
        when(merchantMapper.selectById(17L)).thenReturn(m);

        Map<String, Object> cfg = service.permConfig(17L);

        assertEquals(true, cfg.get("configured"));
        assertEquals(List.of("product:publish"), cfg.get("grantedPerms"));
    }

    // ===== 有效权限解析（权限判定链路）=====

    @Test
    void effectiveOptionalPermsDefaultsToAllWhenUnconfigured() {
        Merchant m = merchant(18L, 1);
        m.setPermCodes(null);
        when(merchantMapper.selectOne(any())).thenReturn(m);

        assertEquals(Set.copyOf(UserIdentity.MERCHANT_OPTIONAL_PERMS), service.effectiveOptionalPerms(7L));
    }

    @Test
    void effectiveOptionalPermsIsEmptyWhenFrozenOrCancelled() {
        Merchant frozen = merchant(19L, 2);
        when(merchantMapper.selectOne(any())).thenReturn(frozen);
        assertTrue(service.effectiveOptionalPerms(7L).isEmpty(), "冻结账户应为空集，即时收回经营权限");

        Merchant cancelled = merchant(20L, 3);
        when(merchantMapper.selectOne(any())).thenReturn(cancelled);
        assertTrue(service.effectiveOptionalPerms(7L).isEmpty());
    }

    @Test
    void effectiveOptionalPermsHonorsExplicitConfigAndIgnoresUnknownCodes() {
        Merchant m = merchant(21L, 1);
        m.setPermCodes("product:publish,admin:all,");
        when(merchantMapper.selectOne(any())).thenReturn(m);

        assertEquals(Set.of("product:publish"), service.effectiveOptionalPerms(7L),
                "白名单外的权限点必须被忽略，防止越权");
    }

    @Test
    void effectiveOptionalPermsIsEmptyWhenNoMerchantProfile() {
        when(merchantMapper.selectOne(any())).thenReturn(null);
        assertTrue(service.effectiveOptionalPerms(7L).isEmpty());
        assertTrue(service.effectiveOptionalPerms(null).isEmpty());
    }

    // ===== 敏感信息查看与出口脱敏 =====

    @Test
    void viewTaxRegNoDecryptsAndWritesAuditLog() {
        Merchant m = merchant(22L, 1);
        m.setTaxRegNo(cryptoUtil.encrypt(TAX_REG_NO));
        when(merchantMapper.selectById(22L)).thenReturn(m);

        Map<String, Object> out = service.viewTaxRegNo(22L, OPERATOR, request);

        assertEquals(TAX_REG_NO, out.get("taxRegNo"));
        assertEquals("915****00XA", out.get("masked"));
        verify(auditService).record(eq(OPERATOR), eq("MERCHANT_TAX_REG_VIEW"), contains("merId=22"), eq(request));
    }

    @Test
    void viewTaxRegNoRejectsWhenNotBound() {
        when(merchantMapper.selectById(23L)).thenReturn(merchant(23L, 1));
        assertThrows(BusinessException.class, () -> service.viewTaxRegNo(23L, OPERATOR, request));
    }

    @Test
    void listForAdminPageFiltersByStatusAndNumericKeyword() {
        Page<Merchant> page = new Page<>(1, 10);
        page.setRecords(List.of(merchant(40L, 2)));
        when(merchantMapper.selectPage(any(), any())).thenReturn(page);

        Page<Merchant> out = service.listForAdminPage(1, 10, null, 2, "40");

        assertEquals(1, out.getRecords().size());
        verify(enterpriseMapper, never()).selectList(any()); // 纯数字关键词按 ID 匹配，不查企业名
    }

    @Test
    void listForAdminPageFiltersByEnterpriseNameKeyword() {
        Enterprise ent = new Enterprise();
        ent.setEntId(50L);
        when(enterpriseMapper.selectList(any())).thenReturn(List.of(ent));
        Page<Merchant> page = new Page<>(1, 10);
        page.setRecords(List.of(merchant(41L, 1)));
        when(merchantMapper.selectPage(any(), any())).thenReturn(page);

        Page<Merchant> out = service.listForAdminPage(1, 10, null, null, "华晨");

        assertEquals(1, out.getRecords().size());
        verify(enterpriseMapper).selectList(any());
    }

    @Test
    void listForAdminPageReturnsEmptyPageWhenKeywordMatchesNoEnterprise() {
        when(enterpriseMapper.selectList(any())).thenReturn(List.of());

        Page<Merchant> out = service.listForAdminPage(1, 10, null, null, "不存在的企业名");

        assertTrue(out.getRecords().isEmpty());
        assertEquals(0, out.getTotal());
        verify(merchantMapper, never()).selectPage(any(), any()); // 不查库，避免退化为全表扫描
    }

    @Test
    void listForAdminPageMasksSensitiveField() {
        Merchant m = merchant(24L, 1);
        m.setTaxRegNo(cryptoUtil.encrypt(TAX_REG_NO));
        Page<Merchant> page = new Page<>(1, 10);
        page.setRecords(List.of(m));
        when(merchantMapper.selectPage(any(), any())).thenReturn(page);

        Page<Merchant> out = service.listForAdminPage(1, 10, null);

        assertEquals("915****00XA", out.getRecords().get(0).getTaxRegNo(), "列表出口必须脱敏");
    }

    @Test
    void getForDisplayMigratesLegacyPlaintextToCipher() {
        Merchant m = merchant(25L, 1);
        m.setTaxRegNo(TAX_REG_NO); // 历史明文
        when(merchantMapper.selectOne(any())).thenReturn(m);

        // 落库瞬间快照：返回前实体被出口脱敏（同一引用），须在调用时取值
        final String[] taxNoAtWrite = new String[1];
        doAnswer(inv -> {
            taxNoAtWrite[0] = ((Merchant) inv.getArgument(0)).getTaxRegNo();
            return 1;
        }).when(merchantMapper).updateById(any(Merchant.class));

        Merchant out = service.getForDisplay(7L);

        assertNotNull(taxNoAtWrite[0], "存量明文应在读取时就地重加密落库");
        assertTrue(cryptoUtil.isEncrypted(taxNoAtWrite[0]), "存量明文应在读取时就地重加密落库");
        assertEquals(TAX_REG_NO, cryptoUtil.decrypt(taxNoAtWrite[0]));
        assertEquals("915****00XA", out.getTaxRegNo(), "对外展示仍为脱敏值");
    }

    @Test
    void getForDisplayIsNoopForAlreadyEncryptedValue() {
        Merchant m = merchant(26L, 1);
        m.setTaxRegNo(cryptoUtil.encrypt(TAX_REG_NO));
        when(merchantMapper.selectOne(any())).thenReturn(m);

        service.getForDisplay(7L);

        verify(merchantMapper, never()).updateById(any(Merchant.class)); // 已加密则不再写库（迁移幂等）
    }

    @Test
    void updateByAdminRejectsMaskedPlaceholderValues() {
        // 前端若把脱敏展示值回填提交，必须被拒绝，避免掩码被当作真实证照号加密落库
        when(merchantMapper.selectById(28L)).thenReturn(merchant(28L, 1));

        MerchantUpdateDTO taxMasked = new MerchantUpdateDTO();
        taxMasked.setTaxRegNo("915****00XA");
        BusinessException e1 = assertThrows(BusinessException.class,
                () -> service.updateByAdmin(28L, taxMasked, OPERATOR, request));
        assertTrue(e1.getMessage().contains("脱敏"));

        MerchantUpdateDTO creditMasked = new MerchantUpdateDTO();
        creditMasked.setCreditCode("915****00XA");
        assertThrows(BusinessException.class, () -> service.updateByAdmin(28L, creditMasked, OPERATOR, request));

        verify(merchantMapper, never()).updateById(any(Merchant.class));
        verify(enterpriseMapper, never()).updateById(any(Enterprise.class));
    }

    @Test
    void updateByAdminAllowsClearingTaxRegNoWithEmptyString() {
        Merchant m = merchant(29L, 1);
        m.setTaxRegNo(cryptoUtil.encrypt(TAX_REG_NO));
        when(merchantMapper.selectById(29L)).thenReturn(m);

        MerchantUpdateDTO dto = new MerchantUpdateDTO();
        dto.setTaxRegNo("   "); // 空白 → 视为清空绑定
        service.updateByAdmin(29L, dto, OPERATOR, request);

        ArgumentCaptor<Merchant> mc = ArgumentCaptor.forClass(Merchant.class);
        verify(merchantMapper).updateById(mc.capture());
        assertNull(mc.getValue().getTaxRegNo(), "空值应清空绑定而不是写入空串");
    }

    @Test
    void persistTaxRegNoRejectsMaskedValue() {
        when(merchantMapper.selectOne(any())).thenReturn(merchant(30L, 1));
        assertThrows(BusinessException.class, () -> service.persistTaxRegNo(7L, "915****00XA"));
        verify(merchantMapper, never()).updateById(any(Merchant.class));
    }

    @Test
    void reviewExitIsSanitized() {
        Merchant m = merchant(31L, 1);
        m.setReviewStatus(0);
        m.setTaxRegNo(cryptoUtil.encrypt(TAX_REG_NO));
        when(merchantMapper.selectById(31L)).thenReturn(m);

        Merchant out = service.review(31L, 1, null, OPERATOR);

        assertEquals("915****00XA", out.getTaxRegNo(), "审核接口出口也必须脱敏，避免密文回传前端");
    }

    @Test
    void changeStatusToCancelledExitIsSanitized() {
        Merchant m = merchant(32L, 1);
        m.setTaxRegNo(cryptoUtil.encrypt(TAX_REG_NO));
        when(merchantMapper.selectById(32L)).thenReturn(m);

        Merchant out = service.changeStatus(32L, 3, "商户主动退出", OPERATOR, request);

        assertEquals(3, out.getStatus());
        assertEquals("915****00XA", out.getTaxRegNo(), "注销分支出口也必须脱敏");
    }

    @Test
    void adminDetailMasksMerchantAndEnterpriseSensitiveFields() {
        Merchant m = merchant(27L, 1);
        m.setTaxRegNo(cryptoUtil.encrypt(TAX_REG_NO));
        Enterprise ent = new Enterprise();
        ent.setEntId(50L);
        ent.setCreditCode(cryptoUtil.encrypt(CREDIT_CODE));
        when(merchantMapper.selectById(27L)).thenReturn(m);
        when(enterpriseMapper.selectById(50L)).thenReturn(ent);
        when(qualificationMapper.selectList(any())).thenReturn(List.of());

        Map<String, Object> detail = service.adminDetail(27L);

        assertEquals("915****00XA", ((Merchant) detail.get("merchant")).getTaxRegNo());
        assertEquals("915****00XA", ((Enterprise) detail.get("enterprise")).getCreditCode());
    }
}
