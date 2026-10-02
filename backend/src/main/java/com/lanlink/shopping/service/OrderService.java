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

    public OrderService(CartMapper cartMapper, ProductMapper productMapper, OrderMapper orderMapper,
                        OrderItemMapper orderItemMapper, UserMapper userMapper, MessageService messageService,
                        IdentityService identityService, AuditService auditService,
                        PricingFacade pricingFacade, ApplicationEventPublisher eventPublisher) {
        this.cartMapper = cartMapper;
        this.productMapper = productMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.userMapper = userMapper;
        this.messageService = messageService;
        this.identityService = identityService;
        this.auditService = auditService;
        this.pricingFacade = pricingFacade;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Order checkout(Long userId, CheckoutDTO dto) {
        User user = userMapper.selectById(userId);
        List<Cart> carts;
        if (dto.getCartIds() != null && !dto.getCartIds().isEmpty()) {
            carts = cartMapper.selectList(Wrappers.<Cart>lambdaQuery()
                    .eq(Cart::getUserId, userId).in(Cart::getCartId, dto.getCartIds()));
        } else {
            carts = cartMapper.selectList(Wrappers.<Cart>lambdaQuery()
                    .eq(Cart::getUserId, userId).eq(Cart::getChecked, 1));
        }
        if (carts.isEmpty()) throw new BusinessException("购物车没有可结算的商品");

        Order order = new Order();
        order.setOrderNo(genOrderNo());
        order.setUserId(userId);
        order.setEntId(user == null ? null : user.getEntId());
        order.setPayType(dto.getPayType());
        order.setPayStatus(0);
        order.setOrderStatus(0);
        order.setReceiver(dto.getReceiver());
        order.setPhone(dto.getPhone());
        order.setAddress(dto.getAddress());
        order.setRemark(dto.getRemark());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        // 先校验库存（并确认商品可售：仅审核通过的商品可下单），累计原价与行业
        BigDecimal total = BigDecimal.ZERO;
        Long orderIndId = null;
        for (Cart c : carts) {
            Product p = productMapper.selectById(c.getProdId());
            if (p == null) throw new BusinessException("商品不存在: " + c.getProdId());
            if (p.getStatus() == null || p.getStatus() != 1) {
                throw new BusinessException("商品暂不可购买(未通过审核或已下架): " + p.getTitle());
            }
            if (p.getStock() < c.getQuantity()) throw new BusinessException("库存不足: " + p.getTitle());
            total = total.add(p.getPrice().multiply(BigDecimal.valueOf(c.getQuantity())));
            if (orderIndId == null) orderIndId = p.getIndId();
        }
        // 营销中台价格编排：促销满减/折扣 与 会员等级折扣 互斥取优（采购方享有会员折扣）
        boolean memberEligible = user != null && user.getRoleId() != null && user.getRoleId() == 1L;
        PricingFacade.Quote quote = pricingFacade.quote(userId, total, orderIndId, memberEligible);
        order.setTotalAmount(quote.finalAmount());
        // 生成订单主表
        orderMapper.insert(order);

        // 明细 + 扣库存
        for (Cart c : carts) {
            Product p = productMapper.selectById(c.getProdId());
            BigDecimal subtotal = p.getPrice().multiply(BigDecimal.valueOf(c.getQuantity()));
            OrderItem item = new OrderItem();
            item.setOrderNo(order.getOrderNo());
            item.setProdId(p.getProdId());
            item.setProdName(p.getTitle());     // 冗余快照
            item.setPrice(p.getPrice());        // 冗余快照
            item.setQuantity(c.getQuantity());
            item.setSubtotal(subtotal);
            item.setCoverUrl(p.getCoverUrl());
            orderItemMapper.insert(item);
            // 扣库存 + 加销量
            p.setStock(p.getStock() - c.getQuantity());
            p.setSales(p.getSales() + c.getQuantity());
            p.setUpdateTime(LocalDateTime.now());
            productMapper.updateById(p);
            // 移除已购购物车项
            cartMapper.deleteById(c.getCartId());
        }
        messageService.send(userId, "order", "下单成功",
                "订单 " + order.getOrderNo() + " 已提交，待支付。", order.getOrderNo());
        return order;
    }

    public List<Order> myOrders(Long userId) {
        return orderMapper.selectList(Wrappers.<Order>lambdaQuery()
                .eq(Order::getUserId, userId).orderByDesc(Order::getCreateTime));
    }

    public OrderDetailVO detail(Long userId, String orderNo) {
        Order o = orderMapper.selectById(orderNo);
        if (o == null || !o.getUserId().equals(userId)) throw new BusinessException("订单不存在");
        OrderDetailVO vo = new OrderDetailVO();
        vo.setOrder(o);
        vo.setItems(orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderNo, orderNo)));
        return vo;
    }

    public Order pay(Long userId, String orderNo) {
        Order o = orderMapper.selectById(orderNo);
        if (o == null || !o.getUserId().equals(userId)) throw new BusinessException("订单不存在");
        if (o.getPayStatus() != null && o.getPayStatus() == 1) throw new BusinessException("订单已支付");
        o.setPayStatus(1);
        o.setPayTime(LocalDateTime.now());
        o.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(o);
        messageService.send(userId, "order", "支付成功",
                "订单 " + orderNo + " 已支付，商家将尽快发货。", orderNo);
        // 跨模块事件：通知营销中台（会员累计积分/成长值、消费活动自动参与）
        eventPublisher.publishEvent(new OrderPaidEvent(this, userId, orderNo, o.getTotalAmount()));
        // 身份平滑切换检测：支付后若累计已支付金额首次达标，采购方自动升级为 VIP（实时生效，无需重新登录）
        User u = userMapper.selectById(userId);
        if (u != null && u.getRoleId() != null && u.getRoleId() == 1L) {
            java.math.BigDecimal before = identityService.paidAmount(userId).subtract(o.getTotalAmount());
            if (before.compareTo(IdentityService.VIP_THRESHOLD) < 0
                    && identityService.paidAmount(userId).compareTo(IdentityService.VIP_THRESHOLD) >= 0) {
                auditService.record(userId, "IDENTITY_CHANGE", "身份升级: buyer -> vip（累计已支付达标）", null);
            }
        }
        return o;
    }

    public void cancel(Long userId, String orderNo) {
        Order o = orderMapper.selectById(orderNo);
        if (o == null || !o.getUserId().equals(userId)) throw new BusinessException("订单不存在");
        if (o.getPayStatus() != null && o.getPayStatus() == 1) throw new BusinessException("已支付订单不可取消");
        // 恢复库存
        List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderNo, orderNo));
        for (OrderItem it : items) {
            Product p = productMapper.selectById(it.getProdId());
            if (p != null) {
                p.setStock(p.getStock() + it.getQuantity());
                p.setSales(Math.max(0, p.getSales() - it.getQuantity()));
                productMapper.updateById(p);
            }
        }
        o.setOrderStatus(3);
        o.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(o);
        messageService.send(userId, "order", "订单已取消", "订单 " + orderNo + " 已取消，库存已恢复。", orderNo);
    }

    private String genOrderNo() {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return ts + ThreadLocalRandom.current().nextInt(100000, 999999);
    }
}
