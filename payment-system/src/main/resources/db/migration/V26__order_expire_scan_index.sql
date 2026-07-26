-- 支付成功与订单超时竞争修复的查询索引：
-- expireUnpaidOrders 按 CREATED + WAIT_PAY + expire_time 扫描候选订单，
-- 随后以条件 UPDATE 与支付成功抢占互斥。
ALTER TABLE sales_order
    ADD INDEX idx_sales_order_expire_scan (order_status, pay_status, expire_time, id);
