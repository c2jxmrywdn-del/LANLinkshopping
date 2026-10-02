package com.lanlink.shopping;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan({"com.lanlink.shopping.mapper", "com.lanlink.shopping.module.*.mapper"})
public class LanlinkShoppingApplication {
    public static void main(String[] args) {
        SpringApplication.run(LanlinkShoppingApplication.class, args);
        System.out.println("==== LANLinkshopping 后端启动成功: http://localhost:8080/api ====");
    }
}
