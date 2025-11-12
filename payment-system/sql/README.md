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
├── 99_init_data.sql           # 初始化数据
├── import_all.sql             # 完整导入脚本
├── update_tables.sql          # 表结构更新脚本
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

### 99_init_data.sql
- 默认租户数据
- 默认管理员账号
- 默认充值规则
- 默认积分规则
- 默认兑换商品
- 默认商家余额

## 更新现有数据库

如果已经有旧版本的数据库，需要更新表结构：

```bash
mysql -u root -p payment_db < update_tables.sql
```

## 注意事项

1. **执行顺序**：必须按照文件编号顺序执行
2. **字符集**：确保使用 utf8mb4 字符集
3. **外键**：当前未使用外键约束，通过应用层保证数据一致性
4. **备份**：导入前请备份现有数据库

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
