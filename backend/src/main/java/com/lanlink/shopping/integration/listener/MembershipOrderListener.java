package com.lanlink.shopping.integration.listener;

import com.lanlink.shopping.integration.event.OrderPaidEvent;
import com.lanlink.shopping.module.membership.service.MembershipService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 会员系统 × 订单系统 事件桥接。
 * 监听订单支付事件 → 累计会员成长值/积分/升级等级。
 * 独立事务（REQUIRES_NEW）：营销侧处理失败不回滚订单支付，保证核心交易一致性；
 * 并通过异步/事件解耦实现模块隔离。
 */
@Component
public class MembershipOrderListener {

    private static final Logger log = LoggerFactory.getLogger(MembershipOrderListener.class);

    private final MembershipService membershipService;

    public MembershipOrderListener(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOrderPaid(OrderPaidEvent event) {
        try {
            membershipService.earnGrowth(event.getUserId(), event.getAmount(), event.getOrderNo());
            log.info("[membership] 订单 {} 支付成功，会员成长值/积分已累计 userId={}", event.getOrderNo(), event.getUserId());
        } catch (Exception e) {
            // 隔离原则：营销侧异常不得影响订单主流程，仅记录
            log.error("[membership] 会员积分累计失败 orderNo={}", event.getOrderNo(), e);
        }
    }
}
