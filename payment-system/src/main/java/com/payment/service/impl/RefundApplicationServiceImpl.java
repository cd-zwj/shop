package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.BusinessException;
import com.payment.dto.RefundCreateDTO;
import com.payment.entity.ExchangeProduct;
import com.payment.entity.RefundApplication;
import com.payment.entity.SalesOrder;
import com.payment.enums.OrderStatusEnum;
import com.payment.enums.RefundApplicationStatus;
import com.payment.mapper.ExchangeProductMapper;
import com.payment.mapper.RefundApplicationMapper;
import com.payment.mapper.SalesOrderMapper;
import com.payment.service.PointsService;
import com.payment.service.RefundApplicationService;
import com.payment.service.UserNotificationService;
import com.payment.util.BizNoGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundApplicationServiceImpl implements RefundApplicationService {

    private static final Set<String> REFUNDABLE_ORDER_STATUSES = Set.of(
            OrderStatusEnum.PAID.name()
    );

    private final RefundApplicationMapper refundApplicationMapper;
    private final SalesOrderMapper salesOrderMapper;
    private final PointsService pointsService;
    private final ExchangeProductMapper exchangeProductMapper;
    private final UserNotificationService notificationService;

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

        // 校验退款金额不超过订单实付金额
        if (salesOrder.getPayableAmount() != null
                && dto.getRefundAmount().compareTo(salesOrder.getPayableAmount()) > 0) {
            throw new BusinessException("退款金额不能超过订单实付金额");
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

    @Override
    public Page<RefundApplication> listMyRefunds(Long platformUserId, Long tenantId, String status, int page, int size) {
        Page<RefundApplication> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<RefundApplication> wrapper = new LambdaQueryWrapper<RefundApplication>()
                .eq(RefundApplication::getPlatformUserId, platformUserId)
                .eq(RefundApplication::getTenantId, tenantId)
                .eq(status != null && !status.isBlank(), RefundApplication::getRefundStatus, status)
                .orderByDesc(RefundApplication::getCreateTime);
        return refundApplicationMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public RefundApplication getRefundDetail(Long platformUserId, Long tenantId, Long refundId) {
        RefundApplication app = refundApplicationMapper.selectById(refundId);
        if (app == null || !platformUserId.equals(app.getPlatformUserId()) || !tenantId.equals(app.getTenantId())) {
            throw new BusinessException("退款申请不存在");
        }
        return app;
    }

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

    @Override
    public Page<RefundApplication> listTenantRefunds(Long tenantId, String status, int page, int size) {
        Page<RefundApplication> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<RefundApplication> wrapper = new LambdaQueryWrapper<RefundApplication>()
                .eq(RefundApplication::getTenantId, tenantId)
                .eq(status != null && !status.isBlank(), RefundApplication::getRefundStatus, status)
                .orderByDesc(RefundApplication::getCreateTime);
        return refundApplicationMapper.selectPage(pageParam, wrapper);
    }

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
            log.info("退款申请已通过: refundNo={}, adminId={}", app.getRefundNo(), adminId);
        } else {
            if (rejectReason == null || rejectReason.isBlank()) {
                throw new BusinessException("拒绝退款时必须填写拒绝原因");
            }
            app.setRefundStatus(RefundApplicationStatus.REJECTED.name());
            app.setRejectReason(rejectReason);
            log.info("退款申请已拒绝: refundNo={}, reason={}", app.getRefundNo(), rejectReason);
        }

        refundApplicationMapper.updateById(app);
        // 实际退款到支付渠道的逻辑留给后续异步处理
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeRefund(Long tenantId, Long refundId) {
        RefundApplication app = refundApplicationMapper.selectById(refundId);
        if (app == null || !tenantId.equals(app.getTenantId())) {
            throw new BusinessException("退款申请不存在");
        }
        // 只有 APPROVED 或 PROCESSING 状态的退款申请才能标记为完成
        if (!RefundApplicationStatus.APPROVED.name().equals(app.getRefundStatus())
                && !RefundApplicationStatus.PROCESSING.name().equals(app.getRefundStatus())) {
            throw new BusinessException("当前退款状态不允许标记为完成");
        }

        app.setRefundStatus(RefundApplicationStatus.COMPLETED.name());
        app.setCompleteTime(LocalDateTime.now());
        refundApplicationMapper.updateById(app);

        // 检查是否为积分兑换订单，如果是则回退积分
        handlePointsRefundIfNeeded(app);

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
}
