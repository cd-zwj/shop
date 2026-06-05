-- 测试用最小表结构（H2 MySQL 模式）

CREATE TABLE IF NOT EXISTS coupon_template (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  template_no VARCHAR(64) NOT NULL,
  tenant_id BIGINT DEFAULT NULL,
  owner_type VARCHAR(20) NOT NULL,
  name VARCHAR(100) NOT NULL,
  coupon_type VARCHAR(32) NOT NULL,
  threshold_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
  discount_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
  discount_rate DECIMAL(8,4) DEFAULT NULL,
  max_discount_amount DECIMAL(18,2) DEFAULT NULL,
  total_stock INT NOT NULL DEFAULT 0,
  received_count INT NOT NULL DEFAULT 0,
  used_quantity INT NOT NULL DEFAULT 0,
  per_user_limit INT NOT NULL DEFAULT 1,
  receive_start_time TIMESTAMP DEFAULT NULL,
  receive_end_time TIMESTAMP DEFAULT NULL,
  valid_days_after_receive INT DEFAULT NULL,
  valid_start_time TIMESTAMP DEFAULT NULL,
  valid_end_time TIMESTAMP DEFAULT NULL,
  min_member_level INT DEFAULT NULL,
  exclude_member_tag_ids VARCHAR(255) DEFAULT NULL,
  stack_strategy VARCHAR(32) NOT NULL DEFAULT 'EXCLUSIVE',
  version INT NOT NULL DEFAULT 0,
  description VARCHAR(255) DEFAULT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  deleted TINYINT NOT NULL DEFAULT 0,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_coupon (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  coupon_no VARCHAR(64) NOT NULL,
  coupon_template_id BIGINT NOT NULL,
  tenant_id BIGINT DEFAULT NULL,
  platform_user_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
  receive_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expire_time TIMESTAMP NOT NULL,
  lock_order_id BIGINT DEFAULT NULL,
  lock_order_no VARCHAR(64) DEFAULT NULL,
  lock_time TIMESTAMP DEFAULT NULL,
  used_time TIMESTAMP DEFAULT NULL,
  release_time TIMESTAMP DEFAULT NULL,
  version INT NOT NULL DEFAULT 0,
  deleted TINYINT NOT NULL DEFAULT 0,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS member_points_account (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  platform_user_id BIGINT NOT NULL,
  points INT NOT NULL DEFAULT 0,
  total_earned INT NOT NULL DEFAULT 0,
  total_used INT NOT NULL DEFAULT 0,
  version INT NOT NULL DEFAULT 0,
  status INT NOT NULL DEFAULT 1,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS member_points_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  platform_user_id BIGINT NOT NULL,
  biz_type VARCHAR(64) DEFAULT NULL,
  biz_no VARCHAR(64) DEFAULT NULL,
  change_points INT NOT NULL DEFAULT 0,
  points_before INT DEFAULT NULL,
  points_after INT DEFAULT NULL,
  status VARCHAR(32) DEFAULT NULL,
  remark VARCHAR(255) DEFAULT NULL,
  confirm_time TIMESTAMP DEFAULT NULL,
  release_time TIMESTAMP DEFAULT NULL,
  release_reason VARCHAR(255) DEFAULT NULL,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 集成测试所需表

CREATE TABLE IF NOT EXISTS platform_user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_no VARCHAR(64) DEFAULT NULL,
  username VARCHAR(100) NOT NULL,
  phone VARCHAR(32) DEFAULT NULL,
  email VARCHAR(128) DEFAULT NULL,
  password_hash VARCHAR(255) DEFAULT NULL,
  status INT NOT NULL DEFAULT 1,
  deleted TINYINT NOT NULL DEFAULT 0,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tenant (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_code VARCHAR(64) NOT NULL,
  name VARCHAR(200) NOT NULL,
  contact VARCHAR(100) DEFAULT NULL,
  phone VARCHAR(32) DEFAULT NULL,
  address VARCHAR(255) DEFAULT NULL,
  status INT NOT NULL DEFAULT 1,
  deleted TINYINT NOT NULL DEFAULT 0,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS product (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  product_code VARCHAR(64) DEFAULT NULL,
  name VARCHAR(200) NOT NULL,
  price DECIMAL(18,2) NOT NULL DEFAULT 0.00,
  unit VARCHAR(32) DEFAULT NULL,
  category VARCHAR(100) DEFAULT NULL,
  image_url VARCHAR(512) DEFAULT NULL,
  description VARCHAR(500) DEFAULT NULL,
  status INT NOT NULL DEFAULT 1,
  deleted TINYINT NOT NULL DEFAULT 0,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 测试种子数据

INSERT INTO platform_user (id, user_no, username, phone, email, password_hash, status, deleted)
VALUES (1, 'U20240101001', 'testuser', '13800000000', 'test@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 1, 0);

INSERT INTO tenant (id, tenant_code, name, contact, phone, address, status, deleted)
VALUES (1, 'T001', '测试商户A', '张三', '13900000000', '北京市朝阳区', 1, 0);

INSERT INTO tenant (id, tenant_code, name, contact, phone, address, status, deleted)
VALUES (2, 'T002', '测试商户B', '李四', '13900000001', '上海市浦东新区', 1, 0);

INSERT INTO tenant (id, tenant_code, name, contact, phone, address, status, deleted)
VALUES (3, 'T003', '已禁用商户', '王五', '13900000002', '深圳市南山区', 0, 0);

INSERT INTO product (id, tenant_id, product_code, name, price, unit, category, status, deleted)
VALUES (1, 1, 'P001', '经典咖啡', 28.00, '杯', '饮品', 1, 0);

INSERT INTO product (id, tenant_id, product_code, name, price, unit, category, status, deleted)
VALUES (2, 1, 'P002', '抹茶拿铁', 32.00, '杯', '饮品', 1, 0);

INSERT INTO product (id, tenant_id, product_code, name, price, unit, category, status, deleted)
VALUES (3, 2, 'P003', '手工蛋糕', 45.00, '个', '甜点', 1, 0);

INSERT INTO product (id, tenant_id, product_code, name, price, unit, category, status, deleted)
VALUES (4, 1, 'P004', '已下架商品', 10.00, '份', '其他', 0, 0);
