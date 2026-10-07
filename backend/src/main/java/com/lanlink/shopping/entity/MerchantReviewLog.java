package com.lanlink.shopping.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 商户审核历史：记录入驻全流程关键节点（提交/重新提交/通过/驳回），供流程跟踪与审计 */
@Data
@TableName("t_merchant_review_log")
public class MerchantReviewLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long merId;
    /** submit 提交申请 / resubmit 重新提交 / approve 审核通过 / reject 审核驳回 */
    private String action;
    /** 操作人 ID（审核动作为运营，申请动作为用户本人） */
    private Long operatorId;
    /** 原因/备注（驳回原因等） */
    private String reason;
    private LocalDateTime createTime;
}
