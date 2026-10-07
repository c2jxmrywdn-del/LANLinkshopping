package com.lanlink.shopping.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 运营端商户基本资料修改入参（字段均为可选，仅提交需要变更的项）。
 * 敏感字段（统一社会信用代码 creditCode、税务登记号 taxRegNo）由服务端加密存储。
 */
@Data
public class MerchantUpdateDTO {

    /** 企业名称（联动更新 t_enterprise） */
    @Size(max = 128, message = "企业名称长度不能超过128")
    private String entName;

    /** 统一社会信用代码（联动更新 t_enterprise，加密存储） */
    @Size(max = 64, message = "统一社会信用代码长度不能超过64")
    private String creditCode;

    /** 注册类型：公司 / 个体工商户 */
    @Size(max = 32, message = "注册类型长度不能超过32")
    private String regType;

    /** 注册资本（元） */
    @DecimalMin(value = "0", message = "注册资本不能为负数")
    private BigDecimal regCapital;

    /** 纳税记录：0未知 1稳定 */
    @Min(value = 0, message = "纳税记录状态不合法")
    @Max(value = 1, message = "纳税记录状态不合法")
    private Integer taxStatus;

    /** 入驻方式：加盟/入驻/邀约 */
    @Size(max = 16, message = "入驻方式长度不能超过16")
    private String joinType;

    /** 税务登记号（加密存储；传空串表示清空绑定） */
    @Size(max = 64, message = "税务登记号长度不能超过64")
    private String taxRegNo;
}
