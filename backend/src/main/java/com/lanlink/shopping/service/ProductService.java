package com.lanlink.shopping.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lanlink.shopping.common.BusinessException;
import com.lanlink.shopping.entity.Product;
import com.lanlink.shopping.mapper.ProductMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 商城与商品服务
 */
@Service
public class ProductService {

    private final ProductMapper productMapper;

    public ProductService(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    /** 分页查询(前台): 关键词/分类/行业/排序 */
    public IPage<Product> page(long current, long size, String keyword, Long catId, Long indId, String sort) {
        LambdaQueryWrapper<Product> qw = Wrappers.<Product>lambdaQuery()
                .eq(Product::getStatus, 1)
                .like(StringUtils.hasText(keyword), Product::getTitle, keyword)
                .eq(catId != null, Product::getCatId, catId)
                .eq(indId != null, Product::getIndId, indId);
        if ("sales".equals(sort)) {
            qw.orderByDesc(Product::getSales);
        } else if ("priceAsc".equals(sort)) {
            qw.orderByAsc(Product::getPrice);
        } else if ("priceDesc".equals(sort)) {
            qw.orderByDesc(Product::getPrice);
        } else {
            qw.orderByDesc(Product::getCreateTime);
        }
        return productMapper.selectPage(new Page<>(current, size), qw);
    }

    public Product detail(Long id) {
        Product p = productMapper.selectById(id);
        if (p == null) throw new BusinessException("商品不存在或已下架");
        return p;
    }

    /** 商户上架商品 */
    public Product publish(Long merId, Product p) {
        p.setMerId(merId);
        p.setStatus(1);
        p.setSales(0);
        p.setCreateTime(LocalDateTime.now());
        p.setUpdateTime(LocalDateTime.now());
        productMapper.insert(p);
        return p;
    }

    public IPage<Product> pageByMerchant(Long merId, long current, long size) {
        return productMapper.selectPage(new Page<>(current, size),
                Wrappers.<Product>lambdaQuery().eq(Product::getMerId, merId).orderByDesc(Product::getCreateTime));
    }

    /** 管理后台: 全量商品分页(含已下架), 可按状态/关键词筛选 */
    public IPage<Product> adminPage(long current, long size, Integer status, String keyword) {
        return productMapper.selectPage(new Page<>(current, size),
                Wrappers.<Product>lambdaQuery()
                        .eq(status != null, Product::getStatus, status)
                        .like(StringUtils.hasText(keyword), Product::getTitle, keyword)
                        .orderByDesc(Product::getCreateTime));
    }

    /** 管理后台: 上架/下架 */
    public Product setStatus(Long prodId, Integer status) {
        Product p = productMapper.selectById(prodId);
        if (p == null) throw new BusinessException("商品不存在");
        p.setStatus(status);
        p.setUpdateTime(LocalDateTime.now());
        productMapper.updateById(p);
        return p;
    }
}
