ALTER TABLE refund_application
    ADD COLUMN evidence_urls_json TEXT NULL COMMENT '用户售后凭证地址JSON' AFTER description;

CREATE TABLE after_sale_action (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    refund_application_id BIGINT NOT NULL COMMENT '售后申请ID',
    refund_no VARCHAR(64) NOT NULL COMMENT '售后单号',
    action VARCHAR(32) NOT NULL COMMENT 'USER_APPLY/MERCHANT_APPROVE/PLATFORM_APPROVE等',
    operator_role VARCHAR(16) NOT NULL COMMENT 'USER/MERCHANT/ADMIN/SYSTEM',
    operator_id BIGINT NULL COMMENT '操作人ID',
    remark VARCHAR(1000) NULL COMMENT '处理说明',
    evidence_urls_json TEXT NULL COMMENT '关联凭证地址JSON',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_after_sale_action_refund (tenant_id, refund_application_id, create_time),
    KEY idx_after_sale_action_refund_no (refund_no, create_time)
) COMMENT='售后处理流水';
