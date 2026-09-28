package com.lanlink.shopping.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_order")
public class Order {
    @TableId      // 主键为订单号(字符串)，非自增
    private String orderNo;
    private Long userId;
    private Long entId;
    private BigDecimal totalAmount;
    private String payType;
    private Integer payStatus;
    private Integer orderStatus;
    private String receiver;
    private String phone;
    private String address;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime payTime;
    private LocalDateTime updateTime;
}
