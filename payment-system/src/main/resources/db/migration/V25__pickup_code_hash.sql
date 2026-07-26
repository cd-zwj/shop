-- 取货码安全加固：
-- 1. 取货码以 SHA-256 哈希落库并建立唯一约束（tenant + hash），核销按哈希走索引查询，
--    生成冲突由唯一约束兜底重试，替代原先对 payload 的 JSON_EXTRACT 应用层查重。
-- 2. 自提门店与核销人提升为一等列，便于核销校验、索引与审计追溯。

ALTER TABLE order_delivery_record
    ADD COLUMN pickup_code_hash VARCHAR(64) NULL COMMENT '取货码 SHA-256 哈希（hex），核销校验与唯一性依据' AFTER payload,
    ADD COLUMN store_id BIGINT NULL COMMENT '自提门店 ID' AFTER pickup_code_hash,
    ADD COLUMN verified_by BIGINT NULL COMMENT '核销人（平台用户 ID）' AFTER store_id;

-- 回填历史数据：从 payload JSON 提取取货码与门店。
UPDATE order_delivery_record
SET pickup_code_hash = SHA2(JSON_UNQUOTE(JSON_EXTRACT(payload, '$.pickupCode')), 256),
    store_id = CAST(JSON_EXTRACT(payload, '$.storeId') AS SIGNED)
WHERE payload IS NOT NULL
  AND JSON_VALID(payload)
  AND JSON_EXTRACT(payload, '$.pickupCode') IS NOT NULL;

-- 唯一约束：同租户下取货码哈希唯一（NULL 不参与唯一性，历史无码记录不受影响）。
ALTER TABLE order_delivery_record
    ADD UNIQUE KEY uk_tenant_pickup_hash (tenant_id, pickup_code_hash);
