package com.lanlink.shopping.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lanlink.shopping.common.R;
import com.lanlink.shopping.config.UserContext;
import com.lanlink.shopping.entity.Product;
import com.lanlink.shopping.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * 商城商品: 前台浏览(公开) + 商户发布(需登录且为商户, 进入平台审核)
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

    @Value("${app.upload-dir:${user.dir}/uploads}")
    private String uploadDirectory;
    private static final long MAX_IMG_SIZE = 5 * 1024 * 1024;

    @GetMapping("/page")
    public R<IPage<Product>> page(@RequestParam(defaultValue = "1")���q�^