# 个人中心基础能力 — 代码审核报告

> 审核范围：收货地址、账号安全、消息通知、钱包/积分占位
> 审核时间：2026-06-03
> 审核人：Claude

---

## 一、审核范围

### 后端新增

| 文件 | 作用 |
|------|------|
| `entity/UserShippingAddress.java` | 收货地址实体 |
| `entity/UserNotification.java` | 用户通知实体 |
| `mapper/UserShippingAddressMapper.java` | 地址 Mapper（含 `clearDefaultByPlatformUserId` 原子 SQL） |
| `mapper/UserNotificationMapper.java` | 通知 Mapper |
| `dto/UserShippingAddressDTO.java` | 地址新增/编辑入参 |
| `dto/AppAccountSecurityVO.java` | 账号安全摘要出参 |
| `dto/AppChangePasswordDTO.java` | 修改密码入参 |
| `service/UserShippingAddressService.java` | 地址服务接口 |
| `service/UserNotificationService.java` | 通知服务接口 |
| `service/AppAccountSecurityService.java` | 账号安全服务接口 |
| `service/impl/UserShippingAddressServiceImpl.java` | 地址服务实现 |
| `service/impl/UserNotificationServiceImpl.java` | 通知服务实现 |
| `service/impl/AppAccountSecurityServiceImpl.java` | 账号安全服务实现 |
| `controller/V1AppUserController.java` | 新增地址/安全/通知/密码/邮箱绑定端点 |
| `sql/22_user_shipping_address.sql` | 地址表建表（含索引） |
| `sql/23_user_notification.sql` | 通知表建表（含索引） |
| `test/.../UserShippingAddressServiceImplTest.java` | 地址服务 4 个测试 |
| `test/.../UserNotificationServiceImplTest.java` | 通知服务 3 个测试 |
| `test/.../AppAccountSecurityServiceImplTest.java` | 账号安全 3 个测试 |

### 前端新增/改动

| 文件 | 作用 |
|------|------|
| `features/profileCenter.ts` | 地址纯函数 + 钱包占位数据结构 |
| `features/profileCenter.test.ts` | 前端 2 个 node:test |
| `services/modules/appUser.ts` | 地址/安全/通知/邮箱绑定 API 封装 |
| `pages/Profile.tsx` | 地址 CRUD + 账号安全 + 通知 + 邮箱绑定 + 密码修改 |
| `pages/Wallet.tsx` | 接真实钱包/积分接口 + 优惠券/会员等级/标签占位 |

---

## 二、审核结论

| 级别 | 数量 | 说明 |
|------|------|------|
| CRITICAL | 0 | — |
| HIGH | 2 | 数据安全 + 接口安全 |
| MEDIUM | 4 | 分页、代码组织、设计冗余 |
| LOW | 4 | 类型、语言、冗余代码 |

整体实现质量良好，地址默认约束三个场景（首地址自动默认、删除后提升、设置时清理）覆盖完整，权限隔离到位。

---

## 三、问题详情

### HIGH-1：地址删除用了硬删除，与 `deleted` 字段矛盾

**文件**：`UserShippingAddressServiceImpl.java`
**位置**：第 82-89 行

**问题**：实体有 `deleted` 字段，`baseUserWrapper` 按 `deleted = 0` 过滤，`requireOwnedAddress` 也检查 `deleted == 1`，说明设计意图是软删除。但 `delete` 方法调用了 `addressMapper.deleteById(addressId)`，执行物理删除。

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void delete(Long platformUserId, Long addressId) {
    UserShippingAddress address = requireOwnedAddress(platformUserId, addressId);
    boolean wasDefault = Integer.valueOf(1).equals(address.getIsDefault());
    addressMapper.deleteById(addressId);  // 物理删除，与 deleted 字段矛盾

    if (wasDefault) {
        promoteFirstAddressAsDefault(platformUserId);
    }
}
```

**后果**：

- 如果未来有订单关联地址 ID，硬删除后订单引用的地址不存在
- `requireOwnedAddress` 检查 `deleted == 1` 永远不会生效（硬删除后记录已不存在）
- 并发场景下 `promoteFirstAddressAsDefault` 可能拿到脏数据

**修复**：

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void delete(Long platformUserId, Long addressId) {
    UserShippingAddress address = requireOwnedAddress(platformUserId, addressId);
    boolean wasDefault = Integer.valueOf(1).equals(address.getIsDefault());
    address.setDeleted(1);
    addressMapper.updateById(address);  // 软删除

    if (wasDefault) {
        promoteFirstAddressAsDefault(platformUserId);
    }
}
```

---

### HIGH-2：修改密码接口没有限流

**文件**：`V1AppUserController.java`
**位置**：第 149-155 行

**问题**：邮箱绑定接口有 `@RateLimit`，但密码修改接口没有。攻击者可以无限尝试旧密码。

```java
@SaCheckLogin
@PutMapping("/password")
public Result<Void> changePassword(@Valid @RequestBody AppChangePasswordDTO dto) {
    // 没有 @RateLimit
}
```

同时 `AppChangePasswordDTO.oldPassword` 只有 `@NotBlank`，没有 `@Size` 限制。`newPassword` 有 `@Size(min = 6, max = 64)`，但 `oldPassword` 可以传任意长度字符串。

**修复**：

```java
@SaCheckLogin
@RateLimit(
    prefix = "app:user:password:change",
    key = "#platformUserId",
    window = 300,
    maxRequests = 5,
    includeIp = true,
    message = "密码修改过于频繁，请稍后再试"
)
@PutMapping("/password")
public Result<Void> changePassword(@Valid @RequestBody AppChangePasswordDTO dto) {
```

`AppChangePasswordDTO` 中给 `oldPassword` 加 `@Size(max = 64)`。

---

### MEDIUM-1：通知列表无分页

**文件**：`UserNotificationServiceImpl.java`
**位置**：第 28-35 行

**问题**：`list` 方法全量返回用户所有通知，老用户通知积累后会一次返回大量数据。

```java
public List<UserNotification> list(Long platformUserId) {
    return notificationMapper.selectList(new LambdaQueryWrapper<UserNotification>()
            .eq(UserNotification::getPlatformUserId, platformUserId)
            .eq(UserNotification::getDeleted, 0)
            .orderByAsc(UserNotification::getReadStatus)
            .orderByDesc(UserNotification::getCreateTime)
            .orderByDesc(UserNotification::getId));
}
```

**修复**：改为 `Page<UserNotification>`，前端传 `current`/`size`，默认 `size = 20`。接口签名改为：

```java
Page<UserNotification> list(Long platformUserId, Integer current, Integer size);
```

---

### MEDIUM-2：Profile.tsx 780 行，职责过多

**文件**：`Profile.tsx`

**问题**：一个组件包含地址 CRUD 表单、地址列表、账号安全摘要、邮箱绑定流程、密码修改、通知列表、菜单导航。状态变量 18 个。

`AddressSection` 虽然已拆为独立函数组件，但仍在同一文件中（第 514-613 行），且与 `SecurityItem`、`EmailBindPanel`、`ProfileInput`、`IconButton` 等辅助组件混在一起。

**建议**：

- 将 `AddressSection`、`SecuritySection`、`NotificationSection` 拆到独立文件
- 每个 section 有自己的 `useEffect` 和状态管理
- `Profile.tsx` 只负责布局和数据分发，目标控制在 200 行以内

---

### MEDIUM-3：`BCryptPasswordEncoder` 重复实例化

**文件**：`AppAccountSecurityServiceImpl.java` 第 35 行、`PlatformEmailAccountServiceImpl.java` 第 27 行

**问题**：两个 Service 各自 `new BCryptPasswordEncoder()`，Spring Boot 自动配置了 `PasswordEncoder` Bean。

```java
// AppAccountSecurityServiceImpl
private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

// PlatformEmailAccountServiceImpl
private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
```

**修复**：通过构造器注入统一使用 Spring 管理的 Bean。

```java
private final PasswordEncoder passwordEncoder; // 构造器注入
```

---

### MEDIUM-4：`update` 方法无法取消默认地址

**文件**：`UserShippingAddressServiceImpl.java`
**位置**：第 72 行

```java
address.setIsDefault(defaultAddress ? 1 : address.getIsDefault());
```

当 `dto.isDefault = false` 时，保持原值不变。用户无法通过更新接口取消默认状态，行为不透明。

**建议**：

- 如果取消默认且该地址是唯一地址，拒绝并提示"至少保留一个默认地址"
- 如果取消默认且有其他地址，允许取消（不做自动提升）

---

### LOW-1：验证消息语言不一致

**文件**：`UserShippingAddressDTO.java`、`AppChangePasswordDTO.java`

```java
@NotBlank(message = "Receiver name is required")   // 英文
@Size(max = 50, message = "Phone must be within 20 characters")  // 英文
```

项目是中文系统，UI 和业务异常（`BusinessException`）都用中文，DTO 验证消息应统一为中文：

```java
@NotBlank(message = "收货人不能为空")
@Size(max = 50, message = "收货人不超过 50 个字符")
```

---

### LOW-2：前端 `ShippingAddress.id` 类型是 `string | number`

**文件**：`profileCenter.ts` 第 2 行

```typescript
id: string | number;
```

后端 `UserShippingAddress.id` 是 `Long`，JSON 序列化后是 `number`。`string | number` 来源于 `profileCenter.ts` 的本地函数用 `Date.now()` 生成 string ID（第 51 行），但真实接口永远返回 number。

**建议**：改为 `number`，本地函数也改为 `number`。

---

### LOW-3：`markRead` 没有检查已读状态

**文件**：`UserNotificationServiceImpl.java`
**位置**：第 42-54 行

已读通知再调用 `markRead` 会重复写入 `readStatus = 1` 和 `readTime`。功能不影响但浪费写操作。

**修复**：

```java
if (Integer.valueOf(1).equals(notification.getReadStatus())) {
    return notification;  // 已读直接返回
}
```

---

### LOW-4：前端 `profileCenter.ts` 的本地地址函数已冗余

**文件**：`profileCenter.ts` 第 44-74 行

`createAddress`、`updateAddress`、`deleteAddress`、`setDefaultAddress` 四个纯函数是前端本地状态管理用的，但地址 CRUD 已接真实后端接口。这些函数只在 `profileCenter.test.ts` 中使用。

**建议**：要么删除（测试也删），要么明确标记为"离线模式 / mock 模式"用途，避免后续维护者误以为仍在使用。

---

## 四、亮点

| 项目 | 说明 |
|------|------|
| 地址默认约束 | 首地址自动默认、删除默认后自动提升、设置默认时先清理旧默认，三个场景覆盖完整 |
| 权限隔离 | 地址/通知都做了 `platformUserId` 归属校验（`requireOwnedAddress`、`markRead` 中的 `!platformUserId.equals(...)`），防止越权操作 |
| 原子 SQL 清理默认 | `clearDefaultByPlatformUserId` 用 `@Update` 批量更新，避免逐条清理的并发问题 |
| SQL 索引设计 | `user_shipping_address` 的 `idx_user_shipping_address_user` 覆盖 `(platform_user_id, deleted, is_default)`，查询高效；`user_notification` 的 `idx_user_notification_user_read` 覆盖 `(platform_user_id, read_status, create_time)` |
| 脱敏输出 | `maskPhone` / `maskEmail` 在账号安全摘要中返回脱敏信息，不暴露原始手机号和邮箱 |
| `isDefault` 类型归一化 | 前端 `normalizeShippingAddress` 处理后端返回 `0/1` 和 `boolean` 的差异 |
| 钱包/积分占位 | `getWalletAccessItems` 结构清晰，明确标注"待接口"，数据结构已就绪，后续接入零改动 |
| 测试覆盖 | 后端 10 个 Mock 测试覆盖核心场景（自动默认、越权拒绝、删除提升、已读写入、密码校验）；前端 2 个 node:test 覆盖地址纯函数和钱包占位 |

---

## 五、修复优先级

| 优先级 | 问题 | 预计耗时 |
|--------|------|----------|
| **P0** | HIGH-1：地址硬删除改软删除 | 5 min |
| **P0** | HIGH-2：密码修改加限流 + oldPassword 加 Size | 5 min |
| **P1** | MEDIUM-1：通知列表加分页 | 15 min |
| **P1** | MEDIUM-3：BCryptPasswordEncoder 改注入 | 5 min |
| **P2** | MEDIUM-2：Profile.tsx 拆分组件 | 30 min |
| **P2** | MEDIUM-4：update 取消默认逻辑 | 10 min |
| **P3** | LOW-1 ~ LOW-4 | 各 5 min |

---

## 六、验证清单

- [x] 后端 `mvn -DskipTests compile` 通过
- [x] 后端 `mvn test` 10 个测试通过
- [x] 前端 `npm run lint` 通过
- [x] 前端 `npm run build` 通过
- [x] 前端 `node --test` 2 个测试通过
- [ ] HIGH-1 修复后验证软删除 + 默认提升逻辑
- [ ] HIGH-2 修复后验证限流生效
- [ ] MEDIUM-1 修复后验证分页参数传递
