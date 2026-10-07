package com.lanlink.shopping.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lanlink.shopping.common.BusinessException;
import com.lanlink.shopping.common.CryptoUtil;
import com.lanlink.shopping.common.UserIdentity;
import com.lanlink.shopping.dto.MerchantApplyDTO;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 商户入驻与筛选服务
 * 筛选机制(对应思维导图):
 *   条件A 工商主体: 公司类型注册, 或 个体工商户且注册资本 > 10万元
 *   条件B 纳税记录: 有稳定纳税记录(taxStatus=1)
 *   A 且 B 满足 -> 审核通过(1); 否则驳回(2)并给出原因
 * 全流程节点写入 t_merchant_review_log（提交/重新提交/通过/驳回），支撑入驻流程跟踪。
 */
@Service
public class MerchantService {

    private static final BigDecimal MIN_CAPITAL = new BigDecimal("100000"); // 10万元

    private final MerchantMapper merchantMapper;
    private final EnterpriseMapper enterpriseMapper;
    private final QualificationMapper qualificationMapper;
    private final AuditService auditService;
    private final UserMapper userMapper;
    private final MerchantReviewLogMapper reviewLogMapper;
    private final CryptoUtil cryptoUtil;

    public MerchantService(MerchantMapper merchantMapper, EnterpriseMapper enterpriseMapper,
                           QualificationMapper qualificationMapper, AuditService auditService,
                           UserMapper userMapper, MerchantReviewLogMapper reviewLogMapper,
                           CryptoUtil cryptoUtil) {
        this.merchantMapper = merchantMapper;
        this.enterpriseMapper = enterpriseMapper;
        this.qualificationMapper = qualificationMapper;
        this.auditService = auditService;
        this.userMapper = userMapper;
        this.reviewLogMapper = reviewLogMapper;
        this.cryptoUtil = cryptoUtil;
    }

    public MerchantService(MerchantMapper merchantMapper, EnterpriseMapper enterpriseMapper,
                           QualificationMapper qualificationMapper, AuditService auditService) {
        this(merchantMapper, enterpriseMapper, qualificationMapper, auditService, null, null, CryptoUtil.devDefault());
    }

    @Transactional
    public Merchant apply(Long userId, Long entId, MerchantApplyDTO dto) {
        // 已有商户记录则不允许重复入驻
        if (merchantMapper.selectCount(Wrappers.<Merchant>lambdaQuery().eq(Merchant::getUserId, userId)) > 0) {
            throw new BusinessException("您已提交过入驻申请");
        }
        // 说明：营业执照/税务证明等证照材料由入驻审核通过后的商户，
        // 通过受 merchant:manage 权限保护的 /merchant/license、/merchant/tax-proof、/merchant/tax-query 补传与绑定；
        // 申请阶段以 taxStatus 申报驱动筛选，不再强制前置上传。
        // 创建企业（统一社会信用代码加密存储）
        Enterprise ent = new Enterprise();
        ent.setName(dto.getEntName());
        ent.setCreditCode(encryptSensitive(dto.getCreditCode(), "统一社会信用代码"));
        ent.setMemberLevel(1);
        ent.setCreateTime(LocalDateTime.now());
        ent.setUpdateTime(LocalDateTime.now());
        enterpriseMapper.insert(ent);

        // 创建商户并执行筛选
        Merchant m = new Merchant();
        m.setEntId(ent.getEntId());
        m.setUserId(userId);
        m.setRegType(dto.getRegType());
        m.setRegCapital(dto.getRegCapital());
        m.setTaxStatus(dto.getTaxStatus());
        m.setJoinType(dto.getJoinType());
        m.setLicenseUrl(dto.getLicenseUrl());
        if (dto.getTaxProofUrls() != null && !dto.getTaxProofUrls().isEmpty()) {
            if (dto.getTaxProofUrls().size() > 3) {
                throw new BusinessException("纳税记录最多上传 3 份");
            }
            m.setTaxProofUrls(String.join(",", dto.getTaxProofUrls()));
        }
        if (StringUtils.hasText(dto.getTaxRegNo())) {
            m.setTaxRegNo(encryptSensitive(dto.getTaxRegNo(), "税务登记号"));
        }
        applyScreening(m);
        m.setCreateTime(LocalDateTime.now());
        m.setUpdateTime(LocalDateTime.now());
        merchantMapper.insert(m);

        // 记录已具备资质(可选)
        if (StringUtils.hasText(dto.getQNames())) {
            for (String q : dto.getQNames().split(",")) {
                if (!StringUtils.hasText(q)) continue;
                Qualification qe = new Qualification();
                qe.setMerId(m.getMerId());
                qe.setQName(q.trim());
                qe.setQType("行业资质");
                qe.setCreateTime(LocalDateTime.now());
                qualificationMapper.insert(qe);
            }
        }

        // 若初筛直接通过，同步更新用户角色为商户(2)并关联企业
        if (m.getReviewStatus() != null && m.getReviewStatus() == 1 && userId != null && userMapper != null) {
            User u = userMapper.selectById(userId);
            if (u != null) {
                u.setRoleId(2L);
                u.setEntId(ent.getEntId());
                u.setUpdateTime(LocalDateTime.now());
                userMapper.updateById(u);
            }
            auditService.record(userId, "IDENTITY_CHANGE", "身份升级: buyer -> merchant（商户初筛通过）", null);
        }
        // 审核历史：提交申请 + 初筛结果
        writeLog(m.getMerId(), "submit", userId, "提交入驻申请");
        writeLog(m.getMerId(), m.getReviewStatus() == 1 ? "approve" : "reject", userId,
                m.getReviewStatus() == 1 ? "自动初筛通过" : ("自动初筛驳回：" + nullToEmpty(m.getRejectReason())));
        return m;
    }

    /**
     * 驳回后重新申请：复用原商户记录（更新企业/商户资料），重跑筛选引擎并记录历史。
     * 仅 review_status=2（已驳回）的申请可重新提交；若重新初筛通过则同步升级商户身份。
     */
    @Transactional
    public Merchant reapply(Long userId, MerchantApplyDTO dto) {
        Merchant m = getByUser(userId);
        if (m == null) throw new BusinessException("尚未提交过入驻申请");
        if (m.getReviewStatus() == null || m.getReviewStatus() != 2) {
            throw new BusinessException("仅被驳回的申请可重新提交");
        }
        if (dto.getTaxProofUrls() != null && dto.getTaxProofUrls().size() > 3) {
            throw new BusinessException("纳税记录最多上传 3 份");
        }
        // 更新企业工商信息
        if (m.getEntId() != null) {
            Enterprise ent = enterpriseMapper.selectById(m.getEntId());
            if (ent != null) {
                ent.setName(dto.getEntName());
                ent.setCreditCode(encryptSensitive(dto.getCreditCode(), "统一社会信用代码"));
                ent.setUpdateTime(LocalDateTime.now());
                enterpriseMapper.updateById(ent);
            }
        }
        // 更新商户资料并重新筛选
        m.setRegType(dto.getRegType());
        m.setRegCapital(dto.getRegCapital());
        m.setTaxStatus(dto.getTaxStatus());
        m.setJoinType(dto.getJoinType());
        if (StringUtils.hasText(dto.getLicenseUrl())) m.setLicenseUrl(dto.getLicenseUrl());
        if (dto.getTaxProofUrls() != null && !dto.getTaxProofUrls().isEmpty()) {
            m.setTaxProofUrls(String.join(",", dto.getTaxProofUrls()));
        }
        if (StringUtils.hasText(dto.getTaxRegNo())) m.setTaxRegNo(encryptSensitive(dto.getTaxRegNo(), "税务登记号"));
        applyScreening(m);
        m.setUpdateTime(LocalDateTime.now());
        merchantMapper.updateById(m);

        // 更新已具备资质（清空重写）
        if (StringUtils.hasText(dto.getQNames())) {
            qualificationMapper.delete(Wrappers.<Qualification>lambdaQuery().eq(Qualification::getMerId, m.getMerId()));
            for (String q : dto.getQNames().split(",")) {
                if (!StringUtils.hasText(q)) continue;
                Qualification qe = new Qualification();
                qe.setMerId(m.getMerId());
                qe.setQName(q.trim());
                qe.setQType("行业资质");
                qe.setCreateTime(LocalDateTime.now());
                qualificationMapper.insert(qe);
            }
        }

        // 审核历史：重新提交 + 重筛结果
        writeLog(m.getMerId(), "resubmit", userId, "重新提交入驻申请");
        writeLog(m.getMerId(), m.getReviewStatus() == 1 ? "approve" : "reject", userId,
                m.getReviewStatus() == 1 ? "重新初筛通过" : ("重新初筛驳回：" + nullToEmpty(m.getRejectReason())));

        // 重筛通过 → 升级商户身份（与首次申请一致）
        if (m.getReviewStatus() == 1 && userId != null && userMapper != null) {
            User u = userMapper.selectById(userId);
            if (u != null) {
                u.setRoleId(2L);
                if (m.getEntId() != null) u.setEntId(m.getEntId());
                u.setUpdateTime(LocalDateTime.now());
                userMapper.updateById(u);
            }
            auditService.record(userId, "IDENTITY_CHANGE", "身份升级: buyer -> merchant（重新申请初筛通过）", null);
        } else if (m.getReviewStatus() == 2 && userId != null && userMapper != null) {
            // 重筛驳回 → 若此前为商户角色则降级
            User u = userMapper.selectById(userId);
            if (u != null && Long.valueOf(2L).equals(u.getRoleId())) {
                u.setRoleId(1L);
                u.setUpdateTime(LocalDateTime.now());
                userMapper.updateById(u);
                auditService.record(userId, "IDENTITY_CHANGE", "身份降级: merchant -> buyer（重新申请驳回）", null);
            }
        }
        return m;
    }

    private void writeLog(Long merId, String action, Long operatorId, String reason) {
        if (reviewLogMapper == null || merId == null) return;
        MerchantReviewLog l = new MerchantReviewLog();
        l.setMerId(merId);
        l.setAction(action);
        l.setOperatorId(operatorId);
        l.setReason(reason);
        l.setCreateTime(LocalDateTime.now());
        reviewLogMapper.insert(l);
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }

    /** 筛选规则引擎 */
    private void applyScreening(Merchant m) {
        boolean regOk = "公司".equals(m.getRegType())
                || ("个体工商户".equals(m.getRegType())
                    && m.getRegCapital() != null && m.getRegCapital().compareTo(MIN_CAPITAL) > 0);
        boolean taxOk = m.getTaxStatus() != null && m.getTaxStatus() == 1;
        if (regOk && taxOk) {
            m.setReviewStatus(1);
            m.setRejectReason(null);
        } else {
            m.setReviewStatus(2);
            StringBuilder reason = new StringBuilder();
            if (!regOk) {
                reason.append("注册资本或主体类型不符(需公司,或个体工商户注册资本>10万元); ");
            }
            if (!taxOk) {
                reason.append("缺少稳定纳税记录; ");
            }
            m.setRejectReason(reason.toString());
        }
    }

    public Merchant getByUser(Long userId) {
        return merchantMapper.selectOne(Wrappers.<Merchant>lambdaQuery().eq(Merchant::getUserId, userId));
    }

    /** 我的商户信息（出口脱敏 + 存量明文读侧迁移） */
    public Merchant getForDisplay(Long userId) {
        Merchant m = getByUser(userId);
        if (m == null) return null;
        migrateLegacyCrypto(m);
        sanitizeForDisplay(m);
        return m;
    }

    /** 出口脱敏：税务登记号解密后按前3后4脱敏展示，完整明文仅管理端授权接口按需返回 */
    private void sanitizeForDisplay(Merchant m) {
        if (m != null && StringUtils.hasText(m.getTaxRegNo())) {
            m.setTaxRegNo(CryptoUtil.mask(cryptoUtil.decrypt(m.getTaxRegNo())));
        }
    }

    /** 存量明文读侧迁移：读到未加密历史数据时就地重加密落库，后续读取即为密文（幂等，仅执行一次） */
    private void migrateLegacyCrypto(Merchant m) {
        if (m == null || m.getMerId() == null) return;
        if (StringUtils.hasText(m.getTaxRegNo()) && !cryptoUtil.isEncrypted(m.getTaxRegNo())) {
            m.setTaxRegNo(cryptoUtil.encrypt(m.getTaxRegNo()));
            m.setUpdateTime(LocalDateTime.now());
            merchantMapper.updateById(m);
        }
    }

    /**
     * 敏感字段写入统一入口：归一化（去空格/转大写）后加密。
     * 防护：拒绝含 **** 的脱敏占位值 —— 避免任何客户端把列表/详情返回的脱敏值原样回填提交，
     * 导致掩码被当作真实证照号加密落库（数据污染且不可逆）。
     */
    private String encryptSensitive(String raw, String fieldLabel) {
        if (raw == null) return null;
        String v = raw.trim();
        if (v.isEmpty()) return null;
        if (CryptoUtil.isMasked(v)) {
            throw new BusinessException(fieldLabel + "不能提交脱敏占位值，请填写完整内容");
        }
        return cryptoUtil.encrypt(v.toUpperCase());
    }

    /** 商户档案必须存在（证照管理仅限已入驻商户） */
    private Merchant mustMerchant(Long userId) {
        Merchant m = getByUser(userId);
        if (m == null) throw new BusinessException("请先完成入驻审核后再管理证照");
        return m;
    }

    /** 营业执照 URL 写入商户档案 */
    public void persistLicense(Long userId, String url) {
        Merchant m = mustMerchant(userId);
        m.setLicenseUrl(url);
        m.setUpdateTime(java.time.LocalDateTime.now());
        merchantMapper.updateById(m);
    }

    /** 税务证明 URL 追加写入商户档案（逗号分隔，最多 3 份） */
    public void persistTaxProof(Long userId, String url) {
        Merchant m = mustMerchant(userId);
        java.util.List<String> urls = new java.util.ArrayList<>();
        if (StringUtils.hasText(m.getTaxProofUrls())) {
            for (String u : m.getTaxProofUrls().split(",")) {
                if (StringUtils.hasText(u)) urls.add(u.trim());
            }
        }
        if (urls.contains(url)) return;
        urls.add(url);
        if (urls.size() > 3) urls = urls.subList(urls.size() - 3, urls.size());
        m.setTaxProofUrls(String.join(",", urls));
        m.setUpdateTime(java.time.LocalDateTime.now());
        merchantMapper.updateById(m);
    }

    /** 税务登记号绑定至商户档案（加密存储 + 掩码占位值防护） */
    public void persistTaxRegNo(Long userId, String taxRegNo) {
        Merchant m = mustMerchant(userId);
        m.setTaxRegNo(encryptSensitive(taxRegNo, "税务登记号"));
        m.setUpdateTime(java.time.LocalDateTime.now());
        merchantMapper.updateById(m);
    }

    /** 税务登记号格式：15/18/20 位数字或大写字母 */
    private static final java.util.regex.Pattern TAX_REG_NO =
            java.util.regex.Pattern.compile("^[0-9A-Z]{15}$|^[0-9A-Z]{18}$|^[0-9A-Z]{20}$");

    /**
     * 通过税务登记号查询近 3 个月税务缴纳记录。
     * 演示环境：无真实税务数据源，基于登记号哈希稳定生成可复现的记录
     * （同一登记号多次查询结果一致），用于入驻流程的纳税记录核验演示。
     */
    public java.util.Map<String, Object> queryTaxRecords(String taxRegNo) {
        String no = taxRegNo == null ? "" : taxRegNo.trim().toUpperCase();
        if (!TAX_REG_NO.matcher(no).matches()) {
            throw new BusinessException("税务登记号格式不正确（应为 15/18/20 位数字或大写字母）");
        }
        java.util.Random r = new java.util.Random((long) no.hashCode() * 31 + no.length());
        java.util.List<java.util.Map<String, Object>> records = new java.util.ArrayList<>();
        java.time.LocalDate now = java.time.LocalDate.now();
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        for (int i = 3; i >= 1; i--) {
            java.time.LocalDate month = now.minusMonths(i);
            java.math.BigDecimal amount = java.math.BigDecimal.valueOf(3000 + r.nextInt(47000)).setScale(2);
            total = total.add(amount);
            java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("month", month.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")));
            row.put("taxType", "增值税");
            row.put("amount", amount);
            row.put("paidAt", month.withDayOfMonth(10 + r.nextInt(18)).toString());
            records.add(row);
        }
        java.util.Map<String, Object> out = new java.util.LinkedHashMap<>();
        out.put("taxRegNo", no);
        out.put("records", records);
        out.put("totalAmount", total);
        out.put("stable", true);
        return out;
    }

    /** 运营端: 分页查询待审/全部商户（兼容入口，敏感字段脱敏出口） */
    public Page<Merchant> listForAdminPage(long page, long size, Integer reviewStatus) {
        return listForAdminPage(page, size, reviewStatus, null, null);
    }

    /**
     * 运营端: 分页查询（审核状态 / 账户状态 / 关键词筛选，敏感字段脱敏出口）。
     * 关键词：纯数字按「商户ID 或 企业ID」精确匹配；否则按企业名称模糊匹配
     * （先查企业ID集合再 in，命中集合有限，避免 join 与全表扫描；无匹配时返回空页而非退化全表）。
     */
    public Page<Merchant> listForAdminPage(long page, long size, Integer reviewStatus, Integer status, String keyword) {
        String kw = keyword == null ? "" : keyword.trim();
        Long kwId = kw.matches("\\d{1,19}") ? Long.valueOf(kw) : null;

        // 企业名称关键词：先解析出候选企业ID，再按 ent_id 集合过滤
        if (!kw.isEmpty() && kwId == null) {
            List<Long> entIds = enterpriseMapper.selectList(Wrappers.<Enterprise>lambdaQuery()
                            .select(Enterprise::getEntId)
                            .like(Enterprise::getName, kw))
                    .stream().map(Enterprise::getEntId).toList();
            if (entIds.isEmpty()) {
                // 关键词没有任何匹配企业：直接返回空页，避免退化为全表扫描
                Page<Merchant> empty = new Page<>(page, size);
                empty.setRecords(List.of());
                empty.setTotal(0);
                return empty;
            }
            return queryMaskedPage(page, size, Wrappers.<Merchant>lambdaQuery()
                    .eq(reviewStatus != null, Merchant::getReviewStatus, reviewStatus)
                    .eq(status != null, Merchant::getStatus, status)
                    .in(Merchant::getEntId, entIds)
                    .orderByDesc(Merchant::getCreateTime));
        }

        LambdaQueryWrapper<Merchant> w = Wrappers.<Merchant>lambdaQuery()
                .eq(reviewStatus != null, Merchant::getReviewStatus, reviewStatus)
                .eq(status != null, Merchant::getStatus, status)
                .orderByDesc(Merchant::getCreateTime);
        if (kwId != null) {
            // 纯数字关键词：商户ID 或 企业ID 精确匹配
            w.and(x -> x.eq(Merchant::getMerId, kwId).or().eq(Merchant::getEntId, kwId));
        }
        return queryMaskedPage(page, size, w);
    }

    /** 执行分页查询并统一做出口脱敏 */
    private Page<Merchant> queryMaskedPage(long page, long size, LambdaQueryWrapper<Merchant> wrapper) {
        Page<Merchant> p = merchantMapper.selectPage(new Page<>(page, size), wrapper);
        p.getRecords().forEach(this::sanitizeForDisplay);
        return p;
    }

    /** 运营端: 全量列表（数据概览统计等内部用途，敏感字段脱敏出口） */
    public java.util.List<Merchant> listForAdmin(Integer reviewStatus) {
        java.util.List<Merchant> list = merchantMapper.selectList(Wrappers.<Merchant>lambdaQuery()
                .eq(reviewStatus != null, Merchant::getReviewStatus, reviewStatus)
                .orderByDesc(Merchant::getCreateTime));
        list.forEach(this::sanitizeForDisplay);
        return list;
    }

    /** 入驻流程跟踪：某商户的审核历史（按时间倒序） */
    public java.util.List<MerchantReviewLog> reviewLogs(Long merId) {
        return reviewLogMapper.selectList(Wrappers.<MerchantReviewLog>lambdaQuery()
                .eq(MerchantReviewLog::getMerId, merId)
                .orderByDesc(MerchantReviewLog::getId)
                .last("LIMIT 50"));
    }

    /**
     * 运营端: 商户详情（资料维护/资质核验视图）。
     * 聚合商户档案、企业工商信息、已具备资质清单，并计算资质材料完整度。
     */
    public java.util.Map<String, Object> adminDetail(Long merId) {
        Merchant m = merchantMapper.selectById(merId);
        if (m == null) throw new BusinessException("商户不存在");
        Enterprise ent = m.getEntId() == null ? null : enterpriseMapper.selectById(m.getEntId());
        // 敏感字段出口脱敏：税务登记号（含存量明文读侧迁移）+ 企业统一社会信用代码
        migrateLegacyCrypto(m);
        sanitizeForDisplay(m);
        if (ent != null && StringUtils.hasText(ent.getCreditCode())) {
            ent.setCreditCode(CryptoUtil.mask(cryptoUtil.decrypt(ent.getCreditCode())));
        }
        java.util.List<Qualification> quals = qualificationMapper.selectList(
                Wrappers.<Qualification>lambdaQuery().eq(Qualification::getMerId, merId));
        int taxProofCount = StringUtils.hasText(m.getTaxProofUrls()) ? m.getTaxProofUrls().split(",").length : 0;
        java.util.Map<String, Object> readiness = new java.util.LinkedHashMap<>();
        readiness.put("hasLicense", StringUtils.hasText(m.getLicenseUrl()));
        readiness.put("taxProofCount", taxProofCount);
        readiness.put("hasTaxRegNo", StringUtils.hasText(m.getTaxRegNo()));
        readiness.put("qualificationCount", quals.size());
        java.util.Map<String, Object> out = new java.util.LinkedHashMap<>();
        out.put("merchant", m);
        out.put("enterprise", ent);
        out.put("qualifications", quals);
        out.put("certReadiness", readiness);
        return out;
    }

    /** 运营端: 人工审核(通过/驳回)。驳回必须给出原因（供商户整改与流程跟踪） */
    public Merchant review(Long merId, Integer reviewStatus, String reason, Long operatorId) {
        Merchant m = merchantMapper.selectById(merId);
        if (m == null) throw new BusinessException("商户不存在");
        if (reviewStatus == null || reviewStatus < 1 || reviewStatus > 2) {
            throw new BusinessException("审核状态不合法");
        }
        if (reviewStatus == 2 && !StringUtils.hasText(reason)) {
            throw new BusinessException("驳回时必须填写原因");
        }
        m.setReviewStatus(reviewStatus);
        m.setRejectReason(reviewStatus == 2 ? reason.trim() : null);
        m.setUpdateTime(LocalDateTime.now());
        merchantMapper.updateById(m);
        // 审核历史：人工审核结果
        writeLog(merId, reviewStatus == 1 ? "approve" : "reject", operatorId,
                reviewStatus == 1 ? "人工审核通过" : ("人工审核驳回：" + reason.trim()));
        // 审核通过 → 商户身份生效（角色置为 2L 并关联企业；IdentityService 实时识别，下次请求即切换为 merchant）
        if (reviewStatus == 1 && m.getUserId() != null) {
            if (userMapper != null) {
                User u = userMapper.selectById(m.getUserId());
                if (u != null) {
                    u.setRoleId(2L);
                    if (m.getEntId() != null) {
                        u.setEntId(m.getEntId());
                    }
                    u.setUpdateTime(LocalDateTime.now());
                    userMapper.updateById(u);
                }
            }
            auditService.record(m.getUserId(), "IDENTITY_CHANGE", "身份升级: buyer -> merchant（商户审核通过）", null);
        } else if (reviewStatus == 2 && m.getUserId() != null) {
            // 审核驳回 → 若之前为商户角色则重置为普通买家(1L)
            if (userMapper != null) {
                User u = userMapper.selectById(m.getUserId());
                if (u != null && Long.valueOf(2L).equals(u.getRoleId())) {
                    u.setRoleId(1L);
                    u.setUpdateTime(LocalDateTime.now());
                    userMapper.updateById(u);
                    auditService.record(m.getUserId(), "IDENTITY_CHANGE", "身份降级: merchant -> buyer（商户审核驳回）", null);
                }
            }
        }
        sanitizeForDisplay(m); // 出口统一脱敏，避免密文回传前端
        return m;
    }

    // ==================== 运营端商户管理：增删改查 / 账户状态 / 权限配置 ====================

    /** 商户档案必须存在 */
    private Merchant mustExists(Long merId) {
        Merchant m = merId == null ? null : merchantMapper.selectById(merId);
        if (m == null) throw new BusinessException("商户不存在");
        return m;
    }

    /**
     * 商户授权权限解析（权限判定链路使用，见 PermInterceptor）：
     *  - 非商户 / 无档案 / 已注销（逻辑删除）→ 空集；
     *  - 账户冻结(2) → 空集（商户档可选权限即时失效，无需重登）；
     *  - perm_codes 为 null（未配置）→ 默认全量授予，保证存量商户不降权；
     *  - 否则按已配置清单返回（空串=显式回收全部可选权限）。
     */
    public Set<String> effectiveOptionalPerms(Long userId) {
        if (userId == null) return Set.of();
        Merchant m = getByUser(userId);
        if (m == null) return Set.of();
        if (m.getStatus() != null && m.getStatus() != 1) return Set.of();
        if (m.getPermCodes() == null) return new LinkedHashSet<>(UserIdentity.MERCHANT_OPTIONAL_PERMS);
        Set<String> granted = new LinkedHashSet<>();
        for (String c : m.getPermCodes().split(",")) {
            String v = c.trim();
            if (!v.isEmpty() && UserIdentity.MERCHANT_OPTIONAL_PERMS.contains(v)) granted.add(v);
        }
        return granted;
    }

    /** 运营端：修改商户基本资料（企业工商信息联动；敏感字段加密存储；全程审计） */
    @Transactional
    public Merchant updateByAdmin(Long merId, MerchantUpdateDTO dto, Long operatorId, HttpServletRequest request) {
        Merchant m = mustExists(merId);
        // 敏感字段前置校验并加密（快速失败，避免「商户已改、企业校验失败」的部分写入）
        // 空串表示清空绑定；含掩码占位值（****）一律拒绝
        String newTaxRegNo = dto.getTaxRegNo() == null ? null : encryptSensitive(dto.getTaxRegNo(), "税务登记号");
        String newCreditCode = dto.getCreditCode() == null ? null : encryptSensitive(dto.getCreditCode(), "统一社会信用代码");

        if (StringUtils.hasText(dto.getRegType())) {
            if (!"公司".equals(dto.getRegType()) && !"个体工商户".equals(dto.getRegType())) {
                throw new BusinessException("注册类型仅支持 公司/个体工商户");
            }
            m.setRegType(dto.getRegType());
        }
        if (dto.getRegCapital() != null) {
            if (dto.getRegCapital().compareTo(BigDecimal.ZERO) < 0) throw new BusinessException("注册资本不能为负数");
            m.setRegCapital(dto.getRegCapital());
        }
        if (dto.getTaxStatus() != null) m.setTaxStatus(dto.getTaxStatus());
        if (StringUtils.hasText(dto.getJoinType())) m.setJoinType(dto.getJoinType());
        if (dto.getTaxRegNo() != null) m.setTaxRegNo(newTaxRegNo);
        m.setUpdateTime(LocalDateTime.now());
        merchantMapper.updateById(m);

        // 企业工商信息联动更新
        boolean entChanged = StringUtils.hasText(dto.getEntName()) || dto.getCreditCode() != null;
        if (entChanged && m.getEntId() != null) {
            Enterprise ent = enterpriseMapper.selectById(m.getEntId());
            if (ent != null) {
                if (StringUtils.hasText(dto.getEntName())) ent.setName(dto.getEntName());
                if (dto.getCreditCode() != null) ent.setCreditCode(newCreditCode);
                ent.setUpdateTime(LocalDateTime.now());
                enterpriseMapper.updateById(ent);
            }
        }
        auditService.record(operatorId, "MERCHANT_UPDATE",
                "修改商户资料 merId=" + merId + " 变更项=" + changedFields(dto), request);
        sanitizeForDisplay(m);
        return m;
    }

    /** 变更项摘要（审计日志用，只记字段名不记敏感明文） */
    private static String changedFields(MerchantUpdateDTO dto) {
        List<String> fields = new ArrayList<>();
        if (StringUtils.hasText(dto.getEntName())) fields.add("企业名称");
        if (dto.getCreditCode() != null) fields.add("统一社会信用代码");
        if (StringUtils.hasText(dto.getRegType())) fields.add("注册类型");
        if (dto.getRegCapital() != null) fields.add("注册资本");
        if (dto.getTaxStatus() != null) fields.add("纳税记录");
        if (StringUtils.hasText(dto.getJoinType())) fields.add("入驻方式");
        if (dto.getTaxRegNo() != null) fields.add("税务登记号");
        return fields.isEmpty() ? "无" : String.join("/", fields);
    }

    /**
     * 运营端：账户状态变更（1正常 2冻结 3注销）。
     *  - 冻结/恢复：单行更新，权限判定链路即时生效（冻结商户立即失去商户档可选权限）；
     *  - 注销(3)：复用注销逻辑（逻辑删除 + 商户身份降级）。
     */
    @Transactional
    public Merchant changeStatus(Long merId, Integer status, String reason, Long operatorId, HttpServletRequest request) {
        if (status == null || status < 1 || status > 3) throw new BusinessException("账户状态不合法");
        if (!StringUtils.hasText(reason)) throw new BusinessException("状态变更必须填写原因");
        Merchant m = mustExists(merId);
        if (status == 3) {
            deleteByAdmin(merId, reason, operatorId, request);
            m.setStatus(3);
            sanitizeForDisplay(m); // 出口统一脱敏，避免密文回传前端
            return m;
        }
        int from = m.getStatus() == null ? 1 : m.getStatus();
        if (from == 3) throw new BusinessException("已注销商户不可再变更账户状态");
        if (from == status) throw new BusinessException("商户已处于该状态");
        m.setStatus(status);
        m.setUpdateTime(LocalDateTime.now());
        merchantMapper.updateById(m);
        String text = status == 2 ? "冻结" : "恢复正常";
        writeLog(merId, "status", operatorId, text + "账户：" + reason.trim());
        auditService.record(operatorId, "MERCHANT_STATUS_CHANGE",
                "商户账户" + text + " merId=" + merId + " 原因=" + reason.trim(), request);
        sanitizeForDisplay(m);
        return m;
    }

    /** 运营端：注销商户（账户状态置 3 + 逻辑删除 + 商户身份降级 + 审计） */
    @Transactional
    public void deleteByAdmin(Long merId, String reason, Long operatorId, HttpServletRequest request) {
        Merchant m = mustExists(merId);
        if (m.getStatus() != null && m.getStatus() == 3) throw new BusinessException("该商户已注销");
        if (!StringUtils.hasText(reason)) throw new BusinessException("注销必须填写原因");
        m.setStatus(3);
        m.setUpdateTime(LocalDateTime.now());
        merchantMapper.updateById(m);
        merchantMapper.deleteById(merId); // 逻辑删除（@TableLogic），数据保留可追溯
        if (m.getUserId() != null && userMapper != null) {
            User u = userMapper.selectById(m.getUserId());
            if (u != null && Long.valueOf(2L).equals(u.getRoleId())) {
                u.setRoleId(1L);
                u.setUpdateTime(LocalDateTime.now());
                userMapper.updateById(u);
                auditService.record(m.getUserId(), "IDENTITY_CHANGE", "身份降级: merchant -> buyer（商户注销）", null);
            }
        }
        writeLog(merId, "delete", operatorId, "商户注销：" + reason.trim());
        auditService.record(operatorId, "MERCHANT_DELETE", "注销商户 merId=" + merId + " 原因=" + reason.trim(), request);
    }

    /** 运营端：商户权限配置详情（白名单 + 当前授权，供管理端面板渲染） */
    public Map<String, Object> permConfig(Long merId) {
        Merchant m = mustExists(merId);
        List<String> granted = m.getPermCodes() == null
                ? new ArrayList<>(UserIdentity.MERCHANT_OPTIONAL_PERMS)
                : splitPerms(m.getPermCodes());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("merId", merId);
        out.put("status", m.getStatus() == null ? 1 : m.getStatus());
        out.put("configured", m.getPermCodes() != null);
        out.put("optionalPerms", UserIdentity.MERCHANT_OPTIONAL_PERMS);
        out.put("grantedPerms", granted);
        return out;
    }

    /** 运营端：商户权限配置（授予/回收商户档可选权限，白名单外权限点一律拒绝） */
    @Transactional
    public Merchant configPerms(Long merId, List<String> permCodes, Long operatorId, HttpServletRequest request) {
        Merchant m = mustExists(merId);
        Set<String> codes = new LinkedHashSet<>();
        if (permCodes != null) {
            for (String c : permCodes) {
                if (!StringUtils.hasText(c)) continue;
                String v = c.trim();
                if (!UserIdentity.MERCHANT_OPTIONAL_PERMS.contains(v)) {
                    throw new BusinessException("不支持的商户权限点：" + v);
                }
                codes.add(v);
            }
        }
        m.setPermCodes(String.join(",", codes)); // 空串=显式回收全部可选权限；null=未配置（默认全量）
        m.setUpdateTime(LocalDateTime.now());
        merchantMapper.updateById(m);
        writeLog(merId, "perm", operatorId, "调整商户权限：" + (codes.isEmpty() ? "全部回收" : String.join(",", codes)));
        auditService.record(operatorId, "MERCHANT_PERM_CHANGE",
                "商户权限配置 merId=" + merId + " 授权=" + m.getPermCodes(), request);
        sanitizeForDisplay(m);
        return m;
    }

    /** 运营端：解密查看完整税务登记号（敏感操作，必写审计日志） */
    public Map<String, Object> viewTaxRegNo(Long merId, Long operatorId, HttpServletRequest request) {
        Merchant m = mustExists(merId);
        if (!StringUtils.hasText(m.getTaxRegNo())) throw new BusinessException("该商户尚未绑定税务登记号");
        migrateLegacyCrypto(m);
        String plain = cryptoUtil.decrypt(m.getTaxRegNo());
        auditService.record(operatorId, "MERCHANT_TAX_REG_VIEW", "查看商户完整税务登记号 merId=" + merId, request);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("merId", merId);
        out.put("taxRegNo", plain);
        out.put("masked", CryptoUtil.mask(plain));
        return out;
    }

    /** 逗号分隔权限串 → 列表 */
    private static List<String> splitPerms(String s) {
        List<String> list = new ArrayList<>();
        if (!StringUtils.hasText(s)) return list;
        for (String c : s.split(",")) {
            String v = c.trim();
            if (!v.isEmpty()) list.add(v);
        }
        return list;
    }
}
