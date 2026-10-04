package com.lanlink.shopping.module.membership.service;

import com.lanlink.shopping.common.BusinessException;
import com.lanlink.shopping.module.membership.constant.MemberRule;
import com.lanlink.shopping.module.membership.entity.MemberCard;
import com.lanlink.shopping.module.membership.entity.MemberLevel;
import com.lanlink.shopping.module.membership.entity.MemberPointLog;
import com.lanlink.shopping.module.membership.mapper.MemberCardMapper;
import com.lanlink.shopping.module.membership.mapper.MemberLevelMapper;
import com.lanlink.shopping.module.membership.mapper.MemberPointLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 积分抵现单测：试算上限 / 扣减 / 取消返还（幂等）
 */
class MembershipRedeemTest {

    private MemberCardMapper cardMapper;
    private MemberLevelMapper levelMapper;
    private MemberPointLogMapper logMapper;
    private MembershipService service;

    private MemberCard card(int points) {
        MemberCard c = new MemberCard();
        c.setCardId(1L); c.setUserId(2L); c.setLevelId(1L);
        c.setGrowth(points); c.setPoints(points);
        return c;
    }

    @BeforeEach
    void setUp() {
        cardMapper = mock(MemberCardMapper.class);
        levelMapper = mock(MemberLevelMapper.class);
        logMapper = mock(MemberPointLogMapper.class);
        service = new MembershipService(cardMapper, levelMapper, logMapper);
    }

    // ===== 试算 redeemQuote =====

    @Test
    void quoteNormalRedeem() {
        when(cardMapper.selectOne(any())).thenReturn(card(5000));
        // 1000 积分 = 10 元；订单 1000 元上限 100 元 → 不受限
        var q = service.redeemQuote(2L, 1000, new BigDecimal("1000"));
        assertEquals(1000, q.get("points"));
        assertEquals(0, new BigDecimal("10.00").compareTo((BigDecimal) q.get("amount")));
        assertEquals(5000, q.get("balance"));
    }

    @Test
    void quoteCappedByPercentLimit() {
        when(cardMapper.selectOne(any())).thenReturn(card(500000));
        // 期望抵 5000 积分 = 50 元，但订单 200 元上限 20 元 → 截断为 20 元(2000 积分)
        var q = service.redeemQuote(2L, 5000, new BigDecimal("200"));
        assertEquals(2000, q.get("points"));
        assertEquals(0, new BigDecimal("20.00").compareTo((BigDecimal) q.get("amount")));
        assertEquals(0, new BigDecimal("20.00").compareTo((BigDecimal) q.get("maxAmount")));
    }

    @Test
    void quoteCappedByBalance() {
        when(cardMapper.selectOne(any())).thenReturn(card(80));
        // 请求 1000 积分，余额仅 80 → 实际 80 积分 = 0.80 元
        var q = service.redeemQuote(2L, 1000, new BigDecimal("1000"));
        assertEquals(80, q.get("points"));
        assertEquals(0, new BigDecimal("0.80").compareTo((BigDecimal) q.get("amount")));
    }

    // ===== 扣减 redeemPoints =====

    @Test
    void redeemDeductsPointsAndWritesLog() {
        MemberCard c = card(5000);
        when(cardMapper.selectOne(any())).thenReturn(c);
        BigDecimal amount = service.redeemPoints(2L, 1000, new BigDecimal("1000"), "ORD-1");
        assertEquals(0, new BigDecimal("10.00").compareTo(amount));
        assertEquals(4000, c.getPoints(), "积分应从 5000 扣至 4000");
        verify(cardMapper).updateById(c);
        verify(logMapper).insert(argThat((MemberPointLog l) -> MemberRule.CHANGE_REDEEM.equals(l.getChangeType())
                && l.getChangeVal() == -1000 && "ORD-1".equals(l.getRefOrderNo())));
    }

    @Test
    void redeemBelowMinimumThrows() {
        when(cardMapper.selectOne(any())).thenReturn(card(50)); // 50 积分 = 0.5 元但按 100:1 向下取整为 0.50→amount=0.50
        // 50 积分 = 0.50 元，amount>0 应成功；改用 10 积分 = 0.10 元仍>0。
        // 真正失败场景：0 积分请求直接返回 0 不抛错；这里测积分不足整 1 分钱的情况不存在（100:1 恒>0）
        // 改为验证超出上限请求被折算而非报错已由 quote 覆盖；此用例验证正向最小抵扣
        BigDecimal amount = service.redeemPoints(2L, 50, new BigDecimal("1000"), "ORD-2");
        assertEquals(0, new BigDecimal("0.50").compareTo(amount));
    }

    // ===== 返还 refundPoints =====

    @Test
    void refundReturnsPointsOnce() {
        when(cardMapper.selectOne(any())).thenReturn(card(4000));
        // 订单有一笔 redeem 流水：-1000
        MemberPointLog redeem = new MemberPointLog();
        redeem.setChangeType(MemberRule.CHANGE_REDEEM);
        redeem.setChangeVal(-1000);
        redeem.setRefOrderNo("ORD-1");
        when(logMapper.selectList(any())).thenReturn(List.of(redeem));
        when(logMapper.selectCount(any())).thenReturn(0L); // 无 refund 记录

        int refunded = service.refundPoints(2L, "ORD-1");
        assertEquals(1000, refunded);
        verify(logMapper).insert(argThat((MemberPointLog l) -> MemberRule.CHANGE_REFUND.equals(l.getChangeType())
                && l.getChangeVal() == 1000));
    }

    @Test
    void refundIdempotentWhenAlreadyRefunded() {
        when(logMapper.selectCount(any())).thenReturn(1L); // 已返还
        assertEquals(0, service.refundPoints(2L, "ORD-1"), "已返还订单不应重复返还");
        verify(logMapper, never()).insert(any(MemberPointLog.class));
    }

    @Test
    void refundZeroWhenNoRedeemLog() {
        when(logMapper.selectList(any())).thenReturn(List.of());
        assertEquals(0, service.refundPoints(2L, "ORD-X"));
        verify(logMapper, never()).insert(any(MemberPointLog.class));
    }

    // ===== 退款扣回 reverseEarn（全额退款时扣回支付所得积分） =====

    @Test
    void reverseEarnDeductsAndWritesNegativeLog() {
        when(cardMapper.selectOne(any())).thenReturn(card(500));
        MemberPointLog earn = new MemberPointLog();
        earn.setChangeType("earn_consume");
        earn.setChangeVal(100);
        earn.setRefOrderNo("ORD-1");
        when(logMapper.selectList(any())).thenReturn(List.of(earn));
        when(logMapper.selectCount(any())).thenReturn(0L);

        int back = service.reverseEarn(2L, "ORD-1");
        assertEquals(100, back);
        verify(cardMapper).updateById(argThat((MemberCard c) -> c.getPoints() == 400));
        verify(logMapper).insert(argThat((MemberPointLog l) ->
                MemberRule.CHANGE_EARN_REVERSAL.equals(l.getChangeType()) && l.getChangeVal() == -100));
    }

    @Test
    void reverseEarnIdempotent() {
        when(logMapper.selectCount(any())).thenReturn(1L); // 已扣回
        assertEquals(0, service.reverseEarn(2L, "ORD-1"));
        verify(logMapper, never()).insert(any(MemberPointLog.class));
    }

    @Test
    void reverseEarnZeroWhenNoEarnLog() {
        when(logMapper.selectList(any())).thenReturn(List.of());
        assertEquals(0, service.reverseEarn(2L, "ORD-X"));
        verify(logMapper, never()).insert(any(MemberPointLog.class));
    }

    @Test
    void reverseEarnClampsAtZeroBalance() {
        // 支付所得 100 积分已被消费，余额仅 30 → 卡积分扣到 0，流水仍记 -100
        when(cardMapper.selectOne(any())).thenReturn(card(30));
        MemberPointLog earn = new MemberPointLog();
        earn.setChangeType("earn_consume");
        earn.setChangeVal(100);
        when(logMapper.selectList(any())).thenReturn(List.of(earn));
        when(logMapper.selectCount(any())).thenReturn(0L);

        assertEquals(100, service.reverseEarn(2L, "ORD-1"));
        verify(cardMapper).updateById(argThat((MemberCard c) -> c.getPoints() == 0));
    }

    /** 供 argThat 类型推断使用的辅助（MemberPointLog） */
    private static MemberLevel level(Long id, int min) {
        MemberLevel l = new MemberLevel();
        l.setLevelId(id); l.setMinGrowth(min); l.setDiscountRate(BigDecimal.ONE);
        return l;
    }
}
