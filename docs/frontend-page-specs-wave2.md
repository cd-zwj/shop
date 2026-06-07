# 前端页面规格说明书 (Wave 2)

> 最后更新：2026-06-07
> 分支：codex-dual-wallet-v1
> 适用目录：`salessystem/`
> 说明：前端页面由你自行实现，本文档只提供页面清单、功能规格和后端API对照

---

## 目录

1. [优惠券下单集成](#1-优惠券下单集成)
2. [商户端 - 优惠券模板管理](#2-商户端---优惠券模板管理)
3. [商户端 - 促销活动管理](#3-商户端---促销活动管理)
4. [商户端 - 会员等级/标签管理](#4-商户端---会员等级标签管理)
5. [管理端 - 平台营销运营](#5-管理端---平台营销运营)
6. [退款/售后](#6-退款售后)

---

## 1. 优惠券下单集成

### 改动范围
- `salessystem/src/services/orderCheckout.ts` — 构建订单 payload 时支持 couponId
- `salessystem/src/types/order.ts` — AppCreateOrderPayload 新增 couponId
- `salessystem/src/pages/Cart.tsx` — 结算前弹出优惠券选择

### 订单 payload 改造

```typescript
// types/order.ts — AppCreateOrderPayload 新增：
interface AppCreateOrderPayload {
  // ... 现有字段 ...
  couponId?: number;  // 新增：用户选择的优惠券ID
}
```

### Cart.tsx 改造方案

在每个商户卡片的「结算该商户商品」按钮上方，新增优惠券选择区域：

```
┌─────────────────────────────────────────┐
│  🏪 商户A                    3件商品     │
│  ─────────────────────────────────────  │
│  [商品1] [商品2] [商品3]                  │
│  ─────────────────────────────────────  │
│  🎫 优惠券:  [选择优惠券 ▼]              │  ← 新增
│     已选: ¥10 满减券（满100可用）         │
│     优惠: -¥10                          │
│  ─────────────────────────────────────  │
│  商户小计: ¥290 → ¥280                  │
│  [结算该商户商品 →]                      │
└─────────────────────────────────────────┘
```

### 实现步骤

1. **获取可用优惠券**：调用 `appCouponService.getAvailableCoupons(tenantId)` 获取该商户的可用优惠券
2. **筛选可用**：只显示 `receivable=true` 且 `remainingStock>0` 且满足门槛条件（subtotal >= thresholdAmount）的优惠券
3. **选择优惠券**：用户选择后记录 couponId，显示优惠金额
4. **优惠金额计算**：
   - FIXED 类型：直接减 discountAmount
   - RATE 类型：subtotal * (1 - discountRate)，不超过 maxDiscountAmount
5. **传递 couponId**：在 `buildOrderPayload` 中将 couponId 加入 payload

### orderCheckout.ts 改造

```typescript
// buildOrderPayload 增加可选 couponId 参数
export function buildOrderPayload(
  items: CartItem[], 
  source: CheckoutSource,
  couponId?: number  // 新增
): AppCreateOrderPayload {
  return {
    // ... 现有字段 ...
    couponId,  // 新增
  };
}

export function createOrderForItems(
  items: CartItem[], 
  source: CheckoutSource,
  couponId?: number  // 新增
): Promise<OrderPayment> {
  return appOrderService.createOrder(buildOrderPayload(items, source, couponId));
}
```

### 交互要点
- 优惠券选择器放在商户小计上方
- 未选择时显示「不使用优惠券」
- 选择后实时更新优惠金额和应付金额
- 优惠券不足门槛时灰显不可选
- 一个订单只能用一张优惠券
- 如果后端返回优惠券不可用错误，Toast 提示并清除选择

---

## 2. 商户端 - 优惠券模板管理

### 路由
`/merchant/marketing/coupons`

### 页面布局

```
┌──────────────────────────────────────────┐
│  优惠券模板管理            [+ 新建优惠券]  │
├──────────────────────────────────────────┤
│  筛选: [全部] [草稿] [已上线] [已下线]     │
├──────────────────────────────────────────┤
│  表格                                     │
│  名称 | 类型 | 面值/折扣 | 门槛 | 已领/总量│
│       | 状态 | 操作(上线/下线/查看范围)     │
└──────────────────────────────────────────┘
```

### 后端 API

```
Base: /v1/merchant/tenants/{tenantId}/marketing

GET    /coupons?status={status}
  → Result<List<CouponTemplate>>
  status: DRAFT | ACTIVE | DISABLED | 不传=全部

POST   /coupons
  → 创建模板
  Body: CouponTemplateCreateDTO {
    name, couponType(FIXED/RATE), thresholdAmount,
    discountAmount, discountRate, maxDiscountAmount,
    totalStock, perUserLimit,
    receiveStartTime, receiveEndTime,
    validStartTime, validEndTime, validDaysAfterReceive,
    minMemberLevel, excludeMemberTagIds, stackStrategy, description
  }

PUT    /coupons/{templateId}/activate
  → 上线（仅 DRAFT 状态可操作）

PUT    /coupons/{templateId}/disable
  → 下线（仅 ACTIVE 状态可操作）

GET    /coupons/{templateId}/scopes
  → 查看适用范围

POST   /coupons/{templateId}/scopes
  → 添加适用范围
  Body: { scopeType, scopeId, scopeCode }
```

### 新建弹窗表单字段

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | 文本 | ✅ | 优惠券名称 |
| couponType | 下拉 | ✅ | FIXED(满减) / RATE(折扣) |
| thresholdAmount | 数字 | | 门槛金额（元），0=无门槛 |
| discountAmount | 数字 | FIXED时必填 | 减免金额（元） |
| discountRate | 数字 | RATE时必填 | 折扣率（如0.85=85折） |
| maxDiscountAmount | 数字 | RATE时建议填 | 折扣上限（元） |
| totalStock | 数字 | ✅ | 发行总量 |
| perUserLimit | 数字 | ✅ | 每人限领，默认1 |
| receiveStartTime | 日期时间 | | 领取开始时间 |
| receiveEndTime | 日期时间 | | 领取结束时间 |
| validStartTime | 日期时间 | 二选一 | 有效期开始（与validDaysAfterReceive互斥） |
| validEndTime | 日期时间 | | 有效期结束 |
| validDaysAfterReceive | 数字 | 二选一 | 领取后N天有效 |
| description | 文本 | | 使用说明 |
| stackStrategy | 下拉 | | 叠加策略 |

### 表格列

| 列 | 展示 |
|----|------|
| 名称 | `coupon.name` |
| 类型 | FIXED→满减 / RATE→折扣 |
| 面值/折扣 | `discountAmount` 元 或 `discountRate*10` 折 |
| 门槛 | 满 `thresholdAmount` 元 |
| 库存 | 已领 / totalStock |
| 状态 | DRAFT→草稿 / ACTIVE→已上线 / DISABLED→已下线 |
| 操作 | 上线(草稿时) / 下线(已上线时) / 查看适用范围 |

### 前端类型

```typescript
// types/marketing.ts
interface MerchantCouponTemplate {
  id: number;
  tenantId: number;
  name: string;
  couponType: 'FIXED' | 'RATE';
  thresholdAmount: number | null;
  discountAmount: number | null;
  discountRate: number | null;
  maxDiscountAmount: number | null;
  totalStock: number;
  perUserLimit: number;
  status: string;
  receiveStartTime: string | null;
  receiveEndTime: string | null;
  validStartTime: string | null;
  validEndTime: string | null;
  validDaysAfterReceive: number | null;
  description: string | null;
  stackStrategy: string | null;
  createTime: string;
}
```

### 前端 Service

```typescript
// services/modules/merchantMarketing.ts
const BASE = (tenantId: number) => `/v1/merchant/tenants/${tenantId}/marketing`;

getCouponTemplates(tenantId: number, status?: string): Promise<MerchantCouponTemplate[]>
createCouponTemplate(tenantId: number, data: CouponTemplateCreateDTO): Promise<MerchantCouponTemplate>
activateCoupon(tenantId: number, templateId: number): Promise<void>
disableCoupon(tenantId: number, templateId: number): Promise<void>
```

---

## 3. 商户端 - 促销活动管理

### 路由
`/merchant/marketing/activities`

### 页面布局

```
┌──────────────────────────────────────────┐
│  促销活动管理              [+ 新建活动]    │
├──────────────────────────────────────────┤
│  筛选: [全部] [未开始] [进行中] [已结束]   │
├──────────────────────────────────────────┤
│  表格                                     │
│  名称 | 类型 | 时间范围 | 状态 | 操作      │
│                                            │
│  展开行: 活动规则列表                       │
│    规则类型 | 条件 | 优惠 | 优先级          │
│    [+ 添加规则]                            │
└──────────────────────────────────────────┘
```

### 后端 API

```
GET    /activities?status={status}
  → Result<List<PromotionActivity>>

POST   /activities
  Body: { name, activityType, startTime, endTime, description }

GET    /activities/{activityId}/rules
  → Result<List<ActivityRule>>

POST   /activities/{activityId}/rules
  Body: { ruleType, thresholdAmount, discountAmount, discountRate, productId, categoryCode, ruleConfigJson, priority }

PUT    /activities/{activityId}/activate
PUT    /activities/{activityId}/disable
```

### 活动规则类型

| ruleType | 说明 | 关键字段 |
|----------|------|---------|
| FULL_REDUCTION | 满减 | thresholdAmount, discountAmount |
| FULL_DISCOUNT | 满折 | thresholdAmount, discountRate |
| BUY_X_GET_Y | 买赠 | productId, ruleConfigJson |
| CATEGORY_DISCOUNT | 分类折扣 | categoryCode, discountRate |

---

## 4. 商户端 - 会员等级/标签管理

### 路由
`/merchant/marketing/members`

### 页面布局

```
┌──────────────────────────────────────────┐
│  会员管理                                 │
├────────────┬─────────────────────────────┤
│ 等级管理    │ 标签管理                     │  ← Tab
├────────────┴─────────────────────────────┤
│  Tab1: 等级管理                           │
│  表格: 等级名 | 等级值 | 消费门槛 | 折扣率 │
│        [+ 新建等级]                       │
│  新建弹窗: level, name, thresholdAmount,  │
│            discountRate                   │
│                                            │
│  Tab2: 标签管理                           │
│  表格: 标签名 | 关联会员数 | 操作          │
│        [+ 新建标签]                       │
│  新建弹窗: name                           │
└──────────────────────────────────────────┘
```

### 后端 API

```
GET    /member-levels
  → Result<List<MemberLevel>>

POST   /member-levels?level=&name=&thresholdAmount=&discountRate=
  → 创建等级（注意：用 @RequestParam，不用 @RequestBody）

GET    /member-tags
  → Result<List<MemberTag>>

POST   /member-tags?name=
  → 创建标签
```

---

## 5. 管理端 - 平台营销运营

### 路由
`/admin/marketing`

### 页面布局

```
┌──────────────────────────────────────────┐
│  平台营销运营                             │
├────────────┬─────────────────────────────┤
│ 平台优惠券  │ 平台活动                     │  ← Tab
├────────────┴─────────────────────────────┤
│  Tab1: 平台优惠券                         │
│  与商户端优惠券管理类似，但 ownerType=     │
│  PLATFORM，无需 tenantId                  │
│  路径: /v1/admin/marketing/coupons        │
│  权限: admin:marketing:list/create/update │
│                                            │
│  Tab2: 平台活动                           │
│  与商户端活动管理类似，ownerType=PLATFORM  │
│  路径: /v1/admin/marketing/activities     │
└──────────────────────────────────────────┘
```

### 后端 API

```
Base: /v1/admin/marketing
权限: 需要 admin:marketing:* 权限码

GET    /coupons?status=          → 平台优惠券列表
POST   /coupons                  → 创建平台优惠券
PUT    /coupons/{id}/activate    → 上线
PUT    /coupons/{id}/disable     → 下线
GET    /coupons/{id}/scopes      → 适用范围
POST   /coupons/{id}/scopes      → 添加适用范围

GET    /activities?status=       → 平台活动列表
POST   /activities               → 创建平台活动
PUT    /activities/{id}/activate → 上线
PUT    /activities/{id}/disable  → 下线
GET    /activities/{id}/rules    → 活动规则
POST   /activities/{id}/rules    → 添加规则
```

### 与商户端的差异
- 路径不同：`/v1/admin/marketing/*` vs `/v1/merchant/tenants/{tenantId}/marketing/*`
- 无 tenantId 参数
- 需要 admin 角色 + admin:marketing 权限码
- 创建时 ownerType 固定为 PLATFORM（后端自动设置）
- **建议**：商户端和管理端的优惠券/活动管理 UI 组件可复用，通过 props 区分 API 路径和权限

---

## 6. 退款/售后

### 用户端路由
`/orders/:orderNo/refund` — 申请退款页
`/profile/refunds` — 我的退款列表（可选，或嵌入订单详情页）

### 后端 API（用户端）

```
Base: /v1/app/tenants/{tenantId}/refunds

POST   /
  → 申请退款
  Body: { orderNo, refundType, refundAmount, reason, description }
  refundType: REFUND_ONLY | RETURN_REFUND

GET    /?status={status}&pageNum=1&pageSize=10
  → 我的退款列表
  status: PENDING | APPROVED | REJECTED | COMPLETED | CANCELLED

GET    /{refundId}
  → 退款详情

PUT    /{refundId}/cancel
  → 取消退款（仅 PENDING 状态）
```

### 商户端 API

```
Base: /v1/merchant/tenants/{tenantId}/refunds

GET    /?status={status}&pageNum=1&pageSize=10
  → 退款申请列表

PUT    /{refundId}/audit
  → 审核退款
  Body: { approved: boolean, rejectReason?: string }
```

### 用户端 - 退款申请页

```
┌──────────────────────────────────────┐
│  ← 申请退款                           │
├──────────────────────────────────────┤
│  订单信息                             │
│  订单号: ORD20260607001               │
│  商品: 商品A x2                       │
│  订单金额: ¥200.00                    │
├──────────────────────────────────────┤
│  退款类型:                            │
│  ○ 仅退款  ○ 退货退款                 │
├──────────────────────────────────────┤
│  退款金额: [¥200.00] (可修改部分退款)  │
├──────────────────────────────────────┤
│  退款原因: [下拉选择]                  │
│  - 不想要了                           │
│  - 商品质量问题                        │
│  - 商品与描述不符                      │
│  - 收到商品损坏                        │
│  - 其他                               │
├──────────────────────────────────────┤
│  详细描述:                            │
│  [________________________]          │
├──────────────────────────────────────┤
│         [提交退款申请]                 │
└──────────────────────────────────────┘
```

### 交互要点
- 从订单详情页「申请售后」按钮进入，URL 携带 orderNo
- 退款金额默认=订单应付金额，可修改但不能超过原单金额
- 提交成功后 Toast + 跳转到退款详情/订单详情
- 只有 PENDING 状态的退款可取消

### 前端类型

```typescript
// types/refund.ts
interface RefundCreateDTO {
  orderNo: string;
  refundType: 'REFUND_ONLY' | 'RETURN_REFUND';
  refundAmount: number;
  reason: string;
  description?: string;
}

interface Refund {
  id: number;
  refundNo: string;
  orderNo: string;
  refundType: string;
  refundStatus: string;
  refundAmount: number;
  reason: string;
  description: string | null;
  rejectReason: string | null;
  auditTime: string | null;
  completeTime: string | null;
  createTime: string;
}
```

---

## 通用约定

### API 响应格式
```typescript
interface Result<T> {
  code: number;      // 200=成功
  message: string;
  data: T;
}
```

### 分页参数
- 后端 MyBatis-Plus: `current`(当前页), `size`(每页大小)
- 前端传参：`pageNum` 或 `current`，`pageSize` 或 `size`（联调时确认）

### 路由守卫
- 商户端页面：`<AuthGuard><RoleGuard allowedRoles={['merchant']}>...</RoleGuard></AuthGuard>`
- 管理端页面：`<AuthGuard><RoleGuard allowedRoles={['admin']}>...</RoleGuard></AuthGuard>`

### 金额
- 后端返回 BigDecimal（元），前端直接使用
- VO 层可能转 Long（分），联调时确认

### Service 文件组织
- 商户端营销：`services/modules/merchantMarketing.ts`
- 管理端营销：`services/modules/adminMarketing.ts`（或复用 merchantMarketing 通过参数区分）
- 退款：`services/modules/appRefund.ts`（用户端）/ `services/modules/merchantRefund.ts`（商户端）
