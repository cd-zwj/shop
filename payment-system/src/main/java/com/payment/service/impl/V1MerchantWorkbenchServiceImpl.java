package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.dto.MerchantWorkbenchTodoItemVO;
import com.payment.dto.MerchantWorkbenchTodoSummaryVO;
import com.payment.entity.RefundApplication;
import com.payment.entity.SalesOrder;
import com.payment.enums.RefundApplicationStatus;
import com.payment.mapper.CompensationTaskMapper;
import com.payment.mapper.OrderDeliveryRecordMapper;
import com.payment.mapper.ProductMapper;
import com.payment.mapper.RefundApplicationMapper;
import com.payment.mapper.RetryTaskMapper;
import com.payment.mapper.SalesOrderMapper;
import com.payment.service.V1MerchantWorkbenchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 本地优先的商户工作台待办统计。
 */
@Service
@RequiredArgsConstructor
public class V1MerchantWorkbenchServiceImpl implements V1MerchantWorkbenchService {

    private static final int LOW_STOCK_THRESHOLD = 5;

    private final SalesOrderMapper salesOrderMapper;
    private final RefundApplicationMapper refundApplicationMapper;
    private final OrderDeliveryRecordMapper orderDeliveryRecordMapper;
    private final ProductMapper productMapper;
    private final CompensationTaskMapper compensationTaskMapper;
    private final RetryTaskMapper retryTaskMapper;

    @Override
    public MerchantWorkbenchTodoSummaryVO getTodoSummary(Long tenantId) {
        List<MerchantWorkbenchTodoItemVO> items = List.of(
                item("payment", "待付款订单", "用户已下单但尚未完成支付，可关注是否需要催付或备货。",
                        countPendingPayments(tenantId), "/merchant/orders?tab=pending", "orange"),
                item("fulfillment", "待履约订单", "用户已支付，需要商家发货、卡密交付或服务核销。",
                        safeCount(orderDeliveryRecordMapper.countDistinctOrdersByTenantAndStatuses(
                                tenantId, List.of("PENDING", "DELIVERING"))),
                        "/merchant/orders?tab=shipping", "blue"),
                item("abnormalOrder", "异常订单", "支付失败或内部状态异常的订单，需要确认失败原因并协助用户重试。",
                        countAbnormalOrders(tenantId), "/merchant/orders?tab=abnormal", "red"),
                item("refund", "待审核退款", "用户已提交售后申请，需要商家审核通过或给出驳回原因。",
                        countRefunds(tenantId, RefundApplicationStatus.PENDING.name()),
                        "/merchant/refunds?status=PENDING", "orange"),
                item("refundFailed", "退款失败单", "内部退款处理失败，需要检查失败原因并继续跟进用户。",
                        countRefunds(tenantId, RefundApplicationStatus.FAILED.name()),
                        "/merchant/refunds?status=FAILED", "red"),
                item("compensation", "待补偿任务", "订单、支付或退款补偿任务未结束，需要管理员介入或等待调度器继续处理。",
                        safeCount(compensationTaskMapper.countMerchantVisibleOpenTasks(tenantId)),
                        "/admin/compensation?type=compensation", "red"),
                item("retry", "待重试任务", "异步重试任务仍在排队、失败或已进入死信，需要排查原因并重试。",
                        safeCount(retryTaskMapper.countMerchantVisibleOpenTasks(tenantId)),
                        "/admin/compensation?type=retry", "orange"),
                item("stock", "低库存商品", "库存低于或等于 5 的上架商品，建议补货或下架。",
                        safeCount(productMapper.countActiveLowStockByTenant(tenantId, LOW_STOCK_THRESHOLD)),
                        "/merchant/products", "red")
        );

        long totalCount = items.stream()
                .map(MerchantWorkbenchTodoItemVO::getCount)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .sum();
        return MerchantWorkbenchTodoSummaryVO.builder()
                .totalCount(totalCount)
                .items(items)
                .build();
    }

    private Long countPendingPayments(Long tenantId) {
        LambdaQueryWrapper<SalesOrder> wrapper = new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getTenantId, tenantId)
                .eq(SalesOrder::getDeleted, 0)
                .notIn(SalesOrder::getOrderStatus, "CANCELLED", "CLOSED")
                .notIn(SalesOrder::getPayStatus, "FAILED", "CLOSED", "SUCCESS", "PAID")
                .and(q -> q.in(SalesOrder::getPayStatus, "WAIT_PAY", "PAYING", "PENDING", "UNPAID")
                        .or()
                        .in(SalesOrder::getOrderStatus, "CREATED", "PENDING"));
        return safeCount(salesOrderMapper.selectCount(wrapper));
    }

    private Long countAbnormalOrders(Long tenantId) {
        return safeCount(salesOrderMapper.countAbnormalOrdersByTenant(tenantId));
    }

    private Long countRefunds(Long tenantId, String status) {
        return safeCount(refundApplicationMapper.selectCount(new LambdaQueryWrapper<RefundApplication>()
                .eq(RefundApplication::getTenantId, tenantId)
                .eq(RefundApplication::getRefundStatus, status)));
    }

    private MerchantWorkbenchTodoItemVO item(String key,
                                             String label,
                                             String description,
                                             Long count,
                                             String path,
                                             String tone) {
        return MerchantWorkbenchTodoItemVO.builder()
                .key(key)
                .label(label)
                .description(description)
                .count(safeCount(count))
                .path(path)
                .tone(tone)
                .build();
    }

    private Long safeCount(Long count) {
        return count == null ? 0L : count;
    }
}
