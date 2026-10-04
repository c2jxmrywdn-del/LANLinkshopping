package com.lanlink.shopping.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 商户流量统计 SQL（商户端「流量管理」数据源）。
 * 口径：销售额/销量仅统计已支付订单（pay_status=1），按订单支付时间归集。
 */
@Mapper
public interface TrafficMapper {

    /** 在售商品数（审核通过且可售） */
    @Select("SELECT COUNT(*) FROM t_product WHERE mer_id = #{merId} AND status = 1 AND deleted = 0")
    long countOnSale(@Param("merId") Long merId);

    /** 近 N 天销售额与订单数（单行） */
    @Select("SELECT COALESCE(SUM(oi.subtotal), 0) AS amount, COUNT(DISTINCT o.order_no) AS orders " +
            "FROM t_order_item oi JOIN t_order o ON oi.order_no = o.order_no " +
            "WHERE o.pay_status = 1 AND o.pay_time >= DATE_SUB(NOW(), INTERVAL #{days} DAY) " +
            "AND oi.prod_id IN (SELECT prod_id FROM t_product WHERE mer_id = #{merId} AND deleted = 0)")
    Map<String, Object> sumRecent(@Param("merId") Long merId, @Param("days") int days);

    /** 累计销售额与总销量（已支付） */
    @Select("SELECT COALESCE(SUM(oi.subtotal), 0) AS amount, COALESCE(SUM(oi.quantity), 0) AS sold " +
            "FROM t_order_item oi JOIN t_order o ON oi.order_no = o.order_no " +
            "WHERE o.pay_status = 1 " +
            "AND oi.prod_id IN (SELECT prod_id FROM t_product WHERE mer_id = #{merId} AND deleted = 0)")
    Map<String, Object> sumTotal(@Param("merId") Long merId);

    /** 上一周期（用于环比基期）：[days, 2*days) 天前窗口 */
    @Select("SELECT COALESCE(SUM(oi.subtotal), 0) AS amount, COUNT(DISTINCT o.order_no) AS orders " +
            "FROM t_order_item oi JOIN t_order o ON oi.order_no = o.order_no " +
            "WHERE o.pay_status = 1 AND o.pay_time >= DATE_SUB(NOW(), INTERVAL #{times2} DAY) " +
            "AND o.pay_time < DATE_SUB(NOW(), INTERVAL #{days} DAY) " +
            "AND oi.prod_id IN (SELECT prod_id FROM t_product WHERE mer_id = #{merId} AND deleted = 0)")
    Map<String, Object> sumPrevPeriod(@Param("merId") Long merId, @Param("days") int days, @Param("times2") int times2);

    /** 支付渠道分布（近 N 天，转化/渠道管理视角） */
    @Select("SELECT COALESCE(o.pay_channel, 'unknown') AS channel, COUNT(DISTINCT o.order_no) AS orders, " +
            "COALESCE(SUM(oi.subtotal), 0) AS amount " +
            "FROM t_order_item oi JOIN t_order o ON oi.order_no = o.order_no " +
            "WHERE o.pay_status = 1 AND o.pay_time >= DATE_SUB(NOW(), INTERVAL #{days} DAY) " +
            "AND oi.prod_id IN (SELECT prod_id FROM t_product WHERE mer_id = #{merId} AND deleted = 0) " +
            "GROUP BY COALESCE(o.pay_channel, 'unknown') ORDER BY amount DESC")
    List<Map<String, Object>> channelStats(@Param("merId") Long merId, @Param("days") int days);

    /** 动销商品数：窗口内有销量的本店商品数 */
    @Select("SELECT COUNT(DISTINCT oi.prod_id) FROM t_order_item oi JOIN t_order o ON oi.order_no = o.order_no " +
            "WHERE o.pay_status = 1 " +
            "AND oi.prod_id IN (SELECT prod_id FROM t_product WHERE mer_id = #{merId} AND deleted = 0)")
    long countActiveProducts(@Param("merId") Long merId);

    /** 近 N 天按日趋势（date/amount/orders） */
    @Select("SELECT DATE_FORMAT(o.pay_time, '%Y-%m-%d') AS date, SUM(oi.subtotal) AS amount, " +
            "COUNT(DISTINCT o.order_no) AS orders " +
            "FROM t_order_item oi JOIN t_order o ON oi.order_no = o.order_no " +
            "WHERE o.pay_status = 1 AND o.pay_time >= DATE_SUB(NOW(), INTERVAL #{days} DAY) " +
            "AND oi.prod_id IN (SELECT prod_id FROM t_product WHERE mer_id = #{merId} AND deleted = 0) " +
            "GROUP BY DATE_FORMAT(o.pay_time, '%Y-%m-%d') ORDER BY date")
    List<Map<String, Object>> trendByDay(@Param("merId") Long merId, @Param("days") int days);

    /** 商品维度销量/销售额排行（含未售商品，便于对比） */
    @Select("SELECT p.prod_id AS prodId, p.title, p.cover_url AS coverUrl, p.price, p.stock, " +
            "COALESCE(SUM(CASE WHEN o.pay_status = 1 THEN oi.quantity END), 0) AS sold, " +
            "COALESCE(SUM(CASE WHEN o.pay_status = 1 THEN oi.subtotal END), 0) AS amount " +
            "FROM t_product p " +
            "LEFT JOIN t_order_item oi ON oi.prod_id = p.prod_id " +
            "LEFT JOIN t_order o ON oi.order_no = o.order_no " +
            "WHERE p.mer_id = #{merId} AND p.deleted = 0 " +
            "GROUP BY p.prod_id, p.title, p.cover_url, p.price, p.stock " +
            "ORDER BY amount DESC, sold DESC")
    List<Map<String, Object>> productRanking(@Param("merId") Long merId);
}
