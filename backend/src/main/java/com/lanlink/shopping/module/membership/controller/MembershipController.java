package com.lanlink.shopping.module.membership.controller;

import com.lanlink.shopping.common.R;
import com.lanlink.shopping.config.UserContext;
import com.lanlink.shopping.module.membership.service.MembershipService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 会员系统 API（营销中台）
 *  - GET /membership/my   我的会员卡（等级/成长值/积分/流水）
 *  - GET /membership/levels 等级体系
 */
@RestController
@RequestMapping("/membership")
public class MembershipController {

    private final MembershipService membershipService;

    public MembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @GetMapping("/my")
    public R<Map<String, Object>> my(HttpServletRequest request) {
        return R.ok(membershipService.myView(UserContext.currentUserId(request)));
    }
}
