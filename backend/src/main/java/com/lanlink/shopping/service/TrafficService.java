package com.lanlink.shopping.service;

import com.lanlink.shopping.common.BusinessException;
import com.lanlink.shopping.entity.Merchant;
import com.lanlink.shopping.mapper.TrafficMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 商户流量统计（商户端「流量管理」）。
 * 数据口径：仅统计已支付订单（pay_status=1）；trend/productRanking 直接透出 Mapper 聚合行。
 */
@Service
public class TrafficService {

    private static final int MAX_DAYS = 90;

    private final TrafficMapper trafficMapper;
    private final MerchantService merchantService;

    public TrafficService(TrafficMapper trafficMapper, MerchantService merchantService) {
        this.trafficMapper = trafficMapper;
        this.merchantService = merchantService;
    }

    /** 概览：在售商品数 / 累计销售额 / 累计销量 / 近30天销售额与订单 / 环比 / 客单价 / 动销率 */
    public Map<String, Object> overview(Long userId) {
        Long merId = mustMerId(userId);
        Map<String, Object> recent = trafficMapper.sumRecent(merId, 30);
        Map<String, Object> prev = trafficMapper.sumPrevPeriod(merId, 30, 60);
        Map<String, Object> total = trafficMapper.sumTotal(merId);
        long onSale = trafficMapper.countOnSale(merId);
        long active = trafficMapper.countActiveProducts(merId);
        java.math.BigDecimal recentAmount = toDecimal(recent == null ? null : recent.get("amount"));
        java.math.BigDecimal prevAmount = toDecimal(prev == null ? null : prev.get("amount"));
        long recentOrders = toLong(recent == null ? null : recent.get("orders"));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("onSaleProducts", onSale);
        out.put("totalAmount", total == null ? 0 : total.get("amount"));
        out.put("totalSold", total == null ? 0 : total.get("sold"));
        out.put("recentAmount", recent == null ? 0 : recent.get("amount"));
        out.put("recentOrders", recentOrders);
        // 环比：（本期-上期）/上期，上期为 0 时不计算（null 前端显示为 —）
        out.put("prevAmount", prev == null ? 0 : prev.get("amount"));
        out.put("chainGrowthPct", prevAmount.signum() > 0
                ? recentAmount.subtract(prevAmount)
                        .divide(prevAmount, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(java.math.BigDecimal.valueOf(100))
                        .doubleValue()
                : null);
        // 客单价：近 30 天销售额 / 订单数
        out.put("avgOrderValue", recentOrders > 0
                ? recentAmount.divide(java.math.BigDecimal.valueOf(recentOrders), 2, java.math.RoundingMode.HALF_UP)
                : java.math.BigDecimal.ZERO);
        // 动销率：有销量商品 / 在售商品
        out.put("activeProducts", active);
        out.put("activeRatePct", onSale > 0
                ? java.math.BigDecimal.valueOf(active * 100L)
                        .divide(java.math.BigDecimal.valueOf(onSale), 1, java.math.RoundingMode.HALF_UP)
                : java.math.BigDecimal.ZERO);
        return out;
    }

    /** 近 N 天按日趋势（补零填充无销售日期，图表连续不失真） */
    public List<Map<String, Object>> trend(Long userId, Integer days) {
        Long merId = mustMerId(userId);
        int d = days == null || days <= 0 ? 30 : Math.min(days, MAX_DAYS);
        List<Map<String, Object>> rows = trafficMapper.trendByDay(merId, d);
        Map<String, Map<String, Object>> byDate = new LinkedHashMap<>();
        for (Map<String, Object> r : rows) {
            byDate.put(String.valueOf(r.get("date")), r);
        }
        List<Map<String, Object>> filled = new java.util.ArrayList<>();
        java.time.LocalDate cur = java.time.LocalDate.now().minusDays(d - 1L);
        for (int i = 0; i < d; i++) {
            String key = cur.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
            Map<String, Object> row = byDate.get(key);
            if (row == null) {
                row = new LinkedHashMap<>();
                row.put("date", key);
                row.put("amount", java.math.BigDecimal.ZERO);
                row.put("orders", 0L);
            }
            filled.add(row);
            cur = cur.plusDays(1);
        }
        return filled;
    }

    /** 支付渠道分布（近 N 天成交，渠道管理与转化优化视角） */
    public List<Map<String, Object>> channels(Long userId, Integer days) {
        Long merId = mustMerId(userId);
        int d = days == null || days <= 0 ? 30 : Math.min(days, MAX_DAYS);
        return trafficMapper.channelStats(merId, d);
    }

    /** 商品维度销量/销售额排行 */
    public List<Map<String, Object>> productRanking(Long userId) {
        Long merId = mustMerId(userId);
        return trafficMapper.productRanking(merId);
    }

    private Long mustMerId(Long userId) {
        Merchant m = merchantService.getByUser(userId);
        if (m == null) throw new BusinessException("仅入驻商户可查看流量数据");
        return m.getMerId();
    }

    private static java.math.BigDecimal toDecimal(Object o) {
        return o == null ? java.math.BigDecimal.ZERO : new java.math.BigDecimal(String.valueOf(o));
    }

    private static long toLong(Object o) {
        return o == null ? 0L : Long.parseLong(String.valueOf(o));
    }
}
