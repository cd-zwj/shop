# 多租户 SaaS 商户支付与会员平台

三端合一的多租户支付系统：C 端用户下单支付、B 端商户经营管理、平台管理端监控审核。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java 21 / Spring Boot 3.5.15 / MyBatis-Plus 3.5.7 |
| 认证 | Sa-Token 1.45.0 + JWT，RBAC 五表权限模型 |
| 前端 | React 19 / TypeScript 5.8 / Vite 6.2 / Tailwind CSS 4 |
| 数据库 | MySQL 8 / Redis / Elasticsearch |
| 消息队列 | RabbitMQ + Outbox 模式 + 死信队列 + 补偿机制 |
| 支付 | 微信支付 V3 SDK / 支付宝 SDK / 6 种支付策略 |
| 存储 | MinIO / 阿里云 OSS |
| 监控 | Spring Boot Actuator + Prometheus + 结构化日志 |

## 快速启动

### 前置条件

- Java 21+
- Maven 3.8+
- Node.js 18+
- MySQL 8.0+
- Redis 6+

### 后端

```bash
cd payment-system

# 初始化数据库
mysql -u root -p < sql/import_all.sql

# 启动（默认 dev profile，Flyway 会自动执行 V1-V24 增量迁移）
mvn spring-boot:run

# API 文档（需开启 Swagger）
# 设置环境变量 APP_SWAGGER_ENABLED=true
# 访问 http://localhost:8080/api/swagger-ui.html
```

### 前端

```bash
cd salessystem

npm install
npm run dev

# 访问 http://localhost:5173
```

### Docker 部署

```bash
# 先创建 .env，并填写强密码；不可使用示例占位值
cp .env.example .env
docker-compose up -d
```

本地 Docker 默认关闭 Elasticsearch，商品搜索会降级到 MySQL 模糊查询。AI/Milvus、OAuth、微信支付、线上支付回调和真实短信服务需要额外第三方配置，本地开发可以不启用。

`sql/import_all.sql` 负责基础表和初始数据；后端启动时 Flyway 会按版本执行 `payment-system/src/main/resources/db/migration` 中的增量迁移，其中包括实体商品、门店库存、履约、评价和售后表结构。不要在生产环境手工跳过 Flyway。

## 本地模式能力边界

- 短信默认使用 mock provider，仅用于本地和测试环境。
- OAuth、微信支付、线上回调地址不在本地模式内。
- 支付宝真实联调需要配置密钥与可访问回调地址；普通本地开发可只验证内部支付单、订单和退款状态流转。

## 目录结构

```
├── payment-system/          # Java Spring Boot 后端
│   ├── src/main/java/       # 源码
│   ├── src/test/java/       # 测试
│   ├── sql/                 # 数据库迁移脚本（33 个）
│   └── pom.xml
├── salessystem/             # React 前端 SPA
│   ├── src/
│   │   ├── pages/           # 50+ 页面组件
│   │   ├── services/        # 28 个 API 模块
│   │   ├── context/         # AuthContext, CartContext, ToastContext
│   │   └── components/      # AuthGuard, RoleGuard, GuestGuard
│   └── package.json
├── docs/                    # 项目文档
├── docker-compose.yml
└── README.md
```

## 核心架构

### 多租户隔离

行级隔离：`tenant_id` 字段 + MyBatis-Plus `TenantLineInnerInterceptor`，所有业务 SQL 自动注入租户条件。

### 双钱包系统

- `unified_wallet_account`：全局统一钱包
- `merchant_wallet_account`：商户级钱包
- 6 种支付策略 + 乐观锁保障余额安全

### 消息可靠性

Outbox 模式 → RabbitMQ → Consumer，配合死信队列 + 补偿任务 + 幂等保障。

### 三端分层

| 前缀 | 端 | 说明 |
|------|-----|------|
| `V1App*` | C 端用户 | 下单、支付、钱包、优惠券 |
| `V1Merchant*` | B 端商户 | 商品管理、订单处理、财务提现 |
| `V1Admin*` | 平台管理 | 商户审核、交易监控、数据看板 |

## 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `SPRING_PROFILES_ACTIVE` | 运行环境 | `dev` |
| `DB_URL` | 数据库连接 | `localhost:3306/payment_db` |
| `DB_USERNAME` | 数据库用户 | — |
| `DB_PASSWORD` | 数据库密码 | — |
| `REDIS_HOST` | Redis 地址 | `localhost` |
| `RABBITMQ_HOST` | RabbitMQ 地址 | `localhost` |
| `AI_API_KEY` | AI 模型 API Key | — |
| `VITE_API_BASE_URL` | 前端 API 基础路径 | `http://localhost:8080/api` |

## 健康检查

```bash
# 健康检查
curl http://localhost:8080/api/actuator/health

# Prometheus 指标
curl http://localhost:8080/api/actuator/prometheus
```

## 测试

```bash
# 后端单元测试
cd payment-system && mvn test

# 前端类型检查
cd salessystem && npx tsc --noEmit
```

## 许可证

私有项目，未公开授权。
