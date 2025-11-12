# API 接口文档 - 充值管理模块

## 模块说明

充值管理模块提供用户余额充值、充值规则配置、余额查询等功能。

**Base URL**: `/api/recharge`

**认证方式**: Bearer Token (JWT)

---

## 接口列表

### 1. 获取充值规则列表（用户端）

获取当前商家的充值规则列表，用户可选择充值套餐。

**接口地址**: `GET /recharge/user/rules`

**请求头**:
```
Authorization: Bearer {token}
```

**请求参数**: 无

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "tenantId": 1,
      "rechargeAmount": 100.00,
      "bonusAmount": 10.00,
      "description": "充100送10",
      "status": 1,
      "createTime": "2024-01-01 10:00:00"
    },
    {
      "id": 2,
      "tenantId": 1,
      "rechargeAmount": 500.00,
      "bonusAmount": 100.00,
      "description": "充500送100",
      "status": 1,
      "createTime": "2024-01-01 10:00:00"
    }
  ]
}
```

---

### 2. 创建充值订单（用户端）

用户选择充值规则创建充值订单。

**接口地址**: `POST /recharge/order`

**请求头**:
```
Authorization: Bearer {token}
Content-Type: application/json
```

**请求参数**:
```json
{
  "ruleId": 1
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| ruleId | Long | 是 | 充值规则ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "orderNo": "RCH202401010001",
    "rechargeAmount": 100.00,
    "bonusAmount": 10.00,
    "totalAmount": 110.00,
    "payInfo": {
      "payUrl": "weixin://wxpay/bizpayurl?pr=xxxxx",
      "qrCode": "data:image/png;base64,iVBORw0KGgoAAAANS...",
      "orderNo": "RCH202401010001"
    }
  }
}
```

---

### 3. 查询用户余额（用户端）

查询当前用户的账户余额。

**接口地址**: `GET /recharge/balance`

**请求头**:
```
Authorization: Bearer {token}
```

**请求参数**: 无

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "balance": 1250.50,
    "userId": 1,
    "tenantId": 1
  }
}
```

---

### 4. 查询余额明细（用户端）

查询用户的余额变动明细记录。

**接口地址**: `GET /recharge/balance/logs`

**请求头**:
```
Authorization: Bearer {token}
```

**请求参数**:
```
pageNum=1&pageSize=10
```

| 参数名 | 类型 | 必填 | 说明 | 默认值 |
|--------|------|------|------|--------|
| pageNum | Integer | 否 | 页码 | 1 |
| pageSize | Integer | 否 | 每页数量 | 10 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "userId": 1,
        "tenantId": 1,
        "changeType": "RECHARGE",
        "amount": 100.00,
        "balanceBefore": 1150.50,
        "balanceAfter": 1250.50,
        "orderNo": "RCH202401010001",
        "remark": "充值",
        "createTime": "2024-01-01 10:00:00"
      },
      {
        "id": 2,
        "userId": 1,
        "tenantId": 1,
        "changeType": "CONSUME",
        "amount": -50.00,
        "balanceBefore": 1200.50,
        "balanceAfter": 1150.50,
        "orderNo": "ORD202401010001",
        "remark": "消费",
        "createTime": "2024-01-01 09:00:00"
      }
    ],
    "total": 50,
    "size": 10,
    "current": 1,
    "pages": 5
  }
}
```

---

### 5. 获取商家充值规则（商家端）

商家查询自己配置的充值规则列表。

**接口地址**: `GET /recharge/rules`

**请求头**:
```
Authorization: Bearer {token}
```

**请求参数**: 无

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "tenantId": 1,
      "rechargeAmount": 100.00,
      "bonusAmount": 10.00,
      "description": "充100送10",
      "status": 1,
      "createTime": "2024-01-01 10:00:00"
    }
  ]
}
```

---

### 6. 设置充值规则（商家端）

商家配置充值规则，支持批量设置。

**接口地址**: `POST /recharge/rules`

**请求头**:
```
Authorization: Bearer {token}
Content-Type: application/json
```

**请求参数**:
```json
[
  {
    "rechargeAmount": 100.00,
    "bonusAmount": 10.00,
    "description": "充100送10"
  },
  {
    "rechargeAmount": 500.00,
    "bonusAmount": 100.00,
    "description": "充500送100"
  }
]
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| rechargeAmount | BigDecimal | 是 | 充值金额 |
| bonusAmount | BigDecimal | 是 | 赠送金额 |
| description | String | 否 | 规则描述 |

**响应示例**:
```json
{
  "code": 200,
  "message": "充值规则设置成功",
  "data": null
}
```

---

## 余额变动类型说明

| changeType | 说明 |
|------------|------|
| RECHARGE | 充值 |
| BONUS | 赠送 |
| CONSUME | 消费 |
| REFUND | 退款 |

---

## 错误码

| 错误码 | 说明 |
|--------|------|
| 400 | 参数错误（如充值金额不合法） |
| 401 | 未登录或登录已过期 |
| 404 | 充值规则不存在 |
| 500 | 服务器内部错误 |

---

## 使用示例

### cURL 示例

```bash
# 获取充值规则列表
curl -X GET http://localhost:8080/api/recharge/user/rules \
  -H "Authorization: Bearer {token}"

# 创建充值订单
curl -X POST http://localhost:8080/api/recharge/order \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "ruleId": 1
  }'

# 查询用户余额
curl -X GET http://localhost:8080/api/recharge/balance \
  -H "Authorization: Bearer {token}"

# 查询余额明细
curl -X GET "http://localhost:8080/api/recharge/balance/logs?pageNum=1&pageSize=10" \
  -H "Authorization: Bearer {token}"

# 设置充值规则（商家端）
curl -X POST http://localhost:8080/api/recharge/rules \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '[
    {
      "rechargeAmount": 100.00,
      "bonusAmount": 10.00,
      "description": "充100送10"
    }
  ]'
```

---

## 注意事项

1. 充值订单创建后会自动生成支付订单
2. 充值成功后，充值金额和赠送金额会同时到账
3. 余额不支持提现，仅用于平台内消费
4. 商家可以随时修改充值规则，但不影响已创建的订单
5. 充值金额必须大于0，赠送金额可以为0
