package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.payment.common.BusinessException;
import com.payment.constant.MerchantPermission;
import com.payment.entity.OrderFulfillmentAction;
import com.payment.entity.SalesOrder;
import com.payment.enums.OrderStatusEnum;
import com.payment.enums.PayStatusEnum;
import com.payment.mapper.OrderFulfillmentActionMapper;
import com.payment.mapper.SalesOrderMapper;
import com.payment.service.OrderFulfillmentService;
import com.payment.service.MerchantStoreScope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 订单备货状态流转，所有成功操作都会写入审计流水。 */
@Service
@RequiredArgsConstructor
public class OrderFulfillmentServiceImpl implements OrderFulfillmentService {

    private final SalesOrderMapper salesOrderMapper;
    private final OrderFulfillmentActionMapper actionMapper;
    private final MerchantStoreScopeService merchantStoreScopeService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startPreparation(Long tenantId, String orderNo, Long operatorId, String remark) {
        transition(tenantId, orderNo, operatorId, remark,
                OrderStatusEnum.PENDING_PREPARATION, OrderStatusEnum.PREPARING, "START_PREPARATION");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completePreparation(Long tenantId, String orderNo, Long operatorId, String remark) {
        transition(tenantId, orderNo, operatorId, remark,
                OrderStatusEnum.PREPARING, OrderStatusEnum.COMPLETED, "COMPLETE_PREPARATION");
    }

    @Override
    public List<OrderFulfillmentAction> listActions(Long tenantId, String orderNo, Long operatorId) {
        MerchantStoreScope scope = merchantStoreScopeService.resolve(
                tenantId, operatorId, MerchantPermission.ORDER_MANAGE);
        SalesOrder order = requireOrder(tenantId, orderNo);
        requireOrderStoreAccess(scope, order);
        return actionMapper.selectList(new LambdaQueryWrapper<OrderFulfillmentAction>()
                .eq(OrderFulfillmentAction::getTenantId, tenantId)
                .eq(OrderFulfillmentAction::getOrderNo, orderNo)
                .orderByAsc(OrderFulfillmentAction::getCreateTime));
    }

    private void transition(Long tenantId, String orderNo, Long operatorId, String remark,
                            OrderStatusEnum expected, OrderStatusEnum target, String action) {
        if (tenantId == null || tenantId <= 0 || operatorId == null || operatorId <= 0 || orderNo == null || orderNo.isBlank()) {
            throw new BusinessException("履约操作参数不合法");
        }
        MerchantStoreScope scope = merchantStoreScopeService.resolve(
                tenantId, operatorId, MerchantPermission.ORDER_MANAGE);
        SalesOrder order = requireOrder(tenantId, orderNo);
        requireOrderStoreAccess(scope, order);
        if (!"STORE_PICKUP".equals(order.getFulfillmentMode()) || order.getStoreId() == null) {
            throw new BusinessException("当前订单不是有效的到店自提订单");
        }
        if (!PayStatusEnum.SUCCESS.name().equals(order.getPayStatus())) {
            throw new BusinessException("订单尚未支付，不能处理备货");
        }
        if (!expected.name().equals(order.getOrderStatus())) {
            throw new BusinessException("当前订单状态不允许执行该履约操作");
        }

        String fromStatus = order.getOrderStatus();
        int changed = salesOrderMapper.update(null, new LambdaUpdateWrapper<SalesOrder>()
                .eq(SalesOrder::getId, order.getId())
                .eq(SalesOrder::getOrderStatus, expected.name())
                .set(SalesOrder::getOrderStatus, target.name()));
        if (changed != 1) {
            throw new BusinessException("订单状态已变更，请刷新后重试");
        }
        OrderFulfillmentAction record = new OrderFulfillmentAction();
        record.setTenantId(order.getTenantId());
        record.setStoreId(order.getStoreId());
        record.setOrderId(order.getId());
        record.setOrderNo(order.getOrderNo());
        record.setAction(action);
        record.setFromStatus(fromStatus);
        record.setToStatus(target.name());
        record.setOperatorId(operatorId);
        record.setRemark(trimToNull(remark));
        actionMapper.insert(record);
    }

    private SalesOrder requireOrder(Long tenantId, String orderNo) {
        SalesOrder order = salesOrderMapper.selectOne(new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getTenantId, tenantId)
                .eq(SalesOrder::getOrderNo, orderNo)
                .eq(SalesOrder::getDeleted, 0));
        if (order == null) {
            throw new BusinessException("订单不存在或无权访问");
        }
        return order;
    }

    private void requireOrderStoreAccess(MerchantStoreScope scope, SalesOrder order) {
        if (order.getStoreId() == null) {
            if (!scope.allStores()) {
                throw new BusinessException("订单不存在或无权访问");
            }
            return;
        }
        merchantStoreScopeService.requireStoreAccess(scope, order.getStoreId());
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
