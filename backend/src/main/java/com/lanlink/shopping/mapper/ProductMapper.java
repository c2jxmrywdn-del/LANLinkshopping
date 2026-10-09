package com.lanlink.shopping.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lanlink.shopping.entity.Product;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
public interface ProductMapper extends BaseMapper<Product> {
    @Update("UPDATE t_product SET stock = stock - #{quantity}, sales = COALESCE(sales, 0) + #{quantity}, " +
            "update_time = CURRENT_TIMESTAMP WHERE prod_id = #{productId} AND deleted = 0 AND status = 1 AND stock >= #{quantity}")
    int decreaseStock(@Param("productId") Long productId, @Param("quantity") int quantity);

    @Update("UPDATE t_product SET stock = COALESCE(stock, 0) + #{quantity}, sales = GREATEST(COALESCE(sales, 0) - #{quantity}, 0), " +
            "update_time = CURRENT_TIMESTAMP WHERE prod_id = #{productId} AND deleted = 0")
    int restoreInventory(@Param("productId") Long productId, @Param("quantity") int quantity);
}
