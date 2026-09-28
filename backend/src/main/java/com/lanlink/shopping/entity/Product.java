package com.lanlink.shopping.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_product")
public class Product {
    @TableId(type = IdType.AUTO)
    private Long prodId;
    private Long merId;
    private Long catId;
    private Long indId;
    private String title;
    private String brand;
    private String spec;
    private BigDecimal price;
    private String tierPriceJson;
    private Integer stock;
    private String coverUrl;
    private String detail;
    private Integer status;
    private Integer sales;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
