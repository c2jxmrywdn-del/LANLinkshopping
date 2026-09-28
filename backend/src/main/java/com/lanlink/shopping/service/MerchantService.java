package com.lanlink.shopping.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lanlink.shopping.common.BusinessException;
import com.lanlink.shopping.dto.MerchantApplyDTO;
import com.lanlink.shopping.entity.Enterprise;
import com.lanlink.shopping.entity.Merchant;
import com.lanlink.shopping.entity.Qualification;
import com.lanlink.shopping.mapper.EnterpriseMapper;
import com.lanlink.shopping.mapper.MerchantMapper;
import com.lanlink.shopping.mapper.QualificationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商户入驻与筛选服务
 * 筛选机制(对应思维导图):
 *   条件A 工商主体: 公司类型注册, 或 个体工商户且注册资本 > 10万元
 *   条件B 纳税记录: 有稳定纳税记录(taxStatus=1)
 *   A 且 B 满足 -> 审核通过(1); 否则驳回(2)并给出原因
 */
@Service
public class MerchantService {

    private static final BigDecimal MIN_CAPITAL = new BigDecimal("100000"); // 10万元

    private final MerchantMapper merchantMapper;
    private final EnterpriseMapper enterpriseMapper;
    private final QualificationMapper qualificationMapper;

    public MerchantService(MerchantMapper merchantMapper, EnterpriseMapper enterpriseMapper,
                           QualificationMapper qualificationMapper) {
        this.merchantMapper = merchantMapper;
        this.enterpriseMapper = enterpriseMapper;
        this.qualificationMapper = qualificationMapper;
    }

    @Transactional
    public Merchant apply(Long userId, Long entId, MerchantApplyDTO dto) {
        // 已有商户记录则不允许重复入驻
        if (merchantMapper.selectCount(Wrappers.<Merchant>lambdaQuery().eq(Merchant::getUserId, userId)) > 0) {
            throw new BusinessException("您已提交过入驻申请");
        }
        // 创建企业
        Enterprise ent = new Enterprise();
        ent.setName(dto.getEntName());
        ent.setCreditCode(dto.getCreditCode());
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
        return m;
    }

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

    /** 运营端: 分页查询待审/全部商户 */
    public java.util.List<Merchant> listForAdmin(Integer reviewStatus) {
        return merchantMapper.selectList(Wrappers.<Merchant>lambdaQuery()
                .eq(reviewStatus != null, Merchant::getReviewStatus, reviewStatus)
                .orderByDesc(Merchant::getCreateTime));
    }

    /** 运营端: 人工审核(通过/驳回) */
    public Merchant review(Long merId, Integer reviewStatus, String reason) {
        Merchant m = merchantMapper.selectById(merId);
        if (m == null) throw new BusinessException("商户不存在");
        m.setReviewStatus(reviewStatus);
        m.setRejectReason(reason);
        m.setUpdateTime(LocalDateTime.now());
        merchantMapper.updateById(m);
        return m;
    }
}
