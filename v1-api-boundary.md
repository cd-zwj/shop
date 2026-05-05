# v1 接口边界冻结说明

## 1. 命名空间冻结

当前阶段所有新增后端能力统一收口到以下命名空间：

- 用户端：`/v1/app/**`
- 商家端：`/v1/merchant/**`
- 管理端：`/v1/admin/**`
- 开放回调：`/v1/open/**`

旧接口只做兼容，不再继续承接新能力。

## 2. 鉴权方式冻结

### 用户端

- 登录入口：`/v1/app/auth/**`
- 登录主体：平台用户
- 访问方式：登录态 + 显式 `tenantId`

### 商家端

- 登录入口：`/v1/merchant/auth/**`
- 登录主体：平台用户登录后校验商家员工身份
- 访问方式：登录态 + 显式 `tenantId`
- 约束：经营类资源全部 `tenant-scoped`

### 管理端

- 登录入口：`/v1/admin/auth/**`
- 登录主体：平台管理员
- 访问方式：平台级登录态，不带 `tenantId`
- 约束：按权限码控制，不使用商家端 tenant 维度路径

## 3. 商家端资源命名

- 商品：`/v1/merchant/tenants/{tenantId}/products`
- 订单：`/v1/merchant/tenants/{tenantId}/orders`
- 资金与配置：
  - `/v1/merchant/tenants/{tenantId}/wallet-summary`
  - `/v1/merchant/tenants/{tenantId}/points-rule`
  - `/v1/merchant/tenants/{tenantId}/recharge-rules`
- 提现：
  - `/v1/merchant/tenants/{tenantId}/withdrawals`
  - `/v1/merchant/tenants/{tenantId}/withdrawals/balance`

统一约定：

- 集合资源使用复数名词
- 详情资源使用 `/{id}` 或稳定业务号
- 商家端不再新增 `/api/merchant/**` 风格接口

## 4. 管理端资源命名

### 认证

- `POST /v1/admin/auth/login`
- `GET /v1/admin/auth/session`
- `POST /v1/admin/auth/logout`

### 平台总览

- `GET /v1/admin/info`
- `GET /v1/admin/dashboard/overview`

### 商户管理

- `GET /v1/admin/merchants`
- `GET /v1/admin/merchants/{tenantId}`
- `POST /v1/admin/merchants`
- `PUT /v1/admin/merchants/{tenantId}`
- `PUT /v1/admin/merchants/{tenantId}/enable`
- `PUT /v1/admin/merchants/{tenantId}/disable`

### 用户管理

- `GET /v1/admin/users`
- `GET /v1/admin/users/{userId}`
- `PUT /v1/admin/users/{userId}/enable`
- `PUT /v1/admin/users/{userId}/disable`
- `GET /v1/admin/permissions`
- `GET /v1/admin/users/{userId}/permissions`
- `PUT /v1/admin/users/{userId}/permissions`
- `DELETE /v1/admin/users/{userId}/permissions/{permissionId}`

### 交易总览

- `GET /v1/admin/trades/overview`
- `GET /v1/admin/orders`
- `GET /v1/admin/orders/{orderNo}`
- `GET /v1/admin/payment-bills`
- `GET /v1/admin/recharge-orders`

### 提现审核

- `GET /v1/admin/withdrawals`
- `PUT /v1/admin/withdrawals/{withdrawalId}/approve`
- `PUT /v1/admin/withdrawals/{withdrawalId}/reject`

## 5. 权限边界

当前最小管理闭环对应的权限建议如下：

- 平台总览：`admin:dashboard`
- 商户管理：`admin:merchant:*`
- 用户管理：`admin:user:list`、`admin:user:update`、`admin:user:permission`
- 提现审核：`admin:withdrawal:*`
- 交易监控：`admin:trade:overview`、`admin:trade:list`、`admin:trade:detail`

为兼容已有权限库，管理端新增接口允许继续使用旧的 `admin:dashboard` 作为兜底权限，不强制一次性切断历史授权。

## 6. 旧接口兼容策略

以下旧接口继续保留兼容，但不再承接新增能力：

- `/admin/**`
- `/product/**`
- `/order/**`
- `/api/recharge/**`
- `/api/points/**`
- `/api/withdrawal/**`
- `/api/merchant/**`

冻结规则：

- 旧接口不删除
- 旧接口不继续扩展新能力
- 新联调、新页面、新文档一律优先使用 `v1` 路径

## 7. 当前最小范围

本轮收口只覆盖“最小可管理、可运营监控”的管理端基础闭环：

- 管理员登录与会话识别
- 商户管理
- 用户管理
- 平台总览
- 交易总览
- 提现审核

本轮不追求一次性铺满以下能力：

- 活动运营与优惠券配置
- 更细粒度的审计日志
- 全量财务报表
- AI 运营能力
- 管理端前端改造
