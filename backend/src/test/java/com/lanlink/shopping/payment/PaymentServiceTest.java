package com.lanlink.shopping.payment;

import com.lanlink.shopping.entity.Order;
import com.lanlink.shopping.mapper.OrderMapper;
import com.lanlink.shopping.payment.alipay.AlipayPayClient;
import com.lanlink.shopping.payment.dto.RefundRequest;
import com.lanlink.shopping.payment.service.PaymentLogService;
import com.lanlink.shopping.payment.service.PaymentService;
import com.lanlink.shopping.payment.vo.PayCreateVO;
import com.lanlink.shopping.payment.wallet.service.WalletService;
import com.lanlink.shopping.payment.wechat.WechatPayClient;
import com.lanlink.shopping.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 支付门面在 mock 模式下的路由与幂等结算（不触网、不连库） */
class PaymentServiceTest {

    private PaymentProperties props;
    private WechatPayClient wechat;
    private AlipayPayClient alipay;
    private OrderService orderService;
    private OrderMapper orderMapper;
    private PaymentLogService payLog;
    private WalletService walletService;
    private PaymentService svc;

    @BeforeEach
    void setUp() {
        props = new PaymentProperties();
        props.setMock(true);
        wechat = mock(WechatPayClient.class);
        alipay = mock(AlipayPayClient.class);
     ���q�^