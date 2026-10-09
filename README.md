# LANLinkshopping · 聚合型一体多元化解决方案电商平台

> 软件技术 2404 · Spring Boot 毕业设计（Web 网站端 · 全栈核心 MVP）
> 技术栈：Spring Boot 3.5.3 + MyBatis-Plus 3.5.9 + MySQL 8 + Vue 3 + Vite + Ant Design Vue 4 + Pinia

本项目依据《LANLinkshopping 项目方案》与《Web 网站搭建方案》落地，实现五大核心业务模块：
**登录鉴权(RBAC) · 商户入驻与筛选 · 商品商城 · 购物车 · 下单支付**，并内置"链式业务拓展"设计。

---

## 一、目录结构

```
LANLinkshopping/
├── backend/                 # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/lanlink/shopping/
│       │   ├── common/      R、BusinessException、GlobalExceptionHandler
│       │   ├── config/      MybatisPlusConfig、WebConfig、AuthInterceptor、UserContext、DataInitializer
│       │   ├── controller/  Auth、Home、Product、Merchant、Cart、Order
│       │   ├── service/     同名业务服务（Order 含事务扣库存）
│       │   ├── mapper/      11 个 BaseMapper
│       │   ├── entity/      11 张表实体
│       │   ├── dto/ vo/     出入参对象
│       │   └── LanlinkShoppingApplication.java
│       └── resources/
│           ├── application.yml / application-dev.yml
│           └── sql/schema.sql        # 建库 + 建表 + 字典数据���q�^