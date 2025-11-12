# POS 收银功能说明

## 功能概述

POS 收银台是一个线下交易收银系统，支持商品搜索、扫码、购物车管理和结账功能。

## 访问路径

- 路由：`/pos/checkout`
- 页面：`PosCheckout.vue`

## 主要功能

### 1. 商品选择

#### 手动搜索
- 在搜索框输入商品名称或编码
- 点击搜索按钮或按回车键搜索
- 点击商品行或"加入"按钮添加到购物车

#### 扫码枪输入
- 将光标聚焦到"扫码枪"输入框（快捷键：F2）
- 使用扫码枪扫描商品条码
- 商品自动添加到购物车

### 2. 购物车管理

#### 添加商品
- 通过搜索列表点击商品
- 通过扫码枪扫描商品条码
- 通过 API：`POST /pos/cart/{sessionId}/add`

#### 修改数量
- 在购物车中使用数字输入框调整商品数量
- API：`PUT /pos/cart/{sessionId}/update`

#### 移除商品
- 点击商品右侧的删除按钮
- API：`DELETE /pos/cart/{sessionId}/remove/{productId}`

#### 清空购物车
- 点击购物车标题栏的"清空"按钮
- API：`DELETE /pos/cart/{sessionId}`

### 3. 结账

- 点击"结账"按钮
- 确认订单信息（商品数量、金额）
- 创建订单并跳转到订单详情页面
- API：`POST /pos/checkout/{sessionId}`

## 快捷键

- **F2**：聚焦到扫码输入框
- **F12**：快速结账（购物车不为空时）
- **Enter**：在搜索框或扫码框按回车执行相应操作

## 后端 API

### 1. 添加商品到购物车
```
POST /pos/cart/{sessionId}/add
Body: {
  "productId": 商品ID,
  "quantity": 数量
}
```

### 2. 移除购物车商品
```
DELETE /pos/cart/{sessionId}/remove/{productId}
```

### 3. 更新商品数量
```
PUT /pos/cart/{sessionId}/update
Body: {
  "productId": 商品ID,
  "quantity": 新数量
}
```

### 4. 查询购物车
```
GET /pos/cart/{sessionId}
```

### 5. 清空购物车
```
DELETE /pos/cart/{sessionId}
```

### 6. 结账
```
POST /pos/checkout/{sessionId}
```

### 7. 扫码（可选）
```
POST /pos/scan
Body: {
  "productCode": 商品条码,
  "deviceId": 设备ID,
  "quantity": 数量
}
```

## 技术实现

### 前端
- Vue 3 + Element Plus
- 实时购物车更新
- 键盘快捷键支持
- 响应式布局

### 后端
- Spring Boot
- Redis 存储购物车（30分钟过期）
- 会话管理（sessionId）
- 商品库存验证

## 数据流程

1. **会话创建**：页面加载时生成唯一的 sessionId
2. **商品添加**：商品信息存储到 Redis Hash 中
3. **购物车查询**：从 Redis 读取当前会话的购物车数据
4. **结账**：创建订单并清空购物车

## 注意事项

1. 购物车数据存储在 Redis 中，30分钟后自动过期
2. 每次操作购物车都会刷新过期时间
3. 商品必须属于当前登录商家
4. 商品必须是上架状态才能添加
5. 结账后购物车自动清空
