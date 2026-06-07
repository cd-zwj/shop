# 前端页面规格说明书 (Wave 1)

> 最后更新：2026-06-07
> 分支：codex-dual-wallet-v1
> 适用目录：`salessystem/`
> 说明：前端页面由你自行实现，本文档只提供页面清单、功能规格和后端API对照

---

## 目录

1. [路由守卫（3个组件）](#1-路由守卫)
2. [优惠券中心 - 用户端](#2-优惠券中心)
3. [积分中心 - 用户端](#3-积分中心)
4. [Profile 死链路修复](#4-profile-死链路修复)
5. [Home 分类入口功能化](#5-home-分类入口功能化)
6. [商户端 - 优惠券模板管理](#6-商户端---优惠券模板管理) *(Wave 2 预留)*
7. [商户端 - 促销活动管理](#7-商户端---促销活动管理) *(Wave 2 预留)*
8. [商户端 - 会员等级/标签管理](#8-商户端---会员等级标签管理) *(Wave 2 预留)*
9. [管理端 - 平台营销运营](#9-管理端---平台营销运营) *(Wave 2 预留)*

---

## 1. 路由守卫

### 需要新建的组件

| 组件 | 路径 | 职责 |
|------|------|------|
| `AuthGuard` | `src/components/guards/AuthGuard.tsx` | 检查 token 是否存在，未登录 → `/login` |
| `RoleGuard` | `src/components/guards/RoleGuard.tsx` | 检查当前用户角色是否匹配，无权限 → 对应首页 |
| `GuestGuard` | `src/components/guards/GuestGuard.tsx` | 已登录用户访问 login/register → `/` |

### App.tsx 路由改造

```
/login          → GuestGuard → Login
/register       → GuestGuard → Register

/user/*         → AuthGuard → 用户页面
/               → AuthGuard → Home
/discovery      → AuthGuard → Discovery
/product/:id    → AuthGuard → ProductDetail
/cart           → AuthGuard → Cart
/orders         → AuthGuard → OrderList
/order/:orderNo → AuthGuard → OrderDetail
/profile        → AuthGuard → Profile
/coupons        → AuthGuard → CouponCenter
/points         → AuthGuard → Points

/merchant/*     → AuthGuard + RoleGuard(merchant) → 商户页面
/admin/*        → AuthGuard + RoleGuard(admin) → 管理页面
```

### AuthGuard 逻辑

```
1. 从 AuthContext 获取 currentUser
2. 从 token util 检查 token 是否存在
3. 有 token 但无 currentUser → 尝试 GET /v1/app/users/me 恢复
4. 无 token → redirect /login
5. 有 token + currentUser → 渲染 children
```

### RoleGuard 逻辑

```
Props: allowedRoles: AuthRole[]
1. 从 AuthContext 获取 currentUser.role
2. role 不在 allowedRoles 中 → redirect 到该角色的默认首页
3. 匹配 → 渲染 children
```

---

## 2. 优惠券中心

### 路由
`/coupons` → `CouponCenter.tsx`

### 页面布局

```
┌─────────────────────────────────────┐
│  ← 优惠券中心                        │
├────────┬────────┬───────────────────┤
│ 可领取  │ 我的券  │ 已过期            │  ← Tab 切换
├────────┴────────┴───────────────────┤
│                                     │
│  ┌─────────────────────────────────┐│
│  │ ¥10     │ 满减券                ││
│  │ 满100可用│ XX商户                ││
│  │         │ 2026.06.01-2026.07.01 ││
│  │         │        [领取]         ││  ← 可领取Tab
│  └─────────────────────────────────┘│
│  ┌─────────────────────────────────┐│
│  │ ¥20     │ 满减券                ││
│  │ 满200可用│ XX商户                ││
│  │         │ 2026.06.01-2026.07.01 ││
│  │         │     [已领完]          ││
│  └─────────────────────────────────┘│
│                                     │
│         [加载更多...]                │
└─────────────────────────────────────┘
```

### 后端 API 对照

**Tab 1: 可领取**

```
GET /v1/app/tenants/{tenantId}/coupons/available
Authorization: Bearer {token}

Response: Result<List<AppCouponTemplateVO>>
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "tenantId": 100,
      "ownerType": "TENANT",
      "name": "新人专享券",
      "couponType": "FIXED",           // FIXED=满减 | RATE=折扣
      "thresholdAmount": 100.00,       // 门槛金额（元）
      "discountAmount": 10.00,         // 满减金额（元，FIXED类型时有值）
      "discountRate": null,            // 折扣率（RATE类型时有值，如0.85=85折）
      "maxDiscountAmount": null,       // 折扣上限（RATE类型时有值）
      "perUserLimit": 1,               // 每人限领
      "remainingStock": 50,            // 剩余库存
      "receivedByCurrentUser": 0,      // 当前用户已领数量
      "receivable": true,              // 是否可领取（综合判断）
      "receiveStartTime": "2026-06-01T00:00:00",
      "receiveEndTime": "2026-07-01T00:00:00",
      "validStartTime": "2026-06-01T00:00:00",
      "validEndTime": "2026-08-01T00:00:00",
      "validDaysAfterReceive": null,   // 领取后N天有效（与validStartTime/EndTime互斥）
      "description": "新用户专享"
    }
  ]
}
```

**领取操作**
```
POST /v1/app/tenants/{tenantId}/coupons/{templateId}/receive
Authorization: Bearer {token}

Response: Result<AppCouponReceiveVO>
{
  "code": 200,
  "data": {
    "userCouponId": 1001,
    "couponNo": "CP202606070001",
    "couponTemplateId": 1,
    "tenantId": 100,
    "status": "USABLE",
    "expireTime": "2026-08-01T00:00:00"
  }
}
// 领取失败（已领完/已领过/不在领取期）: code !== 200, message 包含原因
```

**Tab 2 & 3: 我的券 / 已过期**

```
GET /v1/app/tenants/{tenantId}/coupons?status={status}
Authorization: Bearer {token}
// status: USABLE | USED | EXPIRED (不传=全部)

Response: Result<List<AppUserCouponVO>>
{
  "code": 200,
  "data": [
    {
      "id": 1001,
      "couponNo": "CP202606070001",
      "couponTemplateId": 1,
      "tenantId": 100,
      "status": "USABLE",              // USABLE | USED | EXPIRED
      "name": "新人专享券",
      "couponType": "FIXED",
      "thresholdAmount": 100.00,
      "discountAmount": 10.00,
      "discountRate": null,
      "maxDiscountAmount": null,
      "receiveTime": "2026-06-07T10:00:00",
      "expireTime": "2026-08-01T00:00:00",
      "usedTime": null                 // 使用后才有值
    }
  ]
}
```

### 前端 TypeScript 类型

```typescript
// types/coupon.ts

interface CouponTemplate {
  id: number
  tenantId: number
  ownerType: string
  name: string
  couponType: 'FIXED' | 'RATE'
  thresholdAmount: number     // 元，展示时需格式化
  discountAmount: number | null
  discountRate: number | null
  maxDiscountAmount: number | null
  perUserLimit: number
  remainingStock: number
  receivedByCurrentUser: number
  receivable: boolean
  receiveStartTime: string
  receiveEndTime: string
  validStartTime: string | null
  validEndTime: string | null
  validDaysAfterReceive: number | null
  description: string | null
}

interface UserCoupon {
  id: number
  couponNo: string
  couponTemplateId: number
  tenantId: number
  status: 'USABLE' | 'USED' | 'EXPIRED'
  name: string
  couponType: 'FIXED' | 'RATE'
  thresholdAmount: number
  discountAmount: number | null
  discountRate: number | null
  maxDiscountAmount: number | null
  receiveTime: string
  expireTime: string
  usedTime: string | null
}

interface CouponReceiveResult {
  userCouponId: number
  couponNo: string
  couponTemplateId: number
  tenantId: number
  status: string
  expireTime: string
}
```

### 前端 Service

```typescript
// services/modules/appCoupon.ts

/** 可领取优惠券列表 */
getAvailableCoupons(tenantId: number): Promise<CouponTemplate[]>

/** 我的优惠券 */
getMyCoupons(tenantId: number, status?: 'USABLE' | 'USED' | 'EXPIRED'): Promise<UserCoupon[]>

/** 领取优惠券 */
claimCoupon(tenantId: number, templateId: number): Promise<CouponReceiveResult>
```

### 交互要点
- 「领取」按钮：`receivable=true` 且 `remainingStock>0` 且 `receivedByCurrentUser < perUserLimit` 时可点击
- 已领完 → 按钮置灰显示"已领完"
- 已达上限 → 按钮置灰显示"已领取"
- 领取成功 → Toast "领取成功" + 刷新列表 + 自动切到「我的券」Tab
- 优惠券卡片左侧大字显示面值，右侧显示名称+条件+有效期
- 过期 Tab 的券卡片整体灰色

---

## 3. 积分中心

### 路由
`/points` → `Points.tsx`

### 页面布局

```
┌─────────────────────────────────────┐
│  ← 我的积分                          │
├─────────────────────────────────────┤
│        🪙 1,280                     │  ← 积分余额（醒目展示）
│        当前可用积分                   │
├────────┬────────────────────────────┤
│ 积分明细 │ 积分兑换                   │  ← Tab
├────────┴────────────────────────────┤
│  Tab1: 积分明细                      │
│  ── 2026-06-07 ──                   │
│  订单消费奖励         +100    余额1280│
│  积分兑换商品          -50    余额1180│
│  ── 2026-06-06 ──                   │
│  签到奖励            +10     余额1230│
│                                     │
│  Tab2: 积分兑换                      │
│  ┌────────┐ ┌────────┐ ┌────────┐  │
│  │ [图片]  │ │ [图片]  │ │ [图片]  │  │
│  │ 商品A   │ │ 商品B   │ │ 商品C   │  │
│  │ 500积分 │ │ 800积分 │ │ 1200积分│  │
│  │ 库存:10 │ │ 库存:5  │ │ 库存:0  │  │
│  │ [兑换]  │ │ [兑换]  │ │ [已兑完]│  │
│  └────────┘ └────────┘ └────────┘  │
└─────────────────────────────────────┘
```

### 后端 API 对照

**积分余额**
```
GET /api/points/balance
Authorization: Bearer {token}

Response: Result<{ points: number, userId: number, tenantId: number }>
{
  "code": 200,
  "data": {
    "points": 1280,
    "userId": 1,
    "tenantId": 100
  }
}
```

**积分明细**
```
GET /api/points/logs?pageNum=1&pageSize=20
Authorization: Bearer {token}

Response: Result<Page<PointsLog>>
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 1,
        "tenantId": 100,
        "userId": 1,
        "points": 100,        // 正数=获得，负数=消费
        "balance": 1280,      // 变动后余额
        "type": "GRANT",      // GRANT=发放 | DEDUCT=扣减
        "reason": "订单消费奖励",
        "orderNo": "ORD20260607001",
        "createTime": "2026-06-07T10:30:00"
      }
    ],
    "total": 50,
    "size": 20,
    "current": 1,
    "pages": 3
  }
}
```

**兑换商品列表**
```
GET /api/points/exchange/products
Authorization: Bearer {token}

Response: Result<List<ExchangeProduct>>
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "tenantId": 100,
      "productId": 5001,
      "pointsRequired": 500,
      "stock": 10,
      "status": 1,           // 1=上架 0=下架
      "createTime": "2026-06-01T00:00:00",
      "updateTime": "2026-06-07T00:00:00"
    }
  ]
}
```

**兑换操作**
```
POST /api/points/exchange/{exchangeProductId}
Authorization: Bearer {token}

Response: Result<{ orderNo: string, message: string }>
{
  "code": 200,
  "data": {
    "orderNo": "ORD20260607002",
    "message": "兑换成功"
  }
}
// 失败（积分不足/库存不足）: code !== 200
```

### 前端 TypeScript 类型

```typescript
// types/points.ts

interface PointsBalance {
  points: number
  userId: number
  tenantId: number
}

interface PointsLog {
  id: number
  tenantId: number
  userId: number
  points: number          // 正=获得，负=消费
  balance: number         // 变动后余额
  type: 'GRANT' | 'DEDUCT'
  reason: string
  orderNo: string | null
  createTime: string
}

interface ExchangeProduct {
  id: number
  tenantId: number
  productId: number
  pointsRequired: number
  stock: number
  status: number
  createTime: string
  updateTime: string
}
```

### 前端 Service

```typescript
// services/modules/appPoints.ts

/** 积分余额 */
getPointsBalance(): Promise<PointsBalance>

/** 积分明细（分页） */
getPointsLogs(pageNum?: number, pageSize?: number): Promise<PageResult<PointsLog>>

/** 兑换商品列表 */
getExchangeProducts(): Promise<ExchangeProduct[]>

/** 兑换商品 */
exchangeProduct(exchangeProductId: number): Promise<{ orderNo: string; message: string }>
```

### 交互要点
- 顶部余额区域：大字体显示积分数字，可考虑带动画数字滚动
- 积分明细按日期分组展示，type=GRANT 显示绿色 `+N`，type=DEDUCT 显示红色 `-N`
- 兑换商品需要显示商品图片（productId 关联商品信息，可能需要额外查询）
- 库存=0 → 按钮置灰"已兑完"
- 积分不足 → 按钮置灰"积分不足"
- 兑换成功 → Toast + 刷新余额 + 刷新库存
- 注意：PointsController 的路径是 `/api/points/*`（非 `/v1/app/` 前缀），Service 的 baseURL 需要确认

---

## 4. Profile 死链路修复

### 文件
`salessystem/src/pages/app/Profile.tsx`（或类似路径）

### 需要修复的链接

| 原始 | 当前行为 | 修改为 |
|------|---------|--------|
| 收货地址 | `href="#"` 死链 | Toast "即将上线" 或 跳转 `/profile/coming-soon` |
| 账号安全 | `href="#"` 死链 | Toast "即将上线" 或 跳转 `/profile/coming-soon` |
| 消息通知 | `href="#"` 死链 | Toast "即将上线" 或 跳转 `/profile/coming-soon` |

### 实现建议
- 最简方案：onClick 弹 Toast（可用 react-hot-toast 或自建）
- 中等方案：统一跳转到 `/coming-soon` 路由，显示"功能即将上线"页面
- 按钮样式加 `opacity: 0.5` + `cursor: not-allowed` 表示不可用

---

## 5. Home 分类入口功能化

### 文件
`salessystem/src/pages/app/Home.tsx`（或类似路径）

### 需要修改
当前 8 个分类入口是静态 UI 或全部跳转到同一个页面。

### 修改方案
- 每个分类入口点击后跳转到发现页：`/discovery?category={categoryCode}`
- 如果分类 code 暂不确定，先用分类名称的拼音/英文作为 code
- Discovery 页面需要读取 URL 参数并做对应筛选（如果 Discovery 页已有搜索/筛选功能）
- 如果 Discovery 页还没有筛选功能，先只做路由跳转传参，筛选功能在 Wave 2 补全

---

## 6. 商户端 - 优惠券模板管理 *(Wave 2)*

### 路由
`/merchant/:tenantId/marketing/coupons`

### 后端 API
```
Base: /v1/merchant/tenants/{tenantId}/marketing

GET    /coupons?status={status}              → List<CouponTemplate>
POST   /coupons                              → 创建模板
GET    /coupons/{templateId}/scopes           → 适用范围
POST   /coupons/{templateId}/scopes           → 添加适用范围
PUT    /coupons/{templateId}/activate         → 上线
PUT    /coupons/{templateId}/disable          → 下线
```

### 页面功能
- 表格：名称 | 类型 | 面值/折扣 | 门槛 | 已领/总量 | 状态 | 操作
- 操作：新建（弹窗表单）| 上线/下线 | 查看适用范围
- 新建弹窗字段：name, couponType(FIXED/RATE), thresholdAmount, discountAmount/discountRate, totalStock, perUserLimit, receiveStartTime/EndTime, validStartTime/EndTime(validDaysAfterReceive), description, stackStrategy

---

## 7. 商户端 - 促销活动管理 *(Wave 2)*

### 路由
`/merchant/:tenantId/marketing/activities`

### 后端 API
```
GET    /activities?status={status}           → List<PromotionActivity>
POST   /activities                           → 创建活动
GET    /activities/{activityId}/rules         → 活动规则
POST   /activities/{activityId}/rules         → 添加规则
PUT    /activities/{activityId}/activate      → 上线
PUT    /activities/{activityId}/disable       → 下线
```

### 页面功能
- 表格：名称 | 类型 | 时间范围 | 状态 | 操作
- 操作：新建 | 上线/下线 | 管理规则（展开或弹窗）
- 规则类型：满减/折扣/赠品（ruleType + thresholdAmount + discountAmount/discountRate）

---

## 8. 商户端 - 会员等级/标签管理 *(Wave 2)*

### 路由
`/merchant/:tenantId/marketing/members`

### 后端 API
```
GET    /member-levels                        → List<MemberLevel>
POST   /member-levels?level=&name=&thresholdAmount=&discountRate= → 创建等级
PUT    /members/{memberId}/level?memberLevel= → 调整会员等级
GET    /member-tags                          → List<MemberTag>
POST   /member-tags?name=                    → 创建标签
PUT    /members/{memberId}/tags/{tagId}      → 打标签
DELETE /members/{memberId}/tags/{tagId}      → 移除标签
```

### 页面功能
- Tab 1: 等级管理 - 表格（等级名/门槛/折扣率/会员数）+ 新建
- Tab 2: 标签管理 - 表格（标签名/会员数）+ 新建 + 成员分配

---

## 9. 管理端 - 平台营销运营 *(Wave 2)*

### 路由
`/admin/marketing`

### 后端 API
```
Base: /v1/admin/marketing

GET    /coupons?status=           → 平台优惠券列表
POST   /coupons                   → 创建平台优惠券
PUT    /coupons/{id}/activate     → 上线
PUT    /coupons/{id}/disable      → 下线
GET    /activities?status=        → 平台活动列表
POST   /activities                → 创建平台活动
PUT    /activities/{id}/activate  → 上线
PUT    /activities/{id}/disable   → 下线
```

### 页面功能
- Tab 1: 平台优惠券 - 与商户端类似但 ownerType=PLATFORM
- Tab 2: 平台活动 - 与商户端类似但 ownerType=PLATFORM

---

## 通用约定

### API 响应格式
```typescript
interface Result<T> {
  code: number      // 200=成功
  message: string
  data: T
}
```

### 分页格式
```typescript
interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number   // 注意：后端返回的分页字段是 page 还是 current，需联调确认
  pages: number
}
```

### Service 调用规范
- 统一使用 `salessystem/src/services/request.ts` 封装
- token 通过 http.ts 拦截器自动注入
- 401 响应自动清理 token + 跳转登录

### 金额处理
- 后端返回的是 **BigDecimal 元**（如 100.00），不是分
- 前端展示时直接使用，格式化为 `¥100.00`
- 如后续 VO 层改为 Long（分），前端需要 `/100` 转换——联调时确认

### 时间格式
- 后端返回 ISO 8601（`2026-06-07T10:30:00`）
- 前端展示用 dayjs 或 Intl.DateTimeFormat 格式化
