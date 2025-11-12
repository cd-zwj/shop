# API 接口文档 - 提现管理模块

## 模块说明

提现管理模块提供商家余额查询、提现申请、提现记录查询和审核等功能。

**Base URL**: `/api/withdrawal`

**认证方式**: Bearer Token (JWT)

---

## 接口列表

### 1. 查询商家余额（商家端）

查询当前商家的账户余额信息。

**接口地址**: `GET /withdrawal/balance`

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
    "id": 1,
    "tenantId": 1,
    "balance": 15000.00,
    "frozenBalance": 2000.00,
    "totalIncome": 50000.00,
    "totalWithdrawal": 35000.00,
    "createTime": "2024-01-01 10:00:00",
    "updateTime": "2024-01-10 15:30:00"
  }
}
```

**字段说明**:
- `balance`: 可用余额（可提现金额）
- `frozenBalance`: 冻结余额（提现审核中的金额）
- `totalIncome`: 累计收入
- `totalWithdrawal`: 累计提现

---

### 2. 创建提现申请（商家端）

商家申请提现到银行账户。

**接口地址**: `POST /withdrawal/apply`

**请求头**:
```
Authorization: Bearer {token}
Content-Type: application/json
```

**请求参数**:
```json
{
  "amount": 5000.00,
  "bankName": "中国工商银行",
  "bankAccount": "6222021234567890123",
  "accountName": "张三"
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| amount | BigDecimal | 是 | 提现金额 |
| bankName | String | 是 | 银行名称 |
| bankAccount | String | 是 | 银行账号 |
| accountName | String | 是 | 账户名 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "withdrawalId": 1,
    "status": 0,
    "message": "提现申请已提交，等待审核"
  }
}
```

---

### 3. 查询提现记录（商家端）

查询商家的提现申请记录。

**接口地址**: `GET /withdrawal/list`

**请求头**:
```
Authorization: Bearer {token}
```

**请求参数**:
```
status=0&pageNum=1&pageSize=10
```

| 参数名 | 类型 | 必填 | 说明 | 默认值 |
|--------|------|------|------|--------|
| status | Integer | 否 | 状态（0-待审核，1-已通过，2-已拒绝） | - |
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
        "tenantId": 1,
        "amount": 5000.00,
        "bankName": "中国工商银行",
        "bankAccount": "6222021234567890123",
        "accountName": "张三",
        "status": 0,
        "rejectReason": null,
        "applyTime": "2024-01-10 10:00:00",
        "approveTime": null,
        "approverId": null,
        "createTime": "2024-01-10 10:00:00"
      },
      {
        "id": 2,
        "tenantId": 1,
        "amount": 3000.00,
        "bankName": "中国建设银行",
        "bankAccount": "6217001234567890123",
        "accountName": "张三",
        "status": 1,
        "rejectReason": null,
        "applyTime": "2024-01-05 10:00:00",
        "approveTime": "2024-01-05 15:00:00",
        "approverId": 1,
        "createTime": "2024-01-05 10:00:00"
      }
    ],
    "total": 10,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

---

### 4. 查询所有提现申请（管理端）

平台管理员查询所有商家的提现申请。

**接口地址**: `GET /withdrawal/admin/list`

**请求头**:
```
Authorization: Bearer {token}
```

**请求参数**:
```
tenantId=1&status=0&pageNum=1&pageSize=10
```

| 参数名 | 类型 | 必填 | 说明 | 默认值 |
|--------|------|------|------|--------|
| tenantId | Long | 否 | 商家ID（租户ID） | - |
| status | Integer | 否 | 状态（0-待审核，1-已通过，2-已拒绝） | - |
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
        "tenantId": 1,
        "merchantName": "测试商家",
        "amount": 5000.00,
        "bankName": "中国工商银行",
        "bankAccount": "6222021234567890123",
        "accountName": "张三",
        "status": 0,
        "rejectReason": null,
        "applyTime": "2024-01-10 10:00:00",
        "approveTime": null,
        "approverId": null,
        "approverName": null,
        "createTime": "2024-01-10 10:00:00"
      }
    ],
    "total": 5,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

---

### 5. 审核提现申请（管理端）

平台管理员审核商家的提现申请，可以通过或拒绝。

**接口地址**: `POST /withdrawal/admin/approve`

**请求头**:
```
Authorization: Bearer {token}
Content-Type: application/json
```

**请求参数**:
```json
{
  "withdrawalId": 1,
  "approved": true,
  "rejectReason": ""
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| withdrawalId | Long | 是 | 提现申请ID |
| approved | Boolean | 是 | 是否通过（true-通过，false-拒绝） |
| rejectReason | String | 否 | 拒绝原因（拒绝时必填） |

**响应示例（通过）**:
```json
{
  "code": 200,
  "message": "提现申请已通过",
  "data": null
}
```

**响应示例（拒绝）**:
```json
{
  "code": 200,
  "message": "提现申请已拒绝",
  "data": null
}
```

---

## 提现状态说明

| status | 说明 |
|--------|------|
| 0 | 待审核 |
| 1 | 已通过 |
| 2 | 已拒绝 |

---

## 业务规则

1. **提现金额限制**:
   - 单次提现金额必须大于0
   - 提现金额不能超过可用余额
   - 提现金额会从可用余额转入冻结余额

2. **审核流程**:
   - 提现申请提交后状态为"待审核"
   - 管理员审核通过后，冻结余额解冻并完成提现
   - 管理员审核拒绝后，冻结余额返回可用余额

3. **银行账户**:
   - 银行账号必须为数字
   - 账户名必须与商家注册信息一致

---

## 错误码

| 错误码 | 说明 |
|--------|------|
| 400 | 参数错误（如提现金额不合法、余额不足） |
| 401 | 未登录或登录已过期 |
| 403 | 无权限操作 |
| 404 | 提现申请不存在 |
| 500 | 服务器内部错误 |

---

## 使用示例

### cURL 示例

```bash
# 查询商家余额
curl -X GET http://localhost:8080/api/withdrawal/balance \
  -H "Authorization: Bearer {token}"

# 创建提现申请
curl -X POST http://localhost:8080/api/withdrawal/apply \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 5000.00,
    "bankName": "中国工商银行",
    "bankAccount": "6222021234567890123",
    "accountName": "张三"
  }'

# 查询提现记录
curl -X GET "http://localhost:8080/api/withdrawal/list?status=0&pageNum=1&pageSize=10" \
  -H "Authorization: Bearer {token}"

# 查询所有提现申请（管理端）
curl -X GET "http://localhost:8080/api/withdrawal/admin/list?pageNum=1&pageSize=10" \
  -H "Authorization: Bearer {token}"

# 审核通过提现申请
curl -X POST http://localhost:8080/api/withdrawal/admin/approve \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "withdrawalId": 1,
    "approved": true
  }'

# 拒绝提现申请
curl -X POST http://localhost:8080/api/withdrawal/admin/approve \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "withdrawalId": 1,
    "approved": false,
    "rejectReason": "银行账户信息有误"
  }'
```

---

## 注意事项

1. 提现申请提交后，对应金额会立即从可用余额转入冻结余额
2. 审核通过后，资金会在1-3个工作日内到账
3. 审核拒绝后，冻结金额会立即返回可用余额
4. 商家可以随时查看提现记录和审核状态
5. 建议在工作日提交提现申请，以便快速审核
6. 银行账户信息请务必填写准确，错误信息可能导致审核失败
