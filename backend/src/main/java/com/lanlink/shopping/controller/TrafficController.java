package com.lanlink.shopping.controller;

import com.lanlink.shopping.common.R;
import com.lanlink.shopping.config.UserContext;
import com.lanlink.shopping.integration.security.RequirePerm;
import com.lanlink.shopping.service.TrafficService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 商户流量管理 API（需 merchant:manage 权限，即已入驻商户）：
 *  - GET /merchant/traffic/overview  经营概览
 *  - GET /merchant/traffic/trend     近 N 天按日趋势
 *  - GET /merchant/traffic/products  商品销量/销售额排行
 */
@RestController
@RequestMapping("/merchant/traffic")
public class TrafficController {

    private final TrafficService trafficService;

    public TrafficController(TrafficService trafficService) {
        this.trafficService = trafficService;
    }

    @GetMapping("/overview")
    @RequirePerm("merchant:manage")
    public R<Map<String, Object>> overview(HttpServletRequest request) {
        return R.ok(trafficService.overview(UserContext.currentUserId(request)));
    }

    @GetMapping("/trend")
    @RequirePerm("merchant:manage")
    public R<List<Map<String, Object>>> trend(@RequestParam(required = false) Integer days,
                                              HttpServletRequest request) {
        return R.ok(trafficService.trend(UserContext.currentUserId(request), days));
    }

    /** 支付渠道分布（近 N 天成交占比，渠道管理/转化优化） */
    @GetMapping("/channels")
    @RequirePerm("merchant:manage")
    public R<List<Map<String, Object>>> channels(@RequestParam(required = false) Integer days,
                                                 HttpServletRequest request) {
        return R.ok(trafficService.channels(UserContext.currentUserId(request), days));
    }

    @GetMapping("/products")
    @RequirePerm("merchant:manage")
    public R<List<Map<String, Object>>> products(HttpServletRequest request) {
        return R.ok(trafficService.productRanking(UserContext.currentUserId(request)));
    }
}
