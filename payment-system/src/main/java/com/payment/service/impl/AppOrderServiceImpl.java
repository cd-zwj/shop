package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.BusinessException;
import com.payment.dto.AppCreateOrderDTO;
import com.payment.dto.AppCreateOrderItemDTO;
import com.payment.dto.pricing.DiscountSnapshotPlanVO;
import com.payment.dto.pricing.OrderPricingItemDTO;
import com.payment.dto.pricing.OrderPricingRequestDTO;
import com.payment.dto.pricing.OrderPricingResultVO;
import com.payment.dto.OrderPaymentVO;
import com.payment.dto.PayResponseDTO;
import com.payment.dto.SalesOrderDetailVO;
import com.payment.entity.MemberPointsAccount;
import com.payment.entity.OrderDiscountSnapshot;
import com.payment.entity.PaymentBill;
import com.payment.entity.PointsRule;
import com.payment.entity.Product;
import com.payment.entity.SalesOrder;
import com.payment.entity.SalesOrderItem;
import com.payment.entity.TenantEmployee;
import com.payment.entity.TenantMember;
import com.payment.enums.OrderStatusEnum;
import com.payment.enums.PayStatusEnum;
import com.payment.enums.PaymentBizTypeEnum;
import com.payment.enums.PaymentChannelCodeEnum;
import com.payment.enums.PaymentStatusReasonEnum;
import com.payment.enums.DiscountSourceEnum;
import com.payment.mapper.PointsRuleMapper;
import com.payment.mapper.ProductMapper;
import com.payment.mapper.OrderDiscountSnapshotMapper;
import com.payment.mapper.SalesOrderItemMapper;
import com.payment.mapper.SalesOrderMapper;
import com.payment.mapper.TenantEmployeeMapper;
import com.payment.mapper.TenantMemberMapper;
import com.payment.service.AppOrderService;
import com.payment.service.CouponService;
import com.payment.service.MemberPointsAccountService;
import com.payment.service.MerchantWalletService;
import com.payment.service.OrderPricingService;
import com.payment.service.PaymentBillV1Service;
import com.payment.service.PromotionService;
import com.payment.service.UnifiedWalletService;
import com.payment.service.UserBehaviorLogService;
import com.payment.service.WithdrawalService;
import com.payment.util.BizNoGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户端订单服务实现类，用于实现用户端订单相关业务逻辑。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppOrderServiceImpl implements AppOrderService {

    private final SalesOrderMapper salesOrderMapper;
    private final SalesOrderItemMapper salesOrderItemMapper;
    private final TenantEmployeeMapper tenantEmployeeMapper;
    private final TenantMemberMapper tenantMemberMapper;
    private final ProductMapper productMapper;
    private final UnifiedWalletService unifiedWalletService;
    private final MerchantWalletService merchantWalletService;
    private final PaymentBillV1Service paymentBillV1Service;
    private final WithdrawalService withdrawalService;
    private final MemberPointsAccountService memberPointsAccountService;
    private final PointsRuleMapper pointsRuleMapper;
    private final OrderPricingService orderPricingService;
    private final CouponService couponService;
    private final PromotionService promotionService;
    private final OrderDiscountSnapshotMapper orderDiscountSnapshotMapper;
    private final UserBehaviorLogService userBehaviorLogService;

    /**
     * 创建订单。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderPaymentVO createOrder(Long platformUserId, AppCreateOrderDTO dto) {
        List<OrderLine> orderLines = buildOrderLines(dto);
        ensureTenantMember(dto.getTenantId(), platformUserId);

        String orderNo = BizNoGenerator.generate("SO");
        OrderPricingResultVO pricingResult = orderPricingService.calculate(buildPricingRequest(platformUserId, dto, orderLines, orderNo));
        WalletSplit split = calculateWalletSplit(platformUserId, dto, pricingResult.getPayableAmount());

        SalesOrder salesOrder = new SalesOrder();
        salesOrder.setOrderNo(orderNo);
        salesOrder.setTenantId(dto.getTenantId());
        salesOrder.setPlatformUserId(platformUserId);
        salesOrder.setOrderStatus(OrderStatusEnum.CREATED.name());
        salesOrder.setPayStatus(split.externalPayAmount.compareTo(BigDecimal.ZERO) == 0
                ? PayStatusEnum.SUCCESS.name()
                : PayStatusEnum.WAIT_PAY.name());
        salesOrder.setTotalAmount(pricingResult.getTotalAmount());
        salesOrder.setDiscountAmount(pricingResult.getActivityDiscountAmount().add(pricingResult.getCouponDiscountAmount()));
        salesOrder.setPointsDeductAmount(pricingResult.getPointsDeductAmount());
        salesOrder.setWalletDeductAmount(split.unifiedWalletAmount.add(split.merchantWalletAmount));
        salesOrder.setUnifiedWalletDeductAmount(split.unifiedWalletAmount);
        salesOrder.setMerchantWalletDeductAmount(split.merchantWalletAmount);
        salesOrder.setExternalPayAmount(split.externalPayAmount);
        salesOrder.setPayableAmount(pricingResult.getPayableAmount());
        salesOrder.setSubject(resolveSubject(dto.getSubject(), orderLines));
        salesOrder.setSource(dto.getSource());
        salesOrder.setWalletStrategy(dto.getWalletStrategy().name());
        salesOrder.setExpireTime(LocalDateTime.now().plusMinutes(30));
        salesOrder.setDeleted(0);
        salesOrderMapper.insert(salesOrder);

        insertOrderItems(salesOrder, orderLines);
        insertDiscountSnapshots(salesOrder, pricingResult.getDiscountSnapshots());
        lockCouponIfNeeded(dto, salesOrder, pricingResult);
        holdPointsIfNeeded(salesOrder, pricingResult);

        // 钱包金额在下单时直接扣减，取消订单时再按原路径回补。
        if (split.unifiedWalletAmount.compareTo(BigDecimal.ZERO) > 0) {
            unifiedWalletService.debit(platformUserId, split.unifiedWalletAmount, "SALES_ORDER", orderNo, "订单消费扣减");
        }
        if (split.merchantWalletAmount.compareTo(BigDecimal.ZERO) > 0) {
            merchantWalletService.debit(dto.getTenantId(), platformUserId, split.merchantWalletAmount, "SALES_ORDER", orderNo, "订单消费扣减");
        }

        OrderPaymentVO result = buildOrderPaymentVO(salesOrder);
        if (split.externalPayAmount.compareTo(BigDecimal.ZERO) > 0) {
            PaymentBill paymentBill = paymentBillV1Service.createBill(
                    PaymentBizTypeEnum.SALES_ORDER.name(),
                    orderNo,
                    dto.getTenantId(),
                    platformUserId,
                    split.externalPayAmount,
                    resolveExternalChannel(dto.getPaymentChannelCode())
            );
            PayResponseDTO payResponse = paymentBillV1Service.createExternalPayment(paymentBill);
            result.setPaymentBillNo(paymentBill.getBillNo());
            result.setExternalPayUrl(payResponse.getPayUrl());
        } else {
            salesOrder.setOrderStatus(OrderStatusEnum.PAID.name());
            salesOrder.setPayStatus(PayStatusEnum.SUCCESS.name());
            salesOrderMapper.updateById(salesOrder);
            settlePaidOrder(salesOrder);
            result.setOrderStatus(OrderStatusEnum.PAID.name());
            result.setPayStatus(PayStatusEnum.SUCCESS.name());
        }

        // 记录购买行为（埋点失败不影响主流程）
        try {
            userBehaviorLogService.recordBehavior(
                    platformUserId, dto.getTenantId(), "PURCHASE",
                    "PRODUCT", null, "{\"orderNo\":\"" + orderNo + "\",\"itemCount\":" + orderLines.size() + "}");
        } catch (Exception e) {
            log.warn("记录 PURCHASE 行为日志失败, orderNo={}", orderNo, e);
        }

        return result;
    }

    /**
     * 查询订单。
     */
    @Override
    public Page<SalesOrder> listOrders(Long platformUserId, Integer current, Integer size) {
        return salesOrderMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getPlatformUserId, platformUserId)
                .eq(SalesOrder::getDeleted, 0)
                .orderByDesc(SalesOrder::getCreateTime));
    }

    /**
     * 获取订单No。
     */
    @Override
    public SalesOrder getByOrderNo(Long platformUserId, String orderNo) {
        SalesOrder salesOrder = salesOrderMapper.selectOne(new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getOrderNo, orderNo)
                .eq(SalesOrder::getPlatformUserId, platformUserId)
                .eq(SalesOrder::getDeleted, 0));
        if (salesOrder == null) {
            throw new BusinessException("订单不存在");
        }
        return salesOrder;
    }

    /**
     * 获取订单Detail。
     */
    @Override
    public SalesOrderDetailVO getOrderDetail(Long platformUserId, String orderNo) {
        SalesOrder salesOrder = getByOrderNo(platformUserId, orderNo);
        return buildOrderDetailVO(salesOrder, salesOrderItemMapper.selectByOrderId(salesOrder.getId()));
    }

    /**
     * 获取商家端订单Detail。
     */
    @Override
    public SalesOrderDetailVO getMerchantOrderDetail(Long tenantId, Long platformUserId, String orderNo) {
        TenantEmployee tenantEmployee = tenantEmployeeMapper.selectOne(new LambdaQueryWrapper<TenantEmployee>()
                .eq(TenantEmployee::getTenantId, tenantId)
                .eq(TenantEmployee::getPlatformUserId, platformUserId)
                .eq(TenantEmployee::getStatus, 1));
        if (tenantEmployee == null) {
            throw new BusinessException("当前用户无权查看该商户订单");
        }

        SalesOrder salesOrder = salesOrderMapper.selectOne(new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getOrderNo, orderNo)
                .eq(SalesOrder::getTenantId, tenantId)
                .eq(SalesOrder::getDeleted, 0));
        if (salesOrder == null) {
            throw new BusinessException("订单不存在");
        }

        return buildOrderDetailVO(salesOrder, salesOrderItemMapper.selectByOrderId(salesOrder.getId()));
    }

    /**
     * 查询商家端订单。
     */
    @Override
    public Page<SalesOrder> listMerchantOrders(Long tenantId, Integer current, Integer size, String orderStatus, String payStatus, String keyword) {
        return salesOrderMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getTenantId, tenantId)
                .eq(SalesOrder::getDeleted, 0)
                .eq(orderStatus != null && !orderStatus.isBlank(), SalesOrder::getOrderStatus, orderStatus)
                .eq(payStatus != null && !payStatus.isBlank(), SalesOrder::getPayStatus, payStatus)
                .and(keyword != null && !keyword.isBlank(), wrapper -> wrapper.like(SalesOrder::getOrderNo, keyword)
                        .or()
                        .like(SalesOrder::getSubject, keyword))
                .orderByDesc(SalesOrder::getCreateTime));
    }

    /**
     * 查询订单Item。
     */
    @Override
    public List<SalesOrderItem> listOrderItems(Long platformUserId, String orderNo) {
        SalesOrder salesOrder = getByOrderNo(platformUserId, orderNo);
        return salesOrderItemMapper.selectByOrderId(salesOrder.getId());
    }

    /**
     * 处理repay订单。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderPaymentVO repayOrder(Long platformUserId, String orderNo, PaymentChannelCodeEnum paymentChannelCode) {
        SalesOrder salesOrder = getByOrderNo(platformUserId, orderNo);
        if (!OrderStatusEnum.CREATED.name().equals(salesOrder.getOrderStatus())) {
            throw new BusinessException("当前订单状态不允许重新发起支付");
        }
        if (PayStatusEnum.SUCCESS.name().equals(salesOrder.getPayStatus())) {
            throw new BusinessException("订单已支付成功，无需重复支付");
        }
        if (PayStatusEnum.CLOSED.name().equals(salesOrder.getPayStatus())) {
            throw new BusinessException("订单支付已关闭，请重新下单");
        }
        if (salesOrder.getExternalPayAmount() == null || salesOrder.getExternalPayAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("当前订单不存在待支付的外部支付金额");
        }

        List<PaymentBill> paymentBills = paymentBillV1Service.listByBizTypeAndBizNo(
                PaymentBizTypeEnum.SALES_ORDER.name(),
                orderNo
        );

        PaymentBill activeBill = resolveReusablePaymentBill(paymentBills, salesOrder);
        boolean reusedPaymentBill = activeBill != null;
        if (activeBill == null) {
            PaymentChannelCodeEnum resolvedChannel = paymentChannelCode != null
                    ? paymentChannelCode
                    : PaymentChannelCodeEnum.ALIPAY_PAGE;
            activeBill = paymentBillV1Service.createBill(
                    PaymentBizTypeEnum.SALES_ORDER.name(),
                    orderNo,
                    salesOrder.getTenantId(),
                    salesOrder.getPlatformUserId(),
                    salesOrder.getExternalPayAmount(),
                    resolvedChannel
            );
        }

        PayResponseDTO payResponse = paymentBillV1Service.createExternalPayment(activeBill);

        OrderPaymentVO result = buildOrderPaymentVO(salesOrder);
        result.setPaymentBillNo(activeBill.getBillNo());
        result.setExternalPayUrl(payResponse.getPayUrl());
        result.setReusedPaymentBill(reusedPaymentBill);
        return result;
    }

    /**
     * 判断是否可以cel订单。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long platformUserId, String orderNo) {
        SalesOrder salesOrder = getByOrderNo(platformUserId, orderNo);
        if (OrderStatusEnum.PAID.name().equals(salesOrder.getOrderStatus())) {
            throw new BusinessException("已支付订单不允许取消");
        }
        if (OrderStatusEnum.CANCELLED.name().equals(salesOrder.getOrderStatus())) {
            return;
        }

        if (salesOrder.getUnifiedWalletDeductAmount().compareTo(BigDecimal.ZERO) > 0) {
            unifiedWalletService.credit(platformUserId, salesOrder.getUnifiedWalletDeductAmount(), "ORDER_CANCEL_REFUND", orderNo, "取消订单回退");
        }
        if (salesOrder.getMerchantWalletDeductAmount().compareTo(BigDecimal.ZERO) > 0) {
            merchantWalletService.credit(salesOrder.getTenantId(), platformUserId, salesOrder.getMerchantWalletDeductAmount(), "ORDER_CANCEL_REFUND", orderNo, "取消订单回退");
        }
        releaseDiscountAssets(salesOrder, "订单取消");

        salesOrder.setOrderStatus(OrderStatusEnum.CANCELLED.name());
        salesOrder.setPayStatus(PayStatusEnum.CLOSED.name());
        salesOrderMapper.updateById(salesOrder);

        List<PaymentBill> paymentBills = paymentBillV1Service.listByBizTypeAndBizNo(
                PaymentBizTypeEnum.SALES_ORDER.name(),
                orderNo
        );
        if (paymentBills.isEmpty()) {
            return;
        }

        for (PaymentBill paymentBill : paymentBills) {
            if (!PayStatusEnum.SUCCESS.name().equals(paymentBill.getPayStatus())) {
                paymentBillV1Service.markBillClosed(
                        paymentBill.getBillNo(),
                        PaymentStatusReasonEnum.SALES_ORDER_CANCELLED_REFUND_REQUIRED
                );
            }
        }
    }

    /**
     * 构建订单Line。
     */
    private List<OrderLine> buildOrderLines(AppCreateOrderDTO dto) {
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BusinessException("订单商品明细不能为空");
        }

        LinkedHashMap<Long, Integer> mergedQuantities = new LinkedHashMap<>();
        for (AppCreateOrderItemDTO item : dto.getItems()) {
            if (item.getProductId() == null) {
                throw new BusinessException("商品ID不能为空");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BusinessException("商品数量必须大于0");
            }
            mergedQuantities.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }

        Set<Long> productIds = mergedQuantities.keySet();
        Map<Long, Product> productMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<OrderLine> orderLines = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : mergedQuantities.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();

            Product product = productMap.get(productId);
            if (product == null) {
                throw new BusinessException("商品不存在, productId=" + productId);
            }
            if (product.getDeleted() != null && product.getDeleted() == 1) {
                throw new BusinessException("商品已删除, productId=" + productId);
            }
            if (!dto.getTenantId().equals(product.getTenantId())) {
                throw new BusinessException("商品不属于当前商户, productId=" + productId);
            }
            if (product.getStatus() == null || product.getStatus() != 1) {
                throw new BusinessException("商品已下架, productId=" + productId);
            }
            if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("商品价格非法, productId=" + productId);
            }

            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));
            orderLines.add(new OrderLine(product, quantity, subtotal));
        }

        return orderLines;
    }

    /**
     * 处理calculateTotalAmount。
     */
    private BigDecimal calculateTotalAmount(List<OrderLine> orderLines) {
        return orderLines.stream()
                .map(OrderLine::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private OrderPricingRequestDTO buildPricingRequest(Long platformUserId,
                                                       AppCreateOrderDTO dto,
                                                       List<OrderLine> orderLines,
                                                       String orderNo) {
        OrderPricingRequestDTO request = new OrderPricingRequestDTO();
        request.setTenantId(dto.getTenantId());
        request.setPlatformUserId(platformUserId);
        request.setOrderNo(orderNo);
        List<OrderPricingItemDTO> pricingItems = orderLines.stream().map(this::toPricingItem).collect(Collectors.toList());
        request.setItems(pricingItems);
        request.setPromotionCandidates(promotionService.matchPromotions(dto.getTenantId(), pricingItems));
        request.setSelectedCoupon(couponService.resolveCouponCandidate(
                dto.getSelectedUserCouponId(),
                dto.getTenantId(),
                platformUserId,
                pricingItems
        ));
        request.setRequestedPoints(dto.getRequestedPoints());
        MemberPointsAccount pointsAccount = memberPointsAccountService.getAccount(dto.getTenantId(), platformUserId);
        request.setAvailablePoints(pointsAccount == null ? 0 : pointsAccount.getPoints());
        request.setPointAmount(new BigDecimal("0.01"));
        return request;
    }

    private OrderPricingItemDTO toPricingItem(OrderLine orderLine) {
        OrderPricingItemDTO item = new OrderPricingItemDTO();
        item.setProductId(orderLine.product().getId());
        item.setCategory(orderLine.product().getCategory());
        item.setUnitPrice(orderLine.product().getPrice());
        item.setQuantity(orderLine.quantity());
        return item;
    }

    private void insertDiscountSnapshots(SalesOrder salesOrder, List<DiscountSnapshotPlanVO> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            return;
        }
        for (DiscountSnapshotPlanVO snapshot : snapshots) {
            OrderDiscountSnapshot entity = new OrderDiscountSnapshot();
            entity.setOrderId(salesOrder.getId());
            entity.setOrderNo(salesOrder.getOrderNo());
            entity.setTenantId(salesOrder.getTenantId());
            entity.setActivityId(snapshot.getActivityId());
            entity.setActivityRuleId(snapshot.getActivityRuleId());
            entity.setUserCouponId(snapshot.getUserCouponId());
            entity.setCouponTemplateId(snapshot.getCouponTemplateId());
            entity.setDiscountSource(snapshot.getDiscountSource());
            entity.setDiscountType(snapshot.getDiscountType());
            entity.setDiscountAmount(snapshot.getDiscountAmount());
            entity.setRuleSnapshotJson(snapshot.getRuleSnapshotJson());
            orderDiscountSnapshotMapper.insert(entity);
        }
    }

    private void lockCouponIfNeeded(AppCreateOrderDTO dto, SalesOrder salesOrder, OrderPricingResultVO pricingResult) {
        if (dto.getSelectedUserCouponId() == null || pricingResult.getCouponDiscountAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        couponService.lockCoupon(
                dto.getSelectedUserCouponId(),
                salesOrder.getTenantId(),
                salesOrder.getPlatformUserId(),
                salesOrder.getId(),
                salesOrder.getOrderNo(),
                salesOrder.getOrderNo()
        );
    }

    private void holdPointsIfNeeded(SalesOrder salesOrder, OrderPricingResultVO pricingResult) {
        if (pricingResult.getPointsPlan() == null || !Boolean.TRUE.equals(pricingResult.getPointsPlan().getNeedHold())) {
            return;
        }
        memberPointsAccountService.holdPoints(
                salesOrder.getTenantId(),
                salesOrder.getPlatformUserId(),
                pricingResult.getPointsPlan().getHoldPoints(),
                "ORDER_DEDUCT",
                salesOrder.getOrderNo(),
                "订单积分预占"
        );
    }

    /**
     * 新增订单Item。
     */
    private void insertOrderItems(SalesOrder salesOrder, List<OrderLine> orderLines) {
        List<SalesOrderItem> items = orderLines.stream().map(orderLine -> {
            SalesOrderItem orderItem = new SalesOrderItem();
            orderItem.setOrderId(salesOrder.getId());
            orderItem.setOrderNo(salesOrder.getOrderNo());
            orderItem.setTenantId(salesOrder.getTenantId());
            orderItem.setProductId(orderLine.product().getId());
            orderItem.setProductName(orderLine.product().getName());
            orderItem.setPrice(orderLine.product().getPrice());
            orderItem.setQuantity(orderLine.quantity());
            orderItem.setSubtotal(orderLine.subtotal());
            return orderItem;
        }).collect(java.util.stream.Collectors.toList());
        if (!items.isEmpty()) {
            salesOrderItemMapper.insertBatch(items);
        }
    }

    /**
     * 处理ensure租户会员。
     */
    private void ensureTenantMember(Long tenantId, Long platformUserId) {
        TenantMember tenantMember = tenantMemberMapper.selectOne(new LambdaQueryWrapper<TenantMember>()
                .eq(TenantMember::getTenantId, tenantId)
                .eq(TenantMember::getPlatformUserId, platformUserId));
        if (tenantMember != null) {
            return;
        }

        TenantMember newTenantMember = new TenantMember();
        newTenantMember.setTenantId(tenantId);
        newTenantMember.setPlatformUserId(platformUserId);
        newTenantMember.setMemberNo(BizNoGenerator.generate("TM"));
        newTenantMember.setMemberStatus(1);
        newTenantMember.setRegisterSource("APP");
        tenantMemberMapper.insert(newTenantMember);
    }

    /**
     * 解析Subject。
     */
    private String resolveSubject(String subject, List<OrderLine> orderLines) {
        if (StringUtils.hasText(subject)) {
            return subject.trim();
        }

        OrderLine firstLine = orderLines.get(0);
        if (orderLines.size() == 1) {
            return firstLine.product().getName() + " x " + firstLine.quantity();
        }
        return firstLine.product().getName() + "等" + orderLines.size() + "件商品";
    }

    /**
     * 处理calculate钱包Split。
     */
    private WalletSplit calculateWalletSplit(Long platformUserId, AppCreateOrderDTO dto, BigDecimal totalAmount) {
        BigDecimal unifiedBalance = unifiedWalletService.getWallet(platformUserId).getAvailableAmount();
        BigDecimal merchantBalance = merchantWalletService.getWallet(dto.getTenantId(), platformUserId).getAvailableAmount();
        boolean allowFallback = Boolean.TRUE.equals(dto.getAllowExternalPayFallback());

        return switch (dto.getWalletStrategy()) {
            case NO_WALLET -> new WalletSplit(BigDecimal.ZERO, BigDecimal.ZERO, totalAmount);
            case UNIFIED_ONLY -> calculateSingleWallet(totalAmount, unifiedBalance, allowFallback, true);
            case MERCHANT_ONLY -> calculateSingleWallet(totalAmount, merchantBalance, allowFallback, false);
            case MERCHANT_THEN_UNIFIED -> calculateChainedWallet(totalAmount, merchantBalance, unifiedBalance, allowFallback, false);
            case UNIFIED_THEN_MERCHANT -> calculateChainedWallet(totalAmount, unifiedBalance, merchantBalance, allowFallback, true);
            case CUSTOM_SPLIT -> calculateCustomSplit(totalAmount, dto, unifiedBalance, merchantBalance, allowFallback);
        };
    }

    /**
     * 处理calculateSingle钱包。
     */
    private WalletSplit calculateSingleWallet(BigDecimal totalAmount,
                                              BigDecimal balance,
                                              boolean allowFallback,
                                              boolean unifiedFirst) {
        BigDecimal used = balance.min(totalAmount);
        if (!allowFallback && used.compareTo(totalAmount) < 0) {
            throw new BusinessException("钱包余额不足");
        }

        BigDecimal external = totalAmount.subtract(used);
        /**
         * 处理钱包Split。
         */
        return unifiedFirst
                ? new WalletSplit(used, BigDecimal.ZERO, external)
                : new WalletSplit(BigDecimal.ZERO, used, external);
    /**
     * 处理calculateChained钱包。
     */
    }

    private WalletSplit calculateChainedWallet(BigDecimal totalAmount,
                                               BigDecimal firstBalance,
                                               BigDecimal secondBalance,
                                               boolean allowFallback,
                                               boolean unifiedFirst) {
        BigDecimal firstUsed = firstBalance.min(totalAmount);
        BigDecimal remain = totalAmount.subtract(firstUsed);
        BigDecimal secondUsed = secondBalance.min(remain);
        BigDecimal external = totalAmount.subtract(firstUsed).subtract(secondUsed);
        if (!allowFallback && external.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("钱包余额不足");
        }

        return unifiedFirst
                ? new WalletSplit(firstUsed, secondUsed, external)
                : new WalletSplit(secondUsed, firstUsed, external);
    }

    private WalletSplit calculateCustomSplit(BigDecimal totalAmount,
                                             AppCreateOrderDTO dto,
                                             BigDecimal unifiedBalance,
                                             BigDecimal merchantBalance,
                                             boolean allowFallback) {
        BigDecimal unifiedAmount = defaultAmount(dto.getUnifiedWalletAmount());
        BigDecimal merchantAmount = defaultAmount(dto.getMerchantWalletAmount());

        if (unifiedAmount.compareTo(unifiedBalance) > 0 || merchantAmount.compareTo(merchantBalance) > 0) {
            throw new BusinessException("自定义钱包金额超过可用余额");
        }

        BigDecimal walletTotal = unifiedAmount.add(merchantAmount);
        if (walletTotal.compareTo(totalAmount) > 0) {
            throw new BusinessException("自定义钱包金额不能超过订单金额");
        }

        BigDecimal external = totalAmount.subtract(walletTotal);
        if (!allowFallback && external.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("当前订单不允许剩余金额走外部支付");
        }

        return new WalletSplit(unifiedAmount, merchantAmount, external);
    }

    private BigDecimal defaultAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private PaymentChannelCodeEnum resolveExternalChannel(PaymentChannelCodeEnum channelCode) {
        if (channelCode == null) {
            throw new BusinessException("存在外部支付金额时必须指定 paymentChannelCode");
        }
        return channelCode;
    }

    /**
     * 纯钱包支付没有外部回调，直接在本地事务里完成结算。
     */
    private void settlePaidOrder(SalesOrder salesOrder) {
        confirmDiscountAssets(salesOrder);
        BigDecimal settlementAmount = salesOrder.getPayableAmount().subtract(salesOrder.getMerchantWalletDeductAmount());
        if (settlementAmount.compareTo(BigDecimal.ZERO) > 0) {
            withdrawalService.addMerchantBalance(salesOrder.getTenantId(), settlementAmount, salesOrder.getOrderNo());
        }

        PointsRule pointsRule = pointsRuleMapper.selectOne(new LambdaQueryWrapper<PointsRule>()
                .eq(PointsRule::getTenantId, salesOrder.getTenantId())
                .eq(PointsRule::getStatus, 1));
        if (pointsRule != null && pointsRule.getPointsAmount() != null && pointsRule.getPointsAmount() > 0) {
            int points = pointsRule.getPointsAmount();
            memberPointsAccountService.grantPoints(
                    salesOrder.getTenantId(),
                    salesOrder.getPlatformUserId(),
                    points,
                    "SALES_ORDER",
                    salesOrder.getOrderNo(),
                    "消费赠送积分"
            );
        }
    }

    private OrderPaymentVO buildOrderPaymentVO(SalesOrder salesOrder) {
        OrderPaymentVO vo = new OrderPaymentVO();
        vo.setOrderNo(salesOrder.getOrderNo());
        vo.setOrderStatus(salesOrder.getOrderStatus());
        vo.setPayStatus(salesOrder.getPayStatus());
        vo.setTotalAmount(salesOrder.getTotalAmount());
        vo.setDiscountAmount(salesOrder.getDiscountAmount());
        vo.setPointsDeductAmount(salesOrder.getPointsDeductAmount());
        vo.setPayableAmount(salesOrder.getPayableAmount());
        vo.setUnifiedWalletDeductAmount(salesOrder.getUnifiedWalletDeductAmount());
        vo.setMerchantWalletDeductAmount(salesOrder.getMerchantWalletDeductAmount());
        vo.setExternalPayAmount(salesOrder.getExternalPayAmount());
        return vo;
    }

    private void confirmDiscountAssets(SalesOrder salesOrder) {
        List<OrderDiscountSnapshot> snapshots = listDiscountSnapshots(salesOrder.getOrderNo());
        for (OrderDiscountSnapshot snapshot : snapshots) {
            if (DiscountSourceEnum.COUPON.name().equals(snapshot.getDiscountSource()) && snapshot.getUserCouponId() != null) {
                couponService.writeOffCoupon(
                        snapshot.getUserCouponId(),
                        salesOrder.getTenantId(),
                        salesOrder.getId(),
                        salesOrder.getOrderNo(),
                        salesOrder.getOrderNo(),
                        snapshot.getDiscountAmount()
                );
            }
        }
        if (salesOrder.getPointsDeductAmount() != null && salesOrder.getPointsDeductAmount().compareTo(BigDecimal.ZERO) > 0) {
            memberPointsAccountService.confirmPointsHold(
                    salesOrder.getTenantId(),
                    salesOrder.getPlatformUserId(),
                    "ORDER_DEDUCT",
                    salesOrder.getOrderNo()
            );
        }
    }

    private void releaseDiscountAssets(SalesOrder salesOrder, String releaseReason) {
        List<OrderDiscountSnapshot> snapshots = listDiscountSnapshots(salesOrder.getOrderNo());
        for (OrderDiscountSnapshot snapshot : snapshots) {
            if (DiscountSourceEnum.COUPON.name().equals(snapshot.getDiscountSource()) && snapshot.getUserCouponId() != null) {
                couponService.releaseCoupon(
                        snapshot.getUserCouponId(),
                        salesOrder.getTenantId(),
                        salesOrder.getPlatformUserId(),
                        salesOrder.getId(),
                        salesOrder.getOrderNo(),
                        salesOrder.getOrderNo(),
                        releaseReason
                );
            }
        }
        if (salesOrder.getPointsDeductAmount() != null && salesOrder.getPointsDeductAmount().compareTo(BigDecimal.ZERO) > 0) {
            memberPointsAccountService.releasePointsHold(
                    salesOrder.getTenantId(),
                    salesOrder.getPlatformUserId(),
                    "ORDER_DEDUCT",
                    salesOrder.getOrderNo(),
                    releaseReason
            );
        }
    }

    private List<OrderDiscountSnapshot> listDiscountSnapshots(String orderNo) {
        return orderDiscountSnapshotMapper.selectList(new LambdaQueryWrapper<OrderDiscountSnapshot>()
                .eq(OrderDiscountSnapshot::getOrderNo, orderNo));
    }

    private PaymentBill resolveReusablePaymentBill(List<PaymentBill> paymentBills, SalesOrder salesOrder) {
        if (paymentBills == null || paymentBills.isEmpty()) {
            return null;
        }

        for (PaymentBill bill : paymentBills) {
            PaymentBill paymentBill = bill;
            if (paymentBill == null) {
                continue;
            }
            if (!PaymentBizTypeEnum.SALES_ORDER.name().equals(paymentBill.getBizType())) {
                continue;
            }
            if (!salesOrder.getOrderNo().equals(paymentBill.getBizNo())) {
                continue;
            }

            paymentBill = paymentBillV1Service.syncBillStatus(paymentBill.getBillNo());

            if (PayStatusEnum.SUCCESS.name().equals(paymentBill.getPayStatus())) {
                throw new BusinessException("订单已支付成功，无需重复支付");
            }
            if (PayStatusEnum.WAIT_PAY.name().equals(paymentBill.getPayStatus())
                    || PayStatusEnum.PAYING.name().equals(paymentBill.getPayStatus())) {
                if (paymentBill.getExpireTime() != null && paymentBill.getExpireTime().isBefore(LocalDateTime.now())) {
                    paymentBillV1Service.markBillClosed(
                            paymentBill.getBillNo(),
                            PaymentStatusReasonEnum.MANUAL_REVIEW_REQUIRED
                    );
                    continue;
                }
                return paymentBill;
            }
        }
        return null;
    }

    private SalesOrderDetailVO buildOrderDetailVO(SalesOrder salesOrder, List<SalesOrderItem> orderItems) {
        SalesOrderDetailVO detailVO = new SalesOrderDetailVO();
        detailVO.setOrder(salesOrder);
        detailVO.setItems(orderItems);
        PaymentBill paymentBill = paymentBillV1Service.getLatestByBizTypeAndBizNo(
                PaymentBizTypeEnum.SALES_ORDER.name(),
                salesOrder.getOrderNo()
        );
        detailVO.setPaymentBillNo(paymentBill != null ? paymentBill.getBillNo() : null);
        return detailVO;
    }

    private record WalletSplit(BigDecimal unifiedWalletAmount,
                               BigDecimal merchantWalletAmount,
                               BigDecimal externalPayAmount) {
    }

    private record OrderLine(Product product,
                             Integer quantity,
                             BigDecimal subtotal) {
    }
}
