CREATE TABLE order_fulfillment_action (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    store_id BIGINT NOT NULL COMMENT '履约门店ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    action VARCHAR(32) NOT NULL COMMENT 'START_PREPARATION/COMPLETE_PREPARATION/PICKUP_VERIFIED',
    from_status VARCHAR(32) NULL COMMENT '操作前订单状态',
    to_status VARCHAR(32) NULL COMMENT '操作后订单状态',
    operator_id BIGINT NOT NULL COMMENT '操作人平台用户ID',
    remark VARCHAR(255) NULL COMMENT '操作备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_order_fulfillment_action_order (tenant_id, order_no, create_time),
    KEY idx_order_fulfillment_action_store (tenant_id, store_id, create_time)
) COMMENT='门店自提订单履约操作流水';
