# 前端页面规格说明书 (Wave 3)

> 最后更新：2026-06-07
> 适用目录：`salessystem/`

---

## 目录

1. [Discovery 搜索/筛选功能化](#1-discovery-搜索筛选功能化)
2. [AdminProducts 接真数据](#2-adminproducts-接真数据)

---

## 1. Discovery 搜索/筛选功能化

### 现状
`salessystem/src/pages/Discovery.tsx` 已有搜索框和分类标签 UI，搜索已绑定本地过滤（按 name/address 模糊匹配），分类通过 URL 参数同步。

### 需要改进

#### 1.1 搜索功能增强
当前搜索只在已加载的 `stores` 数组中做本地 `filter`，商户多了之后不够用。

**改造方案**：
- 保持本地过滤（当前已可用），无需后端搜索API（后端暂无全局搜索端点）
- 搜索框增加 debounce（300ms），避免每次输入都触发 filter
- 搜索无结果时显示空状态提示

#### 1.2 分类筛选增强
当前分类是硬编码的 5 个 tab：`['全部分类', '餐饮', '零售', '服务', '娱乐']`。
商户分类分配用 `storeId % 4` 模拟，不是真实数据。

**改造方案**：
- 如果后端 Tenant 实体有 `category` 字段，用真实分类替换模拟分类
- 如果没有，保留当前模拟方案但加 TODO 注释
- 分类 Tab 支持横向滚动（移动端已有 `overflow-x-auto`）

#### 1.3 筛选联动 URL 参数
当前已支持 `?category=xxx` 参数同步，这部分已可用。

**增强**：搜索关键词也同步到 URL：`?category=xxx&keyword=xxx`，支持分享搜索结果链接。

### 无需后端配合，纯前端改造

---

## 2. AdminProducts 接真数据

### 现状
`AdminProducts.tsx` 使用 5 条硬编码假数据，完全不调用后端 API。

### 后端 API 现状
- **无专用管理端商品接口**
- `V1AppCatalogController` 提供 `GET /v1/app/catalog/tenants` 和 `GET /v1/app/catalog/tenants/{tenantId}/products`
- `V1MerchantProductController` 提供商户端商品 CRUD

### 方案A：复用现有接口（推荐，无需后端改动）

管理端商品页面可以复用 Catalog 接口：

1. 先调 `GET /v1/app/catalog/tenants` 获取所有商户列表
2. 遍历商户，调 `GET /v1/app/catalog/tenants/{tenantId}/products` 获取每个商户的商品
3. 前端合并所有商品列表，增加"商户名称"列

**前端改造**：

```typescript
// services/modules/adminProducts.ts
import { appCatalogService } from './appCatalog';

/** 获取全平台商品列表（复用 Catalog API） */
export async function getAllProducts() {
  const tenants = await appCatalogService.listTenants();
  const productGroups = await Promise.all(
    tenants.map(async (tenant) => {
      try {
        const products = await appCatalogService.listTenantProducts(tenant.id);
        return products.map(p => ({ ...p, tenantName: tenant.name }));
      } catch {
        return [];
      }
    })
  );
  return productGroups.flat();
}
```

**AdminProducts.tsx 改造要点**：
- 删除硬编码 `products` 数组
- useEffect 中调用 `getAllProducts()` 加载真实数据
- 表格新增「商户」列显示 `tenantName`
- 价格格式化：后端返回的是 `ProductVO`，金额是 Long（分），用 `formatCurrency(price)` 展示
- 搜索功能：按商品名称/商户名称本地过滤
- 分页：如商品量大，可加分页（后端 Catalog 接口暂无分页，先加载全部）

### 方案B：新建管理端商品接口（如果需要分页/筛选）

后端新增 `V1AdminProductController`：
```
GET /v1/admin/products?pageNum=1&pageSize=20&keyword=&tenantId=
→ PageResult<ProductWithTenantVO>
```

**如果选择方案B，我来派后端团队建接口。**

### 前端类型
```typescript
interface AdminProduct extends Product {
  tenantName: string;
}
```

### 页面改造后布局
```
┌──────────────────────────────────────────┐
│  全平台商品库                             │
├──────────────────────────────────────────┤
│  搜索: [搜索商品名/商户名...]              │
│  筛选: [全部商户 ▼] [全部分类 ▼]           │
├──────────────────────────────────────────┤
│  表格:                                    │
│  商品名 | 商户 | 价格 | 分类 | 库存 | 状态│
│  ...（真实数据）                           │
└──────────────────────────────────────────┘
```

---

## 通用约定
- 路由守卫：管理端页面需 `<AuthGuard><RoleGuard allowedRoles={['admin']}>`
- 金额：后端 VO 返回 Long（分），用 `formatCurrency` 展示
- Service 复用：尽量复用已有的 `appCatalogService`
