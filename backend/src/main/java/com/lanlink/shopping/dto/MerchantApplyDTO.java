package com.lanlink.shopping.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 商户入驻申请
 */
@Data
public class MerchantApplyDTO {
    @NotBlank(message = "企业名称不能为空")
    private String entName;
    private String creditCode;
    @NotBlank(message = "注册类型不能为空")
    private String regType;          // 公司 / 个体工商户
    @NotNull(message = "注册资本不能为空")
    private BigDecimal regCapital;   // 单位: 元
    @NotNull(message = "请选择是否有稳定纳税记录")
    private Integer taxStatus;       // 1稳定 0无
    private String joinType = "入驻"; // 加盟/入驻/邀约
    private String qNames;           // 已具备资质名称,逗号分隔(可选)
}
