package com.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.dto.RefundCreateDTO;
import com.payment.entity.RefundApplication;

/**
 * 售后退款申请服务。
 * 与已有的 RefundService（支付渠道退款执行）区分，此处管理用户退款申请的生命周期。
 */
public interface RefundApplicationService {

    /** 用户端：申请退款 */
    RefundApplication createRefund(Long platformUserId, Long tenantId, RefundCreateDTO dto);

    /** 用户端：查询我的退款列表 */
    Page<RefundApplication> listMyRefunds(Long platformUserId, Long tenantId, String status, int page, int size);

    /** 用户端：查询退款详情 */
    RefundApplication getRefundDetail(Long platformUserId, Long tenantId, Long refundId);

    /** 用户端：取消退款 */
    void cancelRefund(Long platformUserId, Long tenantId, Long refundId);

    /** 商户端：查询退款申请列表 */
    Page<RefundApplication> listTenantRefunds(Long tenantId, String status, int page, int size);

    /** 商户端：审核退款 */
    void auditRefund(Long tenantId, Long refundId, Long adminId, boolean approved, String rejectReason);
}
