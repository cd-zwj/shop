# SQL 文件说明

## 文件结构

```
sql/
├── 00_init_database.sql      # 数据库初始化
├── 01_tenant_user.sql         # 租户和用户模块
├── 02_product.sql             # 商品模块
├── 03_order.sql               # 订单模块
├── 04_user_balance.sql        # 用户余额模块
├── 05_user_points.sql         # 用户积分模块
├── 06_recharge.sql            # 充值模块
├── 07_exchange.sql            # 积分兑换模块
├── 08_merchant_finance.sql    # 商家财务模块
├── 09_pos.sql                 # POS 收银模块
├── 10_analytics.sql           # 数据分析模块
├── 11_message_idempotent.sql  # 消息幂等模块
├── 12_rbac_permission.sql     # RBAC 权限模块
├── 13_user_permission.sql     # 用户额外权限模块
├── 14_platform_wallet_v1.sql  # 平台用户 / 双钱包 / 统一支付模型
├── 15_coupon_marketing.sql    # 优惠券与营销活动模块
├── 16_refund.sql              # 退款模块
├── 17_auth_security.sql       # 登录与安全模块
├── 18_store_membership.sql    # 门店与会员扩展模块
├── 19_message_retry.sql       # 消息重试与消费日志模块
├── 99_init_data.sql           # 初始化数据
├── import_all.sql             # 完整导入脚本
├── payment_db.sql             # 单租户版本（旧）
└── payment_db_multitenant.sql # 多租户版本（旧）
```

## 导入方式

### 方式一：使用主导入脚本（推荐）

```bash
# 进入 SQL 目录
cd payment-system/sql

# 执行导入
mysql -u root -p < import_all.sql
```

### 方式二：逐个导入

```bash
cd payment-system/sql

mysql -u root -p < 00_init_database.sql
mysql -u root -p < 01_tenant_user.sql
mysql -u root -p < 02_product.sql
mysql -u root -p < 03_order.sql
mysql -u root -p < 04_user_balance.sql
mysql -u root -p < 05_user_points.sql
mysql -u root -p < 06_recharge.sql
mysql -u root -p < 07_exchange.sql
mysql -u root -p < 08_merchant_finance.sql
mysql -u root -p < 09_pos.sql
mysql -u root -p < 10_analytics.sql
mysql -u root -p < 11_message_idempotent.sql
mysql -u root -p < 12_rbac_permission.sql
mysql -u root -p < 13_user_permission.sql
mysql -u root -p < 14_platform_wallet_v1.sql
mysql -u root -p < 15_coupon_marketing.sql
mysql -u root -p < 16_refund.sql
mysql -u root -p < 17_auth_security.sql
mysql -u root -p < 18_store_membership.sql
mysql -u root -p < 19_message_retry.sql
mysql -u root -p < 99_init_data.sql
```

### 方式三：使用旧版完整文件

```bash
# 多租户版本
mysql -u root -p < payment_db_multitenant.sql
```

## 模块说明

### 00_init_database.sql
- 创建数据库 `payment_db`
- 设置字符集为 utf8mb4
- 设置时区

### 01_tenant_user.sql
- `tenant` - 租户（商家）表
- `sys_user` - 用户表

### 02_product.sql
- `product` - 商品表
- `product_stock` - 商品库存表

### 03_order.sql
- `payment_order` - 订单表
- `order_item` - 订单商品明细表
- `payment_record` - 支付记录表

### 04_user_balance.sql
- `user_balance` - 用户余额表
- `balance_log` - 余额变动日志表

### 05_user_points.sql
- `user_points` - 用户积分表
- `points_log` - 积分变动日志表
- `points_rule` - 积分规则表

### 06_recharge.sql
- `recharge_rule` - 充值规则表
- `recharge_order` - 充值订单表

### 07_exchange.sql
- `exchange_product` - 积分兑换商品表

### 08_merchant_finance.sql
- `merchant_balance` - 商家余额表
- `withdrawal` - 提现申请表

### 09_pos.sql
- `pos_session` - POS 会话表
- `scan_record` - 扫码记录表

### 10_analytics.sql
- `user_behavior_log` - 用户行为日志表
- `data_analysis_result` - 数据分析结果表

### 11_message_idempotent.sql
- `message_idempotent` - 消息幂等记录表

### 12_rbac_permission.sql
- `sys_role` - 角色表
- `sys_permission` - 权限表
- `sys_user_role` - 用户角色关联表
- `sys_role_permission` - 角色权限关联表

### 13_user_permission.sql
- `sys_user_permission` - 用户额外权限关联表

### 14_platform_wallet_v1.sql
- `platform_user` - 平台用户主表
- `platform_user_auth` - 第三方认证绑定表
- `tenant_employee` - 商户员工关系表
- `tenant_member` - 商户会员关系表
- `unified_wallet_account` / `merchant_wallet_account` - 双钱包账户表
- `sales_order` / `recharge_order_v1` / `payment_bill` - 统一业务单模型
- `message_outbox` / `compensation_task` / `dead_letter_task` - 消息与补偿表

### 15_coupon_marketing.sql
- `coupon_template` - 优惠券模板表
- `user_coupon` - 用户领券表
- `coupon_operation_log` - 优惠券操作日志表
- `order_coupon_detail` - 订单优惠明细表
- `marketing_activity` - 营销活动表
- `activity_grant_record` - 活动发放记录表

### 16_refund.sql
- `refund_order` - 退款业务单表
- `refund_record` - 退款流水表
- `refund_callback_record` - 退款回调记录表
- `refund_reconcile_task` - 退款补查任务表

### 17_auth_security.sql
- `sms_verify_code` - 短信验证码表
- `login_session` - 登录会话表
- `user_login_log` - 登录日志表
- `login_fail_record` - 登录失败控制表
- `password_reset_record` - 密码重置记录表

### 18_store_membership.sql
- `store` - 门店表
- `member_level` - 会员等级表
- `member_tag` - 会员标签表
- `member_tag_relation` - 会员标签关联表
- `member_growth_log` - 会员成长值日志表

### 19_message_retry.sql
- `message_consume_log` - 消息消费日志表
- `retry_task` - 重试任务表

### 99_init_data.sql
- 默认租户数据
- 默认管理员账号
- 默认充值规则
- 默认积分规则
- 默认兑换商品
- 默认商家余额

## 更新现有数据库

如果已经有旧版本的数据库，建议按版本新增增量 SQL 脚本并按顺序执行，不再依赖单一的 `update_tables.sql` 文件。

## 注意事项

1. **执行顺序**：必须按照文件编号顺序执行
2. **字符集**：确保使用 utf8mb4 字符集
3. **外键**：当前未使用外键约束，通过应用层保证数据一致性
4. **备份**：导入前请备份现有数据库
5. **模型说明**：`14_platform_wallet_v1.sql` 及后续 `15-19` 扩展表默认基于平台用户 / 双钱包 / 统一支付模型设计
6. **渐进演进**：`18_store_membership.sql` 当前仅落地门店主数据与会员扩展，未引入新的商品分类与门店库存真相源

## 默认账号

### 管理员账号
- 用户名：`admin`
- 密码：`admin123`
- 租户：`TENANT_001`

**⚠️ 生产环境请立即修改默认密码！**

## 验证导入

```sql
-- 检查数据库
SHOW DATABASES LIKE 'payment_db';

-- 检查表
USE payment_db;
SHOW TABLES;

-- 检查数据
SELECT * FROM tenant;
SELECT * FROM sys_user;
SELECT * FROM recharge_rule;
```

## 常见问题

### 1. 导入时报错：Table already exists

**解决方案**：
- 删除现有数据库重新导入
- 或者跳过已存在的表

### 2. 字符集问题

**解决方案**：
```sql
ALTER DATABASE payment_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. 权限不足

**解决方案**：
```sql
GRANT ALL PRIVILEGES ON payment_db.* TO 'root'@'localhost';
FLUSH PRIVILEGES;
```

## 维护建议

1. **版本控制**：每次表结构变更创建新的更新脚本
2. **文档同步**：修改表结构后更新文档
3. **测试验证**：在测试环境验证后再应用到生产环境
4. **备份策略**：定期备份数据库

## 相关文档

- [数据库初始化说明.md](../../数据库初始化说明.md)
- [数据库表结构说明.md](../../数据库表结构说明.md)
- [SQL表结构与Entity对比.md](../../SQL表结构与Entity对比.md)
