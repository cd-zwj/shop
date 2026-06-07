package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.BusinessException;
import com.payment.dto.RefundCreateDTO;
import com.payment.entity.RefundApplication;
import com.payment.entity.SalesOrder;
import com.payment.enums.OrderStatusEnum;
import com.payment.enums.RefundApplicationStatus;
import com.payment.mapper.RefundApplicationMapper;
import com.payment.mapper.SalesOrderMapper;
import com.payment.service.RefundApplicationService;
import com.payment.util.BizNoGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

        // 检查是否已有进行中的退款申请
        Long existingCount = refundApplicationMapper.selectCount(new LambdaQueryWrapper<RefundApplication>()
                .eq(RefundApplication::getOrderNo, dto.getOrderNo())
                .eq(dto.getOrderItemId() != null, RefundApplication::getOrderItemId, dto.getOrderItemId())
                .in(RefundApplication::getRefundStatus,
                        RefundApplicationStatus.PENDING.name(),
                        RefundApplicationStatus.APPROVED.name(),
                        RefundApplicationStatus.PROCESSING.name()));
        if (existingCount > 0) {
            throw new BusinessException("该订单已有进行中的退款申请");
        }

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

        refundApplicationMapper.insert(app);
        log.info("退款申请已创建: refundNo={}, orderNo={}, amount={}", app.getRefundNo(), app.getOrderNo(), app.getRefundAmount());
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

    private void validateRefundType(String refundType) {
        if (!"REFUND_ONLY".equals(refundType) && !"RETURN_REFUND".equals(refundType)) {
            throw new BusinessException("退款类型不合法，仅支持 REFUND_ONLY 或 RETURN_REFUND");
        }
    }
}
