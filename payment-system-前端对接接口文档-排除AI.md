# 支付系统前后端对接接口文档（排除 AI 相关）

## 说明

本文档基于 `payment-system` 后端控制器代码整理，已按你的要求排除 AI 相关接口。排除规则采用“按路径/模块名排除”，因此以下内容 **未纳入**：

- `AiConrtoller`
- `DataAnalysisController`
- 明显用于 AI/聊天/分析推荐的 DTO、配置与工具类

保留了支付回调、订单、商户、钱包、积分、上传等非 AI 接口。

统一返回结构大多为：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": 1710000000000
}
```

其中：
- `code = 200` 表示成功
- `message` 为提示信息
- `data` 为实际业务数据
- `timestamp` 为服务端时间戳

## 鉴权说明

项目使用 Sa-Token 全局拦截。默认情况下接口需要登录，以下路径在配置中明确放行：

- `/user/login`
- `/user/register`
- `/admin/login`
- `/v1/app/auth/register`
- `/v1/app/auth/login/password`
- `/v1/app/auth/login/sms`
- `/v1/app/auth/login/third-party`
- `/v1/admin/auth/login`
- `/v1/merchant/auth/login`
- `/v1/open/payments/**`
- `/api/payment/notify/**`

另外，部分接口还额外使用：
- `@SaCheckLogin`：要求登录
- `@SaCheckPermission(...)`：要求具体权限

前端对接时建议：
- 登录成功后统一保存 token
- 非开放接口统一在请求头或 Sa-Token 约定方式中携带 token
- 管理端、商户端、用户端接口虽然都走同一套认证框架，但权限范围不同

## 一、v1 用户端接口（推荐优先对接）

### 1.1 用户认证 `/v1/app/auth`

#### 1）注册
- 方法：`POST`
- 路径：`/v1/app/auth/register`
- 鉴权：否（白名单）
- 请求体：`PlatformRegisterDTO`

```json
{
  "username": "string",
  "password": "string",
  "phone": "string",
  "email": "string"
}
```

- 返回：`PlatformUser`
- 说明：注册平台用户账号

#### 2）密码登录
- 方法：`POST`
- 路径：`/v1/app/auth/login/password`
- 鉴权：否（白名单）
- 请求体：`PlatformLoginDTO`

```json
{
  "username": "string",
  "password": "string"
}
```

- 返回：`string`
- 说明：返回登录 token

#### 3）短信登录
- 方法：`POST`
- 路径：`/v1/app/auth/login/sms`
- 鉴权：否（白名单）
- 请求体：`PlatformLoginDTO`
- 返回：`string`
- 说明：当前代码仍复用用户名密码登录 DTO，后续如果前端做短信登录要先和后端确认实际字段

#### 4）第三方登录
- 方法：`POST`
- 路径：`/v1/app/auth/login/third-party`
- 鉴权：否（白名单）
- 请求体：`PlatformLoginDTO`
- 返回：`string`
- 说明：当前代码同样复用用户名密码 DTO

#### 5）退出登录
- 方法：`POST`
- 路径：`/v1/app/auth/logout`
- 鉴权：是
- 请求参数：无
- 返回：`null`

### 1.2 当前用户 `/v1/app/users`

#### 1）获取当前登录用户
- 方法：`GET`
- 路径：`/v1/app/users/me`
- 鉴权：`@SaCheckLogin`
- 请求参数：无
- 返回：`PlatformUser`

### 1.3 商户/商品浏览 `/v1/app`

#### 1）商户列表
- 方法：`GET`
- 路径：`/v1/app/tenants`
- 鉴权：否
- 请求参数：无
- 返回：`List<Tenant>`
- 说明：只返回状态正常、未删除商户

#### 2）商户详情
- 方法：`GET`
- 路径：`/v1/app/tenants/{tenantId}`
- 鉴权：否
- 路径参数：`tenantId: Long`
- 返回：`Tenant`

#### 3）商户商品列表
- 方法：`GET`
- 路径：`/v1/app/tenants/{tenantId}/products`
- 鉴权：否
- 路径参数：`tenantId: Long`
- 返回：`List<Product>`
- 说明：仅返回未删除且上架商品

#### 4）商品详情
- 方法：`GET`
- 路径：`/v1/app/products/{productId}`
- 鉴权：否
- 路径参数：`productId: Long`
- 返回：`Product`

### 1.4 用户钱包/积分 `/v1/app`

#### 1）统一钱包信息
- 方法：`GET`
- 路径：`/v1/app/wallets/unified`
- 鉴权：`@SaCheckLogin`
- 返回：`WalletAccountVO`

```json
{
  "walletType": "string",
  "tenantId": 1,
  "availableAmount": 0,
  "frozenAmount": 0,
  "totalRecharge": 0,
  "totalConsume": 0
}
```

#### 2）统一钱包流水
- 方法：`GET`
- 路径：`/v1/app/wallets/unified/logs`
- 鉴权：`@SaCheckLogin`
- Query：`current`、`size`
- 返回：`Page<WalletLogVO>`

#### 3）创建统一钱包充值单
- 方法：`POST`
- 路径：`/v1/app/wallets/unified/recharges`
- 鉴权：`@SaCheckLogin`
- 请求体：`CreateUnifiedWalletRechargeDTO`

```json
{
  "amount": 100.00,
  "paymentChannelCode": "ALIPAY_PAGE"
}
```

- 返回：`RechargePaymentVO`

```json
{
  "rechargeNo": "string",
  "walletType": "string",
  "tenantId": 1,
  "rechargeAmount": 100.00,
  "giftAmount": 0,
  "giftPoints": 0,
  "paymentBillNo": "string",
  "externalPayUrl": "string"
}
```

#### 4）商户钱包信息
- 方法：`GET`
- 路径：`/v1/app/tenants/{tenantId}/wallet`
- 鉴权：`@SaCheckLogin`
- 返回：`WalletAccountVO`

#### 5）商户钱包流水
- 方法：`GET`
- 路径：`/v1/app/tenants/{tenantId}/wallet/logs`
- 鉴权：`@SaCheckLogin`
- Query：`current`、`size`
- 返回：`Page<WalletLogVO>`

#### 6）商户充值规则列表
- 方法：`GET`
- 路径：`/v1/app/tenants/{tenantId}/recharge-rules`
- 鉴权：`@SaCheckLogin`
- 返回：`List<MerchantRechargeRule>`

#### 7）创建商户钱包充值单
- 方法：`POST`
- 路径：`/v1/app/tenants/{tenantId}/wallet/recharges`
- 鉴权：`@SaCheckLogin`
- 请求体：`CreateMerchantWalletRechargeDTO`

```json
{
  "ruleId": 1,
  "paymentChannelCode": "ALIPAY_PAGE"
}
```

- 返回：`RechargePaymentVO`

#### 8）积分账户信息
- 方法：`GET`
- 路径：`/v1/app/tenants/{tenantId}/points`
- 鉴权：`@SaCheckLogin`
- 返回：`MemberPointsAccount`

#### 9）积分流水
- 方法：`GET`
- 路径：`/v1/app/tenants/{tenantId}/points/logs`
- 鉴权：`@SaCheckLogin`
- Query：`current`、`size`
- 返回：`Page<MemberPointsLog>`

### 1.5 用户订单 `/v1/app/orders`

#### 1）创建订单
- 方法：`POST`
- 路径：`/v1/app/orders`
- 鉴权：`@SaCheckLogin`
- 请求体：`AppCreateOrderDTO`

```json
{
  "tenantId": 1,
  "totalAmount": 99.90,
  "subject": "订单标题",
  "source": "APP",
  "items": [
    {
      "productId": 1,
      "quantity": 2,
      "price": 49.95
    }
  ],
  "walletStrategy": "EXTERNAL_ONLY",
  "paymentChannelCode": "ALIPAY_PAGE",
  "unifiedWalletAmount": 0,
  "merchantWalletAmount": 0,
  "allowExternalPayFallback": true
}
```

- 返回：`OrderPaymentVO`

```json
{
  "orderNo": "string",
  "orderStatus": "string",
  "payStatus": "string",
  "totalAmount": 99.90,
  "unifiedWalletDeductAmount": 0,
  "merchantWalletDeductAmount": 0,
  "externalPayAmount": 99.90,
  "paymentBillNo": "string",
  "externalPayUrl": "string"
}
```

#### 2）订单列表
- 方法：`GET`
- 路径：`/v1/app/orders`
- 鉴权：`@SaCheckLogin`
- Query：`current`、`size`
- 返回：`Page<SalesOrder>`

#### 3）订单详情
- 方法：`GET`
- 路径：`/v1/app/orders/{orderNo}`
- 鉴权：`@SaCheckLogin`
- 返回：`SalesOrderDetailVO`

#### 4）取消订单
- 方法：`POST`
- 路径：`/v1/app/orders/{orderNo}/cancel`
- 鉴权：`@SaCheckLogin`
- 返回：`null`

### 1.6 支付单 `/v1/app/payment-bills`

#### 1）支付单详情
- 方法：`GET`
- 路径：`/v1/app/payment-bills/{billNo}`
- 鉴权：`@SaCheckLogin`
- 返回：`PaymentBill`

#### 2）同步支付单状态
- 方法：`POST`
- 路径：`/v1/app/payment-bills/{billNo}/sync`
- 鉴权：`@SaCheckLogin`
- 返回：`PaymentBill`

## 二、v1 商户端接口（推荐优先对接）

### 2.1 商户认证 `/v1/merchant/auth`

#### 1）商户员工登录
- 方法：`POST`
- 路径：`/v1/merchant/auth/login`
- 鉴权：否（白名单）
- 请求体：`V1MerchantLoginDTO`

```json
{
  "username": "string",
  "password": "string"
}
```

- 返回：`V1MerchantSessionVO`

```json
{
  "token": "string",
  "expiresIn": 7200,
  "platformUserId": 1,
  "username": "string",
  "tenantId": 1,
  "tenantName": "string",
  "employeeRole": "string",
  "tenants": []
}
```

#### 2）获取当前商户会话
- 方法：`GET`
- 路径：`/v1/merchant/auth/me`
- 鉴权：`@SaCheckLogin`
- 返回：`V1MerchantSessionVO`

#### 3）退出登录
- 方法：`POST`
- 路径：`/v1/merchant/auth/logout`
- 鉴权：`@SaCheckLogin`
- 返回：`null`

### 2.2 商户商品管理 `/v1/merchant/tenants/{tenantId}/products`

说明：这组接口虽然未显式写 `@SaCheckLogin`，但仍受全局登录拦截，并且每次会校验 `requireEmployee(tenantId, currentUserId)`。

#### 1）商品分页列表
- 方法：`GET`
- 路径：`/v1/merchant/tenants/{tenantId}/products`
- 鉴权：登录 + 商户员工身份
- Query：
  - `current`
  - `size`
  - `search`
  - `category`
  - `status`（`active` / `inactive` / `out_of_stock`）
- 返回：`Page<V1MerchantProductVO>`

#### 2）商品详情
- 方法：`GET`
- 路径：`/v1/merchant/tenants/{tenantId}/products/{productId}`
- 鉴权：登录 + 商户员工身份
- 返回：`V1MerchantProductVO`

#### 3）新建商品
- 方法：`POST`
- 路径：`/v1/merchant/tenants/{tenantId}/products`
- 鉴权：登录 + 商户员工身份
- 请求体：`V1MerchantProductUpsertDTO`

```json
{
  "productCode": "PRD001",
  "name": "可乐",
  "price": 3.50,
  "unit": "瓶",
  "category": "饮料",
  "description": "冰镇可乐",
  "imageUrl": "https://...",
  "stock": 100,
  "status": "active"
}
```

- 返回：`V1MerchantProductVO`

#### 4）更新商品
- 方法：`PUT`
- 路径：`/v1/merchant/tenants/{tenantId}/products/{productId}`
- 鉴权：登录 + 商户员工身份
- 请求体：同上
- 返回：`V1MerchantProductVO`

#### 5）删除商品
- 方法：`DELETE`
- 路径：`/v1/merchant/tenants/{tenantId}/products/{productId}`
- 鉴权：登录 + 商户员工身份
- 返回：`null`

### 2.3 商户财务规则 `/v1/merchant/tenants/{tenantId}`

#### 1）钱包汇总
- 方法：`GET`
- 路径：`/v1/merchant/tenants/{tenantId}/wallet-summary`
- 鉴权：登录 + 商户员工身份
- 返回：`V1MerchantBalanceVO`

#### 2）积分规则详情
- 方法：`GET`
- 路径：`/v1/merchant/tenants/{tenantId}/points-rule`
- 鉴权：登录 + 商户员工身份
- 返回：`V1MerchantPointsRuleDTO`

#### 3）更新积分规则
- 方法：`PUT`
- 路径：`/v1/merchant/tenants/{tenantId}/points-rule`
- 鉴权：登录 + 商户员工身份
- 请求体：`V1MerchantPointsRuleDTO`
- 返回：`null`

#### 4）充值规则列表
- 方法：`GET`
- 路径：`/v1/merchant/tenants/{tenantId}/recharge-rules`
- 鉴权：登录 + 商户员工身份
- 返回：`List<MerchantRechargeRule>`

#### 5）替换充值规则
- 方法：`PUT`
- 路径：`/v1/merchant/tenants/{tenantId}/recharge-rules`
- 鉴权：登录 + 商户员工身份
- 请求体：`List<V1MerchantRechargeRuleDTO>`
- 返回：`null`

### 2.4 商户提现 `/v1/merchant/tenants/{tenantId}/withdrawals`

#### 1）查询商户余额
- 方法：`GET`
- 路径：`/v1/merchant/tenants/{tenantId}/withdrawals/balance`
- 鉴权：登录 + 商户员工身份
- 返回：`V1MerchantBalanceVO`

#### 2）提现列表
- 方法：`GET`
- 路径：`/v1/merchant/tenants/{tenantId}/withdrawals`
- 鉴权：登录 + 商户员工身份
- Query：`current`、`size`、`status`
- 返回：`Page<Withdrawal>`

#### 3）发起提现
- 方法：`POST`
- 路径：`/v1/merchant/tenants/{tenantId}/withdrawals`
- 鉴权：登录 + 商户员工身份
- 请求体：`WithdrawalApplyDTO`

```json
{
  "amount": 100.00,
  "bankName": "中国银行",
  "bankAccount": "6222...",
  "accountName": "张三"
}
```

- 返回：`Withdrawal`

### 2.5 商户订单 `/v1/merchant/tenants/{tenantId}/orders`

#### 1）订单列表
- 方法：`GET`
- 路径：`/v1/merchant/tenants/{tenantId}/orders`
- 鉴权：`@SaCheckLogin` + 商户员工身份
- Query：`current`、`size`、`orderStatus`、`payStatus`、`keyword`
- 返回：`Page<SalesOrder>`

#### 2）订单详情
- 方法：`GET`
- 路径：`/v1/merchant/tenants/{tenantId}/orders/{orderNo}`
- 鉴权：`@SaCheckLogin` + 商户员工身份
- 返回：`SalesOrderDetailVO`

## 三、v1 管理端接口（推荐优先对接）

### 3.1 管理员认证 `/v1/admin/auth`

#### 1）登录
- 方法：`POST`
- 路径：`/v1/admin/auth/login`
- 鉴权：否（白名单）
- 请求体：`V1AdminLoginDTO`

```json
{
  "username": "string",
  "password": "string"
}
```

- 返回：`string`（token）

#### 2）获取当前会话
- 方法：`GET`
- 路径：`/v1/admin/auth/session`
- 鉴权：是
- 返回：`V1AdminSessionVO`

#### 3）退出登录
- 方法：`POST`
- 路径：`/v1/admin/auth/logout`
- 鉴权：是
- 返回：`null`

### 3.2 管理端总览 `/v1/admin`

#### 1）管理员信息
- 方法：`GET`
- 路径：`/v1/admin/info`
- 鉴权：`admin:dashboard`
- 返回：`Map<String, Object>`

#### 2）总览统计
- 方法：`GET`
- 路径：`/v1/admin/dashboard/overview`
- 鉴权：`admin:dashboard`
- 返回：`AdminDashboardOverviewVO`

```json
{
  "totalPlatformUsers": 0,
  "totalMerchants": 0,
  "activeMerchants": 0,
  "totalOrders": 0,
  "paidOrders": 0,
  "totalOrderAmount": 0,
  "totalPaymentBills": 0,
  "totalPaymentAmount": 0,
  "totalRechargeOrders": 0,
  "totalRechargeAmount": 0,
  "pendingWithdrawals": 0
}
```

### 3.3 商户管理 `/v1/admin/merchants`

#### 1）商户分页列表
- 方法：`GET`
- 路径：`/v1/admin/merchants`
- 鉴权：`admin:merchant:list`
- Query：`current`、`size`、`name`、`status`
- 返回：`Page<MerchantListVO>`

#### 2）商户详情
- 方法：`GET`
- 路径：`/v1/admin/merchants/{tenantId}`
- 鉴权：`admin:merchant:detail`
- 返回：`MerchantDetailVO`

#### 3）创建商户
- 方法：`POST`
- 路径：`/v1/admin/merchants`
- 鉴权：`admin:merchant:create`
- 请求体：`MerchantDTO`

```json
{
  "tenantCode": "tenant001",
  "name": "演示商户",
  "contact": "张三",
  "phone": "13800000000",
  "address": "xx路xx号"
}
```

- 返回：`Tenant`

#### 4）更新商户
- 方法：`PUT`
- 路径：`/v1/admin/merchants/{tenantId}`
- 鉴权：`admin:merchant:update`
- 请求体：`MerchantDTO`
- 返回：`null`

#### 5）启用商户
- 方法：`PUT`
- 路径：`/v1/admin/merchants/{tenantId}/enable`
- 鉴权：`admin:merchant:enable`
- 返回：`null`

#### 6）禁用商户
- 方法：`PUT`
- 路径：`/v1/admin/merchants/{tenantId}/disable`
- 鉴权：`admin:merchant:disable`
- 返回：`null`

### 3.4 用户与权限 `/v1/admin`

#### 1）平台用户列表
- 方法：`GET`
- 路径：`/v1/admin/users`
- 鉴权：`admin:user:list` 或 `admin:dashboard`
- Query：`current`、`size`、`keyword`、`status`
- 返回：`Page<AdminPlatformUserVO>`

#### 2）平台用户详情
- 方法：`GET`
- 路径：`/v1/admin/users/{userId}`
- 鉴权：`admin:user:list` 或 `admin:dashboard`
- 返回：`AdminPlatformUserVO`

#### 3）启用用户
- 方法：`PUT`
- 路径：`/v1/admin/users/{userId}/enable`
- 鉴权：`admin:user:update` 或 `admin:dashboard`
- 返回：`null`

#### 4）禁用用户
- 方法：`PUT`
- 路径：`/v1/admin/users/{userId}/disable`
- 鉴权：`admin:user:update` 或 `admin:dashboard`
- 返回：`null`

#### 5）权限列表
- 方法：`GET`
- 路径：`/v1/admin/permissions`
- 鉴权：`admin:permission:list`
- 返回：`Map<String, List<Permission>>`

#### 6）获取用户权限
- 方法：`GET`
- 路径：`/v1/admin/users/{userId}/permissions`
- 鉴权：`admin:user:permission`
- 返回：`UserPermissionVO`

#### 7）设置用户权限
- 方法：`PUT`
- 路径：`/v1/admin/users/{userId}/permissions`
- 鉴权：`admin:user:permission`
- 请求体：`UserPermissionDTO`

```json
{
  "permissionIds": [1, 2, 3]
}
```

- 返回：`null`

#### 8）移除用户单个权限
- 方法：`DELETE`
- 路径：`/v1/admin/users/{userId}/permissions/{permissionId}`
- 鉴权：`admin:user:permission`
- 返回：`null`

### 3.5 交易/订单/支付单 `/v1/admin`

#### 1）交易总览
- 方法：`GET`
- 路径：`/v1/admin/trades/overview`
- 鉴权：`admin:trade:overview` 或 `admin:dashboard`
- 返回：`AdminTradeOverviewVO`

#### 2）订单列表
- 方法：`GET`
- 路径：`/v1/admin/orders`
- 鉴权：`admin:trade:list` 或 `admin:dashboard`
- Query：`current`、`size`、`orderNo`、`orderStatus`、`payStatus`、`tenantId`
- 返回：`Page<AdminOrderListVO>`

#### 3）订单详情
- 方法：`GET`
- 路径：`/v1/admin/orders/{orderNo}`
- 鉴权：`admin:trade:detail` 或 `admin:dashboard`
- 返回：`SalesOrderDetailVO`

#### 4）支付单列表
- 方法：`GET`
- 路径：`/v1/admin/payment-bills`
- 鉴权：`admin:trade:list` 或 `admin:dashboard`
- Query：`current`、`size`、`bizType`、`payStatus`、`channelCode`
- 返回：`Page<AdminPaymentBillVO>`

#### 5）充值单列表
- 方法：`GET`
- 路径：`/v1/admin/recharge-orders`
- 鉴权：`admin:trade:list` 或 `admin:dashboard`
- Query：`current`、`size`、`walletType`、`bizStatus`、`tenantId`
- 返回：`Page<AdminRechargeOrderVO>`

### 3.6 提现审核 `/v1/admin/withdrawals`

#### 1）提现审核列表
- 方法：`GET`
- 路径：`/v1/admin/withdrawals`
- 鉴权：`admin:withdrawal:list`
- Query：`current`、`size`、`merchantName`、`status`、`startDate`、`endDate`
- 返回：`Page<WithdrawalVO>`

#### 2）通过提现
- 方法：`PUT`
- 路径：`/v1/admin/withdrawals/{withdrawalId}/approve`
- 鉴权：`admin:withdrawal:approve`
- 返回：`null`

#### 3）拒绝提现
- 方法：`PUT`
- 路径：`/v1/admin/withdrawals/{withdrawalId}/reject`
- 鉴权：`admin:withdrawal:reject`
- 请求体：

```json
{
  "reason": "拒绝原因"
}
```

- 返回：`null`

## 四、开放支付回调接口

### 4.1 v1 开放支付回调 `/v1/open/payments`

这组接口已在全局白名单中放行，主要供第三方支付平台回调。

#### 1）通用支付回调
- 方法：`POST`
- 路径：`/v1/open/payments/callbacks/{channelCode}`
- 鉴权：否
- 请求体：`PaymentCallbackDTO`

```json
{
  "billNo": "string",
  "callbackRequestId": "string",
  "thirdPartyBillNo": "string",
  "success": true,
  "rawBody": "string"
}
```

- 返回：`null`

#### 2）充值回调
- 方法：`POST`
- 路径：`/v1/open/payments/callbacks/{channelCode}/recharge`
- 鉴权：否
- 请求体：`PaymentCallbackDTO`
- 返回：`null`

#### 3）订单回调
- 方法：`POST`
- 路径：`/v1/open/payments/callbacks/{channelCode}/order`
- 鉴权：否
- 请求体：`PaymentCallbackDTO`
- 返回：`null`

#### 4）支付宝页面回调
- 方法：`POST`
- 路径：`/v1/open/payments/callbacks/alipay-page`
- 鉴权：否
- 请求参数：表单参数 `out_trade_no`、`notify_id`、`trade_no`、`trade_status` 等
- 返回：`success` 字符串

#### 5）同步查询支付单状态
- 方法：`GET`
- 路径：`/v1/open/payments/bills/{billNo}/status`
- 鉴权：否
- 返回：`PaymentBill`

#### 6）支付宝页面跳转返回
- 方法：`GET`
- 路径：`/v1/open/payments/returns/alipay-page`
- 鉴权：否
- Query：`out_trade_no`、`trade_no`
- 返回：HTML 页面

#### 7）扩展支付渠道回调
- 方法：`GET`
- 路径：`/v1/open/payments/callbacks/ext-provider`
- 鉴权：否
- 请求参数：第三方回调参数
- 返回：`success` 字符串

### 4.2 旧版支付回调 `/payment`

#### 1）微信支付回调
- 方法：`POST`
- 路径：`/payment/wechat/notify`
- 鉴权：代码未见显式白名单，前端一般不直接调用
- 请求：第三方回调表单参数
- 返回：`SUCCESS` / `FAIL`

#### 2）支付宝支付回调
- 方法：`POST`
- 路径：`/payment/alipay/notify`
- 鉴权：代码未见显式白名单，前端一般不直接调用
- 请求：第三方回调表单参数
- 返回：`success` / `fail`

#### 3）支付宝同步跳转
- 方法：`GET`
- 路径：`/payment/alipay/return`
- 鉴权：否
- 返回：字符串“支付成功”

## 五、文件上传接口

### 5.1 文件上传 `/api/file`

#### 1）检查文件是否存在（秒传）
- 方法：`GET`
- 路径：`/api/file/check-exists`
- 鉴权：代码未见显式权限注解
- Query：
  - `fileMd5`
  - `fileName`
- 返回：`Map<String, Object>`

可能返回示例：

```json
{
  "exists": true,
  "fileUrl": "https://...",
  "message": "文件已存在，秒传成功"
}
```

#### 2）普通上传
- 方法：`POST`
- 路径：`/api/file/upload`
- 鉴权：代码未见显式权限注解
- Content-Type：`multipart/form-data`
- 表单字段：
  - `file`: 文件
  - `fileMd5`: 可选
- 返回：`string`（文件 URL）

#### 3）分片上传
- 方法：`POST`
- 路径：`/api/file/upload-chunk`
- 鉴权：代码未见显式权限注解
- Content-Type：`multipart/form-data`
- 表单字段：
  - `file`
  - `fileId`
  - `chunkNumber`
  - `totalChunks`
  - `fileMd5`
- 返回：`Map<String, Object>`

#### 4）上传进度查询
- 方法：`GET`
- 路径：`/api/file/upload-progress`
- 鉴权：代码未见显式权限注解
- Query：`fileId`
- 返回：`Map<String, Object>`

## 六、旧版接口（可作为兼容接口，建议谨慎对接）

下面这些接口与 v1 接口存在明显功能重叠，命名和返回结构相对老，建议你优先对接 v1；如果前端历史包袱较大，再考虑这些旧接口。

### 6.1 用户 `/user`
- `POST /user/login`：用户登录，返回 token
- `POST /user/logout`：退出登录
- `POST /user/register`：注册用户，直接提交 `User`
- `GET /user/info`：获取当前用户

### 6.2 订单 `/order`
- `POST /order/create`：创建订单，`CreateOrderDTO`
- `POST /order/pay`：发起支付，参数 `orderNo`
- `GET /order/query`：查询订单，参数 `orderNo`
- `POST /order/cancel`：取消订单，参数 `orderNo`

### 6.3 商品 `/product`
- `POST /product/create`：创建商品，`@ModelAttribute ProductDTO`
- `PUT /product/update/{id}`：更新商品，`@ModelAttribute ProductDTO`
- `DELETE /product/delete/{id}`：删除商品
- `GET /product/detail/{id}`：商品详情
- `GET /product/code/{productCode}`：按商品编码查询
- `GET /product/list`：商品列表，`keyword`、`category`
- `GET /product/page`：分页接口，目前代码里像是占位实现
- `GET /product/search`：商品搜索
- `GET /product/search/category`：分类搜索

### 6.4 商户 `/api/merchant`
- `POST /api/merchant/admin/create`
- `PUT /api/merchant/admin/update/{tenantId}`
- `POST /api/merchant/admin/enable/{tenantId}`
- `POST /api/merchant/admin/disable/{tenantId}`
- `GET /api/merchant/admin/list`
- `GET /api/merchant/detail/{tenantId}`

### 6.5 平台管理 `/api/admin`
- `POST /api/admin/login`
- `POST /api/admin/logout`
- `GET /api/admin/info`
- `GET /api/admin/dashboard/stats`
- `GET /api/admin/dashboard/merchant-trend`
- `GET /api/admin/dashboard/sales-trend`
- `GET /api/admin/merchants`
- `GET /api/admin/merchant/{id}`
- `PUT /api/admin/merchant/{id}/enable`
- `PUT /api/admin/merchant/{id}/disable`
- `GET /api/admin/withdrawals`
- `PUT /api/admin/withdrawal/{id}/approve`
- `PUT /api/admin/withdrawal/{id}/reject`
- `GET /api/admin/permissions`
- `GET /api/admin/user/{userId}/permissions`
- `POST /api/admin/user/{userId}/permissions`
- `DELETE /api/admin/user/{userId}/permissions/{permissionId}`

### 6.6 积分 `/api/points`
- `GET /api/points/rule`
- `POST /api/points/rule`
- `GET /api/points/balance`
- `GET /api/points/logs`
- `GET /api/points/exchange/products`
- `POST /api/points/exchange/{exchangeProductId}`
- `POST /api/points/exchange/product`
- `PUT /api/points/exchange/product/{id}`
- `DELETE /api/points/exchange/product/{id}`

### 6.7 提现 `/api/withdrawal`
- `GET /api/withdrawal/balance`
- `POST /api/withdrawal/apply`
- `GET /api/withdrawal/list`
- `GET /api/withdrawal/admin/list`
- `POST /api/withdrawal/admin/approve`

### 6.8 销售统计 `/api/sales`
- `GET /api/sales/overview`
- `GET /api/sales/trend`
- `GET /api/sales/product-rank`
- `GET /api/sales/export`

### 6.9 小程序 `/miniprogram`
- `POST /miniprogram/auth/login`
- `GET /miniprogram/auth/userinfo`
- `GET /miniprogram/product/list`
- `GET /miniprogram/product/detail/{id}`
- `GET /miniprogram/product/search`
- `POST /miniprogram/order/create`
- `GET /miniprogram/order/list`
- `GET /miniprogram/order/detail/{orderNo}`
- `POST /miniprogram/pay/create`
- `GET /miniprogram/pay/status`
- `GET /miniprogram/points/balance`
- `GET /miniprogram/points/exchange/list`
- `POST /miniprogram/points/exchange`
- `GET /miniprogram/recharge/rules`
- `POST /miniprogram/recharge/create`
- `GET /miniprogram/recharge/balance`

### 6.10 POS `/pos`
- `POST /pos/cart/{sessionId}/add`
- `DELETE /pos/cart/{sessionId}/remove/{productId}`
- `PUT /pos/cart/{sessionId}/update`
- `GET /pos/cart/{sessionId}`
- `DELETE /pos/cart/{sessionId}`
- `POST /pos/checkout/{sessionId}`

## 七、前端对接建议

1. 新功能优先对接 `v1/app`、`v1/merchant`、`v1/admin` 三套新接口。
2. 旧接口与 v1 接口有重叠，避免同一页面混用两套接口。
3. 充值、下单后如果返回 `externalPayUrl`，前端应直接跳转或拉起支付。
4. 支付结果页不要只依赖同步跳转页，应结合“支付单状态查询”接口轮询确认。
5. 商户端很多接口都要求 `tenantId`，前端登录后要保存当前选中的商户上下文。
6. 文件上传接口目前代码里没有明确权限注解，如需对外开放建议后端再确认鉴权策略。
7. `/product/page` 旧接口当前看起来是占位实现，前端不要直接依赖其分页结果。

## 八、本次排除的 AI 相关接口

本次未整理进文档的主要 AI 模块包括：
- `AiConrtoller`
- `DataAnalysisController`
- 与 chat / ai / model / prompt / analysis 强相关的配置、DTO、工具类

如果你要，我下一步可以继续帮你做两件事中的任意一种：
1. 把这份文档再整理成更规范的前端对接版（按“登录 / 商城 / 商户后台 / 管理后台”重新分组）
2. 直接输出一份 Apifox / Swagger 风格的表格版接口清单
