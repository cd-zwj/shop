# API 接口文档 - POS 收银模块

## 基础信息

- **Base URL**: `http://localhost:8080/api`
- **认证方式**: Bearer Token (JWT)
- **请求头**:
  ```
  Content-Type: application/json
  Authorization: Bearer {token}
  ```

---

## 1. 添加商品到购物车

### 接口信息
- **URL**: `/pos/cart/{sessionId}/add`
- **Method**: `POST`
- **权限**: 需要登录

### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| sessionId | String | 是 | 会话ID（路径参数） |
| productId | Long | 是 | 商品ID |
| quantity | Integer | 是 | 数量 |

### 请求示例

```http
POST /api/pos/cart/session_1234567890/add
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "productId": 1,
  "quantity": 2
}
```

### 响应示例

```json
{
  "code": 200,
  "message": "商品已添加到购物车",
  "data": null
}
```

---

## 2. 查询购物车

### 接口信息
- **URL**: `/pos/cart/{sessionId}`
- **Method**: `GET`
- **权限**: 需要登录

### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| sessionId | String | 是 | 会话ID（路径参数） |

### 请求示例

```http
GET /api/pos/cart/session_1234567890
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "productId": 1,
      "productCode": "P001",
      "productName": "iPhone 15 Pro",
      "price": 7999.00,
      "quantity": 2,
      "imageUrl": "https://example.com/images/iphone15.jpg"
    },
    {
      "productId": 2,
      "productCode": "P002",
      "productName": "华为 Mate 60",
      "price": 6999.00,
      "quantity": 1,
      "imageUrl": "https://example.com/images/mate60.jpg"
    }
  ]
}
```

---

## 3. 更新购物车商品数量

### 接口信息
- **URL**: `/pos/cart/{sessionId}/update`
- **Method**: `PUT`
- **权限**: 需要登录

### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| sessionId | String | 是 | 会话ID（路径参数） |
| productId | Long | 是 | 商品ID |
| quantity | Integer | 是 | 新数量 |

### 请求示例

```http
PUT /api/pos/cart/session_1234567890/update
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "productId": 1,
  "quantity": 3
}
```

### 响应示例

```json
{
  "code": 200,
  "message": "购物车已更新",
  "data": null
}
```

---

## 4. 移除购物车商品

### 接口信息
- **URL**: `/pos/cart/{sessionId}/remove/{productId}`
- **Method**: `DELETE`
- **权限**: 需要登录

### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| sessionId | String | 是 | 会话ID（路径参数） |
| productId | Long | 是 | 商品ID（路径参数） |

### 请求示例

```http
DELETE /api/pos/cart/session_1234567890/remove/1
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 响应示例

```json
{
  "code": 200,
  "message": "商品已从购物车移除",
  "data": null
}
```

---

## 5. 清空购物车

### 接口信息
- **URL**: `/pos/cart/{sessionId}`
- **Method**: `DELETE`
- **权限**: 需要登录

### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| sessionId | String | 是 | 会话ID（路径参数） |

### 请求示例

```http
DELETE /api/pos/cart/session_1234567890
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 响应示例

```json
{
  "code": 200,
  "message": "购物车已清空",
  "data": null
}
```

---

## 6. 结账（创建订单）

### 接口信息
- **URL**: `/pos/checkout/{sessionId}`
- **Method**: `POST`
- **权限**: 需要登录

### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| sessionId | String | 是 | 会话ID（路径参数） |

### 请求示例

```http
POST /api/pos/checkout/session_1234567890
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR:parameter name="text">...
```

### 响应示例

```json
{
  "code": 200,
  "message": "订单创建成功",
  "data": {
    "id": 1001,
    "tenantId": 1,
    "orderNo": "POS1704096000123",
    "userId": 1,
    "amount": 22997.00,
    "payAmount": 0.00,
    "payType": "WECHAT",
    "orderStatus": "PENDING",
    "payStatus": "PENDING",
    "subject": "POS收银订单",
    "body": "POS收银订单 - 3件商品",
    "createTime": "2024-01-01 12:00:00",
    "expireTime": "2024-01-01 12:30:00"
  }
}
```

---

## 7. 扫码添加商品

### 接口信息
- **URL**: `/pos/scan`
- **Method**: `POST`
- **权限**: 需要登录

### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| productCode | String | 是 | 商品条码 |
| deviceId | String | 是 | 设备ID |
| quantity | Integer | 否 | 数量，默认1 |

### 请求示例

```http
POST /api/pos/scan
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "productCode": "P001",
  "deviceId": "POS_001",
  "quantity": 1
}
```

### 响应示例

#### 成功

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "status": "SUCCESS",
    "message": "商品已添加到购物车",
    "productCode": "P001",
    "productId": 1,
    "productName": "iPhone 15 Pro",
    "productImage": "https://example.com/images/iphone15.jpg",
    "price": 7999.00,
    "stock": 100,
    "cartData": {
      "cartTotal": 3,
      "cartAmount": 22997.00
    }
  }
}
```

#### 商品不存在

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "status": "NOT_FOUND",
    "message": "商品不存在",
    "productCode": "P999"
  }
}
```

#### 商品已下架

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "status": "UNAVAILABLE",
    "message": "商品已下架",
    "productCode": "P001",
    "productId": 1,
    "productName": "iPhone 15 Pro"
  }
}
```

---

## 使用流程示例

### 完整的 POS 收银流程

```javascript
// 1. 生成会话ID
const sessionId = 'session_' + Date.now();

// 2. 扫码或手动添加商品
POST /api/pos/cart/${sessionId}/add
{
  "productId": 1,
  "quantity": 2
}

// 3. 继续添加其他商品
POST /api/pos/cart/${sessionId}/add
{
  "productId": 2,
  "quantity": 1
}

// 4. 查看购物车
GET /api/pos/cart/${sessionId}

// 5. 修改商品数量（可选）
PUT /api/pos/cart/${sessionId}/update
{
  "productId": 1,
  "quantity": 3
}

// 6. 结账
POST /api/pos/checkout/${sessionId}

// 7. 获取订单信息，跳转支付
```

---

## 错误响应示例

### 购物车为空

```json
{
  "code": 400,
  "message": "购物车为空，无法创建订单",
  "data": null
}
```

### 商品不存在

```json
{
  "code": 404,
  "message": "商品不存在",
  "data": null
}
```

### 商品已下架

```json
{
  "code": 400,
  "message": "商品已下架",
  "data": null
}
```

### 商品数量必须大于0

```json
{
  "code": 400,
  "message": "商品数量必须大于0",
  "data": null
}
```
