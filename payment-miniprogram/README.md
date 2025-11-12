# 多商户电商平台 - 微信小程序用户端

## 项目简介

这是一个多商户电商平台的微信小程序用户端，支持商品浏览、下单购买、积分兑换、余额充值等功能。

## 功能特性

### 1. 商品浏览
- 首页商品推荐
- 商品列表（支持分类、排序）
- 商品详情
- 商品搜索（集成Elasticsearch）

### 2. 订单管理
- 创建订单
- 订单列表（支持状态筛选）
- 订单详情
- 取消订单
- 确认收货

### 3. 支付功能
- 微信支付
- 余额支付
- 组合支付（余额+微信）
- 支付结果页面

### 4. 积分系统
- 积分余额查询
- 积分明细
- 积分兑换商品

### 5. 充值功能
- 余额充值
- 充值规则选择
- 余额明细

### 6. 用户中心
- 用户信息展示
- 资产信息（积分、余额）
- 订单快捷入口
- 功能菜单

## 技术栈

- 微信小程序原生开发
- ES6+
- Promise
- 模块化开发

## 项目结构

```
payment-miniprogram/
├── api/                    # API接口
│   ├── product.js         # 商品接口
│   ├── order.js           # 订单接口
│   ├── payment.js         # 支付接口
│   └── user.js            # 用户接口
├── pages/                 # 页面
│   ├── index/            # 首页
│   ├── product/          # 商品相关页面
│   │   ├── list/        # 商品列表
│   │   ├── detail/      # 商品详情
│   │   └── search/      # 商品搜索
│   ├── order/           # 订单相关页面
│   │   ├── create/      # 创建订单
│   │   ├── list/        # 订单列表
│   │   └── detail/      # 订单详情
│   ├── payment/         # 支付相关页面
│   │   ├── pay/         # 支付页面
│   │   └── result/      # 支付结果
│   └── user/            # 用户相关页面
│       ├── index/       # 个人中心
│       ├── points/      # 我的积分
│       ├── exchange/    # 积分兑换
│       ├── recharge/    # 充值中心
│       └── balance/     # 我的余额
├── utils/                # 工具类
│   ├── request.js       # 网络请求封装
│   └── util.js          # 工具函数
├── images/              # 图片资源
├── app.js               # 小程序入口
├── app.json             # 小程序配置
├── app.wxss             # 全局样式
└── project.config.json  # 项目配置

```

## 开发指南

### 1. 环境准备

- 安装微信开发者工具
- 注册微信小程序账号
- 获取AppID

### 2. 配置

1. 修改 `project.config.json` 中的 `appid` 为你的小程序AppID
2. 修改 `app.js` 中的 `apiBase` 为后端API地址

```javascript
globalData: {
  apiBase: 'http://your-api-domain.com/api'
}
```

### 3. 运行

1. 使用微信开发者工具打开项目
2. 编译运行

### 4. 图片资源

需要准备以下图片资源（放在 `images/` 目录）：

- 首页相关：home.png, home-active.png, banner1-3.jpg, category图标
- 商品相关：product.png, product-active.png, search.png, empty.png
- 订单相关：order.png, order-active.png, order状态图标
- 用户相关：user.png, user-active.png, avatar-default.png
- 支付相关：wechat-pay.png, success.png, fail.png
- 功能图标：points.png, exchange.png, recharge.png, balance.png
- 通用图标：arrow-right.png, checked.png, unchecked.png, close.png, delete.png, plus.png, minus.png

## API接口说明

### 基础配置

- 请求地址：`{apiBase}/miniprogram/*`
- 认证方式：JWT Token（放在请求头 `Authorization: Bearer {token}`）
- 返回格式：JSON

### 接口列表

#### 用户相关
- `POST /miniprogram/login` - 微信登录
- `GET /miniprogram/user/info` - 获取用户信息

#### 商品相关
- `GET /miniprogram/products` - 商品列表
- `GET /miniprogram/products/{id}` - 商品详情
- `GET /miniprogram/products/search` - 搜索商品

#### 订单相关
- `POST /miniprogram/orders` - 创建订单
- `GET /miniprogram/orders` - 订单列表
- `GET /miniprogram/orders/{orderNo}` - 订单详情
- `POST /miniprogram/orders/{orderNo}/cancel` - 取消订单
- `POST /miniprogram/orders/{orderNo}/confirm` - 确认收货

#### 支付相关
- `POST /miniprogram/payment/create` - 创建支付
- `GET /miniprogram/payment/status/{orderNo}` - 查询支付状态

#### 积分相关
- `GET /miniprogram/points/balance` - 积分余额
- `GET /miniprogram/points/logs` - 积分明细
- `GET /miniprogram/points/exchange-products` - 兑换商品列表
- `POST /miniprogram/points/exchange` - 积分兑换

#### 充值相关
- `GET /miniprogram/recharge/balance` - 余额查询
- `GET /miniprogram/recharge/balance-logs` - 余额明细
- `GET /miniprogram/recharge/rules` - 充值规则
- `POST /miniprogram/recharge/create` - 创建充值订单

## 注意事项

1. 小程序需要配置服务器域名白名单
2. 微信支付需要配置商户号和密钥
3. 图片资源需要使用HTTPS地址
4. 开发时可以在开发者工具中关闭域名校验

## 后续优化

- [ ] 添加商品收藏功能
- [ ] 添加购物车功能
- [ ] 添加优惠券功能
- [ ] 添加分享功能
- [ ] 添加客服功能
- [ ] 性能优化（图片懒加载、分包加载）
- [ ] 添加埋点统计

## 许可证

MIT
