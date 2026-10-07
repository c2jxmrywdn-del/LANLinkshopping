# 本地开发环境与 GitHub main 分支差异对比报告

> 生成时间：2026-10-07
> 本地仓库：`d:/桌面/创新创业/Spring boot毕业设计/LANLinkshopping`
> 远程：`origin = https://github.com/c2jxmrywdn-del/LANLinkshopping.git`
> 对比对象：本地工作区快照（HEAD） vs `origin/main`

## 1. 结论摘要

| 项目 | 值 |
| --- | --- |
| 本地对比前 HEAD | `05609db`（2026-10-05） |
| `origin/main` | `236514d`（2026-10-06） |
| 本地相对 main 的分叉 | **落后 232 个提交**（本地 HEAD 是 `origin/main` 的祖先，无独有提交） |
| 本次收尾后的本地快照 | `09d28b7`（docs）、`bd04aac`（代码+测试） |
| 相对 `origin/main` 的差异路径总数 | **129**（A 13 / M 63 / D 53） |
| 其中「**本次商户模块改动**」 | **28 个文件**（A 13 / M 15）→ 进入 PR |
| 其中「**本地基线落后于 main**」 | **101 个路径**（D 53 / M 48）→ **不进入 PR，禁止回退或删除** |

**关键判定**：本地并非「比 main 更新」，而是**基线落后 232 个提交**，只额外带了一套未提交的商户管理模块改动。
因此本报告与后续推送遵循：**保留 main 全部提交与独有文件，仅把本次 28 个文件的改动叠加到最新 main 之上**。

## 2. 对比方法与分类判定

### 2.1 数据采集命令

| 目的 | 命令 |
| --- | --- |
| 全量 A/M/D 状态 | `git diff --name-status origin/main..HEAD` |
| 行级增删 | `git diff --numstat origin/main..HEAD`、`git diff --numstat 05609db..HEAD` |
| 未跟踪文件 | `git ls-files --others --exclude-standard` |
| 本次改动集 | `git diff --name-only 05609db..HEAD`（自旧基线以来的本地改动） |
| main 现状核对 | `git ls-tree -r --name-only origin/main -- <path>`、`git show origin/main:<path>` |
| 落后提交数 | `git rev-list --count HEAD..origin/main` |
| 祖先关系 | `git merge-base --is-ancestor HEAD origin/main` |

### 2.2 分类判定算法

对每个相对 `origin/main` 存在差异的路径 `P`：

1. `git diff --quiet 05609db -- P` → 退出码 0（本地自旧基线以来**未改动** `P`）⇒ 归类为「**本地落后（main 领先）**」；
2. 退出码非 0（本地改过 `P`）⇒ 归类为「**本次改动**」，进入推送范围。

该算法保证：main 的 232 个提交所引入/更新的文件不会被误判为「本地改动」，从而避免在 PR 中回退或删除 main 的内容。

### 2.3 环境/工具文件处理

`.codebuddy/`（AI 编辑器规则与会话数据，含 `plan.json`/`plan.md` 内部计划文件）属本机工作区状态，
已追加到 `.gitignore`（新增段：`# AI 编辑器本地数据 … .codebuddy/`）予以排除，不进入任何提交。

## 3. 表 1：本次商户管理模块改动清单（28 个文件，进入 PR）

行数为相对旧基线 `05609db` 的增删统计（`git diff --numstat 05609db..HEAD`）。

| # | 文件 | 状态 | +行 | -行 | 变更性质 |
| --- | --- | :-: | --: | --: | --- |
| 1 | `.gitignore` | M | 3 | 0 | 追加 `.codebuddy/` 忽略段（含原因注释） |
| 2 | `backend/.../common/CryptoUtil.java` | A | 142 | 0 | AES-256-GCM 加解密工具：`enc:` 前缀密文、随机 IV、篡改/密钥不匹配拒绝、`mask` 脱敏、`isMasked` 掩码识别 |
| 3 | `backend/.../common/UserIdentity.java` | M | 11 | 0 | 新增 `MERCHANT_OPTIONAL_PERMS` 商户档可选权限白名单与判定说明 |
| 4 | `backend/.../config/AppCryptoProperties.java` | A | 18 | 0 | 加密密钥配置绑定（`lanlink.crypto.key` / `MERCHANT_CRYPTO_KEY`） |
| 5 | `backend/.../config/DataInitializer.java` | M | 5 | 2 | 示例企业统一社会信用代码改为加密写入（注入 `CryptoUtil`） |
| 6 | `backend/.../controller/MerchantController.java` | M | 101 | 14 | 新增运营端资料修改/注销/状态变更/权限查询与配置/税务号解密/分页筛选 6 类接口 |
| 7 | `backend/.../dto/MerchantStatusDTO.java` | A | 26 | 0 | 状态变更入参：`status` 1-3、`reason` 必填（Bean Validation） |
| 8 | `backend/.../dto/MerchantUpdateDTO.java` | A | 46 | 0 | 资料增量修改入参：全部字段可选 + 长度/数值约束 |
| 9 | `backend/.../entity/Merchant.java` | M | 3 | 1 | 新增 `status`（1正常/2冻结/3注销）、`permCodes` 字段；`taxRegNo` 注释更新 |
| 10 | `backend/.../entity/MerchantReviewLog.java` | A | 24 | 0 | 入驻与流程跟踪日志实体（`t_merchant_review_log`） |
| 11 | `backend/.../integration/security/PermInterceptor.java` | M | 40 | 2 | 商户级授权合并判定、冻结/注销即时失效、请求级缓存（单请求最多 1 次索引点查） |
| 12 | `backend/.../mapper/MerchantReviewLogMapper.java` | A | 9 | 0 | 流程跟踪 Mapper |
| 13 | `backend/.../service/MerchantService.java` | M | 487 | 10 | 资料 CRUD（含企业联动）、状态流转、注销与身份降级、权限配置与解析、敏感字段加密/脱敏/存量迁移、分页与关键词筛选；含此前未收尾的解冲突结果 |
| 14 | `backend/.../resources/application.yml` | M | 6 | 0 | 新增 `lanlink.crypto.key` 配置项（环境变量驱动） |
| 15 | `backend/.../resources/sql/schema.sql` | M | 115 | 2 | 幂等迁移：`t_merchant` 新增 `status`/`perm_codes` 列、3 个索引（`idx_merchant_user`/`_review_status`/`_status`）、敏感列扩长至 255；含解冲突结果 |
| 16 | `backend/.../test/common/CryptoUtilTest.java` | A | 133 | 0 | 加密工具 12 例：往返、密钥形态、明文兼容、随机 IV、篡改与密钥隔离、脱敏与掩码边界 |
| 17 | `backend/.../test/integration/security/MerchantAdminEndpointTest.java` | A | 139 | 0 | 接口契约 8 例：路径/HTTP 方法、`/admin/` 安全边界、审计所需请求参数、DTO 约束 |
| 18 | `backend/.../test/integration/security/PermInterceptorTest.java` | M | 62 | 2 | 扩充商户授权放行、权限回收/冻结 403、管理员旁路、非白名单不查档案等用例 |
| 19 | `backend/.../test/service/MerchantAdminServiceTest.java` | A | 570 | 0 | 服务层 34 例：CRUD/加密落库与掩码拒绝/状态流转/注销降级/权限白名单/有效权限解析/脱敏与迁移/分页筛选/审计调用 |
| 20 | `backend/.../test/service/MerchantServiceRoleUpgradeTest.java` | A→M | 116 | 0 | 适配新构造器（7 参）与 `review(merId, status, reason, operatorId)`；本地旧基线曾删除该文件，本次恢复为可运行版本 |
| 21 | `docs/商户管理模块接口文档.md` | A | 325 | 0 | 接口/错误码/数据模型/加密与脱敏/权限链路/审计/性能/验收/排查记录 |
| 22 | `docs/访问控制规则.md` | M | 20 | 2 | 补充「商户级可选权限」机制、契约测试条目与测试规模 |
| 23 | `frontend/src/api/index.js` | M | 12 | 1 | 新增运营端维护接口与 `/merchant/reapply`、`/merchant/admin/page` 封装 |
| 24 | `frontend/src/components/merchant/MerchantEditModal.vue` | A | 135 | 0 | 资料增量编辑弹窗：留空不改、掩码占位值提交前拦截、清空税务号绑定 |
| 25 | `frontend/src/components/merchant/MerchantPermModal.vue` | A | 104 | 0 | 权限配置面板：白名单勾选、冻结状态告警、即时生效 |
| 26 | `frontend/src/components/merchant/MerchantStatusModal.vue` | A | 69 | 0 | 冻结/恢复确认弹窗：后果说明 + 原因必填 |
| 27 | `frontend/src/views/MerchantApply.vue` | M | 10 | 2 | 接入驳回后「重新提交申请」链路（`/merchant/reapply`）与整改提示 |
| 28 | `frontend/src/views/admin/MerchantReview.vue` | M | 183 | 20 | 账户状态列与筛选、资料/权限/冻结/注销维护入口、详情抽屉扩展、表格服务端分页 |

**本次改动合计：新增 ≈ 2914 行，删除 ≈ 58 行，覆盖 28 个文件（后端 15 / 前端 6 / 测试 5 / 文档 2，含 `.gitignore`）。**

## 4. 表 2：本地落后于 main 的路径清单（101 个，不进入 PR）

> 成因：本地基线 `05609db` 落后 `origin/main` **232 个提交**。
> 处理口径：**全部保留**，不得在 PR 中回退或删除；移植时以 main 版本为准。

### 4.1 main 独有文件（本地缺失，53 个）

**部署与配置（3）**

```
backend/Dockerfile
backend/railway.json
backend/src/main/resources/application-prod.yml
```

**账期（credit）域后端（7）**

```
backend/.../controller/CreditTermController.java
backend/.../entity/CreditAccount.java
backend/.../entity/CreditBill.java
backend/.../entity/CreditRepayment.java
backend/.../mapper/CreditAccountMapper.java
backend/.../mapper/CreditBillMapper.java
backend/.../mapper/CreditRepaymentMapper.java
backend/.../service/CreditTermService.java
```

**测试（2）**

```
backend/.../test/service/IdentityServiceTest.java
backend/.../test/service/OrderDetailAdminTest.java
```

**前端资源与工具（3）**

```
frontend/public/assets/contact/whatsapp-qr.png
frontend/scripts/check-i18n-duplicates.mjs
frontend/src/styles/startup-gradient.css
frontend/src/utils/busy.js
frontend/src/utils/cookieConsent.js
```

**前端组件（9）**

```
frontend/src/components/AuthorCard.vue
frontend/src/components/BrandDecodeReveal.vue
frontend/src/components/BrandSymbol.vue
frontend/src/components/BrandWordmark.vue
frontend/src/components/BusinessBusyLoading.vue
frontend/src/components/CookieConsent.vue
frontend/src/components/IndustryCategoryMenu.vue
frontend/src/components/LanguageSwitcher.vue
```

**前端页面（25）**

```
frontend/src/views/AccountAddresses.vue
frontend/src/views/AccountLoginLog.vue
frontend/src/views/AccountMessages.vue
frontend/src/views/AccountSettings.vue
frontend/src/views/ActivityDetail.vue
frontend/src/views/ActivityMy.vue
frontend/src/views/ActivityParticipationResult.vue
frontend/src/views/ActivityRules.vue
frontend/src/views/CreditTermApply.vue
frontend/src/views/CreditTermBillDetail.vue
frontend/src/views/CreditTermBills.vue
frontend/src/views/CreditTermCenter.vue
frontend/src/views/MembershipBenefits.vue
frontend/src/views/MembershipLevels.vue
frontend/src/views/MembershipPointDetail.vue
frontend/src/views/MembershipPoints.vue
frontend/src/views/OrderDetail.vue
frontend/src/views/PrivacyCenter.vue
frontend/src/views/PromotionCenter.vue
frontend/src/views/PromotionDetail.vue
frontend/src/views/WalletRecharge.vue
frontend/src/views/WalletRechargeResult.vue
frontend/src/views/WalletTransactionDetail.vue
frontend/src/views/WalletTransactions.vue
frontend/src/views/admin/AdminCreditTerms.vue
frontend/src/views/admin/MerchantDetail.vue
frontend/src/views/admin/PaymentDetail.vue
```

> ⚠️ `frontend/src/views/admin/MerchantDetail.vue` 属 main 独有：main 已有独立商户详情页，
> 而本地版本没有该页面（本地把资料/权限/状态维护收敛进列表页与抽屉）。**移植时保留 main 的该页面与路由**。

### 4.2 main 已更新、本地未同步的文件（48 个）

**后端（19）**

```
README.md
backend/.../common/GlobalExceptionHandler.java
backend/.../config/WebConfig.java
backend/.../controller/CartController.java
backend/.../controller/OrderController.java
backend/.../controller/ProductController.java
backend/.../controller/TrafficController.java
backend/.../mapper/TrafficMapper.java
backend/.../module/activity/service/ActivityService.java
backend/.../module/membership/service/MembershipService.java
backend/.../payment/PaymentController.java
backend/.../payment/service/PaymentService.java
backend/.../service/CartService.java
backend/.../service/IdentityService.java
backend/.../service/OrderService.java
backend/.../service/TrafficService.java
backend/.../test/.../ActivityServiceTest.java
backend/.../test/.../PaymentServiceTest.java
backend/.../test/.../TrafficServiceTest.java
```

**前端（29）**

```
frontend/package.json
frontend/src/App.vue
frontend/src/api/request.js
frontend/src/components/BannerCarousel.vue
frontend/src/components/SplashScreen.vue
frontend/src/components/account/SettingsPanel.vue
frontend/src/i18n/index.js
frontend/src/i18n/index.test.js
frontend/src/layouts/AdminLayout.vue
frontend/src/layouts/MainLayout.vue
frontend/src/main.js
frontend/src/router/index.js
frontend/src/store/cart.js
frontend/src/styles/theme.css
frontend/src/views/AboutUs.vue
frontend/src/views/ActivityCenter.vue
frontend/src/views/Cart.vue
frontend/src/views/Checkout.vue
frontend/src/views/Home.vue
frontend/src/views/Login.vue
frontend/src/views/Mall.vue
frontend/src/views/Me.vue
frontend/src/views/MembershipCenter.vue
frontend/src/views/MerchantProducts.vue
frontend/src/views/MerchantTraffic.vue
frontend/src/views/MyOrders.vue
frontend/src/views/ProductDetail.vue
frontend/src/views/Wallet.vue
frontend/src/views/admin/AdminPayments.vue
```

> ⚠️ 其中 `frontend/src/i18n/index.js`（全站中英双语层）、`frontend/src/i18n/index.test.js`（i18n 重复键守卫）、
> `frontend/src/router/index.js`、`frontend/src/layouts/AdminLayout.vue`、`frontend/src/views/MerchantProducts.vue`、
> `frontend/src/views/MerchantTraffic.vue` 与本次商户模块改动**强相关**：移植时必须以 main 版本为基，
> 在其上叠加商户模块改动（新增文案复用 main 既有翻译键，不得重复）。

## 5. 统计汇总

| 维度 | 数量 | 说明 |
| --- | --- | --- |
| 相对 `origin/main` 差异路径 | 129 | A 13 / M 63 / D 53 |
| └ 本次商户模块改动 | 28 | A 13 / M 15，进入 PR |
| └ 本地落后于 main | 101 | D 53 / M 48，保留不动 |
| 本次改动新增行 | ≈ 2914 | 含 325 行接口文档、570 行服务层测试 |
| 本次改动删除行 | ≈ 58 | 主要为签名与调用点适配 |
| 未跟踪文件（排除忽略项前） | 217 | 其中绝大多数为 `.codebuddy/`，现已忽略 |

## 6. 推送范围界定

**进入 PR（新分支 `feature/merchant-admin-module`，基于最新 `origin/main`）**

- 表 1 全部 28 个文件的改动 + 本报告文件本身；
- 移植过程中为适配 main 基线所需的最小必要修改（例如在 main 的 i18n 层中补充商户文案键、在 main 的路由/布局中接入商户维护入口）；
- `.gitignore` 的 `.codebuddy/` 忽略段。

**明确排除**

- 表 2 的 101 个路径：main 独有文件（不得删除）与 main 的更新（不得回退）；
- `.codebuddy/` 全部内容；
- `stash@{0}`（`WIP on main: 236514d`）保留为本地回滚备份，不推送。

**安全约束**

- 不使用 `git push --force`；不直接向 `main` 推送；不使用 `reset --hard` / `clean -fd`；
- main 仅通过 PR 合并变更，合并动作由仓库维护者决定。

## 7. 验证证据

### 7.1 移植前（本地旧基线 `05609db` + 本次改动，即快照 `bd04aac`/`09d28b7`）

```
后端  mvn clean test  →  Tests run: 174, Failures: 0, Errors: 0, Skipped: 0  /  BUILD SUCCESS
前端  npm run build   →  ✓ built successfully（3407 modules transformed）
      npm test        →  3 files, 27 tests passed
```

### 7.2 移植后（最新 `origin/main` 基线 + 本次改动）

移植方式：`git checkout -b feature/merchant-admin-module origin/main` 后 `git cherry-pick` 上述 3 个快照提交。
共产生 4 处冲突（`MerchantService.java`、`schema.sql`、`MerchantServiceRoleUpgradeTest.java`、`MerchantReview.vue`），
全部按「保留 main 内容 + 叠加商户能力」解决（详见第 8 节）。cherry-pick 后新分支相对 `origin/main` 的净变更为
**29 个路径：A 14 / M 15 / D 0** —— 无任何删除，main 的 232 个提交与 101 个独有/更新路径全部保留。

```
后端  mvn clean test  →  Tests run: 182, Failures: 0, Errors: 0, Skipped: 0  /  BUILD SUCCESS
前端  npm run build   →  ✓ built successfully（3480 modules transformed）
      npm test        →  3 files, 28 tests passed（含 main 的 i18n 重复键守卫）
```

> 后端用例数由 174（旧基线）增至 182：差额来自 main 已有的额外测试（`IdentityServiceTest`、`OrderDetailAdminTest` 等）
> 与本模块新增用例的合并结果，全部通过；前端用例数由 27 增至 28 同为 main 侧新增用例。

### 7.3 合并冲突解决记录

| 文件 | 冲突性质 | 解决口径 |
| --- | --- | --- |
| `backend/.../service/MerchantService.java` | 6 处（import / 字段与构造器 / 兼容构造器 / `apply()` 审核历史 / `review()` 审核历史与签名） | 取本地版本：经 `git diff 05609db origin/main` 核验，main 自共同基线以来的 48 行改动（`userMapper` 注入 + 审核角色升降级）**已被本地版本完全覆盖且内容一致**，无丢失 |
| `backend/.../resources/sql/schema.sql` | 1 处（文件尾部建表位置重叠） | 保留双方：main 的流量/账期等建表全部保留，并追加本地 `t_merchant_review_log` 建表 |
| `backend/.../test/.../MerchantServiceRoleUpgradeTest.java` | add/add（main 亦有该文件） | 取本地版本：与 main 版本仅差 3 处签名适配（`CryptoUtil` 导入、7 参构造器、4 参 `review`） |
| `frontend/src/views/admin/MerchantReview.vue` | 2 处（操作列按钮组 / `<script setup>` 导入） | 保留双方：保留 main 的「快速查看 + 档案（跳转 `admin-merchant-detail`）」入口与 `useRouter`，同时叠加本地的资料/权限/冻结/注销入口与 `reactive` 分页状态 |

其余 24 个文件（含 `application.yml`、`api/index.js`、`MerchantController.java` 等）由 Git 三方合并自动完成，
其中 `application.yml` 同时保留了 main 的跨站 Session Cookie 配置与本地新增的 `lanlink.crypto` 密钥项。

## 8. 移植风险与处理口径

| 风险点 | 说明 | 处理口径 |
| --- | --- | --- |
| `MerchantService.java` 语义分叉 | main 的提交含 "fix: resolve merchant role upgrade"，其对审核后身份同步的设计可能与本地实现不同 | 以 main 设计为准逐段合并，保留商户模块新增能力，并以 `MerchantServiceRoleUpgradeTest` 回归 |
| 前端 i18n 层 | main 已有全站双语层与重复键守卫测试 | 复用 main 既有键；新增键不得重复，`npm test` 必须通过 |
| 独立商户详情页 | main 已有 `admin/MerchantDetail.vue` 与对应路由 | 保留 main 页面与路由；本地新增的维护弹窗与抽屉作为能力扩展接入 |
| 同名文件重复 | `MerchantReviewLog.java`、`MerchantReviewLogMapper.java` 等本地未跟踪文件 | 移植前用 `git ls-tree origin/main` 核对，main 已存在则以 main 版本为准 |
| 部署配置 | `Dockerfile`、`railway.json`、`application-prod.yml` 为 main 独有 | 一律保留，不因本地缺失而删除 |
