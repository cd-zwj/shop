package com.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.dto.RefundCreateDTO;
import com.payment.entity.RefundApplication;

/**
 * 售后退款申请服务接口。
 * <p>
 * 管理用户退款申请的完整生命周期，包括申请创建、查询、取消、
 * 商户审核以及退款完成后的后续处理（如积分回退）。
 * 与 {@link RefundService}（支付渠道退款执行）区分，本服务专注于退款申请的业务流程管理。
 */
public interface RefundApplicationService {

    /**
     * 用户端：提交退款申请。
     *
     * @param platformUserId 平台用户 ID
     * @param tenantId       租户 ID
     * @param dto            退款申请创建 DTO，包含退款原因、金额等信息
     * @return 创建的退款申请实体
     */
    RefundApplication createRefund(Long platformUserId, Long tenantId, RefundCreateDTO dto);

    /**
     * 用户端：查询我的退款申请列表。
     *
     * @param platformUserId 平台用户 ID
     * @param tenantId       租户 ID
     * @param status         退款状态筛选条件（可选）
     * @param page           页码
     * @param size           每页数量
     * @return 退款申请分页结果
     */
    Page<RefundApplication> listMyRefunds(Long platformUserId, Long tenantId, String status, int page, int size);

    /**
     * 用户端：查询退款申请详情。
     *
     * @param platformUserId 平台用户 ID
     * @param tenantId       租户 ID
     * @param refundId       退款申请 ID
     * @return 退款申请实体
     */
    RefundApplication getRefundDetail(Long platformUserId, Long tenantId, Long refundId);

    /**
     * 用户端：取消退款申请。
     * <p>
     * 仅当退款申请处于待审核状态时可取消。
     *
     * @param platformUserId 平台用户 ID
     * @param tenantId       租户 ID
     * @param refundId       退款申请 ID
     */
    void cancelRefund(Long platformUserId, Long tenantId, Long refundId);

    /**
     * 商户端：查询退款申请列表。
     *
     * @param tenantId 租户 ID
     * @param status   退款状态筛选条件（可选）
     * @param page     页码
     * @param size     每页数量
     * @return 退款申请分页结果
     */
    Page<RefundApplication> listTenantRefunds(Long tenantId, String status, int page, int size);

    /**
     * 商户端：审核退款申请。
     * <p>
     * 审核通过后将触发退款执行流程（调用第三方支付退款接口），
     * 审核拒绝时需提供拒绝原因。
     *
     * @param tenantId     租户 ID
     * @param refundId     退款申请 ID
     * @param adminId      审核人 ID
     * @param approved     是否通过
     * @param rejectReason 拒绝原因（approved 为 false 时必填）
     */
    void auditRefund(Long tenantId, Long refundId, Long adminId, boolean approved, String rejectReason);

    /**
     * 退款完成回调。
     * <p>
     * 渠道退款到账后由退款回调链路调用，处理积分回退、优惠券恢复等后续逻辑。
     *
     * @param tenantId 租户 ID
     * @param refundId 退款申请 ID
     */
    void completeRefund(Long tenantId, Long refundId);
}
