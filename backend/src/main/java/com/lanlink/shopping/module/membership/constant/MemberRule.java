package com.lanlink.shopping.module.membership.constant;

/**
 * 会员积分规则（参考主流电商开源项目 macrozheng/mall 积分消费设置 的简化实现）。
 *
 * 规则总览（对用户展示于会员中心）：
 *  - 消费获取：每消费 1 元获得 1 积分（VIP 双倍）
 *  - 积分抵现：每 100 积分抵 1 元
 *  - 抵现上限：单笔订单最高抵扣应付金额的 10%
 *  - 取消返还：订单取消后使用的积分全额返还
 */
public final class MemberRule {

    private MemberRule() {}

    /** 消费获取比例：每 X 元 = 1 积分（当前 1 元 = 1 积分） */
    public static final int POINTS_PER_YUAN_EARN = 1;

    /** 抵现比例：X 积分 = 1 元 */
    public static final int REDEEM_POINTS_PER_YUAN = 100;

    /** 单笔抵现上限：最高抵扣订单应付金额的百分比 */
    public static final int REDEEM_MAX_PERCENT = 10;

    /** VIP 消费积分倍数 */
    public static final int VIP_EARN_MULTIPLIER = 2;

    /** 积分流水类型：下单抵现 */
    public static final String CHANGE_REDEEM = "redeem_order";
    /** 积分流水类型：取消返还 */
    public static final String CHANGE_REFUND = "refund_order";
    /** 积分流水类型：全额退款扣回支付所得 */
    public static final String CHANGE_EARN_REVERSAL = "earn_reversal";

    /** 按积分数计算可抵扣金额（元，两位小数向下取整到分） */
    public static java.math.BigDecimal pointsToAmount(int points) {
        if (points <= 0) return java.math.BigDecimal.ZERO;
        return java.math.BigDecimal.valueOf(points)
                .divide(java.math.BigDecimal.valueOf(REDEEM_POINTS_PER_YUAN), 2, java.math.RoundingMode.DOWN);
    }

    /** 按金额计算可获得积分数（1元=1分，向下取整） */
    public static int amountToPoints(java.math.BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) return 0;
        return amount.intValue(); // 1 积分/元，不足 1 元部分不计
    }
}
