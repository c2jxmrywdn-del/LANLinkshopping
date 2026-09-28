package com.lanlink.shopping.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

/**
 * 购物车结算下单
 */
@Data
public class CheckoutDTO {
    /** 结算的购物车行ID列表 */
    private List<Long> cartIds;
    @NotBlank(message = "收货人不能为空")
    private String receiver;
    @NotBlank(message = "联系电话不能为空")
    private String phone;
    @NotBlank(message = "收货地址不能为空")
    private String address;
    private String remark;
    /** 支付方式 balance 余额 / corporate 对公转账 / term 账期 */
    private String payType = "corporate";
}
