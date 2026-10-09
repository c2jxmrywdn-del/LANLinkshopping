package com.lanlink.shopping.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lanlink.shopping.entity.Order;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

public interface OrderMapper extends BaseMapper<Order> {

    /** 用户累计已支付订单金额（pay_status=1） */
    @Select("SELECT COALESCE(SUM(total_amount), 0) FROM t_order " +
            "WHERE user_id = #{userId} AND pay_status = 1")
    BigDecimal sumPaidAmount(@Param("userId") Long userId);
    @Update("UPDATE t_order SET order_status = 3, pay_status = CASE WHEN pay_type = 'term' THEN 2 ELSE pay_status END, " +
            "update_time = CURRENT_TIMESTAMP WHERE order_no = #{orderNo} AND user_id = #{userId} AND COALESCE(order_status, 0) <> 3 " +
            "AND (COALESCE(pay_status, 0) <> 1 OR pay_type = 'term')")
    int claimCancellation(@Param("orderNo") String orderNo, @Param("userId") Long userId);
}
