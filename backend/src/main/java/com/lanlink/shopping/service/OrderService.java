package com.lanlink.shopping.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lanlink.shopping.common.BusinessException;
import com.lanlink.shopping.dto.CheckoutDTO;
import com.lanlink.shopping.entity.*;
import com.lanlink.shopping.mapper.*;
import com.lanlink.shopping.vo.OrderDetailVO;
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

    public OrderService(CartMapper cartMapper, ProductMapper productMapper, OrderMapper orderMapper,
                        OrderItemMapper orderItemMapper, UserMapper userMapper) {
        this.cartMapper = cartMapper;
        this.productMapper = productMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.userMapper = userMapper;
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

        BigDecimal total = BigDecimal.ZERO;
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

        // 先校验库存
        for (Cart c : carts) {
            Product p = productMapper.selectById(c.getProdId());
            if (p == null) throw new BusinessException("商品不存在: " + c.getProdId());
            if (p.getStock() < c.getQuantity()) throw new BusinessException("库存不足: " + p.getTitle());
        }
        // 生成订单主表
        orderMapper.insert(order);

        // 明细 + 扣库存
        for (Cart c : carts) {
            Product p = productMapper.selectById(c.getProdId());
            BigDecimal subtotal = p.getPrice().multiply(BigDecimal.valueOf(c.getQuantity()));
            total = total.add(subtotal);
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
        order.setTotalAmount(total);
        orderMapper.updateById(order);
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
    }

    private String genOrderNo() {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return ts + ThreadLocalRandom.current().nextInt(100000, 999999);
    }
}
