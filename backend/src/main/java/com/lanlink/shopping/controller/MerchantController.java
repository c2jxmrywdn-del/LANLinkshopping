package com.lanlink.shopping.controller;

import com.lanlink.shopping.common.R;
import com.lanlink.shopping.config.UserContext;
import com.lanlink.shopping.dto.MerchantApplyDTO;
import com.lanlink.shopping.entity.Merchant;
import com.lanlink.shopping.service.MerchantService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商户入驻与筛选 + 运营审核
 */
@RestController
@RequestMapping("/merchant")
public class MerchantController {

    private final MerchantService merchantService;

    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    /** 提交入驻申请(自动执行筛选机制) */
    @PostMapping("/apply")
    public R<Merchant> apply(@Valid @RequestBody MerchantApplyDTO dto, HttpServletRequest request) {
        Long userId = UserContext.currentUserId(request);
        return R.ok("入驻申请已提交", merchantService.apply(userId, null, dto));
    }

    /** 我的商户信息 */
    @GetMapping("/my")
    public R<Merchant> my(HttpServletRequest request) {
        return R.ok(merchantService.getByUser(UserContext.currentUserId(request)));
    }

    /** 运营端: 商户审核列表 (需 admin 角色, 由拦截器鉴权) */
    @GetMapping("/admin/list")
    public R<List<Merchant>> adminList(@RequestParam(required = false) Integer reviewStatus) {
        return R.ok(merchantService.listForAdmin(reviewStatus));
    }

    /** 运营端: 审核通过/驳回 */
    @PostMapping("/admin/review/{merId}")
    public R<Merchant> review(@PathVariable Long merId,
                              @RequestParam Integer reviewStatus,
                              @RequestParam(required = false) String reason) {
        return R.ok(merchantService.review(merId, reviewStatus, reason));
    }
}
