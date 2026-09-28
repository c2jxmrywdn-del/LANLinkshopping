package com.lanlink.shopping.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lanlink.shopping.entity.*;
import com.lanlink.shopping.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 启动初始化: 若示例数据不存在则写入(密码用 BCrypt 正确加密)
 * 演示账号(密码均为 123456):
 *   13800000000 平台运营 admin
 *   13900000001 采购方 buyer
 *   13700000002 商户 merchant(已通过审核)
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final EnterpriseMapper enterpriseMapper;
    private final MerchantMapper merchantMapper;
    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserMapper userMapper, RoleMapper roleMapper, EnterpriseMapper enterpriseMapper,
                           MerchantMapper merchantMapper, ProductMapper productMapper,
                           CategoryMapper categoryMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper; this.roleMapper = roleMapper;
        this.enterpriseMapper = enterpriseMapper; this.merchantMapper = merchantMapper;
        this.productMapper = productMapper; this.categoryMapper = categoryMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userMapper.selectCount(null) > 0) {
            log.info("已存在用户数据, 跳过初始化");
            return;
        }
        log.info("==== 初始化示例数据 ====");
        Role buyer = roleMapper.selectOne(Wrappers.<Role>lambdaQuery().eq(Role::getRoleCode, "buyer"));
        Role merchantRole = roleMapper.selectOne(Wrappers.<Role>lambdaQuery().eq(Role::getRoleCode, "merchant"));
        Role admin = roleMapper.selectOne(Wrappers.<Role>lambdaQuery().eq(Role::getRoleCode, "admin"));
        if (buyer == null || merchantRole == null || admin == null) {
            throw new IllegalStateException("t_role 缺少 buyer/merchant/admin 基础数据, 请检查 sql/schema.sql 是否执行成功");
        }

        // 企业
        Enterprise ent = new Enterprise();
        ent.setName("华晨建材集团有限公司");
        ent.setCreditCode("91500000MA5U0000XA");
        ent.setMemberLevel(2);
        ent.setCreateTime(LocalDateTime.now());
        enterpriseMapper.insert(ent);

        // 用户
        User uAdmin = mkUser("13800000000", "平台运营", admin.getRoleId(), null);
        User uBuyer = mkUser("13900000001", "重庆建工采购", buyer.getRoleId(), null);
        User uMer   = mkUser("13700000002", "华晨建材", merchantRole.getRoleId(), ent.getEntId());

        // 商户(已通过筛选: 公司类型 + 稳定纳税)
        Merchant m = new Merchant();
        m.setEntId(ent.getEntId()); m.setUserId(uMer.getUserId());
        m.setRegType("公司"); m.setRegCapital(new BigDecimal("5000000"));
        m.setTaxStatus(1); m.setJoinType("邀约"); m.setReviewStatus(1);
        m.setCreateTime(LocalDateTime.now()); m.setUpdateTime(LocalDateTime.now());
        merchantMapper.insert(m);

        // 商品(覆盖四大行业, 关联到已审核商户)
        List<Product> products = new ArrayList<>();
        products.add(mkProduct(m.getMerId(), "建筑钢材 HRB400 螺纹钢 Φ20", "华晨", "Φ20*9m", "3850.00", 1L, 5000, "吨", 320));
        products.add(mkProduct(m.getMerId(), "硅酸盐水泥 P.O42.5 袋装", "华新", "50kg/袋", "420.00", 2L, 8000, "吨", 210));
        products.add(mkProduct(m.getMerId(), "PPR 给水管材 DN25", "日丰", "25mm*4m", "28.50", 3L, 12000, "根", 150));
        products.add(mkProduct(m.getMerId(), "全棉坯布 40*40 133*72", "华纺", "幅宽160cm", "8.60", 4L, 30000, "米", 90));
        products.add(mkProduct(m.getMerId(), "涤纶短纤 1.56D*38mm", "恒力", "1.56D", "6800.00", 5L, 2000, "吨", 60));
        products.add(mkProduct(m.getMerId(), "工业级碳酸钠 99.2%", "三友", "25kg/袋", "1850.00", 7L, 4000, "吨", 45));
        products.add(mkProduct(m.getMerId(), "抗磨液压油 L-HM46", "长城", "18L/桶", "360.00", 8L, 1500, "桶", 75));
        products.add(mkProduct(m.getMerId(), "贴片电阻 0805 10KΩ", "风华", "0805", "0.02", 9L, 500000, "只", 980));
        products.add(mkProduct(m.getMerId(), "板对板连接器 2.0mm 双排", "立讯", "2x10P", "1.85", 10L, 60000, "只", 260));
        products.add(mkProduct(m.getMerId(), "IPS 显示面板 7寸 1024*600", "京东方", "7inch", "96.00", 11L, 3000, "片", 130));
        for (Product p : products) {
            productMapper.insert(p);
            // 挂上无水印商品图（public/products/{id}.jpg）
            p.setCoverUrl("/products/" + p.getProdId() + ".jpg");
            productMapper.updateById(p);
        }

        log.info("==== 初始化完成: 3用户/1商户/10商品, 演示账号密码均为 123456 ====");
    }

    private User mkUser(String phone, String nick, Long roleId, Long entId) {
        User u = new User();
        u.setPhone(phone); u.setPassword(passwordEncoder.encode("123456"));
        u.setNickname(nick); u.setRoleId(roleId); u.setEntId(entId);
        u.setCreateTime(LocalDateTime.now()); u.setUpdateTime(LocalDateTime.now());
        userMapper.insert(u);
        return u;
    }

    private Product mkProduct(Long merId, String title, String brand, String spec, String price,
                              Long catId, int stock, String unit, int sales) {
        Product p = new Product();
        p.setMerId(merId); p.setTitle(title); p.setBrand(brand); p.setSpec(spec);
        p.setPrice(new BigDecimal(price)); p.setCatId(catId);
        // 行业: 1-3建筑,4-5纺织,7-8石化,9-11电子 (按分类首段映射)
        p.setIndId(mapInd(catId));
        p.setStock(stock); p.setSales(sales); p.setStatus(1);
        p.setCoverUrl(""); p.setDetail("规格: " + spec + "；计量单位: " + unit + "。源头厂商直供，支持对公转账与账期结算。");
        p.setTierPriceJson("[{\"qty\":100,\"price\":" + price + "}]");
        p.setCreateTime(LocalDateTime.now()); p.setUpdateTime(LocalDateTime.now());
        return p;
    }

    private Long mapInd(Long catId) {
        if (catId <= 3) return 1L;      // 建筑
        if (catId <= 6) return 2L;      // 纺织
        if (catId <= 8) return 3L;      // 石化
        return 4L;                       // 电子
    }
}
