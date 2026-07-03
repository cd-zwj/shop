-- ========================================
-- 35. 虚拟商品类型、分类与履约形态
-- ========================================

USE payment_db;

ALTER TABLE product
    ADD COLUMN virtual_type_id BIGINT NULL COMMENT '虚拟商品类型ID' AFTER store_id,
    ADD COLUMN virtual_category_id BIGINT NULL COMMENT '虚拟商品分类ID' AFTER virtual_type_id,
    ADD COLUMN fulfillment_mode VARCHAR(32) NULL COMMENT '履约形态: ONLINE_VIRTUAL/OFFLINE_SERVICE/EXPRESS_DELIVERY' AFTER virtual_category_id,
    ADD INDEX idx_product_virtual_type (tenant_id, virtual_type_id),
    ADD INDEX idx_product_fulfillment_mode (tenant_id, fulfillment_mode);

CREATE TABLE IF NOT EXISTS virtual_product_type (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    type_code VARCHAR(64) NOT NULL COMMENT '类型编码',
    type_name VARCHAR(128) NOT NULL COMMENT '类型名称',
    delivery_strategy VARCHAR(32) NOT NULL COMMENT '映射交付策略: VIRTUAL/CARD_KEY/SERVICE/SUBSCRIPTION',
    description VARCHAR(500) NULL COMMENT '描述',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0停用 1启用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_vpt_tenant_code (tenant_id, type_code, deleted),
    INDEX idx_vpt_tenant_status (tenant_id, status),
    CONSTRAINT chk_vpt_delivery_strategy CHECK (delivery_strategy IN ('VIRTUAL','CARD_KEY','SERVICE','SUBSCRIPTION'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='虚拟商品类型字典';

CREATE TABLE IF NOT EXISTS virtual_product_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    type_id BIGINT NOT NULL COMMENT '虚拟商品类型ID',
    category_code VARCHAR(64) NOT NULL COMMENT '分类编码',
    category_name VARCHAR(128) NOT NULL COMMENT '分类名称',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父分类ID,0表示顶级',
    description VARCHAR(500) NULL COMMENT '描述',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0停用 1启用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_vpc_tenant_code (tenant_id, category_code, deleted),
    INDEX idx_vpc_tenant_type (tenant_id, type_id),
    INDEX idx_vpc_parent (tenant_id, parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='虚拟商品分类字典';

INSERT INTO virtual_product_type
    (tenant_id, type_code, type_name, delivery_strategy, description, status, sort_order)
SELECT id, 'ONLINE_MATERIAL', '在线资料', 'VIRTUAL', '网盘资料、课程文件、在线内容', 1, 10
FROM tenant
WHERE deleted = 0
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;

INSERT INTO virtual_product_type
    (tenant_id, type_code, type_name, delivery_strategy, description, status, sort_order)
SELECT id, 'CARD_REDEEM', '卡密兑换', 'CARD_KEY', '卡密、兑换码、序列号', 1, 20
FROM tenant
WHERE deleted = 0
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;

INSERT INTO virtual_product_type
    (tenant_id, type_code, type_name, delivery_strategy, description, status, sort_order)
SELECT id, 'STORE_SERVICE', '到店服务', 'SERVICE', '预约、到店核销、线下服务', 1, 30
FROM tenant
WHERE deleted = 0
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;

INSERT INTO virtual_product_type
    (tenant_id, type_code, type_name, delivery_strategy, description, status, sort_order)
SELECT id, 'MEMBER_BENEFIT', '会员权益', 'SUBSCRIPTION', '会员套餐、权益包、订阅服务', 1, 40
FROM tenant
WHERE deleted = 0
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;

INSERT INTO virtual_product_category
    (tenant_id, type_id, category_code, category_name, parent_id, description, status, sort_order)
SELECT t.id, vpt.id, 'COURSE_MATERIAL', '课程资料', 0, '课程文件、学习资料、在线文档', 1, 10
FROM tenant t
JOIN virtual_product_type vpt ON vpt.tenant_id = t.id AND vpt.type_code = 'ONLINE_MATERIAL' AND vpt.deleted = 0
WHERE t.deleted = 0
ON DUPLICATE KEY UPDATE type_id = VALUES(type_id), update_time = CURRENT_TIMESTAMP;

INSERT INTO virtual_product_category
    (tenant_id, type_id, category_code, category_name, parent_id, description, status, sort_order)
SELECT t.id, vpt.id, 'SOFTWARE_LICENSE', '软件授权', 0, '软件授权码、序列号、激活权益', 1, 20
FROM tenant t
JOIN virtual_product_type vpt ON vpt.tenant_id = t.id AND vpt.type_code = 'CARD_REDEEM' AND vpt.deleted = 0
WHERE t.deleted = 0
ON DUPLICATE KEY UPDATE type_id = VALUES(type_id), update_time = CURRENT_TIMESTAMP;

INSERT INTO virtual_product_category
    (tenant_id, type_id, category_code, category_name, parent_id, description, status, sort_order)
SELECT t.id, vpt.id, 'GAME_CARD', '游戏点卡', 0, '游戏点卡、充值卡、兑换码', 1, 30
FROM tenant t
JOIN virtual_product_type vpt ON vpt.tenant_id = t.id AND vpt.type_code = 'CARD_REDEEM' AND vpt.deleted = 0
WHERE t.deleted = 0
ON DUPLICATE KEY UPDATE type_id = VALUES(type_id), update_time = CURRENT_TIMESTAMP;

INSERT INTO virtual_product_category
    (tenant_id, type_id, category_code, category_name, parent_id, description, status, sort_order)
SELECT t.id, vpt.id, 'APPOINTMENT_SERVICE', '预约服务', 0, '到店预约、线下服务、人工核销', 1, 40
FROM tenant t
JOIN virtual_product_type vpt ON vpt.tenant_id = t.id AND vpt.type_code = 'STORE_SERVICE' AND vpt.deleted = 0
WHERE t.deleted = 0
ON DUPLICATE KEY UPDATE type_id = VALUES(type_id), update_time = CURRENT_TIMESTAMP;

INSERT INTO virtual_product_category
    (tenant_id, type_id, category_code, category_name, parent_id, description, status, sort_order)
SELECT t.id, vpt.id, 'MEMBER_PACKAGE', '会员套餐', 0, '会员套餐、权益包、订阅权益', 1, 50
FROM tenant t
JOIN virtual_product_type vpt ON vpt.tenant_id = t.id AND vpt.type_code = 'MEMBER_BENEFIT' AND vpt.deleted = 0
WHERE t.deleted = 0
ON DUPLICATE KEY UPDATE type_id = VALUES(type_id), update_time = CURRENT_TIMESTAMP;
