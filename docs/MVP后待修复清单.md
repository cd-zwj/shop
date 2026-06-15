# MVP 后待修复清单

> 生成日期: 2026-06-15
> 说明: 核心支付流程已打通，以下为剩余需要修复/补齐的问题。

---

## 🔴 优先级 P0 — 线上风险

### 1. Redisson 分布式锁缺少 leaseTime（已核验未复现）

**文件**: `payment-system/src/main/java/com/payment/service/impl/RefundServiceImpl.java`

**问题**: `tryLock(30, TimeUnit.SECONDS)` 只设置了 waitTime，没有设置 leaseTime。持锁线程如果挂掉（OOM、宕机），锁永远不会释放，后续退款请求全部阻塞。

**修复方案**:
```java
// 改为 tryLock(waitTime, leaseTime, unit)
lock.tryLock(30, 60, TimeUnit.SECONDS);
```

**2026-06-15 核验结果**: 全仓未发现 `tryLock(30, TimeUnit.SECONDS)` 两参调用；当前 Redisson 锁封装已使用 `tryLock(waitTime, leaseTime, unit)`。该项按“未复现/无需代码修改”关闭。

---

### 2. 权限缓存无失效机制（已修复第一批）

**文件**: `payment-system/src/main/java/com/payment/config/StpInterfaceImpl.java`

**问题**: Sa-Token 的 `getPermissionList` / `getRoleList` 将权限缓存到 Session 中，但没有缓存失效机制。管理员修改用户权限后，用户必须重新登录才能生效。

**修复方案**:
- 方案 A: 每次请求都从数据库读取（性能换实时性）
- 方案 B: 使用 Redis 缓存 + TTL（如 5 分钟），修改权限时主动删除缓存 key
- 方案 C: 修改权限后调用 `SaSession.delete("sp:xxx")` 主动清除 Sa-Token Session 缓存

**2026-06-15 修复结果**: 已新增权限缓存失效服务，用户权限变更后统一清理 Sa-Token Session 中的 `permissions` 和 `roles`，并在清理失败时记录 warn 日志，不影响主事务。

---

## 🟠 优先级 P1 — 功能缺陷

### 3. 密码重置功能缺失

**前端**: `salessystem/src/pages/Login.tsx` 第 496 行 — "忘记密码"按钮只弹 toast `"密码重置功能开发中，请联系管理员"`

**后端**: `appAuthService` 中没有忘记密码/重置密码的 API 方法

**需要做的事**:
- [ ] 后端：新增 `POST /v1/app/auth/password/reset/send-code` — 发送重置验证码（邮箱或短信）
- [ ] 后端：新增 `POST /v1/app/auth/password/reset/verify` — 验证码校验 + 设置新密码
- [ ] 前端：新建 `ResetPassword.tsx` 页面（输入账号 → 收验证码 → 输入新密码 → 确认）
- [ ] 前端：Login.tsx "忘记密码"跳转到重置密码页

**注意**: 依赖短信/邮件服务可用（当前只有 MockSmsSender）

---

### 4. 会员等级只升不降

**文件**: `payment-system/src/main/java/com/payment/service/impl/MemberGrowthServiceImpl.java`

**问题**: `checkAndUpgradeLevel()` 只有升级逻辑，搜索不到任何 downgrade/降级相关代码。`MemberLevel` 实体没有定义降级条件或有效期。

**需要做的事**:
- [ ] `MemberLevel` 新增字段：`downgradeGrowth`（降级阈值）、`levelValidityDays`（等级有效期天数）
- [ ] 新增定时任务 `MemberLevelScheduler`：每月/季度检查，成长值不足的自动降级
- [ ] 成长值增加衰减机制：超过 N 天未消费的成长值按比例衰减（可选）

---

### 5. 积分无过期机制

**文件**: 无相关 Scheduler

**问题**: 积分有基本增减和兑换，但没有定时任务清理过期积分，积分永不过期。

**需要做的事**:
- [ ] `MemberPointsLog` 新增字段：`expireTime`（过期时间）
- [ ] 新增定时任务 `PointsExpireScheduler`：每日扫描过期积分，批量扣减
- [ ] 前端积分页面展示"即将过期积分"提醒

---

### 6. Controller 绕过 Service 层

**文件**:
- `V1AppCatalogController.java` — 直接注入 `TenantMapper`、`ProductMapper`
- `V1AppNotificationController.java` — 直接注入 `UserNotificationMapper`
- `V1MerchantProductController.java` — 主要 CRUD 逻辑在 Controller 内，直接操作 `ProductMapper`、`ProductStockMapper`

**修复方案**: 将 Mapper 操作下沉到 Service 层，Controller 只调用 Service 方法。

---

### 7. SQL 迁移编号冲突

**文件**: `payment-system/sql/` 目录

**问题**:
- `19_email_auth.sql` 和 `19_message_retry.sql` 共用编号 19
- `20_payment_bill_status_remark.sql` 和 `20_platform_auth_provider.sql` 共用编号 20
- 缺少 25 号迁移

**修复方案**: 重命名冲突文件，补齐缺失编号。建议统一迁移到 Flyway 管理。

---

### 8. docker-compose 缺少 Elasticsearch

**文件**: `docker-compose.yml`

**问题**: `application.yml` 配置了 ES 连接（spring.elasticsearch.uris），但 docker-compose 没有 ES 服务，`docker compose up` 会因连接 ES 失败而报错（虽然有降级，但日志会持续报错）。

**修复方案**: 在 docker-compose.yml 中添加 ES 服务，或在 application.yml 中将 ES 配置改为可选（`@ConditionalOnProperty`）。

---

## 🟡 优先级 P2 — 体验优化

### 9. 全局错误处理不一致

**问题**: 不同页面对 API 错误处理方式不统一：
- 部分用 `try-catch` + `showToast()`（CouponCenter、Points）
- 部分用 `try-catch` + `setError()`（AdminDashboard、Recharge）
- 部分 catch 中静默吞掉（Home.tsx、Discovery.tsx、Points.tsx）

**修复方案**:
- [ ] 制定统一的错误处理规范（推荐：页面级 try-catch + showToast，关键页面 + setError 展示重试按钮）
- [ ] 添加全局 `unhandledrejection` 事件监听
- [ ] 添加网络状态检测（offline/online）

---

### 10. 前端测试覆盖不足

**现状**: 133 个源文件，9 个测试文件，覆盖率约 7%

**优先补充的测试**:
- [ ] Cart.tsx — 支付方式选择、下单流程
- [ ] ProductDetails.tsx — 购买流程
- [ ] ErrorBoundary.tsx — 错误捕获验证
- [ ] AuthContext.tsx — 登录/登出/token 恢复
- [ ] orderCheckout.ts — buildOrderPayload 参数组合

---

### 11. 后端集成测试不足

**现状**: 53 个测试文件，集成测试仅 4 个，Contract 测试仅 5 个

**优先补充的测试**:
- [ ] 支付回调完整链路集成测试（下单→支付→回调→状态更新）
- [ ] 退款流程集成测试（申请→审核→退款→回调）
- [ ] DeadLetterRecoveryScheduler 集成测试
- [ ] 多租户隔离验证（确保 tenant_id 自动注入）

---

## 修复建议顺序

```
第一批（安全/稳定性）: #1 Redisson锁 → #2 权限缓存
第二批（核心功能）:   #3 密码重置 → #4 会员降级 → #5 积分过期
第三批（代码质量）:   #6 Controller重构 → #7 SQL迁移 → #8 docker-compose
第四批（体验/测试）:  #9 错误处理 → #10 前端测试 → #11 后端测试
```
