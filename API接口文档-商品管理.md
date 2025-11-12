# API 接口文档 - 商品管理模块

## 基础信息

- **Base URL**: `http://localhost:8080/api`
- **认证方式**: Bearer Token (JWT)
- **请求头**:
  ```
  Content-Type: application/json
  Authorization: Bearer {token}
  ```

## 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

**状态码说明**:
- `200`: 成功
- `400`: 请求参数错误
- `401`: 未授权
- `403`: 无权限
- `404`: 资源不存在
- `500`: 服务器错误

---

## 1. 商品列表

### 接口信息
- **URL**: `/product/list`
- **Method**: `GET`
- **权限**: 需要登录

### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| current | Integer | 否 | 当前页，默认1 |
| size | Integer | 否 | 每页数量，默认10 |
| keyword | String | 否 | 搜索关键词 |
| category | String | 否 | 商品分类 |
| status | Integer | 否 | 状态：0-下架，1-上架 |

### 请求示例

```http
GET /api/product/list?current=1&size=10&keyword=手机&status=1
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
        "productCode": "P001",
        "name": "iPhone 15 Pro",
        "price": 7999.00,
        "unit": "台",
        "category": "电子产品",
        "imageUrl": "https://example.com/images/iphone15.jpg",
        "description": "最新款iPhone",
        "status": 1,
        "createTime": "2024-01-01 10:00:00",
        "updateTime": "2024-01-01 10:00:00"
      },
      {
        "id": 2,
        "tenantId": 1,
        "productCode": "P002",
        "name": "华为 Mate 60",
        "price": 6999.00,
        "unit": "台",
        "category": "电子产品",
        "imageUrl": "https://example.com/images/mate60.jpg",
        "description": "华为旗舰手机",
        "status": 1,
        "createTime": "2024-01-02 10:00:00",
        "updateTime": "2024-01-02 10:00:00"
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

## 2. 商品详情

### 接口信息
- **URL**: `/product/{id}`
- **Method**: `GET`
- **权限**: 需要登录

### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 商品ID（路径参数） |

### 请求示例

```http
GET /api/product/1
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "tenantId": 1,
    "productCode": "P001",
    "name": "iPhone 15 Pro",
    "price": 7999.00,
    "unit": "台",
    "category": "电子产品",
    "imageUrl": "https://example.com/images/iphone15.jpg",
    "description": "最新款iPhone，搭载A17 Pro芯片，支持5G网络",
    "status": 1,
    "stock": 100,
    "createTime": "2024-01-01 10:00:00",
    "updateTime": "2024-01-01 10:00:00"
  }
}
```

---

## 3. 创建商品

### 接口信息
- **URL**: `/product/create`
- **Method**: `POST`
- **权限**: 需要管理员权限

### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| productCode | String | 是 | 商品编码 |
| name | String | 是 | 商品名称 |
| price | BigDecimal | 是 | 单价 |
| unit | String | 否 | 单位 |
| category | String | 否 | 分类 |
| imageUrl | String | 否 | 图片URL |
| description | String | 否 | 描述 |
| status | Integer | 否 | 状态，默认1 |

### 请求示例

```http
POST /api/product/create
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "productCode": "P003",
  "name": "小米14 Ultra",
  "price": 5999.00,
  "unit": "台",
  "category": "电子产品",
  "imageUrl": "https://example.com/images/mi14.jpg",
  "description": "小米旗舰手机，徕卡影像",
  "status": 1
}
```

### 响应示例

```json
{
  "code": 200,
  "message": "商品创建成功",
  "data": {
    "id": 3,
    "tenantId": 1,
    "productCode": "P003",
    "name": "小米14 Ultra",
    "price": 5999.00,
    "unit": "台",
    "category": "电子产品",
    "imageUrl": "https://example.com/images/mi14.jpg",
    "description": "小米旗舰手机，徕卡影像",
    "status": 1,
    "createTime": "2024-01-03 10:00:00",
    "updateTime": "2024-01-03 10:00:00"
  }
}
```

---

## 4. 更新商品

### 接口信息
- **URL**: `/product/update/{id}`
- **Method**: `PUT`
- **权限**: 需要管理员权限

### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 商品ID（路径参数） |
| name | String | 否 | 商品名称 |
| price | BigDecimal | 否 | 单价 |
| unit | String | 否 | 单位 |
| category | String | 否 | 分类 |
| imageUrl | String | 否 | 图片URL |
| description | String | 否 | 描述 |
| status | Integer | 否 | 状态 |

### 请求示例

```http
PUT /api/product/update/3
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "name": "小米14 Ultra（升级版）",
  "price": 6299.00,
  "description": "小米旗舰手机，徕卡影像，升级版"
}
```

### 响应示例

```json
{
  "code": 200,
  "message": "商品更新成功",
  "data": null
}
```

---

## 5. 删除商品

### 接口信息
- **URL**: `/product/delete/{id}`
- **Method**: `DELETE`
- **权限**: 需要管理员权限

### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 商品ID（路径参数） |

### 请求示例

```http
DELETE /api/product/delete/3
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 响应示例

```json
{
  "code": 200,
  "message": "商品删除成功",
  "data": null
}
```

---

## 6. 商品搜索

### 接口信息
- **URL**: `/product/search`
- **Method**: `GET`
- **权限**: 需要登录

### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| keyword | String | 是 | 搜索关键词 |
| current | Integer | 否 | 当前页，默认1 |
| size | Integer | 否 | 每页数量，默认10 |

### 请求示例

```http
GET /api/product/search?keyword=手机&current=1&size=10
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
        "productCode": "P001",
        "name": "iPhone 15 Pro",
        "price": 7999.00,
        "category": "电子产品",
        "imageUrl": "https://example.com/images/iphone15.jpg",
        "status": 1
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

---

## 错误响应示例

### 参数错误

```json
{
  "code": 400,
  "message": "商品名称不能为空",
  "data": null
}
```

### 未授权

```json
{
  "code": 401,
  "message": "未登录或登录已过期",
  "data": null
}
```

### 无权限

```json
{
  "code": 403,
  "message": "无权限访问",
  "data": null
}
```

### 资源不存在

```json
{
  "code": 404,
  "message": "商品不存在",
  "data": null
}
```

### 服务器错误

```json
{
  "code": 500,
  "message": "服务器内部错误",
  "data": null
}
```
