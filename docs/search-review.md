# 搜索发现真链路 — 代码审核报告

> 审核范围：本轮"搜索发现真链路"全部新增/改动文件
> 审核时间：2026-06-03
> 审核人：Claude

---

## 一、审核范围

### 后端新增

| 文件 | 作用 |
|------|------|
| `entity/Store.java` | 门店实体（区域、经纬度、评分、服务标签） |
| `mapper/StoreMapper.java` | 门店 MyBatis-Plus Mapper |
| `dto/AppCatalogProductSearchQueryDTO.java` | 商品搜索入参 |
| `dto/AppCatalogTenantSearchQueryDTO.java` | 商户搜索入参 |
| `dto/AppCatalogSearchProductVO.java` | 商品搜索卡片出参 |
| `dto/AppCatalogSearchTenantVO.java` | 商户搜索卡片出参 |
| `service/AppCatalogSearchService.java` | 搜索服务接口 |
| `service/impl/AppCatalogSearchServiceImpl.java` | 搜索服务实现 |
| `controller/V1AppCatalogController.java` | 新增 `/search/products`、`/search/tenants` |
| `test/.../AppCatalogSearchServiceImplTest.java` | 6 个边界测试 |
| `sql/21_store_rating_migration.sql` | rating 字段幂等迁移 |

### 前端新增/改动

| 文件 | 作用 |
|------|------|
| `types/catalog.ts` | 新增 `CatalogSearchProduct`、`CatalogSearchTenant`、搜索参数类型 |
| `services/modules/appCatalog.ts` | 新增 `searchProducts`、`searchTenants` |
| `utils/catalogSearch.ts` | 统一搜索卡片适配器 |
| `pages/Home.tsx` | 搜索框接真实接口，支持分页/加载/空结果/错误态 |
| `pages/Discovery.tsx` | 分类/区域/评分/距离/排序全部走后端查询 |
| `pages/PublicMerchantDetail.tsx` | 店内商品按 tenantId + keyword/category/sort 查询 |

---

## 二、审核结论

| 级别 | 数量 | 说明 |
|------|------|------|
| CRITICAL | 0 | — |
| HIGH | 2 | 性能问题，生产数据量增长后会暴露 |
| MEDIUM | 2 | 正确性/安全边界 |
| LOW | 4 | 设计冗余、测试覆盖、前端展示 |

整体架构清晰，DTO → Service → Controller 分层合理，前端 `catalogSearch.ts` 适配器复用度好。

---

## 三、问题详情

### HIGH-1：N+1 查询 — `countProducts` 逐个查库

**文件**：`AppCatalogSearchServiceImpl.java`
**位置**：第 259-272 行

**问题**：`toTenantVO` 对每个 tenant 调用一次 `countProducts`，一页 10 个商户 = 10 次额外 SQL 查询。

```java
private Long countProducts(Long tenantId) {
    return productMapper.selectCount(new LambdaQueryWrapper<Product>()
            .eq(Product::getTenantId, tenantId)
            .eq(Product::getDeleted, 0)
            .eq(Product::getStatus, 1));
}
```

**建议**：改为批量查询后 Map 匹配。

```java
private Map<Long, Long> batchCountProducts(List<Long> tenantIds) {
    if (tenantIds == null || tenantIds.isEmpty()) {
        return Collections.emptyMap();
    }
    return productMapper.selectMaps(new LambdaQueryWrapper<Product>()
            .select(Product::getTenantId, "count(*) as cnt")
            .in(Product::getTenantId, tenantIds)
            .eq(Product::getDeleted, 0)
            .eq(Product::getStatus, 1)
            .groupBy(Product::getTenantId))
        .stream()
        .collect(Collectors.toMap(
            m -> ((Number) m.get("tenant_id")).longValue(),
            m -> ((Number) m.get("cnt")).longValue()));
}
```

在 `searchTenants` 方法中调用一次 `batchCountProducts`，把结果传给 `toTenantVO`。

---

### HIGH-2：商户搜索全量加载后内存分页

**文件**：`AppCatalogSearchServiceImpl.java`
**位置**：第 70-86 行

**问题**：`searchTenants` 先 `tenantMapper.selectList` 全量加载，再在 Java 内存中过滤、排序、分页。商户量增长后有 OOM 风险。

```java
List<Tenant> tenants = tenantMapper.selectList(buildTenantWrapper(safeQuery, storeContext));
// ... 全量 stream 过滤、排序 ...
List<AppCatalogSearchTenantVO> pageRecords = slicePage(filteredRecords, current, size);
```

当有 store 筛选条件时，`buildTenantWrapper` 会加 `IN (storeContext.tenantIds())` 限制范围，这部分问题不大。但**只传 keyword 不传任何 store 筛选**时，所有匹配的 tenant 都会加载到内存。

**建议**：

- 对 keyword-only 查询（无 store 筛选）走数据库分页 `tenantMapper.selectPage(...)`，只在有 store 筛选时才做内存分页。
- 或者至少给无 store 筛选路径加一个 `LIMIT` 兜底（如 `MAX_SIZE * 10`），防止全表扫描。

---

### MEDIUM-1：keyword 中的 SQL 通配符未转义

**文件**：`AppCatalogSearchServiceImpl.java`
**位置**：第 97-102 行（商品）、第 115-122 行（商户）

**问题**：keyword 直接传入 `.like()`，MyBatis-Plus 会生成 `LIKE '%keyword%'`。用户输入 `%` 或 `_` 会变成通配符，导致匹配范围异常。

```java
.like(Product::getName, keyword)  // keyword = "%" → 匹配所有行
```

**建议**：增加 LIKE 转义工具方法。

```java
private String escapeLike(String value) {
    if (value == null) return null;
    return value.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
}
```

调用 `.like()` 时传入 `escapeChar`：

```java
.like(StringUtils.hasText(keyword), Product::getName, escapeLike(keyword), '\\')
```

---

### MEDIUM-2：`minRating` 过滤逻辑在两层重复且不一致

**文件**：`AppCatalogSearchServiceImpl.java`
**位置**：第 159-166 行（buildStoreSearchContext）、第 196-213 行（buildStoreWrapper）、第 297-307 行（shouldKeepTenant）、第 371-376 行（normalizeMinRating）

**问题**：`minRating` 的过滤分散在三处：

1. `buildStoreWrapper`：`ge(minRating != null, Store::getRating, minRating)` — 数据库层
2. `shouldKeepTenant`：`vo.getRating().compareTo(minRating) >= 0` — Java 内存层
3. `normalizeMinRating`：负数返回 null，正数 clamp 到 5.00

当只传 `minRating` 不传其他 store 筛选条件时，`shouldLoadStores` 会因 `minRating != null` 变成 true，触发 store 查询。但 `normalizeMinRating` 对负数返回 null，会让 `shouldKeepTenant` 的检查失效（null = 不过滤），两层行为不一致。

**建议**：统一到一处过滤。要么只在 store 查询层过滤（去掉 `shouldKeepTenant` 中的 rating 检查），要么只在内存层过滤（去掉 `buildStoreWrapper` 中的 `ge` 条件）。

---

### LOW-1：Home 搜索结果排序不合理

**文件**：`Home.tsx`
**位置**：第 100 行

```typescript
setSearchResults([...tenants.records, ...products.records]);
```

先商户后商品硬拼接。用户搜"咖啡"会先看到所有商户再看到商品。

**建议**：按相关性交替排列，或统一按评分/距离排序。

---

### LOW-2：测试全是 Mock，未验证 DAO 层

**文件**：`AppCatalogSearchServiceImplTest.java`

6 个测试全部用 `mock(ProductMapper.class)`，只验证了 service 逻辑，没有验证：

- LambdaQueryWrapper 构建是否正确
- 实际 SQL 是否符合预期
- 数据库分页参数是否传递正确

**建议**：补一个 `@MybatisPlusTest` + H2 的集成测试，验证 wrapper 构建和分页参数。

---

### LOW-3：VO 里 `id` 和 `tenantId` 重复

**文件**：`AppCatalogSearchTenantVO.java` 第 14-16 行、`AppCatalogSearchProductVO.java` 第 14-16 行

```java
private Long id;       // 始终等于 tenantId
private Long tenantId; // 始终等于 id
```

前端 `CatalogSearchTenant` 也保留了两个字段，`catalogSearch.ts` 里到处做 `tenant.tenantId || tenant.id` 判断。

**建议**：去掉 `id`，统一用 `tenantId` 做标识，减少前端判断复杂度。

---

### LOW-4：`store.serviceTags` 的分类匹配用 LIKE 不精确

**文件**：`AppCatalogSearchServiceImpl.java`
**位置**：第 201 行

```java
.like(Store::getServiceTags, category)
```

如果 `serviceTags` 是 `"餐饮,轻食"`，搜"食"会命中，搜"餐饮"也会命中，但搜"轻"也会命中（子串匹配）。

**建议**：后端存 JSON 数组用 `JSON_CONTAINS`，或用独立的标签关联表做精确匹配。短期可接受，但标签量增长后需要升级。

---

## 四、亮点

| 项目 | 说明 |
|------|------|
| Store 实体分离 | 把位置/评分/服务标签从 Tenant 分离，职责清晰 |
| Haversine 距离计算 | 实现正确，`Math.atan2` 避免了极点和对踵点的数值问题 |
| 分页参数兜底 | `normalizeSize` 对 0/负数/超大值兜底到 `[1, 50]`，防止恶意大分页 |
| 迁移脚本幂等 | 用存储过程检查 `information_schema.columns`，生产环境安全可重复执行 |
| 前端适配器复用 | `catalogSearch.ts` 的 `toSearchCardView` 在三个页面统一使用，卡片字段一致 |
| 分类点击联动 | Home 分类图标点击跳转 `/discovery?category=xxx`，Discovery 从 URL 读取初始状态 |

---

## 五、建议修复优先级

| 优先级 | 问题 | 预计耗时 |
|--------|------|----------|
| P0 | HIGH-1：countProducts N+1 | 15 min |
| P0 | HIGH-2：searchTenants 全量加载 | 30 min |
| P1 | MEDIUM-1：LIKE 通配符转义 | 10 min |
| P1 | MEDIUM-2：minRating 两层过滤统一 | 15 min |
| P2 | LOW-1 ~ LOW-4 | 各 5-10 min |

---

## 六、验证清单

- [x] `npm run lint` 通过
- [x] `npm run build` 通过（原有 CSS @import 警告非本轮引入）
- [x] `mvn -DskipTests compile` 通过
- [x] `mvn '-Dtest=AppCatalogSearchServiceImplTest' test` 6 个测试通过
- [ ] HIGH-1 修复后跑测试验证 N+1 消除
- [ ] HIGH-2 修复后跑测试验证分页正确性
- [ ] MEDIUM-1 修复后补 `%` 和 `_` 输入的测试用例
