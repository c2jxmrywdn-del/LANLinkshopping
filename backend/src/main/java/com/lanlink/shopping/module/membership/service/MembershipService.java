package com.lanlink.shopping.module.membership.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lanlink.shopping.common.BusinessException;
import com.lanlink.shopping.module.membership.entity.MemberCard;
import com.lanlink.shopping.module.membership.entity.MemberLevel;
import com.lanlink.shopping.module.membership.entity.MemberPointLog;
import com.lanlink.shopping.module.membership.mapper.MemberCardMapper;
import com.lanlink.shopping.module.membership.mapper.MemberLevelMapper;
import com.lanlink.shopping.module.membership.mapper.MemberPointLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 会员系统服务（营销中台·独立模块）。
 * 对外能力：getOrCreateCard / earnGrowth / levelDiscountRate / 积分流水。
 * 成长值由订单支付事件（MembershipOrderListener）触发累计，模块间通过事件解耦。
 */
@Service
public class MembershipService {

    private final MemberCardMapper cardMapper;
    private final MemberLevelMapper levelMapper;
    private final MemberPointLogMapper pointLogMapper;

    public MembershipService(MemberCardMapper cardMapper, MemberLevelMapper levelMapper,
                             MemberPointLogMapper pointLogMapper) {
        this.cardMapper = cardMapper;
        this.levelMapper = levelMapper;
        this.pointLogMapper = pointLogMapper;
    }

    /** 获取或创建用户会员卡（user_id 唯一） */
    @Transactional
    public MemberCard getOrCreateCard(Long userId) {
        if (userId == null) throw new BusinessException("请先登录");
        MemberCard card = cardMapper.selectOne(Wrappers.<MemberCard>lambdaQuery()
                .eq(MemberCard::getUserId, userId));
        if (card == null) {
            MemberLevel base = levelMapper.selectOne(Wrappers.<MemberLevel>lambdaQuery()
                    .eq(MemberLevel::getMinGrowth, 0).orderByAsc(MemberLevel::getSort).last("LIMIT 1"));
            card = new MemberCard();
            card.setUserId(userId);
            card.setLevelId(base == null ? 1L : base.getLevelId());
            card.setGrowth(0);
            card.setPoints(0);
            card.setUpdateTime(LocalDateTime.now());
            cardMapper.insert(card);
        }
        return card;
    }

    /** 累计成长值 + 积分（订单支付后调用；幂等由业务方保证） */
    @Transactional
    public void earnGrowth(Long userId, BigDecimal orderAmount, String orderNo) {
        MemberCard card = getOrCreateCard(userId);
        int growth = orderAmount == null ? 0 : orderAmount.intValue();
        int points = orderAmount == null ? 0 : orderAmount.intValue();
        card.setGrowth(card.getGrowth() == null ? 0 : card.getGrowth() + growth);
        card.setPoints(card.getPoints() == null ? 0 : card.getPoints() + points);
        card.setLevelId(levelForGrowth(card.getGrowth()));
        card.setUpdateTime(LocalDateTime.now());
        cardMapper.updateById(card);

        MemberPointLog log = new MemberPointLog();
        log.setUserId(userId);
        log.setChangeType("earn_consume");
        log.setChangeVal(points);
        log.setRefOrderNo(orderNo);
        log.setRemark("订单消费获得积分");
        log.setCreateTime(LocalDateTime.now());
        pointLogMapper.insert(log);
    }

    /** 依据成长值匹配最高等级 */
    public Long levelForGrowth(int growth) {
        List<MemberLevel> levels = levelMapper.selectList(Wrappers.<MemberLevel>lambdaQuery()
                .orderByDesc(MemberLevel::getMinGrowth));
        for (MemberLevel l : levels) {
            if (growth >= l.getMinGrowth()) return l.getLevelId();
        }
        return levels.isEmpty() ? 1L : levels.get(levels.size() - 1).getLevelId();
    }

    /** 当前等级折扣率（默认 1.0 = 无折扣） */
    public BigDecimal levelDiscountRate(Long userId) {
        MemberCard card = getOrCreateCard(userId);
        MemberLevel level = levelMapper.selectById(card.getLevelId());
        return level == null || level.getDiscountRate() == null ? BigDecimal.ONE : level.getDiscountRate();
    }

    /** 我的会员视图（卡 + 等级 + 积分流水） */
    public Map<String, Object> myView(Long userId) {
        MemberCard card = getOrCreateCard(userId);
        MemberLevel level = levelMapper.selectById(card.getLevelId());
        List<MemberLevel> levels = levelMapper.selectList(Wrappers.<MemberLevel>lambdaQuery()
                .orderByAsc(MemberLevel::getSort));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("card", card);
        out.put("level", level);
        out.put("levels", levels);
        out.put("pointsLog", pointLogMapper.selectList(Wrappers.<MemberPointLog>lambdaQuery()
                .eq(MemberPointLog::getUserId, userId).orderByDesc(MemberPointLog::getLogId).last("LIMIT 20")));
        return out;
    }
}
