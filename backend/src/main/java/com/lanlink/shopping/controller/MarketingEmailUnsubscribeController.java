package com.lanlink.shopping.controller;

import com.lanlink.shopping.common.R;
import com.lanlink.shopping.service.MarketingEmailUnsubscribeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Public one-click unsubscribe endpoint. The signed token authorizes only the
 * preference change represented by the token, not general account access.
 */
@RestController
@RequestMapping("/marketing-email")
public class MarketingEmailUnsubscribeController {
    private final MarketingEmailUnsubscribeService unsubscribeService;

    public MarketingEmailUnsubscribeController(MarketingEmailUnsubscribeService unsubscribeService) {
        this.unsubscribeService = unsubscribeService;
    }

    @GetMapping("/unsubscribe")
    public R<Map<String, String>> unsubscribe(
            @RequestParam("userId") Long userId,
            @RequestParam("token") String token) {
        unsubscribeService.unsubscribe(userId, token);
        return R.ok(Map.of("message", "已成功退订 LANLinkshopping 商业推广邮件"));
    }
}
