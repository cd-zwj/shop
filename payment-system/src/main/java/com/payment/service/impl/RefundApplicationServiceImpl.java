package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.BusinessException;
import com.payment.constant.MerchantPermission;
import com.payment.dto.RefundCreateDTO;
import com.payment.entity.OrderDeliveryRecord;
import com.payment.entity.RefundApplication;
import com.payment.entity.AfterSaleAction;
import com.payment.entity.SalesOrder;
import com.payment.entity.SalesOrderItem;
import com.payment.enums.DeliveryStatusEnum;
import com.payment.enums.OrderStatusEnum;
import com.payment.enums.RefundApplicationStatus;
import com.payment.mapper.RefundApplicationMapper;
import com.payment.mapper.AfterSaleActionMapper;
import com.payment.mapper.SalesOrderItemMapper;
import com.payment.mapper.SalesOrderMapper;
import com.payment.service.RefundApplicationService;
import com.payment.service.MerchantStoreScope;
import com.payment.service.RefundService;
import com.payment.service.StoreInventoryService;
import com.payment.service.UserNotificationService;
import com.payment.service.delivery.OrderDeliveryService;
import com.payment.util.BizNoGenerator;
import com.payment.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 退款申请服务实现类，管理用户退款申请的完整生命周期。
 * <p>
 * 核心职责：
 * <ul>
 *   <li><b>创建退款申请</b>：校验订单状态、退款金额、退款类型，防止并发重复退款（唯一约束）</li>
 *   <li><b>查询退款</b>：用户端"我的退款"列表与详情、商家端退款列表</li>
 *   <li><b>取消退款</b>：用户可取消待审核状态的退款申请</li>
 *   <li><b>商家审核</b>：通过或拒绝退款申请，通过时处理交付撤销并触发渠道退款</li>
 *   <li><b>完成退款</b>：退款到账后标记完成，回退积分、回收交付资源</li>
 * </ul>
 * <p>
 * 支持仅退款（REFUND_ONLY）和退货退款（RETURN_REFUND）两种类型。
 * 审核通过时根据自提凭证状态决定是否需要撤销领取凭证。
 *
 * @see com.payment.service.RefundApplicationService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundApplicationServiceImpl implements RefundApplicationService {

    private static final Set<String> REFUNDABLE_ORDER_STATUSES = Set.of(
            OrderStatusEnum.PAID.name(),
            OrderStatusEnum.PENDING_PREPARATION.name(),
            OrderStatusEnum.PREPARING.name(),
            OrderStatusEnum.COMPLETED.name()
    );
    private static final Set<String> ACTIVE_REFUND_STATUSES = Set.of(
            RefundApplicationStatus.PENDING.name(),
            RefundApplicationStatus.APPROVED.name(),
            RefundApplicationStatus.PROCESSING.name()
    );
    private static final Set<String> REVOKE_REQUIRED_DELIVERY_STATUSES = Set.of(
            DeliveryStatusEnum.DELIVERED.name(),
            DeliveryStatusEnum.CONFIRMED.name()
    );

    private final RefundApplicationMapper refundApplicationMapper;
    private final SalesOrderMapper salesOrderMapper;
    private final SalesOrderItemMapper salesOrderItemMapper;
    private final UserNotificationService notificationService;
    private final OrderDeliveryService orderDeliveryService;
    private final RefundService refundService;
    private final StoreInventoryService storeInventoryService;
    private final AfterSaleActionMapper afterSaleActionMapper;
    private final MerchantStoreScopeService merchantStoreScopeService;

    /**
     * 创建退款申请。
     * <p>
     * 校验订单归属、状态、退款金额上限、退款类型，并检查是否存在进行中的退款。
     * 通过 INSERT 唯一约束防止并发重复退款。创建成功后发送通知。
     *
     * @param platformUserId 申请用户 ID
     * @param tenantId       商户 ID
     * @param dto            退款申请参数
     * @return 已创建的退款申请实体
     * @throws BusinessException 校验不通过时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefundApplication createRefund(Long platformUserId, Long tenantId, RefundCreateDTO dto) {
        // FOR UPDATE 锁订单行：同一订单的退款创建在事务内串行化，
        // 防止“整单退款”与“单项退款”并发提交穿透活跃退款互斥检查
        // （两者分别命中不同的生成列唯一键，互不冲突，先查后插挡不住）。
        SalesOrder salesOrder = salesOrderMapper.selectOne(new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getOrderNo, dto.getOrderNo())
                .eq(SalesOrder::getTenantId, tenantId)
                .last(" FOR UPDATE"));
        if (salesOrder == null) {
            throw new BusinessException("订单不存在");
        }
        if (!platformUserId.equals(salesOrder.getPlatformUserId())) {
            throw new BusinessException("无权操作该订单");
        }
        if (!REFUNDABLE_ORDER_STATUSES.contains(salesOrder.getOrderStatus())) {
            throw new BusinessException("当前订单状态不允许退款");
        }

        // 校验退款类型合法；退货退款必须落到具体订单项，
        // 否则退款完成后无法按订单项回补门店库存（账实不符）。
        validateRefundType(dto.getRefundType());
        if ("RETURN_REFUND".equals(dto.getRefundType()) && dto.getOrderItemId() == null) {
            throw new BusinessException("退货退款需选择具体商品");
        }

        SalesOrderItem refundItem = validateRefundItem(dto.getOrderItemId(), salesOrder);
        ensureNoActiveRefund(salesOrder.getOrderNo(), dto.getOrderItemId(), tenantId);

        BigDecimal refundableAmount = calculateRefundableAmount(salesOrder, refundItem);
        if (dto.getRefundAmount().compareTo(refundableAmount) > 0) {
            throw new BusinessException("退款金额不能超过订单可退余额");
        }

        // 先构建实体，通过 INSERT 唯一约束防止并发重复退款
        RefundApplication app = new RefundApplication();
        app.setRefundNo(BizNoGenerator.generate("RA"));
        app.setOrderNo(dto.getOrderNo());
        app.setOrderItemId(dto.getOrderItemId());
        app.setPlatformUserId(platformUserId);
        app.setTenantId(tenantId);
        app.setRefundType(dto.getRefundType());
        app.setRefundStatus(RefundApplicationStatus.PENDING.name());
        app.setRefundAmount(dto.getRefundAmount());
        app.setReason(dto.getReason());
        app.setDescription(dto.getDescription());
        app.setEvidenceUrlsJson(dto.getEvidenceUrls() == null || dto.getEvidenceUrls().isEmpty()
                ? null : JsonUtils.toJson(dto.getEvidenceUrls()));
        applyRefundDisplaySnapshot(app, salesOrder, refundItem);

        try {
            refundApplicationMapper.insert(app);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("该订单已有进行中的退款申请");
        }
        recordAction(app, "USER_APPLY", "USER", platformUserId, app.getDescription(), app.getEvidenceUrlsJson());

        log.info("退款申请已创建: refundNo={}, orderNo={}, amount={}", app.getRefundNo(), app.getOrderNo(), app.getRefundAmount());

        // 通知用户：退款申请已提交
        try {
            notificationService.send(
                    platformUserId,
                    "退款申请已提交",
                    "订单 " + app.getOrderNo() + " 的退款申请 " + app.getRefundNo()
                            + " 已提交，金额 ¥" + app.getRefundAmount() + "，等待审核",
                    "REFUND");
        } catch (Exception e) {
            log.warn("发送退款申请通知失败, refundNo={}", app.getRefundNo(), e);
        }

        return app;
    }

    /**
     * 查询用户退款申请列表，支持按状态筛选。
     *
     * @param platformUserId 用户 ID
     * @param tenantId       商户 ID
     * @param status         退款状态筛选，可为 null
     * @param page           页码
     * @param size           每页条数
     * @return 退款申请分页结果（含可退金额等展示快照）
     */
    @Override
    public Page<RefundApplication> listMyRefunds(Long platformUserId, Long tenantId, String status, int page, int size) {
        Page<RefundApplication> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<RefundApplication> wrapper = new LambdaQueryWrapper<RefundApplication>()
                .eq(RefundApplication::getPlatformUserId, platformUserId)
                .eq(RefundApplication::getTenantId, tenantId)
                .eq(status != null && !status.isBlank(), RefundApplication::getRefundStatus, status)
                .orderByDesc(RefundApplication::getCreateTime);
        Page<RefundApplication> result = refundApplicationMapper.selectPage(pageParam, wrapper);
        enrichRefundApplications(result.getRecords());
        return result;
    }

    /**
     * 获取退款申请详情，校验归属用户与商户。
     *
     * @param platformUserId 用户 ID
     * @param tenantId       商户 ID
     * @param refundId       退款申请 ID
     * @return 退款申请详情（含可退金额等展示快照）
     * @throws BusinessException 退款申请不存在或无权访问时抛出
     */
    @Override
    public RefundApplication getRefundDetail(Long platformUserId, Long tenantId, Long refundId) {
        RefundApplication app = refundApplicationMapper.selectById(refundId);
        if (app == null || !platformUserId.equals(app.getPlatformUserId()) || !tenantId.equals(app.getTenantId())) {
            throw new BusinessException("退款申请不存在");
        }
        enrichRefundApplication(app);
        return app;
    }

    /**
     * 取消退款申请。
     * <p>
     * 仅允许取消待审核（PENDING）状态的退款申请。
     *
     * @param platformUserId 用户 ID
     * @param tenantId       商户 ID
     * @param refundId       退款申请 ID
     * @throws BusinessException 退款申请不存在、无权或状态不允许取消时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelRefund(Long platformUserId, Long tenantId, Long refundId) {
        RefundApplication app = refundApplicationMapper.selectById(refundId);
        if (app == null || !platformUserId.equals(app.getPlatformUserId()) || !tenantId.equals(app.getTenantId())) {
            throw new BusinessException("退款申请不存在");
        }
        if (!RefundApplicationStatus.PENDING.name().equals(app.getRefundStatus())) {
            throw new BusinessException("只有待审核状态的退款申请才能取消");
        }
        app.setRefundStatus(RefundApplicationStatus.CANCELLED.name());
        refundApplicationMapper.updateById(app);
        recordAction(app, "USER_CANCEL", "USER", platformUserId, null, null);
        log.info("退款申请已取消: refundNo={}", app.getRefundNo());
    }

    /**
     * 商户端分页查询退款申请列表，支持按状态筛选。
     *
     * @param tenantId 商户 ID
     * @param status   退款状态筛选，可为 null
     * @param page     页码
     * @param size     每页条数
     * @return 退款申请分页结果（含可退金额等展示快照）
     */
    @Override
    public Page<RefundApplication> listTenantRefunds(Long tenantId, String status, int page, int size) {
        Page<RefundApplication> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<RefundApplication> wrapper = new LambdaQueryWrapper<RefundApplication>()
                .eq(RefundApplication::getTenantId, tenantId)
                .eq(status != null && !status.isBlank(), RefundApplication::getRefundStatus, status)
                .orderByDesc(RefundApplication::getCreateTime);
        Page<RefundApplication> result = refundApplicationMapper.selectPage(pageParam, wrapper);
        enrichRefundApplications(result.getRecords());
        return result;
    }

    @Override
    public Page<RefundApplication> listMerchantRefunds(Long tenantId, Long operatorId, String status,
                                                       int page, int size) {
        MerchantStoreScope scope = merchantStoreScopeService.resolve(
                tenantId, operatorId, MerchantPermission.REFUND_MANAGE);
        Page<RefundApplication> result = refundApplicationMapper.selectMerchantPage(
                new Page<>(page, size), tenantId, status,
                scope.allStores() ? null : scope.storeIds());
        enrichRefundApplications(result.getRecords());
        return result;
    }

    /**
     * 审核退款申请。
     * <p>
     * 通过时：检查交付状态，若已交付则先撤销交付资源，再触发渠道退款流程。
     * 拒绝时：必须填写拒绝原因。
     *
     * @param tenantId     商户 ID
     * @param refundId     退款申请 ID
     * @param adminId      审核人 ID
     * @param approved     是否通过
     * @param rejectReason 拒绝原因（拒绝时必填）
     * @throws BusinessException 审核不通过校验时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditRefund(Long tenantId, Long refundId, Long adminId, boolean approved, String rejectReason) {
        RefundApplication app = refundApplicationMapper.selectById(refundId);
        if (app == null || !tenantId.equals(app.getTenantId())) {
            throw new BusinessException("退款申请不存在");
        }
        if (!RefundApplicationStatus.PENDING.name().equals(app.getRefundStatus())) {
            throw new BusinessException("只有待审核状态的退款申请才能审核");
        }
        processDecision(app, adminId, approved, rejectReason, "MERCHANT",
                approved ? "MERCHANT_APPROVE" : "MERCHANT_REJECT");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditMerchantRefund(Long tenantId, Long refundId, Long operatorId,
                                    boolean approved, String rejectReason) {
        MerchantStoreScope scope = merchantStoreScopeService.resolve(
                tenantId, operatorId, MerchantPermission.REFUND_MANAGE);
        RefundApplication app = refundApplicationMapper.selectById(refundId);
        if (app == null || !tenantId.equals(app.getTenantId())) {
            throw new BusinessException("退款申请不存在");
        }
        requireRefundStoreAccess(scope, app);
        if (!RefundApplicationStatus.PENDING.name().equals(app.getRefundStatus())) {
            throw new BusinessException("只有待审核状态的退款申请才能审核");
        }
        processDecision(app, operatorId, approved, rejectReason, "MERCHANT",
                approved ? "MERCHANT_APPROVE" : "MERCHANT_REJECT");
    }

    @Override
    public List<AfterSaleAction> listMerchantActions(Long tenantId, Long refundId, Long operatorId) {
        MerchantStoreScope scope = merchantStoreScopeService.resolve(
                tenantId, operatorId, MerchantPermission.REFUND_MANAGE);
        RefundApplication app = refundApplicationMapper.selectById(refundId);
        if (app == null || !tenantId.equals(app.getTenantId())) {
            throw new BusinessException("退款申请不存在");
        }
        requireRefundStoreAccess(scope, app);
        return listActions(tenantId, refundId);
    }

    private void requireRefundStoreAccess(MerchantStoreScope scope, RefundApplication app) {
        SalesOrder order = salesOrderMapper.selectOne(new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getTenantId, app.getTenantId())
                .eq(SalesOrder::getOrderNo, app.getOrderNo())
                .eq(SalesOrder::getDeleted, 0));
        if (order == null || order.getStoreId() == null) {
            if (!scope.allStores()) {
                throw new BusinessException("退款申请不存在或无权访问");
            }
            return;
        }
        merchantStoreScopeService.requireStoreAccess(scope, order.getStoreId());
    }

    private void processDecision(RefundApplication app, Long operatorId, boolean approved, String rejectionReason,
                                 String operatorRole, String action) {
        String normalizedReason = rejectionReason == null ? null : rejectionReason.trim();

        app.setAdminId(operatorId);
        app.setAuditTime(LocalDateTime.now());

        if (approved) {
            app.setRefundStatus(RefundApplicationStatus.APPROVED.name());
            app.setRejectReason(null);
            refundApplicationMapper.updateById(app);
            log.info("退款申请已通过: refundNo={}, operatorId={}, role={}", app.getRefundNo(), operatorId, operatorRole);

            DeliverySnapshot deliverySnapshot = resolveDeliverySnapshot(app);
            if (deliverySnapshot.revokeRequired()) {
                String revokeFailure = revokeDelivery(app);
                if (revokeFailure != null) {
                    app.setRefundStatus(RefundApplicationStatus.FAILED.name());
                    app.setRejectReason(revokeFailure);
                    refundApplicationMapper.updateById(app);
                    recordAction(app, action + "_FAILED", operatorRole, operatorId, revokeFailure, null);
                    log.warn("退款申请交付撤销失败: refundNo={}, reason={}", app.getRefundNo(), revokeFailure);
                    return;
                }
            }

            refundService.prepareMerchantApprovedRefund(app);
            app.setRefundStatus(RefundApplicationStatus.PROCESSING.name());
            applySnapshot(app, deliverySnapshot);
        } else {
            if (normalizedReason == null || normalizedReason.isBlank()) {
                throw new BusinessException("拒绝退款时必须填写拒绝原因");
            }
            app.setRefundStatus(RefundApplicationStatus.REJECTED.name());
            app.setRejectReason(normalizedReason);
            log.info("退款申请已拒绝: refundNo={}, reason={}", app.getRefundNo(), normalizedReason);
        }

        refundApplicationMapper.updateById(app);
        String actionRemark = "ADMIN".equals(operatorRole) ? normalizedReason : (approved ? null : normalizedReason);
        recordAction(app, action, operatorRole, operatorId, actionRemark, null);
    }

    /**
     * 标记退款申请为已完成。
     * <p>
     * 退款到账后由退款服务调用。操作包括：
     * <ul>
     *   <li>将退款状态置为 COMPLETED</li>
 *   <li>兜底撤销自提凭证</li>
     * </ul>
     * 幂等处理：已完成的退款不会重复处理。
     *
     * @param tenantId 商户 ID
     * @param refundId 退款申请 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeRefund(Long tenantId, Long refundId) {
        RefundApplication app = refundApplicationMapper.selectById(refundId);
        if (app == null || !tenantId.equals(app.getTenantId())) {
            throw new BusinessException("退款申请不存在");
        }
        boolean alreadyCompleted = RefundApplicationStatus.COMPLETED.name().equals(app.getRefundStatus());
        if (alreadyCompleted && app.getCompleteTime() != null) {
            log.info("退款申请已完成，跳过重复完成处理: refundNo={}", app.getRefundNo());
            return;
        }
        // 只有 APPROVED/PROCESSING，或历史上已置 COMPLETED 但缺少完成时间的申请才能进入完成补偿。
        if (!RefundApplicationStatus.APPROVED.name().equals(app.getRefundStatus())
                && !RefundApplicationStatus.PROCESSING.name().equals(app.getRefundStatus())
                && !alreadyCompleted) {
            throw new BusinessException("当前退款状态不允许标记为完成");
        }

        app.setRefundStatus(RefundApplicationStatus.COMPLETED.name());
        app.setCompleteTime(LocalDateTime.now());
        refundApplicationMapper.updateById(app);
        recordAction(app, "REFUND_COMPLETED", "SYSTEM", null, null, null);

        restockReturnedItem(app);

        // 退款到账后的交付回收兜底。审核阶段已尝试撤销已交付资源，这里依赖交付服务幂等，
        // 防止人工补偿、对账重放或历史状态修复时遗漏自提凭证撤销。
        try {
            if (app.getOrderItemId() != null) {
                orderDeliveryService.revokeByOrderItem(app.getOrderItemId());
            } else {
                orderDeliveryService.revokeByOrderNo(app.getOrderNo());
            }
        } catch (Exception e) {
            log.warn("退款回收交付记录失败 refundNo={}, orderNo={}", app.getRefundNo(), app.getOrderNo(), e);
        }

        log.info("退款已完成: refundNo={}, orderNo={}", app.getRefundNo(), app.getOrderNo());
    }

    private void restockReturnedItem(RefundApplication app) {
        if (!"RETURN_REFUND".equals(app.getRefundType())) {
            return;
        }
        if (app.getOrderItemId() == null) {
            log.warn("整单退货退款缺少退货商品明细，未自动回补库存: refundNo={}", app.getRefundNo());
            return;
        }
        SalesOrder order = salesOrderMapper.selectOne(new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getOrderNo, app.getOrderNo())
                .eq(SalesOrder::getTenantId, app.getTenantId()));
        SalesOrderItem item = salesOrderItemMapper.selectById(app.getOrderItemId());
        if (order == null || item == null || !order.getOrderNo().equals(item.getOrderNo())) {
            throw new BusinessException("退款关联订单商品不存在");
        }
        if (order.getStoreId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
            throw new BusinessException("退款订单缺少可回补的门店库存信息");
        }
        storeInventoryService.restock(app.getTenantId(), order.getStoreId(), item.getProductId(), item.getQuantity(),
                "REFUND_APPLICATION", app.getRefundNo(), app.getAdminId(), "退货退款入库");
    }

    private void validateRefundType(String refundType) {
        if (!"REFUND_ONLY".equals(refundType) && !"RETURN_REFUND".equals(refundType)) {
            throw new BusinessException("退款类型不合法，仅支持 REFUND_ONLY 或 RETURN_REFUND");
        }
    }

    private SalesOrderItem validateRefundItem(Long orderItemId, SalesOrder salesOrder) {
        if (orderItemId == null) {
            return null;
        }
        SalesOrderItem item = salesOrderItemMapper.selectById(orderItemId);
        if (item == null
                || !Objects.equals(item.getTenantId(), salesOrder.getTenantId())
                || !Objects.equals(item.getOrderNo(), salesOrder.getOrderNo())) {
            throw new BusinessException("订单项不存在或不属于该订单");
        }
        return item;
    }

    private void ensureNoActiveRefund(String orderNo, Long orderItemId, Long tenantId) {
        LambdaQueryWrapper<RefundApplication> wrapper = new LambdaQueryWrapper<RefundApplication>()
                .eq(RefundApplication::getOrderNo, orderNo)
                .eq(RefundApplication::getTenantId, tenantId)
                .in(RefundApplication::getRefundStatus, ACTIVE_REFUND_STATUSES);
        if (orderItemId == null) {
            // 整单退款会覆盖所有订单项，任何进行中的退款都算重叠。
        } else {
            wrapper.and(w -> w
                    .isNull(RefundApplication::getOrderItemId)
                    .or()
                    .eq(RefundApplication::getOrderItemId, orderItemId));
        }

        Long activeCount = refundApplicationMapper.selectCount(wrapper);
        if (activeCount != null && activeCount > 0) {
            throw new BusinessException("该订单已有进行中的退款申请");
        }
    }

    private BigDecimal calculateRefundableAmount(SalesOrder salesOrder) {
        return calculateRefundableAmount(salesOrder, null);
    }

    private BigDecimal calculateRefundableAmount(SalesOrder salesOrder, SalesOrderItem refundItem) {
        BigDecimal paidAmount = refundItem == null
                ? nullToZero(salesOrder.getPayableAmount())
                : nullToZero(refundItem.getSubtotal());
        LambdaQueryWrapper<RefundApplication> wrapper = new LambdaQueryWrapper<RefundApplication>()
                .eq(RefundApplication::getOrderNo, salesOrder.getOrderNo())
                .eq(RefundApplication::getTenantId, salesOrder.getTenantId())
                .eq(RefundApplication::getRefundStatus, RefundApplicationStatus.COMPLETED.name());
        if (refundItem != null) {
            wrapper.eq(RefundApplication::getOrderItemId, refundItem.getId());
        }

        List<RefundApplication> completedRefunds = refundApplicationMapper.selectList(wrapper);
        BigDecimal refundedAmount = completedRefunds == null ? BigDecimal.ZERO : completedRefunds.stream()
                .map(RefundApplication::getRefundAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refundableAmount = paidAmount.subtract(refundedAmount);
        return refundableAmount.signum() < 0 ? BigDecimal.ZERO : refundableAmount;
    }

    private BigDecimal nullToZero(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private void enrichRefundApplications(List<RefundApplication> applications) {
        if (applications == null || applications.isEmpty()) {
            return;
        }
        applications.forEach(this::enrichRefundApplication);
    }

    private void enrichRefundApplication(RefundApplication app) {
        if (app == null) {
            return;
        }
        SalesOrder salesOrder = salesOrderMapper.selectOne(new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getOrderNo, app.getOrderNo())
                .eq(SalesOrder::getTenantId, app.getTenantId()));
        SalesOrderItem refundItem = app.getOrderItemId() == null ? null : salesOrderItemMapper.selectById(app.getOrderItemId());
        applyRefundDisplaySnapshot(app, salesOrder, refundItem);
    }

    private void applyRefundDisplaySnapshot(RefundApplication app, SalesOrder salesOrder, SalesOrderItem refundItem) {
        if (salesOrder != null) {
            app.setRefundableAmount(calculateRefundableAmount(salesOrder, refundItem));
        }
        applySnapshot(app, resolveDeliverySnapshot(app, refundItem));
    }

    private DeliverySnapshot resolveDeliverySnapshot(RefundApplication app) {
        SalesOrderItem refundItem = app.getOrderItemId() == null ? null : salesOrderItemMapper.selectById(app.getOrderItemId());
        return resolveDeliverySnapshot(app, refundItem);
    }

    private DeliverySnapshot resolveDeliverySnapshot(RefundApplication app, SalesOrderItem refundItem) {
        List<String> statuses;
        if (refundItem != null) {
            statuses = List.of(normalizeDeliveryStatus(refundItem.getDeliveryStatus()));
        } else {
            List<SalesOrderItem> items = salesOrderItemMapper.selectByOrderNo(app.getOrderNo());
            statuses = items == null || items.isEmpty()
                    ? List.of(DeliveryStatusEnum.PENDING.name())
                    : items.stream()
                    .map(SalesOrderItem::getDeliveryStatus)
                    .map(this::normalizeDeliveryStatus)
                    .toList();
        }

        boolean revokeRequired = statuses.stream().anyMatch(REVOKE_REQUIRED_DELIVERY_STATUSES::contains);
        String deliveryStatus = statuses.stream().distinct().count() == 1
                ? statuses.get(0)
                : statuses.stream().distinct().sorted().reduce((left, right) -> left + "," + right).orElse(DeliveryStatusEnum.PENDING.name());
        boolean quickRefundSuggested = !revokeRequired;
        String suggestion = quickRefundSuggested
                ? "未完成备货，商家同意后可快速进入渠道退款"
                : "已生成取货凭证或已完成履约，商家同意后系统将先撤销凭证再退款";
        return new DeliverySnapshot(deliveryStatus, quickRefundSuggested, suggestion, revokeRequired);
    }

    private String normalizeDeliveryStatus(String deliveryStatus) {
        if (deliveryStatus == null || deliveryStatus.isBlank()) {
            return DeliveryStatusEnum.PENDING.name();
        }
        try {
            return DeliveryStatusEnum.valueOf(deliveryStatus).name();
        } catch (IllegalArgumentException e) {
            return DeliveryStatusEnum.PENDING.name();
        }
    }

    private void applySnapshot(RefundApplication app, DeliverySnapshot snapshot) {
        app.setDeliveryStatus(snapshot.deliveryStatus());
        app.setQuickRefundSuggested(snapshot.quickRefundSuggested());
        app.setRefundSuggestion(snapshot.suggestion());
    }

    private String revokeDelivery(RefundApplication app) {
        try {
            List<OrderDeliveryRecord> records = app.getOrderItemId() == null
                    ? orderDeliveryService.revokeByOrderNo(app.getOrderNo())
                    : orderDeliveryService.revokeByOrderItem(app.getOrderItemId());
            boolean failed = records != null && records.stream()
                    .anyMatch(record -> DeliveryStatusEnum.REVOKE_FAILED.name().equals(record.getStatus()));
            return failed ? "交付撤销失败，请人工处理后再退款" : null;
        } catch (Exception e) {
            log.warn("交付撤销异常 refundNo={}, orderNo={}", app.getRefundNo(), app.getOrderNo(), e);
            return "交付撤销异常：" + e.getMessage();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void intervene(Long tenantId, Long refundId, Long adminId, boolean approved, String remark) {
        if (remark == null || remark.isBlank()) {
            throw new BusinessException("平台处理说明不能为空");
        }
        RefundApplication app = refundApplicationMapper.selectById(refundId);
        if (app == null || !tenantId.equals(app.getTenantId())) {
            throw new BusinessException("退款申请不存在");
        }
        if (!RefundApplicationStatus.PENDING.name().equals(app.getRefundStatus())
                && !RefundApplicationStatus.REJECTED.name().equals(app.getRefundStatus())) {
            throw new BusinessException("当前售后状态不允许平台介入处理");
        }
        if (approved && RefundApplicationStatus.REJECTED.name().equals(app.getRefundStatus())) {
            // A rejected request can be reconsidered only when the user has not opened a replacement request.
            ensureNoActiveRefund(app.getOrderNo(), app.getOrderItemId(), tenantId);
        }
        processDecision(app, adminId, approved, remark, "ADMIN",
                approved ? "PLATFORM_APPROVE" : "PLATFORM_REJECT");
    }

    @Override
    public List<AfterSaleAction> listActions(Long tenantId, Long refundId) {
        RefundApplication app = refundApplicationMapper.selectById(refundId);
        if (app == null || !tenantId.equals(app.getTenantId())) {
            throw new BusinessException("退款申请不存在");
        }
        return afterSaleActionMapper.selectList(new LambdaQueryWrapper<AfterSaleAction>()
                .eq(AfterSaleAction::getRefundApplicationId, refundId)
                .eq(AfterSaleAction::getTenantId, tenantId)
                .orderByAsc(AfterSaleAction::getCreateTime));
    }

    private void recordAction(RefundApplication app, String action, String operatorRole, Long operatorId,
                              String remark, String evidenceUrlsJson) {
        AfterSaleAction record = new AfterSaleAction();
        record.setTenantId(app.getTenantId());
        record.setRefundApplicationId(app.getId());
        record.setRefundNo(app.getRefundNo());
        record.setAction(action);
        record.setOperatorRole(operatorRole);
        record.setOperatorId(operatorId);
        record.setRemark(remark);
        record.setEvidenceUrlsJson(evidenceUrlsJson);
        afterSaleActionMapper.insert(record);
    }

    private record DeliverySnapshot(String deliveryStatus,
                                    boolean quickRefundSuggested,
                                    String suggestion,
                                    boolean revokeRequired) {
    }
}
