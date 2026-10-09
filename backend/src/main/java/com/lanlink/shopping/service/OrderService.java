package com.lanlink.shopping.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lanlink.shopping.common.BusinessException;
import com.lanlink.shopping.dto.CheckoutDTO;
import com.lanlink.shopping.entity.*;
import com.lanlink.shopping.integration.event.OrderPaidEvent;
import com.lanlink.shopping.integration.pricing.PricingFacade;
import com.lanlink.shopping.mapper.*;
import com.lanlink.shopping.vo.OrderDetailVO;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 订单服务(下单主流程含事务: 校验库存 -> 生成订单+明细 -> 扣库存 -> 清购物车)
 */
@Service
public class OrderService {

    private final CartMapper cartMapper;
    private final ProductMapper productMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserMapper userMapper;
    private final MessageService messageService;
    private final IdentityService identityService;
    private final AuditService auditService;
    private final PricingFacade pricingFacade;
    private final ApplicationEventPublisher eventPublisher;
    private final com.lanlink.shopping.module.membership.service.MembershipServic���q�^