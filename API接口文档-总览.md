# API 接口文档 - 总览

## 项目信息

- **项目名称**: 多商家电商支付系统
- **Base URL**: `http://localhost:8080/api`
- **版本**: v1.0
- **认证方式**: Bearer Token (JWT)

## 文档结构

本项目的 API 接口文档按模块分为以下几个部分：

1. [商品管理模块](./API接口文档-商品管理.md)
2. [POS 收银模块](./API接口文档-POS收银.md)
3. [平台管理模块](./API接口文档-平台管理.md)
4. [订单管理模块](./API接口文档-订单管理.md)
5. [用户管理模块](./API接口文档-用户管理.md)
6. [积分管理模块](./API接口文档-积分管理.md)
7. [充值管理模块](./API接口文档-充值管理.md)
8. [提现管理模块](./API接口文档-提现管理.md)

## 快速开始

### 1. 获取 Token

```bash
curl -X POST http://localhost:8080/api/admin/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

响应：
```json
{
  "code": 200,
  "message": "登录成功",
  "data": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### 2. 使用 Token 访问接口

```bash
curl -X GET http://localhost:8080/api/product/list \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

## 统一响应格式

所有接口都遵循统一的响应格式：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

### 状态码说明

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权（未登录或 Token 过期） |
| 403 | 无权限访问 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

## 接口列表

### 商品管理模块

| 接口名称 | 方法 | 路径 | 说明 |
|---------|------|------|------|
| 商品列表 | GET | `/product/list` | 获取商品列表（分页） |
| 商品详情 | GET | `/product/{id}` | 获取商品详情 |
| 创建商品 | POST | `/product/create` | 创建新商品 |
| 更新商品 | PUT | `/product/update/{id}` | 更新商品信息 |
| 删除商品 | DELETE | `/product/delete/{id}` | 删除商品（软删除） |
| 商品搜索 | GET | `/product/search` | 搜索商品 |

### POS 收银模块

| 接口名称 | 方法 | 路径 | 说明 |
|---------|------|------|------|
| 添加到购物车 | POST | `/pos/cart/{sessionId}/add` | 添加商品到购物车 |
| 查询购物车 | GET | `/pos/cart/{sessionId}` | 查询购物车内容 |
| 更新数量 | PUT | `/pos/cart/{sessionId}/update` | 更新购物车商品数量 |
| 移除商品 | DELETE | `/pos/cart/{sessionId}/remove/{productId}` | 移除购物车商品 |
| 清空购物车 | DELETE | `/pos/cart/{sessionId}` | 清空购物车 |
| 结账 | POST | `/pos/checkout/{sessionId}` | 创建订单并结账 |
| 扫码 | POST | `/pos/scan` | 扫码添加商品 |

### 平台管理模块

| 接口名称 | 方法 | 路径 | 说明 |
|---------|------|------|------|
| 管理员登录 | POST | `/admin/login` | 管理员登录 |
| 管理员信息 | GET | `/admin/info` | 获取管理员信息 |
| 平台数据概览 | GET | `/admin/dashboard/stats` | 平台数据统计 |
| 商家注册趋势 | GET | `/admin/dashboard/merchant-trend` | 商家注册趋势图 |
| 平台销售趋势 | GET | `/admin/dashboard/sales-trend` | 平台销售趋势图 |
| 商家列表 | GET | `/admin/merchants` | 获取商家列表 |
| 商家详情 | GET | `/admin/merchant/{id}` | 获取商家详情 |
| 启用商家 | PUT | `/admin/merchant/{id}/enable` | 启用商家 |
| 禁用商家 | PUT | `/admin/merchant/{id}/disable` | 禁用商家 |
| 提现列表 | GET | `/admin/withdrawals` | 提现申请列表 |
| 审核通过 | PUT | `/admin/withdrawal/{id}/approve` | 审核通过提现 |
| 审核拒绝 | PUT | `/admin/withdrawal/{id}/reject` | 拒绝提现申请 |

### 订单管理模块

| 接口名称 | 方法 | 路径 | 说明 |
|---------|------|------|------|
| 订单列表 | GET | `/order/list` | 获取订单列表 |
| 订单详情 | GET | `/order/{orderNo}` | 获取订单详情 |
| 创建订单 | POST | `/order/create` | 创建订单 |
| 取消订单 | PUT | `/order/{orderNo}/cancel` | 取消订单 |
| 订单支付 | POST | `/order/{orderNo}/pay` | 支付订单 |

### 用户管理模块

| 接口名称 | 方法 | 路径 | 说明 |
|---------|------|------|------|
| 用户登录 | POST | `/user/login` | 用户登录 |
| 用户注册 | POST | `/user/register` | 用户注册 |
| 用户信息 | GET | `/user/info` | 获取用户信息 |
| 更新信息 | PUT | `/user/update` | 更新用户信息 |

### 积分管理模块

| 接口名称 | 方法 | 路径 | 说明 |
|---------|------|------|------|
| 积分余额 | GET | `/points/balance` | 获取积分余额 |
| 积分明细 | GET | `/points/log` | 积分变动明细 |
| 积分规则 | GET | `/points/rules` | 获取积分规则 |
| 兑换商品列表 | GET | `/points/exchange/products` | 积分兑换商品列表 |
| 兑换商品 | POST | `/points/exchange` | 兑换商品 |

### 充值管理模块

| 接口名称 | 方法 | 路径 | 说明 |
|---------|------|------|------|
| 充值规则 | GET | `/recharge/rules` | 获取充值规则 |
| 创建充值订单 | POST | `/recharge/create` | 创建充值订单 |
| 充值记录 | GET | `/recharge/records` | 充值记录列表 |

### 提现管理模块

| 接口名称 | 方法 | 路径 | 说明 |
|---------|------|------|------|
| 商家余额 | GET | `/merchant/balance` | 获取商家余额 |
| 申请提现 | POST | `/withdrawal/apply` | 申请提现 |
| 提现记录 | GET | `/withdrawal/records` | 提现记录列表 |

## 认证说明

### 获取 Token

通过登录接口获取 JWT Token：

```bash
POST /api/admin/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

### 使用 Token

在请求头中添加 Authorization 字段：

```
Authorization: Bearer {token}
```

### Token 过期

Token 默认有效期为 24 小时，过期后需要重新登录获取新的 Token。

## 错误处理

### 常见错误响应

#### 参数错误 (400)

```json
{
  "code": 400,
  "message": "商品名称不能为空",
  "data": null
}
```

#### 未授权 (401)

```json
{
  "code": 401,
  "message": "未登录或登录已过期",
  "data": null
}
```

#### 无权限 (403)

```json
{
  "code": 403,
  "message": "无权限访问",
  "data": null
}
```

#### 资源不存在 (404)

```json
{
  "code": 404,
  "message": "商品不存在",
  "data": null
}
```

#### 服务器错误 (500)

```json
{
  "code": 500,
  "message": "服务器内部错误",
  "data": null
}
```

## 测试工具

### Postman

推荐使用 Postman 进行接口测试：

1. 导入 Postman Collection（如果提供）
2. 设置环境变量：
   - `base_url`: http://localhost:8080/api
   - `token`: 登录后获取的 Token

### cURL

使用 cURL 命令行工具测试：

```bash
# 登录
curl -X POST http://localhost:8080/api/admin/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 获取商品列表
curl -X GET "http://localhost:8080/api/product/list?current=1&size=10" \
  -H "Authorization: Bearer {token}"
```

### Swagger UI

访问 Swagger UI 进行在线测试：

```
http://localhost:8080/swagger-ui.html
```

## 版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| v1.0 | 2024-01-01 | 初始版本 |

## 联系方式

如有问题，请联系开发团队。

## 附录

### 数据字典

详见 [数据库表结构说明.md](./数据库表结构说明.md)

### 部署说明

详见 [快速启动指南.md](./快速启动指南.md)

### Nginx 配置

详见 [NGINX部署说明.md](./NGINX部署说明.md)
