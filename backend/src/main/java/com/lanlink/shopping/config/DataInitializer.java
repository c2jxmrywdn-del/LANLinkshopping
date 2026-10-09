package com.lanlink.shopping.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lanlink.shopping.entity.*;
import com.lanlink.shopping.mapper.*;
import com.lanlink.shopping.module.activity.entity.Activity;
import com.lanlink.shopping.module.activity.mapper.ActivityMapper;
import com.lanlink.shopping.module.promotion.entity.Promotion;
import com.lanlink.shopping.module.promotion.mapper.PromotionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.demo-data.enabled:false}")
    private boolean demoDataEnabled;

    @Value("${RAILWAY_ENVIRONMENT:}")
    private String railwayEnvironment;

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final En���q�^