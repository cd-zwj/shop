# API 接口文档 - 订单管理模块

## 模块说明

订单管理模块提供订单创建、支付、查询和取消等功能。

**Base URL**: `/api/order`

**认证方式**: Bearer Token (JWT)

---

## 接口列表

### 1. 创建订单

创建新的支付订单。

**接口地址**: `POST /order/create`

**请求头**:
```
Authorization: Bearer {token}
Content-Type: application/json
```

**请求参数**:
```json
{
  "productId": 1,
  "quantity": 2,
  "remark": "备注信息"
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| productId | Long | 是 | 商品ID |
| quantity | Integer | 是 | 购买数量 |
| remark | String | 否 | 订单备注 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "orderNo": "ORD202401010001",
    "tenantId": 1,
    "userId": 1,
    "amount": 100.00,
    "payAmount": 100.00,
    "payType": "WECHAT",
    "orderStatus": "PENDING",
    "payStatus": null,
    "subject": "商品名称",
    "body": "商品描述",
    "createTime": "2024-01-01 10:00:00"
  }
}
```

---

### 2. 发起支付

对已创建的订单发起支付。

**接口地址**: `POST /order/pay`

**请求头**:
```
Authorization: Bearer {token}
```

**请求参数**:
```
orderNo=ORD202401010001
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| orderNo | String | 是 | 订单号 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "payUrl": "weixin://wxpay/bizpayurl?pr=xxxxx",
    "qrCode": "data:image/png;base64,iVBORw0KGgoAAAANS...",
    "orderNo": "ORD202401010001"
  }
}
```

---

### 3. 查询订单

根据订单号查询订单详情。

**接口地址**: `GET /order/query`

**请求头**:
```
Authorization: Bearer {token}
```

**请求参数**:
```
orderNo=ORD202401010001
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| orderNo | String | 是 | 订单号 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "orderNo": "ORD202401010001",
    "tenantId": 1,
    "userId": 1,
    "amount": 100.00,
    "payAmount": 100.00,
    "payType": "WECHAT",
    "orderStatus": "PAID",
    "payStatus": "SUCCESS",
    "thirdPartyOrderNo": "4200001234567890",
    "subject": "商品名称",
    "body": "商品描述",
    "payTime": "2024-01-01 10:05:00",
    "createTime": "2024-01-01 10:00:00"
  }
}
```

---

### 4. 取消订单

取消未支付的订单。

**接口地址**: `POST /order/cancel`

**请求头**:
```
Authorization: Bearer {token}
```

**请求参数**:
```
orderNo=ORD202401010001
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| orderNo | String | 是 | 订单号 |

**响应示例**:
```json
{
  "code": 200,
  "message": "订单已取消",
  "data": null
}
```

---

## 订单状态说明

### orderStatus (订单状态)

| 状态值 | 说明 |
|--------|------|
| PENDING | 待支付 |
| PAID | 已支付 |
| CANCELLED | 已取消 |
| REFUNDED | 已退款 |

### payStatus (支付状态)

| 状态值 | 说明 |
|--------|------|
| SUCCESS | 支付成功 |
| FAIL | 支付失败 |

---

## 错误码

| 错误码 | 说明 |
|--------|------|
| 400 | 参数错误 |
| 401 | 未登录或登录已过期 |
| 404 | 订单不存在 |
| 500 | 服务器内部错误 |

---

## 使用示例

### cURL 示例

```bash
# 创建订单
curl -X POST http://localhost:8080/api/order/create \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "quantity": 2
  }'

# 发起支付
curl -X POST "http://localhost:8080/api/order/pay?orderNo=ORD202401010001" \
  -H "Authorization: Bearer {token}"

# 查询订单
curl -X GET "http://localhost:8080/api/order/query?orderNo=ORD202401010001" \
  -H "Authorization: Bearer {token}"

# 取消订单
curl -X POST "http://localhost:8080/api/order/cancel?orderNo=ORD202401010001" \
  -H "Authorization: Bearer {token}"
```
