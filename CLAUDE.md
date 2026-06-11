# Multi-Tenant SaaS Payment Platform — Claude Code 指南

## 项目概述

多租户 SaaS 商户支付与会员平台，三端合一：
- **C 端用户**：下单、支付、钱包、优惠券、积分、成长值、退款
- **B 端商户**：商品管理、订单处理、财务提现、营销活动、会员管理
- **平台管理端**：商户审核、交易监控、数据看板、权限管理

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java 17 / Spring Boot 3.3.8 / MyBatis-Plus 3.5.5 |
| 认证 | Sa-Token 1.37.0 + JWT，RBAC 五表权限模型 |
| 前端 | React 19 / TypeScript 5.8 / Vite 6.2 / Tailwind CSS 4 |
| 数据库 | MySQL 8（~65 张表）/ Redis / Elasticsearch |
| 消息队列 | RabbitMQ + Outbox 模式 + 死信队列 + 补偿机制 |
| 支付 | 微信支付 V3 SDK / 支付宝 SDK / 6 种支付策略 |
| 存储 | MinIO / 阿里云 OSS |
| 分布式 | Redisson 分布式锁 / Druid 连接池 |
| 测试 | JUnit 5 + Mockito + H2（后端）/ Vitest（前端）|

## 目录结构

```
多租户系统/
├── payment-system/                 # Java Spring Boot 后端
│   ├── src/main/java/com/payment/
│   │   ├── common/                 # Result, 异常, 分页, 全局异常处理, TenantContextHolder
│   │   ├── config/                 # 18 个配置类（Sa-Token, RabbitMQ, Redis, ES, MinIO, MyBatis-Plus...）
│   │   ├── controller/             # 39 个 Controller
│   │   │   ├── V1App*             # C 端用户（12 个）
│   │   │   ├── V1Merchant*        # B 端商户（7 个）
│   │   │   ├── V1Admin*           # 平台管理（10 个）
│   │   │   └── Open*/Legacy*      # 开放接口 / 遗留接口
│   │   ├── entity/                 # 73 个实体
│   │   ├── dto/                    # ~100 个 DTO/VO
│   │   ├── enums/                  # 25 个枚举
│   │   ├── mapper/                 # 66 个 MyBatis-Plus Mapper
│   │   ├── service/                # 48 个 Service 接口
│   │   ├── service/impl/           # 46+ 个 Service 实现
│   │   ├── consumer/               # 5 个 RabbitMQ Consumer
│   │   ├── interceptor/            # 请求拦截器
│   │   ├── aspect/                 # RateLimitAspect（Redis 限流）
│   │   └── netty/                  # WebSocket 服务（端口 8888）
│   ├── src/main/resources/
│   │   ├── application.yml         # 主配置（端口 8080，context-path /api）
│   │   └── application-dev.yml     # 开发环境配置
│   └── sql/                        # 33 个增量迁移脚本
│
├── salessystem/                    # React 前端 SPA
│   ├── src/
│   │   ├── pages/                  # 50+ 页面组件
│   │   ├── services/               # 28 个 API 模块
│   │   ├── context/                # AuthContext, CartContext, ToastContext
│   │   ├── components/             # AuthGuard, RoleGuard, GuestGuard
│   │   └── types/                  # 15 个 TypeScript 类型定义
│   └── package.json
│
├── docs/                           # 项目文档（架构、前端规范、实施计划等）
└── .agents/skills/                 # ECC 技能（61 个，含 Spring Boot 专用）
```

## 核心架构模式

### 多租户隔离
- 行级隔离：`tenant_id` 字段 + MyBatis-Plus `TenantLineInnerInterceptor`
- `TenantContextHolder` 维护当前租户上下文
- **所有业务 SQL 自动注入 tenant_id 条件**

### 双钱包系统
- `unified_wallet_account`：全局统一钱包
- `merchant_wallet_account`：商户级钱包
- 6 种支付策略：NO_WALLET / UNIFIED_ONLY / MERCHANT_ONLY / MERCHANT_THEN_UNIFIED / UNIFIED_THEN_MERCHANT / CUSTOM_SPLIT
- 乐观锁 `@Version` 保障余额安全

### 消息可靠性
- Outbox 模式：支付回调 → message_outbox → RabbitMQ → Consumer
- 死信队列 + 补偿任务：`dead_letter_task` / `compensation_task` / `retry_task`
- 幂等保障：`message_idempotent` 表

### 控制器三端分层
| 前缀 | 端 | 示例 |
|------|-----|------|
| `V1App*` | C 端用户 | V1AppAuthController, V1AppOrderController |
| `V1Merchant*` | B 端商户 | V1MerchantProductController, V1MerchantFinanceController |
| `V1Admin*` | 平台管理 | V1AdminDashboardController, V1AdminTradeController |

## AI 团队编排

### 自动触发规则

| 场景 | 触发 Agent | 说明 |
|------|-----------|------|
| 复杂功能需求 | `planner` | 拆解任务、制定实施计划 |
| 架构决策 | `architect` | 技术选型、系统设计 |
| 代码刚写完/修改 | `code-reviewer` + `java-reviewer` | **强制触发**，每次代码变更后 |
| 安全敏感代码 | `security-reviewer` | **强制触发**，见下方安全模块清单 |
| 新功能/修 Bug | `tdd-guide` | 测试先行，RED → GREEN → REFACTOR |
| 构建失败 | `java-build-resolver` / `react-build-resolver` | 自动修复编译错误 |
| SQL/Schema 变更 | `database-reviewer` | 查询优化、索引策略 |
| 性能问题 | `performance-optimizer` | N+1 查询、缓存策略 |
| 功能完成后 | `doc-updater` | 接口文档、架构文档同步 |
| 死代码清理 | `refactor-cleaner` | 技术债清理 |

### 安全审查强制触发模块

以下模块涉及认证、资金、数据隔离，任何变更**必须**触发 `security-reviewer`：

| 模块 | 关键文件 | 风险等级 |
|------|---------|---------|
| **认证鉴权** | V1App*Auth*, V1Merchant*Auth*, V1Admin*Auth*, Sa-TokenConfig, JwtService | 🔴 CRITICAL |
| **支付核心** | PaymentBill*, OpenPaymentController, PaymentCallbackRecord | 🔴 CRITICAL |
| **双钱包** | V1AppWallet*, unified_wallet_*, merchant_wallet_*, BalanceService | 🔴 CRITICAL |
| **多租户隔离** | TenantLineInnerInterceptor, TenantContextHolder, MyBatisPlusConfig | 🔴 CRITICAL |
| **退款提现** | V1AppRefund*, V1MerchantRefund*, V1AdminWithdrawal*, RefundService | 🟠 HIGH |
| **优惠券/营销** | CouponTemplate, UserCoupon, MarketingActivity, CouponLockRecord | 🟠 HIGH |
| **RBAC 权限** | sys_role, sys_permission, sys_user_role, tenant_employee | 🟠 HIGH |
| **短信/验证码** | SmsVerifyCode, LoginFailRecord, CaptchaController | 🟡 MEDIUM |

## 编码规范

### Java 后端
- 遵循 `springboot-patterns` 技能：Controller → Service → Mapper 三层分离
- DTO 与 Entity 严格分离，Controller 层使用 VO，Service 层使用 DTO
- `@Transactional` 仅在 Service 层，禁止在 Controller
- 乐观锁 `@Version` 用于所有涉及余额/库存的操作
- 异常统一通过 `BusinessException` + `GlobalExceptionHandler` 处理
- 禁止在 Entity 上直接使用 `@TableName` 以外的业务注解

### React 前端
- 遵循 TypeScript 严格模式，禁止 `any`
- API 调用统一通过 `services/` 层，组件内禁止直接 fetch
- 路由守卫三层：AuthGuard → RoleCheck → GuestGuard
- 状态管理：Context API，禁止 prop drilling 超过 3 层
- 样式：Tailwind CSS 原子类，禁止内联 style 对象

### Git 提交
```
<type>(<scope>): <description>

type: feat | fix | refactor | docs | test | chore | perf | ci
scope: app | merchant | admin | wallet | order | payment | coupon | mq | ...
示例: feat(wallet): 新增商户钱包余额查询接口
示例: fix(mq): 修复死信队列消息丢失问题
```

## 测试要求

- **覆盖率下限**：80%
- **后端**：JUnit 5 + Mockito 单元测试 + H2 集成测试，参考 `springboot-tdd` 技能
- **前端**：Vitest 单元测试
- **关键路径必测**：支付流程、钱包扣款、优惠券核销、退款流程、多租户隔离

## 工作流

```
需求输入 → [story-generator] 用户故事拆解
         → [planner] 实施计划
         → [architect] 技术方案评审（如需）
                    ↓
         [tdd-guide] 测试先行（RED）
         编码实现（GREEN）
         [refactor-cleaner] 重构（IMPROVE）
                    ↓
         [code-reviewer + java-reviewer] 代码审查 ← 强制
         [security-reviewer] 安全审查 ← 涉及安全模块时强制
                    ↓
         [doc-updater] 文档同步
         [performance-optimizer] 性能检查（按需）
                    ↓
         git commit → git push
```

## 禁止事项

- ❌ 禁止硬编码密钥、Token、密码（使用环境变量或 application-dev.yml）
- ❌ 禁止 Controller 直接调用 Mapper（必须经过 Service 层）
- ❌ 禁止绕过多租户拦截器（TenantLineInnerInterceptor）
- ❌ 禁止在资金操作中跳过乐观锁
- ❌ 禁止 Entity 暴露到前端接口（必须通过 VO 转换）
- ❌ 禁止 console.log / System.out.println 调试代码残留
- ❌ 禁止 SQL 字符串拼接（必须使用参数化查询）
