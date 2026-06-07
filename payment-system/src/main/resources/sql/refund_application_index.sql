-- 退款申请表防并发唯一索引：同一订单+订单项下，只允许一个进行中的退款申请
-- 状态为 PENDING/APPROVED/PROCESSING 时不允许重复
-- 注：MySQL 不支持部分唯一索引（partial unique index），此处使用触发器或应用层 DuplicateKeyException 兜底
-- 方案：添加 (order_no, order_item_id, refund_status) 组合唯一索引
-- 当退款状态变更为 REJECTED/COMPLETED/CANCELLED 后，该索引不再阻止新的退款申请

ALTER TABLE refund_application
    ADD UNIQUE KEY uk_order_refund_active (order_no, order_item_id, refund_status);
