# 商家管理系统前端

## 项目简介

这是多商户电商平台的商家管理端前端应用，基于 Vue 3 + Element Plus 构建。

## 功能模块

### 1. 商家登录和首页
- 商家登录页面（支持用户名密码登录）
- 首页数据概览（今日销售额、订单数、本月销售额、账户余额）
- 快捷操作入口
- 待处理订单列表

### 2. 商品管理
- 商品列表（支持搜索、筛选、分页）
- 商品上架（支持图片上传）
- 商品编辑
- 商品上下架
- 商品删除

### 3. 订单管理
- 订单列表（支持搜索、筛选、分页）
- 订单详情查看
- 订单发货功能
- 订单状态跟踪

### 4. 提现管理
- 账户余额展示（可用余额、冻结余额、累计收入）
- 提现申请
- 提现记录查询

### 5. 销售数据
- 销售数据概览
- 销售趋势图表（基于 ECharts）
- 商品销售排行 TOP 10
- 销售报表导出

### 6. 营销管理
- 积分规则设置
- 积分兑换商品管理
- 充值规则设置

## 技术栈

- **Vue 3**: 渐进式 JavaScript 框架
- **Vue Router**: 官方路由管理器
- **Pinia**: 状态管理
- **Element Plus**: UI 组件库
- **Axios**: HTTP 客户端
- **ECharts**: 数据可视化图表库
- **Vite**: 前端构建工具

## 开发环境

### 环境要求
- Node.js >= 16.0.0
- npm >= 8.0.0

### 安装依赖
```bash
npm install
```

### 启动开发服务器
```bash
npm run dev
```

访问地址：http://localhost:3001

### 构建生产版本
```bash
npm run build
```

### 预览生产构建
```bash
npm run preview
```

## 项目结构

```
payment-frontend-admin/
├── src/
│   ├── api/              # API 接口封装
│   │   └── index.js      # Axios 实例配置
│   ├── router/           # 路由配置
│   │   └── index.js      # 路由定义
│   ├── stores/           # Pinia 状态管理
│   │   └── user.js       # 用户状态
│   ├── views/            # 页面组件
│   │   ├── Login.vue                # 登录页
│   │   ├── Dashboard.vue            # 首页
│   │   ├── ProductList.vue          # 商品列表
│   │   ├── ProductCreate.vue        # 商品上架
│   │   ├── ProductEdit.vue          # 商品编辑
│   │   ├── OrderList.vue            # 订单列表
│   │   ├── OrderDetail.vue          # 订单详情
│   │   ├── WithdrawalList.vue       # 提现管理
│   │   ├── SalesStatistics.vue      # 销售数据
│   │   └── MarketingManage.vue      # 营销管理
│   ├── App.vue           # 根组件
│   ├── main.js           # 入口文件
│   └── style.css         # 全局样式
├── index.html            # HTML 模板
├── package.json          # 项目配置
├── vite.config.js        # Vite 配置
└── README.md             # 项目文档
```

## API 接口

### 基础配置
- 基础 URL: `http://localhost:8080/api`
- 认证方式: Bearer Token (JWT)
- 代理配置: Vite 开发服务器已配置 `/api` 代理

### 主要接口
- 用户登录: `POST /user/login`
- 获取用户信息: `GET /user/info`
- 商品管理: `/product/*`
- 订单管理: `/order/*`
- 提现管理: `/withdrawal/*`
- 销售统计: `/sales/statistics/*`
- 积分管理: `/points/*`
- 充值管理: `/recharge/*`

## 路由说明

| 路径 | 组件 | 说明 |
|------|------|------|
| `/login` | Login | 登录页 |
| `/dashboard` | Dashboard | 首页 |
| `/product/list` | ProductList | 商品列表 |
| `/product/create` | ProductCreate | 商品上架 |
| `/product/edit/:id` | ProductEdit | 商品编辑 |
| `/order/list` | OrderList | 订单列表 |
| `/order/detail/:orderNo` | OrderDetail | 订单详情 |
| `/withdrawal/list` | WithdrawalList | 提现管理 |
| `/sales/statistics` | SalesStatistics | 销售数据 |
| `/marketing/manage` | MarketingManage | 营销管理 |

## 状态管理

### User Store
- `token`: JWT 令牌
- `username`: 用户名
- `merchantName`: 商家名称
- `userId`: 用户 ID
- `tenantId`: 租户 ID
- `login()`: 登录方法
- `logout()`: 登出方法
- `fetchUserInfo()`: 获取用户信息

## 注意事项

1. **认证**: 所有需要认证的接口都会自动添加 Bearer Token
2. **多租户**: 系统自动处理租户隔离，无需手动传递 tenantId
3. **图片上传**: 需要配置后端的阿里云 OSS 或其他图片存储服务
4. **跨域**: 开发环境使用 Vite 代理，生产环境需要配置 Nginx 反向代理

## 开发规范

1. 组件命名使用 PascalCase
2. 文件命名使用 PascalCase
3. 使用 Composition API（`<script setup>`）
4. 使用 Element Plus 组件库
5. 统一使用 Axios 进行 HTTP 请求
6. 错误处理统一在 Axios 拦截器中处理

## 浏览器支持

- Chrome >= 87
- Firefox >= 78
- Safari >= 14
- Edge >= 88

## 许可证

MIT
