# API 接口文档 - 用户管理模块

## 模块说明

用户管理模块提供用户登录、注册和信息查询等功能。

**Base URL**: `/api/user`

**认证方式**: Bearer Token (JWT)

---

## 接口列表

### 1. 用户登录

用户登录获取访问令牌。

**接口地址**: `POST /user/login`

**请求头**:
```
Content-Type: application/json
```

**请求参数**:
```json
{
  "username": "user001",
  "password": "123456"
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| username | String | 是 | 用户名 |
| password | String | 是 | 密码 |

**响应示例**:
```json
{
  "code": 200,
  "message": "登录成功",
  "data": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

---

### 2. 用户注册

注册新用户账号。

**接口地址**: `POST /user/register`

**请求头**:
```
Content-Type: application/json
```

**请求参数**:
```json
{
  "username": "user001",
  "password": "123456",
  "nickname": "张三",
  "phone": "13800138000",
  "email": "user@example.com"
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| username | String | 是 | 用户名（唯一） |
| password | String | 是 | 密码 |
| nickname | String | 否 | 昵称 |
| phone | String | 否 | 手机号 |
| email | String | 否 | 邮箱 |

**响应示例**:
```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "id": 1,
    "username": "user001",
    "nickname": "张三",
    "phone": "13800138000",
    "email": "user@example.com",
    "userType": 1,
    "status": 1,
    "createTime": "2024-01-01 10:00:00"
  }
}
```

---

### 3. 获取当前用户信息

获取当前登录用户的详细信息。

**接口地址**: `GET /user/info`

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
    "username": "user001",
    "nickname": "张三",
    "phone": "13800138000",
    "email": "user@example.com",
    "avatar": "https://example.com/avatar.jpg",
    "userType": 1,
    "status": 1,
    "createTime": "2024-01-01 10:00:00",
    "updateTime": "2024-01-01 10:00:00"
  }
}
```

---

## 用户类型说明

| userType | 说明 |
|----------|------|
| 1 | 普通用户 |
| 2 | 管理员 |

## 用户状态说明

| status | 说明 |
|--------|------|
| 0 | 禁用 |
| 1 | 启用 |

---

## 错误码

| 错误码 | 说明 |
|--------|------|
| 400 | 参数错误（如用户名已存在） |
| 401 | 未登录或登录已过期 |
| 403 | 用户已被禁用 |
| 500 | 服务器内部错误 |

---

## 使用示例

### cURL 示例

```bash
# 用户登录
curl -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user001",
    "password": "123456"
  }'

# 用户注册
curl -X POST http://localhost:8080/api/user/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user001",
    "password": "123456",
    "nickname": "张三",
    "phone": "13800138000"
  }'

# 获取用户信息
curl -X GET http://localhost:8080/api/user/info \
  -H "Authorization: Bearer {token}"
```

---

## 注意事项

1. 密码在传输前建议进行加密处理
2. Token 默认有效期为 24 小时
3. 用户名一旦注册不可修改
4. 手机号和邮箱需要符合格式要求
