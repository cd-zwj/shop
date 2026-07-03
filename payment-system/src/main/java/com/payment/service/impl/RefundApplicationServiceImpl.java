package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.BusinessException;
import com.payment.dto.RefundCreateDTO;
import com.payment.entity.ExchangeProduct;
import com.payment.entity.OrderDeliveryRecord;
import com.payment.entity.RefundApplication;
import com.payment.entity.SalesOrder;
import com.payment.entity.SalesOrderItem;
import com.payment.enums.DeliveryStatusEnum;
import com.payment.enums.OrderStatusEnum;
import com.payment.enums.RefundApplicationStatus;
import com.payment.mapper.ExchangeProductMapper;
import com.payment.mapper.RefundApplicationMapper;
import com.payment.mapper.SalesOrderItemMapper;
import com.payment.mapper.SalesOrderMapper;
import com.payment.service.PointsService;
import com.payment.service.RefundApplicationService;
import com.payment.service.RefundService;
import com.payment.service.UserNotificationService;
import com.payment.service.delivery.OrderDeliveryService;
import com.payment.util.BizNoGenerator;
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
 * 审核通过时根据交付状态决定是否需要先撤销交付资源（卡密作废、权益冻结等）。
 *
 * @see com.payment.service.RefundApplicationService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundApplicationServiceImpl implements RefundApplicationService {

    private static final Set<String> REFUNDABLE_ORDER_STATUSES = Set.of(
            OrderStatusEnum.PAID.name()
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
    private final PointsService pointsService;
    private final ExchangeProductMapper exchangeProductMapper;
    private final UserNotificationService notificationService;
    private final OrderDeliveryService orderDeliveryService;
    private final RefundService refundService;

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
        SalesOrder salesOrder = salesOrderMapper.selectOne(new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getOrderNo, dto.getOrderNo())
                .eq(SalesOrder::getTenantId, tenantId));
        if (salesOrder == null) {
            throw new BusinessException("订单不存在");
        }
        if (!platformUserId.equals(salesOrder.getPlatformUserId())) {
            throw new BusinessException("无权操作该订单");
        }
        if (!REFUNDABLE_ORDER_STATUSES.contains(salesOrder.getOrderStatus())) {
            throw new BusinessException("当前订单状态不允许退款");
        }

        SalesOrderItem refundItem = validateRefundItem(dto.getOrderItemId(), salesOrder);
        ensureNoActiveRefund(salesOrder.getOrderNo(), dto.getOrderItemId(), tenantId);

        BigDecimal refundableAmount = calculateRefundableAmount(salesOrder, refundItem);
        if (dto.getRefundAmount().compareTo(refundableAmount) > 0) {
            throw new BusinessException("退款金额不能超过订单可退余额");
        }

        // 校验退款类型合法
        validateRefundType(dto.getRefundType());

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
        applyRefundDisplaySnapshot(app, salesOrder, refundItem);

        try {
            refundApplicationMapper.insert(app);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("该订单已有进行中的退款申请");
        }

        log.info("退款申请已创建: refundNo={}, orderNo={}, amount={}", app.getRefundNo(), app.getOrderNo(), app.getRefundAmount());

        // 通知用户：退款申请已提交
        try {
            notificationService.send(
                    platformUserId,
                    "退款申请已提交",
                    "您的退款申请 " + app.getRefundNo() + " 已提交，金额 ¥" + app.getRefundAmount() + "，等待审核",
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

        app.setAdminId(adminId);
        app.setAuditTime(LocalDateTime.now());

        if (approved) {
            app.setRefundStatus(RefundApplicationStatus.APPROVED.name());
            app.setRejectReason(null);
            refundApplicationMapper.updateById(app);
            log.info("退款申请已通过: refundNo={}, adminId={}", app.getRefundNo(), adminId);

            DeliverySnapshot deliverySnapshot = resolveDeliverySnapshot(app);
            if (deliverySnapshot.revokeRequired()) {
                String revokeFailure = revokeDelivery(app);
                if (revokeFailure != null) {
                    app.setRefundStatus(RefundApplicationStatus.FAILED.name());
                    app.setRejectReason(revokeFailure);
                    refundApplicationMapper.updateById(app);
                    log.warn("退款申请交付撤销失败: refundNo={}, reason={}", app.getRefundNo(), revokeFailure);
                    return;
                }
            }

            refundService.prepareMerchantApprovedRefund(app);
            app.setRefundStatus(RefundApplicationStatus.PROCESSING.name());
            applySnapshot(app, deliverySnapshot);
        } else {
            if (rejectReason == null || rejectReason.isBlank()) {
                throw new BusinessException("拒绝退款时必须填写拒绝原因");
            }
            app.setRefundStatus(RefundApplicationStatus.REJECTED.name());
            app.setRejectReason(rejectReason);
            log.info("退款申请已拒绝: refundNo={}, reason={}", app.getRefundNo(), rejectReason);
        }

        refundApplicationMapper.updateById(app);
    }

    /**
     * 标记退款申请为已完成。
     * <p>
     * 退款到账后由退款服务调用。操作包括：
     * <ul>
     *   <li>将退款状态置为 COMPLETED</li>
     *   <li>积分兑换订单回退积分</li>
     *   <li>兜底回收交付资源（卡密作废/权益冻结）</li>
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

        // 检查是否为积分兑换订单，如果是则回退积分
        handlePointsRefundIfNeeded(app);

        // 退款到账后的交付回收兜底。审核阶段已尝试撤销已交付资源，这里依赖交付服务幂等，
        // 防止人工补偿、对账重放或历史状态修复时遗漏卡密作废/权益冻结。
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

    /**
     * 积分兑换订单退款时回退积分。
     * 判断依据：订单 source 为 EXCHANGE，或订单号以 "EX" 开头。
     */
    private void handlePointsRefundIfNeeded(RefundApplication app) {
        SalesOrder salesOrder = salesOrderMapper.selectOne(new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getOrderNo, app.getOrderNo())
                .eq(SalesOrder::getTenantId, app.getTenantId()));

        boolean isExchangeOrder = false;
        Integer pointsToRefund = null;

        if (salesOrder != null) {
            // 通过 source 字段判断
            if ("EXCHANGE".equalsIgnoreCase(salesOrder.getSource())) {
                isExchangeOrder = true;
            }
            // 通过积分抵扣金额推算积分（1元=1积分的兜底逻辑）
            if (isExchangeOrder && salesOrder.getPointsDeductAmount() != null
                    && salesOrder.getPointsDeductAmount().signum() > 0) {
                pointsToRefund = salesOrder.getPointsDeductAmount().intValue();
            }
        }

        // 兜底：订单号以 "EX" 开头也视为积分兑换订单
        if (!isExchangeOrder && app.getOrderNo() != null && app.getOrderNo().startsWith("EX")) {
            isExchangeOrder = true;
        }

        if (!isExchangeOrder) {
            return;
        }

        // 如果从订单上无法推算具体积分，尝试从兑换商品表查询
        if (pointsToRefund == null || pointsToRefund <= 0) {
            pointsToRefund = resolvePointsFromExchangeProduct(app);
        }

        if (pointsToRefund == null || pointsToRefund <= 0) {
            log.warn("无法确定退款应返还的积分数量，跳过积分回退。refundNo={}, orderNo={}",
                    app.getRefundNo(), app.getOrderNo());
            return;
        }

        try {
            pointsService.refundPoints(
                    app.getPlatformUserId(),
                    app.getTenantId(),
                    pointsToRefund,
                    app.getOrderNo(),
                    "积分兑换商品退款回退，退款单号：" + app.getRefundNo()
            );
        } catch (Exception e) {
            log.error("积分回退失败，refundNo={}, orderNo={}, points={}",
                    app.getRefundNo(), app.getOrderNo(), pointsToRefund, e);
            throw new BusinessException("积分回退失败，请稍后重试");
        }
    }

    /**
     * 从兑换商品表中查找关联的积分数量。
     * 尝试通过退款申请的 orderItemId 或退款金额匹配。
     */
    private Integer resolvePointsFromExchangeProduct(RefundApplication app) {
        // 尝试通过 orderItemId 查找
        if (app.getOrderItemId() != null) {
            ExchangeProduct ep = exchangeProductMapper.selectById(app.getOrderItemId());
            if (ep != null && ep.getPointsRequired() != null && ep.getPointsRequired() > 0) {
                return ep.getPointsRequired();
            }
        }
        log.warn("无法从兑换商品表推算积分数量，refundNo={}", app.getRefundNo());
        return null;
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
                ? "未发货/未交付，商家同意后可快速进入渠道退款"
                : "已发货/已交付，商家同意后系统将先撤销交付再退款";
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

    private record DeliverySnapshot(String deliveryStatus,
                                    boolean quickRefundSuggested,
                                    String suggestion,
                                    boolean revokeRequired) {
    }
}
