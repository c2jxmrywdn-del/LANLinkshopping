-- =====================================================
-- LANLinkshopping 聚合型B2B电商平台 数据库脚本
-- 库名: lanlink_shopping  字符集: utf8mb4  排序规则: utf8mb4_general_ci  引擎: InnoDB
--
-- 【幂等承诺】本脚本只做三件事: 建库 CREATE DATABASE IF NOT EXISTS,
--   建表 CREATE TABLE IF NOT EXISTS, 写字典 INSERT IGNORE 加显式主键。
--   全文不含 DROP DATABASE / DROP TABLE / TRUNCATE / DELETE 等任何会删库删表或清空业务数据的语句,
--   因此可以被反复执行: 第一次建好结构, 之后每次都只是跳过, 已产生的订单/商品/用户数据不会丢。
-- =====================================================
-- 【引导式执行顺序 · 从零到可运行】
--   第 1 步 创建数据库: 见本文件 第 1 步 段落, 一句 CREATE DATABASE IF NOT EXISTS 加一句 USE,
--          之后所有建表与插入语句都确定作用在 lanlink_shopping 上, 不依赖客户端是否已选库。
--   第 2 步 建表 共 11 张, 顺序按依赖关系排列(先字典后业务, 仅逻辑顺序, 无外键约束):
--          t_role 角色, t_enterprise 企业, t_user 用户, t_merchant 商户, t_qualification 资质,
--          t_industry 行业, t_category 分类, t_product 商品, t_cart 购物车, t_order 订单, t_order_item 订单明细。
--   第 3 步 插入基础字典数据: 角色 3 条, 行业 4 条, 分类 11 条,
--          全部显式指定主键并用 INSERT IGNORE, 保证重复执行不报错、不产生重复行, 且 ID 稳定
--          (后端 DataInitializer 与示例商品按 cat_id 1-11, ind_id 1-4 硬编码映射, 依赖这份稳定性)。
--   第 4 步 示例业务数据(用户/企业/商户/商品): 不在本脚本内, 由后端 DataInitializer 启动时写入,
--          密码用 BCrypt 加密, 仅当 t_user 为空时执行一次, 详见 README 第二节 演示账号。
-- =====================================================
-- 【两种执行方式 · 与 application-dev.yml 的实际配置一致】
--   方式 A 自动执行(推荐, dev 环境默认):
--     application-dev.yml 已配置 spring.sql.init.mode 为 always,
--     spring.sql.init.encoding 为 UTF-8, spring.sql.init.schema-locations 为 classpath 下的 sql/schema.sql,
--     所以只要启动后端(端口 8080, 上下文 /api), 本脚本的第 1 到第 3 步就会自动跑完, 无需手动导入。
--     关于谁来真正建库: 数据源 URL 上的 createDatabaseIfNotExist=true 才是自动路径下真正建库的那一步,
--     JDBC 驱动必须先连上(或先建好)目标库, 连接建立成功之后才轮到 spring.sql.init 执行本脚本,
--     因此脚本里的 CREATE DATABASE 在自动路径下是幂等的双保险, 执行时会直接跳过。
--     需要留意的是 createDatabaseIfNotExist=true 有一个前置条件: 当前账号必须能连上服务器,
--     且该参数只对 URL 中指定的那个库名生效; 若 URL 里库名被改成别的(例如换测试库),
--     请同步修改本脚本 CREATE DATABASE 与 USE 两行的库名, 或者删掉这两行完全交给 URL 决定。
--     需要手动先建库的情况只有两种: 一是 URL 去掉了 createDatabaseIfNotExist=true,
--     二是执行账号没有 CREATE DATABASE 权限而由 DBA 代为建库。
--   方式 B 手动导入(适合装环境、交给评审复现、或不想依赖自动执行时):
--     直接执行本脚本即可, 第 1 步语句会自己建库, 不需要提前手建; 仅当执行账号无建库权限时才需先请 DBA 建库。
--     命令行写法(PowerShell 与 CMD)以及 Navicat / DataGrip / IDEA Database 的图形化做法统一写在 README 第二节,
--     那里给了可直接复制的完整命令(注意 PowerShell 不支持 小于号 重定向, 需用 cmd /c 或管道方式)。
--   防中文乱码三件套: 本文件以 UTF-8 无 BOM 保存, 库表字符集为 utf8mb4,
--     客户端连接加 --default-character-set=utf8mb4, Spring 侧由 spring.sql.init.encoding 指定 UTF-8。
--     导入后若中文显示为问号或乱码, 优先检查文件是否被另存为 GBK 以及客户端字符集参数。
-- =====================================================

-- ==================== 第 1 步 创建数据库 ====================
CREATE DATABASE IF NOT EXISTS lanlink_shopping DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_general_ci;
USE lanlink_shopping;

-- ==================== 第 2 步 建表 共 11 张 ====================
-- 角色表
CREATE TABLE IF NOT EXISTS t_role (
  role_id   BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
  role_name VARCHAR(32) NOT NULL COMMENT '角色名称',
  role_code VARCHAR(32) NOT NULL COMMENT '角色编码 buyer/merchant/admin'
) ENGINE=InnoDB COMMENT='角色';

-- 企业表
CREATE TABLE IF NOT EXISTS t_enterprise (
  ent_id      BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '企业ID',
  name        VARCHAR(128) NOT NULL COMMENT '企业名称',
  credit_code VARCHAR(64)  COMMENT '统一社会信用代码',
  member_level INT DEFAULT 1 COMMENT '会员等级',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='企业';

-- 用户表
CREATE TABLE IF NOT EXISTS t_user (
  user_id     BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
  phone       VARCHAR(20) NOT NULL UNIQUE COMMENT '手机号(登录账号)',
  password    VARCHAR(100) NOT NULL COMMENT 'BCrypt密码',
  nickname    VARCHAR(64) COMMENT '昵称',
  role_id     BIGINT NOT NULL COMMENT '角色ID',
  ent_id      BIGINT COMMENT '所属企业ID',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     TINYINT DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB COMMENT='用户';

-- 商户表
CREATE TABLE IF NOT EXISTS t_merchant (
  mer_id        BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '商户ID',
  ent_id        BIGINT NOT NULL COMMENT '企业ID',
  user_id       BIGINT NOT NULL COMMENT '商户负责人用户ID',
  reg_type      VARCHAR(32) COMMENT '注册类型 公司/个体工商户',
  reg_capital   DECIMAL(14,2) COMMENT '注册资本(元)',
  tax_status    TINYINT DEFAULT 0 COMMENT '纳税记录 0未知 1稳定',
  join_type     VARCHAR(16) COMMENT '入驻方式 加盟/入驻/邀约',
  review_status TINYINT DEFAULT 0 COMMENT '审核状态 0待审 1通过 2驳回',
  reject_reason VARCHAR(255) COMMENT '驳回原因',
  create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted       TINYINT DEFAULT 0
) ENGINE=InnoDB COMMENT='商户';

-- 资质表
CREATE TABLE IF NOT EXISTS t_qualification (
  q_id        BIGINT PRIMARY KEY AUTO_INCREMENT,
  mer_id      BIGINT NOT NULL COMMENT '商户ID',
  q_type      VARCHAR(64) COMMENT '资质类型',
  q_name      VARCHAR(128) COMMENT '资质名称',
  file_url    VARCHAR(255) COMMENT '附件地址',
  expire_date DATE COMMENT '有效期至',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='商户资质';

-- 行业表
CREATE TABLE IF NOT EXISTS t_industry (
  ind_id      BIGINT PRIMARY KEY AUTO_INCREMENT,
  name        VARCHAR(64) NOT NULL COMMENT '行业名称',
  code        VARCHAR(32) NOT NULL COMMENT '行业编码',
  config_json TEXT COMMENT '行业配置(资质要求等)',
  sort        INT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='行业';

-- 分类表
CREATE TABLE IF NOT EXISTS t_category (
  cat_id    BIGINT PRIMARY KEY AUTO_INCREMENT,
  parent_id BIGINT DEFAULT 0 COMMENT '父级分类,0为顶级',
  ind_id    BIGINT COMMENT '所属行业ID',
  name      VARCHAR(64) NOT NULL,
  level     INT DEFAULT 1,
  sort      INT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='商品分类';

-- 商品表
CREATE TABLE IF NOT EXISTS t_product (
  prod_id        BIGINT PRIMARY KEY AUTO_INCREMENT,
  mer_id         BIGINT NOT NULL COMMENT '所属商户ID',
  cat_id         BIGINT COMMENT '分类ID',
  ind_id         BIGINT COMMENT '行业ID',
  title          VARCHAR(200) NOT NULL COMMENT '商品名称',
  brand          VARCHAR(64) COMMENT '品牌',
  spec           VARCHAR(128) COMMENT '规格',
  price          DECIMAL(12,2) NOT NULL COMMENT '销售单价',
  tier_price_json VARCHAR(500) COMMENT '阶梯价JSON [{qty,price}]',
  stock          INT DEFAULT 0 COMMENT '库存',
  cover_url      VARCHAR(255) COMMENT '主图',
  detail         TEXT COMMENT '详情',
  status         TINYINT DEFAULT 1 COMMENT '上架状态 1上架 0下架',
  sales          INT DEFAULT 0 COMMENT '销量',
  create_time    DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted        TINYINT DEFAULT 0
) ENGINE=InnoDB COMMENT='商品';

-- 购物车表
CREATE TABLE IF NOT EXISTS t_cart (
  cart_id     BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id     BIGINT NOT NULL,
  prod_id     BIGINT NOT NULL,
  quantity    INT DEFAULT 1,
  checked     TINYINT DEFAULT 1 COMMENT '是否勾选结算',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='购物车';

-- 订单表
CREATE TABLE IF NOT EXISTS t_order (
  order_no     VARCHAR(40) PRIMARY KEY COMMENT '订单号',
  user_id      BIGINT NOT NULL COMMENT '采购方用户ID',
  ent_id       BIGINT COMMENT '采购方企业ID',
  total_amount DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '订单总额',
  pay_type     VARCHAR(16) COMMENT '支付方式 balance/corporate/term(账期)',
  pay_status   TINYINT DEFAULT 0 COMMENT '0未支付 1已支付',
  order_status TINYINT DEFAULT 0 COMMENT '0待发货 1已发货 2已完成 3已取消',
  receiver     VARCHAR(64) COMMENT '收货人',
  phone        VARCHAR(20) COMMENT '联系电话',
  address      VARCHAR(255) COMMENT '收货地址',
  remark       VARCHAR(255) COMMENT '备注',
  create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
  pay_time     DATETIME,
  update_time  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='订单';

-- 幂等兜底：为历史库中已存在、无默认值的 t_order.total_amount 补 DEFAULT 0
-- （代码层已保证插入前写入总额；此处确保任何遗漏路径都不会整单失败）
SET @ord1 = (SELECT IF(COUNT(*) = 1 AND ISNULL(COLUMN_DEFAULT),
    'ALTER TABLE t_order MODIFY total_amount DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT ''订单总额''', 'SELECT 1')
    FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='lanlink_shopping' AND TABLE_NAME='t_order' AND COLUMN_NAME='total_amount');
PREPARE ord_stmt FROM @ord1; EXECUTE ord_stmt; DEALLOCATE PREPARE ord_stmt;

-- 订单明细表
CREATE TABLE IF NOT EXISTS t_order_item (
  item_id    BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no   VARCHAR(40) NOT NULL,
  prod_id    BIGINT NOT NULL,
  prod_name  VARCHAR(200) COMMENT '下单时商品名(冗余快照)',
  price      DECIMAL(12,2) COMMENT '下单时单价(冗余快照)',
  quantity   INT,
  subtotal   DECIMAL(14,2),
  cover_url  VARCHAR(255)
) ENGINE=InnoDB COMMENT='订单明细';

-- ==================== 第 3 步 基础字典数据 ====================
-- 显式主键加 INSERT IGNORE: 重复执行时主键冲突被忽略, 既不报错也不会插出重复行,
-- 同时把 ID 钉死, 与后端 DataInitializer 里按 cat_id 与 ind_id 的硬编码映射保持一致。
INSERT IGNORE INTO t_role (role_id, role_name, role_code) VALUES
 (1,'采购方','buyer'), (2,'商户','merchant'), (3,'平台运营','admin');

INSERT IGNORE INTO t_industry (ind_id, name, code, config_json, sort) VALUES
 (1,'建筑行业','construction','{"needQual":["建筑业企业资质证书","安全生产许可证"]}',1),
 (2,'纺织行业','textile','{"needQual":["纺织品检测报告"]}',2),
 (3,'石化行业','petrochemical','{"needQual":["危险化学品经营许可证"]}',3),
 (4,'电子行业','electronics','{"needQual":["3C认证","RoHS报告"]}',4);

INSERT IGNORE INTO t_category (cat_id, parent_id, ind_id, name, level, sort) VALUES
 (1,0,1,'建筑钢材',1,1),(2,0,1,'水泥混凝土',1,2),(3,0,1,'管材管件',1,3),
 (4,0,2,'面料坯布',1,1),(5,0,2,'纱线',1,2),(6,0,2,'辅料',1,3),
 (7,0,3,'基础化工原料',1,1),(8,0,3,'润滑油品',1,2),
 (9,0,4,'电子元器件',1,1),(10,0,4,'连接器线束',1,2),(11,0,4,'显示面板',1,3);

-- ==================== 第 4 步 交由后端完成 ====================
-- 本脚本到此结束。用户/企业/商户/商品等示例数据由 DataInitializer 在启动时以 BCrypt 写入,
-- 且只在 t_user 为空时执行一次, 所以重启后端不会重复插入, 也不会覆盖你改过的数据。
-- 执行后的自检 SQL 与预期结果写在 README 第二节 初始化数据库 小节。
