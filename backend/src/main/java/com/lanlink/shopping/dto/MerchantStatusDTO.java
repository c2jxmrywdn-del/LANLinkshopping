package com.lanlink.shopping.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 运营端商户账户状态变更入参。
 * 状态：1正常 2冻结 3注销（3 走注销逻辑：逻辑删除 + 商户身份降级）。
 * 状态变更属于高危操作，必须填写原因（写入入驻流程跟踪与审计日志）。
 */
@Data
public class MerchantStatusDTO {

    @NotNull(message = "请选择账户状态")
    @Min(value = 1, message = "账户状态不合法")
    @Max(value = 3, message = "账户状态不合法")
    private Integer status;

    @NotBlank(message = "状态变更必须填写原因")
    @Size(max = 200, message = "原因长度不能超过200")
    private String reason;
}
