# 支付系统

一个完整的支付系统，包含用户端和管理端，支持微信支付和支付宝支付，集成AI数据分析模块。

## 技术栈

### 后端
- Spring Boot 2.7.14
- MyBatis Plus 3.5.3.1
- Redis
- MySQL 8.0
- Elasticsearch
- RabbitMQ
- JWT认证
- Swagger API文档

### 前端
- Vue 3
- Element Plus
- Vite
- Pinia
- Axios

## 项目结构

```
payment-system/
├── payment-system/          # 后端SpringBoot项目
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/payment/
│   │   │   │   ├── common/         # 通用类
│   │   │   │   ├── config/         # 配置类
│   │   │   │   ├── controller/     # 控制器
│   │   │   │   ├── dto/            # 数据传输对象
│   │   │   │   ├── entity/         # 实体类
│   │   │   │   ├── mapper/         # Mapper接口
│   │   │   │   ├── service/        # 服务层
│   │   │   │   └── util/           # 工具类
│   │   │   └── resources/
│   │   │       └── application.yml # 配置文件
│   │   └── sql/
│   │       └── payment_db.sql      # 数据库脚本
├── payment-frontend-user/    # 用户端前端
├── payment-frontend-admin/   # 管理端前端
└── API接口文档.md            # API接口文档
```

## 环境要求

- JDK 1.8+
- Maven 3.6+
- Node.js 16+
- MySQL 8.0+
- Redis 6.0+
- RabbitMQ 3.8+
- Elasticsearch 7.0+

## 快速开始

### 1. 数据库初始化

执行 `payment-system/sql/payment_db.sql` 脚本创建数据库和表。

### 2. 配置后端

修改 `payment-system/src/main/resources/application.yml` 中的配置：

- 数据库连接信息
- Redis连接信息
- RabbitMQ连接信息
- Elasticsearch连接信息
- 微信支付配置（app-id, mch-id, api-v3-key等）
- 支付宝支付配置（app-id, private-key, public-key等）
- AI模块地址（base-url）

### 3. 启动后端

```bash
cd payment-system
mvn clean install
mvn spring-boot:run
```

后端服务将在 `http://localhost:8080` 启动。

Swagger API文档地址：`http://localhost:8080/swagger-ui/index.html`

### 4. 启动前端

#### 用户端
```bash
cd payment-frontend-user
npm install
npm run dev
```

用户端将在 `http://localhost:3000` 启动。

#### 管理端
```bash
cd payment-frontend-admin
npm install
npm run dev
```

管理端将在 `http://localhost:3001` 启动。

## AI模块集成

AI模块需要提供以下接口：

**接口地址:** `POST {ai.base-url}/api/ai/analyze`

**请求格式:**
```json
{
  "analysisType": "USER_BEHAVIOR",
  "data": {
    "logs": [...]
  }
}
```

**响应格式:**
```json
{
  "analysisData": {
    "total": 100,
    "active": 80
  },
  "chartUrl": "http://example.com/charts/chart123.png"
}
```

## 功能特性

### 用户端功能
- 用户注册/登录
- 创建订单
- 微信/支付宝支付
- 订单查询
- 订单取消

### 管理端功能
- 管理员登录
- 订单管理
- 用户管理
- 数据分析（用户行为、支付趋势、用户分群）
- 数据可视化（AI生成图表）

## 支付流程

1. 用户创建订单
2. 选择支付方式（微信/支付宝）
3. 调用支付接口获取支付二维码或支付链接
4. 用户完成支付
5. 支付平台回调通知
6. 系统更新订单状态

## 数据分析流程

1. 管理员发起数据分析请求
2. 系统收集相关数据（用户行为日志、订单数据等）
3. 异步调用AI模块进行分析
4. AI模块返回分析结果和图表URL
5. 系统保存分析结果
6. 前端轮询获取分析结果并展示图表

## 默认账号

- 管理员账号：admin
- 管理员密码：admin123（需要在数据库中修改）

## 注意事项

1. 微信支付和支付宝支付需要配置真实的商户信息
2. AI模块需要单独实现，本系统提供集成接口
3. 生产环境需要修改JWT密钥
4. 建议使用HTTPS协议
5. 支付回调地址需要配置为公网可访问的地址

## 许可证

MIT License

