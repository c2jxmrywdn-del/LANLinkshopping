package com.lanlink.shopping.controller;

import com.lanlink.shopping.common.R;
import com.lanlink.shopping.config.UserContext;
import com.lanlink.shopping.dto.CheckoutDTO;
import com.lanlink.shopping.entity.Order;
import com.lanlink.shopping.service.OrderService;
import com.lanlink.shopping.vo.OrderDetailVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单 (需登录)
 */
@RestController
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/checkout")
    public R<Order> checkout(@Valid @RequestBody CheckoutDTO dto, HttpServletRequest request) {
        return R.ok("下单成功", orderService.checkout(UserContext.currentUserId(request), dto));
    }

    @GetMapping("/my")
    public R<List<Order>> my(HttpServletRequest request) {
        return R.ok(orderService.myOrders(UserContext.currentUserId(request)));
    }

    @GetMapping("/detail/{orderNo}")
    public R<OrderDetailVO> detail(@PathVariable String orderNo, HttpServletRequest request) {
        return R.ok(orderService.detail(UserContext.currentUserId(request), orderNo));
    }

    @PostMapping("/pay/{orderNo}")
    public R<Order> pay(@PathVariable String orderNo, HttpServletRequest request) {
        return R.ok("支付成功", orderService.pay(UserContext.currentUserId(request), orderNo));
    }

    @PostMapping("/cancel/{orderNo}")
    public R<Void> cancel(@PathVariable String orderNo, HttpServletRequest request) {
        orderService.cancel(UserContext.currentUserId(request), orderNo);
        return R.ok("订单已取消", null);
    }
}
