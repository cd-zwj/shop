# API 接口文档 - 平台管理模块

## 基础信息

- **Base URL**: `http://localhost:8080/api`
- **认证方式**: Bearer Token (JWT)
- **请求头**:
  ```
  Content-Type: application/json
  Authorization: Bearer {token}
  ```

---

## 1. 管理员登录

### 接口信息
- **URL**: `/admin/login`
- **Method**: `POST`
- **权限**: 无需认证

### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| username | String | 是 | 用户名 |
| password | String | 是 | 密码 |

### 请求示例

```http
POST /api/admin/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

### 响应示例

```json
{
  "code": 200,
  "message": "登录成功",
  "data": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
}
```

---

## 2. 获取管理员信息

### 接口信息
- **URL**: `/admin/info`
- **Method**: `GET`
- **权限**: 需要管理员权限

### 请求示例

```http
GET /api/admin/info
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "admin",
    "role": "ADMIN"
  }
}
```

---

## 3. 平台数据概览

### 接口信息
- **URL**: `/admin/dashboard/stats`
- **Method**: `GET`
- **权限**: 需要管理员权限

### 请求示例

```http
GET /api/admin/dashboard/stats
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalMerchants": 150,
    "activeMerchants": 120,
    "totalSales": "1250000.00",
    "pendingWithdrawals": 25
  }
}
```

---

## 4. 商家注册趋势

### 接口信息
- **URL**: `/admin/dashboard/merchant-trend`
- **Method**: `GET`
- **权限**: 需要管理员权限

### 请求示例

```http
GET /api/admin/dashboard/merchant-trend
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "dates": ["2024-01", "2024-02", "2024-03", "2024-04", "2024-05", "2024-06"],
    "counts": [10, 15, 20, 18, 25, 30]
  }
}
```

---

## 5. 平台销售趋势

### 接口信息
- **URL**: `/admin/dashboard/sales-trend`
- **Method**: `GET`
- **权限**: 需要管理员权限

### 请求示例

```http
GET /api/admin/dashboard/sales-trend
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "dates": ["2024-01", "2024-02", "2024-03", "2024-04", "2024-05", "2024-06"],
    "amounts": [150000.00, 180000.00, 220000.00, 200000.00, 250000.00, 280000.00]
  }
}
```

---

## 6. 商家列表

### 接口信息
- **URL**: `/admin/merchants`
- **Method**: `GET`
- **权限**: 需要管理员权限

### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| current | Integer | 否 | 当前页，默认1 |
| size | Integer | 否 | 每页数量，默认10 |
| name | String | 否 | 商家名称 |
| phone | String | 否 | 联系电话 |
| status | Integer | 否 | 状态：0-禁用，1-启用 |

### 请求示例

```http
GET /api/admin/merchants?current=1&size=10&status=1
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "tenantCode": "MERCHANT001",
        "name": "测试商家1",
        "contactName": "张三",
        "contactPhone": "13800138000",
        "createTime": "2024-01-01 10:00:00",
        "status": 1
      },
      {
        "id": 2,
        "tenantCode": "MERCHANT002",
        "name": "测试商家2",
        "contactName": "李四",
        "contactPhone": "13800138001",
        "createTime": "2024-01-02 10:00:00",
        "status": 1
      }
    ],
    "total": 150,
    "size": 10,
    "current": 1,
    "pages": 15
  }
}
```

---

## 7. 商家详情

### 接口信息
- **URL**: `/admin/merchant/{id}`
- **Method**: `GET`
- **权限**: 需要管理员权限

### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 商家ID（路径参数） |

### 请求示例

```http
GET /api/admin/merchant/1
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "merchant": {
      "id": 1,
      "tenantCode": "MERCHANT001",
      "name": "测试商家1",
      "contact": "张三",
      "phone": "13800138000",
      "address": "北京市朝阳区",
      "status": 1,
      "createTime": "2024-01-01 10:00:00"
    },
    "stats": {
      "productCount": 50,
      "orderCount": 1200,
      "totalSales": 350000.00
    },
    "balance": {
      "tenantId": 1,
      "balance": 50000.00,
      "frozenBalance": 5000.00,
      "totalIncome": 350000.00,
      "totalWithdrawal": 300000.00
    }
  }
}
```

---

## 8. 启用商家

### 接口信息
- **URL**: `/admin/merchant/{id}/enable`
- **Method**: `PUT`
- **权限**: 需要管理员权限

### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 商家ID（路径参数） |

### 请求示例

```http
PUT /api/admin/merchant/1/enable
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 响应示例

```json
{
  "code": 200,
  "message": "启用成功",
  "data": null
}
```

---

## 9. 禁用商家

### 接口信息
- **URL**: `/admin/merchant/{id}/disable`
- **Method**: `PUT`
- **权限**: 需要管理员权限

### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 商家ID（路径参数） |

### 请求示例

```http
PUT /api/admin/merchant/1/disable
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 响应示例

```json
{
  "code": 200,
  "message": "禁用成功",
  "data": null
}
```

---

## 10. 提现申请列表

### 接口信息
- **URL**: `/admin/withdrawals`
- **Method**: `GET`
- **权限**: 需要管理员权限

### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| current | Integer | 否 | 当前页，默认1 |
| size | Integer | 否 | 每页数量，默认10 |
| merchantName | String | 否 | 商家名称 |
| status | Integer | 否 | 状态：0-待审核，1-已通过，2-已拒绝 |
| startDate | String | 否 | 开始日期 |
| endDate | String | 否 | 结束日期 |

### 请求示例

```http
GET /api/admin/withdrawals?current=1&size=10&status=0
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "tenantId": 1,
        "merchantName": "测试商家1",
        "amount": 10000.00,
        "bankName": "中国工商银行",
        "bankAccount": "6222021234567890",
        "accountName": "张三",
        "status": 0,
        "applyTime": "2024-01-01 10:00:00"
      },
      {
        "id": 2,
        "tenantId": 2,
        "merchantName": "测试商家2",
        "amount": 5000.00,
        "bankName": "中国建设银行",
        "bankAccount": "6227001234567890",
        "accountName": "李四",
        "status": 0,
        "applyTime": "2024-01-02 10:00:00"
      }
    ],
    "total": 25,
    "size": 10,
    "current": 1,
    "pages": 3
  }
}
```

---

## 11. 审核通过提现申请

### 接口信息
- **URL**: `/admin/withdrawal/{id}/approve`
- **Method**: `PUT`
- **权限**: 需要管理员权限

### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 提现ID（路径参数） |

### 请求示例

```http
PUT /api/admin/withdrawal/1/approve
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 响应示例

```json
{
  "code": 200,
  "message": "审核通过",
  "data": null
}
```

---

## 12. 拒绝提现申请

### 接口信息
- **URL**: `/admin/withdrawal/{id}/reject`
- **Method**: `PUT`
- **权限**: 需要管理员权限

### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 提现ID（路径参数） |
| reason | String | 是 | 拒绝原因 |

### 请求示例

```http
PUT /api/admin/withdrawal/1/reject
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "reason": "银行账号信息有误，请核实后重新申请"
}
```

### 响应示例

```json
{
  "code": 200,
  "message": "已拒绝",
  "data": null
}
```
