package com.lanlink.shopping.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lanlink.shopping.common.R;
import com.lanlink.shopping.config.UserContext;
import com.lanlink.shopping.entity.Product;
import com.lanlink.shopping.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

/**
 * 商城商品: 前台浏览(公开) + 商户上架(需登录且为商户)
 */
@RestController
@RequestMapping("/product")
public class ProductController {

    private final ProductService productService;
    private final com.lanlink.shopping.service.MerchantService merchantService;

    public ProductController(ProductService productService,
                             com.lanlink.shopping.service.MerchantService merchantService) {
        this.productService = productService;
        this.merchantService = merchantService;
    }

    @GetMapping("/page")
    public R<IPage<Product>> page(@RequestParam(defaultValue = "1") long current,
                                  @RequestParam(defaultValue = "12") long size,
                                  @RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) Long catId,
                                  @RequestParam(required = false) Long indId,
                                  @RequestParam(required = false) String sort) {
        return R.ok(productService.page(current, size, keyword, catId, indId, sort));
    }

    @GetMapping("/detail/{id}")
    public R<Product> detail(@PathVariable Long id) {
        return R.ok(productService.detail(id));
    }

    /** 商户上架商品 */
    @PostMapping("/publish")
    public R<Product> publish(@RequestBody Product product, HttpServletRequest request) {
        Long userId = UserContext.currentUserId(request);
        var merchant = merchantService.getByUser(userId);
        if (merchant == null) return R.fail("请先完成商户入驻");
        if (merchant.getReviewStatus() == null || merchant.getReviewStatus() != 1) {
            return R.fail("商户审核未通过，暂不能上架商品");
        }
        return R.ok(productService.publish(merchant.getMerId(), product));
    }

    /** 管理后台: 全量商品列表(含已下架) */
    @GetMapping("/admin/list")
    public R<IPage<Product>> adminList(@RequestParam(defaultValue = "1") long current,
                                       @RequestParam(defaultValue = "20") long size,
                                       @RequestParam(required = false) Integer status,
                                       @RequestParam(required = false) String keyword) {
        return R.ok(productService.adminPage(current, size, status, keyword));
    }

    /** 管理后台: 上架/下架 (status 1上架 0下架) */
    @PostMapping("/admin/status/{prodId}")
    public R<Product> adminStatus(@PathVariable Long prodId, @RequestParam Integer status) {
        return R.ok(productService.setStatus(prodId, status));
    }
}
