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
import com.payment.entity.ProductStock;
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
import com.payment.util.TenantContextHolder;
import com.payment.vo.VoConverterUtil;
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
 * C 端用户订单服务实现类，处理用户端订单全生命周期。
 * <p>
 * 核心职责包括：
 * <ul>
 *   <li><b>下单</b>：校验商品、计算定价（活动/优惠券/积分/钱包）、生成订单与支付账单</li>
 *   <li><b>支付回调</b>：处理外部支付成功后的订单状态流转与商户结算</li>
 *   <li><b>结算</b>：纯钱包支付或支付回调成功后，确认优惠券核销、积分消耗、商户入账、触发交付</li>
 *   <li><b>重新支付</b>：对待支付订单重新发起外部支付</li>
 *   <li><b>取消</b>：回退钱包扣减、释放优惠券与积分、关闭支付账单</li>
 *   <li><b>查询</b>：用户端和商家端的订单列表与详情查询</li>
 * </ul>
 * <p>
 * 支持 6 种钱包支付策略（NO_WALLET / UNIFIED_ONLY / MERCHANT_ONLY / MERCHANT_THEN_UNIFIED /
 * UNIFIED_THEN_MERCHANT / CUSTOM_SPLIT），钱包金额在下单时直接扣减，取消时按原路径回补。
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
    private final com.payment.service.delivery.OrderDeliveryService orderDeliveryService;

    /**
     * 创建订单并发起支付。
     * <p>
     * 流程：
     * <ol>
     *   <li>校验商品合法性（存在性、上下架、价格、归属商户）</li>
     *   <li>确保用户已成为该商户会员（自动注册）</li>
     *   <li>调用定价引擎计算活动折扣、优惠券抵扣、积分抵扣</li>
     *   <li>按钱包策略拆分支付金额（钱包 / 外部支付）</li>
     *   <li>持久化订单、订单项、优惠快照，锁定优惠券、预占积分</li>
     *   <li>扣减钱包余额，发起外部支付或直接标记支付成功并结算</li>
     * </ol>
     *
     * @param platformUserId 平台用户 ID
     * @param dto            创建订单请求参数
     * @return 订单支付信息，包含支付链接（若有外部支付）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderPaymentVO createOrder(Long platformUserId, AppCreateOrderDTO dto) {
        Long previousTenantId = TenantContextHolder.getTenantId();
        try {
            TenantContextHolder.setTenantId(dto.getTenantId());
            return createOrderInTenantContext(platformUserId, dto);
        } finally {
            TenantContextHolder.clear();
            if (previousTenantId != null) {
                TenantContextHolder.setTenantId(previousTenantId);
            }
        }
    }

    private OrderPaymentVO createOrderInTenantContext(Long platformUserId, AppCreateOrderDTO dto) {
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
        salesOrder.setStoreId(resolveStoreId(orderLines));
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
     * 分页查询用户订单列表，按创建时间降序。
     *
     * @param platformUserId 平台用户 ID
     * @param current        当前页码
     * @param size           每页条数
     * @return 订单分页结果
     */
    @Override
    public Page<SalesOrder> listOrders(Long platformUserId, Integer current, Integer size) {
        return salesOrderMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getPlatformUserId, platformUserId)
                .eq(SalesOrder::getDeleted, 0)
                .orderByDesc(SalesOrder::getCreateTime));
    }

    /**
     * 根据订单号获取订单实体，校验归属用户。
     *
     * @param platformUserId 平台用户 ID
     * @param orderNo        订单号
     * @return 订单实体
     * @throws BusinessException 订单不存在时抛出
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
     * 获取用户端订单详情，包含订单项与支付账单信息。
     *
     * @param platformUserId 平台用户 ID
     * @param orderNo        订单号
     * @return 订单详情 VO
     */
    @Override
    public SalesOrderDetailVO getOrderDetail(Long platformUserId, String orderNo) {
        SalesOrder salesOrder = getByOrderNo(platformUserId, orderNo);
        return buildOrderDetailVO(salesOrder, salesOrderItemMapper.selectByOrderId(salesOrder.getId()));
    }

    /**
     * 获取商家端订单详情。
     * <p>
     * 先校验当前用户是否为该商户的在职员工，再返回订单详情。
     *
     * @param tenantId       商户 ID
     * @param platformUserId 当前操作用户 ID
     * @param orderNo        订单号
     * @return 订单详情 VO
     * @throws BusinessException 用户无权或订单不存在时抛出
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
     * 分页查询商家端订单列表，支持按订单状态、支付状态和关键词筛选。
     *
     * @param tenantId    商户 ID
     * @param current     当前页码
     * @param size        每页条数
     * @param orderStatus 订单状态筛选，可为 null
     * @param payStatus   支付状态筛选，可为 null
     * @param keyword     搜索关键词（匹配订单号或商品标题），可为 null
     * @return 订单分页结果
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
     * 查询指定订单的订单项列表。
     *
     * @param platformUserId 平台用户 ID
     * @param orderNo        订单号
     * @return 订单项列表
     */
    @Override
    public List<SalesOrderItem> listOrderItems(Long platformUserId, String orderNo) {
        SalesOrder salesOrder = getByOrderNo(platformUserId, orderNo);
        return salesOrderItemMapper.selectByOrderId(salesOrder.getId());
    }

    /**
     * 重新发起订单支付（补单）。
     * <p>
     * 仅允许对已创建但未支付的订单操作。优先复用未过期的已有支付账单，
     * 否则创建新的支付账单并调用第三方支付。
     *
     * @param platformUserId    平台用户 ID
     * @param orderNo           订单号
     * @param paymentChannelCode 支付渠道编码，可为 null（默认支付宝网页支付）
     * @return 订单支付信息，包含支付链接
     * @throws BusinessException 订单状态不允许或已支付时抛出
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
     * 取消订单。
     * <p>
     * 仅允许取消未支付的订单。操作包括：
     * <ul>
     *   <li>回退已扣减的钱包余额（统一钱包 + 商户钱包）</li>
     *   <li>释放已锁定的优惠券和预占的积分</li>
     *   <li>关闭关联的支付账单</li>
     * </ul>
     *
     * @param platformUserId 平台用户 ID
     * @param orderNo        订单号
     * @throws BusinessException 已支付订单不允许取消时抛出
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
     * 校验并构建订单行列表。
     * <p>
     * 合并相同商品的数量，校验商品存在性、上下架状态、归属商户和价格合法性。
     *
     * @param dto 创建订单请求
     * @return 订单行列表（商品 + 数量 + 小计）
     * @throws BusinessException 商品校验不通过时抛出
     */
    private List<OrderLine> buildOrderLines(AppCreateOrderDTO dto) {
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BusinessException("订单商品明细不能为空");
        }

        LinkedHashMap<Long, Integer> mergedQuantities = new LinkedHashMap<>();
        Map<Long, BigDecimal> submittedPriceMap = new LinkedHashMap<>();
        for (AppCreateOrderItemDTO item : dto.getItems()) {
            if (item.getProductId() == null) {
                throw new BusinessException("商品ID不能为空");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BusinessException("商品数量必须大于0");
            }
            if (item.getPrice() != null && item.getPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("商品价格非法, productId=" + item.getProductId());
            }
            if (item.getPrice() != null) {
                BigDecimal existingPrice = submittedPriceMap.putIfAbsent(item.getProductId(), item.getPrice());
                if (existingPrice != null && existingPrice.compareTo(item.getPrice()) != 0) {
                    throw new BusinessException("同一商品提交价格不一致, productId=" + item.getProductId());
                }
            }
            mergedQuantities.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }

        Set<Long> productIds = mergedQuantities.keySet();
        Map<Long, Product> productMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        List<ProductStock> stockRows = productMapper.selectStockByTenantAndProductIds(dto.getTenantId(), productIds);
        Map<Long, Integer> stockMap = (stockRows == null ? List.<ProductStock>of() : stockRows).stream()
                .collect(Collectors.toMap(
                        ProductStock::getProductId,
                        stock -> stock.getQuantity() == null ? 0 : stock.getQuantity(),
                        (left, right) -> left
                ));

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
            BigDecimal submittedPrice = submittedPriceMap.get(productId);
            if (submittedPrice != null) {
                BigDecimal currentPrice = BigDecimal.valueOf(VoConverterUtil.toFen(product.getPrice()));
                if (currentPrice.compareTo(submittedPrice) != 0) {
                    throw new BusinessException("商品价格已变化, productId=" + productId);
                }
            }
            Integer stockQuantity = stockMap.get(productId);
            if (stockQuantity == null || stockQuantity < quantity) {
                throw new BusinessException("商品库存不足, productId=" + productId);
            }

            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));
            orderLines.add(new OrderLine(product, quantity, subtotal));
        }

        return orderLines;
    }

    /**
     * 计算订单总金额（所有订单行小计之和）。
     *
     * @param orderLines 订单行列表
     * @return 总金额
     */
    private BigDecimal calculateTotalAmount(List<OrderLine> orderLines) {
        return orderLines.stream()
                .map(OrderLine::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 构建定价引擎请求参数。
     *
     * @param platformUserId 平台用户 ID
     * @param dto            创建订单请求
     * @param orderLines     订单行列表
     * @param orderNo        订单号
     * @return 定价请求 DTO
     */
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

    /**
     * 将订单行转换为定价项 DTO。
     *
     * @param orderLine 订单行
     * @return 定价项 DTO
     */
    private OrderPricingItemDTO toPricingItem(OrderLine orderLine) {
        OrderPricingItemDTO item = new OrderPricingItemDTO();
        item.setProductId(orderLine.product().getId());
        item.setCategory(orderLine.product().getCategory());
        item.setUnitPrice(orderLine.product().getPrice());
        item.setQuantity(orderLine.quantity());
        return item;
    }

    /**
     * 持久化优惠快照列表，记录订单享受的折扣来源与规则。
     *
     * @param salesOrder 订单实体
     * @param snapshots  优惠快照列表
     */
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

    /**
     * 若订单使用了优惠券，则锁定该优惠券。
     *
     * @param dto            创建订单请求
     * @param salesOrder     订单实体
     * @param pricingResult  定价结果
     */
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

    /**
     * 若订单使用了积分抵扣，则预占积分。
     *
     * @param salesOrder    订单实体
     * @param pricingResult 定价结果
     */
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
     * 插入订单商品明细。
     * <p>
     * 冗余商品类型与初始交付状态，避免后续商品改类型影响历史订单的交付路由。
     *
     * @param salesOrder 订单实体
     * @param orderLines 订单行列表
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
            // 冗余商品类型与初始交付状态，避免后续商品改类型影响历史订单的交付路由
            String productType = orderLine.product().getProductType();
            orderItem.setProductType(productType == null || productType.isBlank()
                    ? com.payment.enums.ProductTypeEnum.PHYSICAL.name()
                    : productType);
            orderItem.setDeliveryStatus(com.payment.enums.DeliveryStatusEnum.PENDING.name());
            return orderItem;
        }).collect(java.util.stream.Collectors.toList());
        if (!items.isEmpty()) {
            salesOrderItemMapper.insertBatch(items);
        }
    }

    /**
     * 确保用户已成为该商户的会员，不存在则自动注册。
     *
     * @param tenantId       商户 ID
     * @param platformUserId 平台用户 ID
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
     * 解析订单标题，未指定时自动根据首个商品名称生成。
     *
     * @param subject    用户指定的标题，可为 null
     * @param orderLines 订单行列表
     * @return 订单标题
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
     * 解析门店 ID，取订单行中第一个非空的 storeId。
     *
     * @param orderLines 订单行列表
     * @return 门店 ID，无门店时返回 null
     */
    private Long resolveStoreId(List<OrderLine> orderLines) {
        return orderLines.stream()
                .map(orderLine -> orderLine.product().getStoreId())
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据钱包支付策略计算钱包与外部支付的金额拆分。
     * <p>
     * 支持 6 种策略：NO_WALLET、UNIFIED_ONLY、MERCHANT_ONLY、MERCHANT_THEN_UNIFIED、
     * UNIFIED_THEN_MERCHANT、CUSTOM_SPLIT。
     *
     * @param platformUserId 平台用户 ID
     * @param dto            创建订单请求（含钱包策略）
     * @param totalAmount    应付总金额
     * @return 钱包拆分结果（统一钱包 + 商户钱包 + 外部支付）
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
     * 单钱包策略：仅从一个钱包扣减，余额不足时可选降级为外部支付。
     *
     * @param totalAmount  应付总金额
     * @param balance      可用钱包余额
     * @param allowFallback 余额不足时是否允许降级到外部支付
     * @param unifiedFirst true 表示从统一钱包扣减，false 表示从商户钱包扣减
     * @return 钱包拆分结果
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
         * 处理钱包拆分（单钱包场景返回结果）。
         */
        return unifiedFirst
                ? new WalletSplit(used, BigDecimal.ZERO, external)
                : new WalletSplit(BigDecimal.ZERO, used, external);
    /**
     * 处理calculateChained钱包。
     */
    }

    /**
     * 链式钱包策略：先扣第一个钱包，余额不足时再扣第二个钱包。
     *
     * @param totalAmount   应付总金额
     * @param firstBalance  第一个钱包余额
     * @param secondBalance 第二个钱包余额
     * @param allowFallback 余额不足时是否允许降级到外部支付
     * @param unifiedFirst  true 表示统一钱包优先，false 表示商户钱包优先
     * @return 钱包拆分结果
     */
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

    /**
     * 自定义拆分策略：由用户指定统一钱包和商户钱包的各自扣减金额。
     *
     * @param totalAmount    应付总金额
     * @param dto            创建订单请求（含自定义金额）
     * @param unifiedBalance 统一钱包可用余额
     * @param merchantBalance 商户钱包可用余额
     * @param allowFallback  余额不足时是否允许降级到外部支付
     * @return 钱包拆分结果
     */
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

    /** 返回非空金额，null 时返回 0。 */
    private BigDecimal defaultAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    /**
     * 校验外部支付渠道不为空。
     *
     * @param channelCode 支付渠道编码
     * @return 校验通过的渠道编码
     * @throws BusinessException 渠道为空时抛出
     */
    private PaymentChannelCodeEnum resolveExternalChannel(PaymentChannelCodeEnum channelCode) {
        if (channelCode == null) {
            throw new BusinessException("存在外部支付金额时必须指定 paymentChannelCode");
        }
        return channelCode;
    }

    /**
     * 支付成功后执行结算流程。
     * <p>
     * 包括：确认优惠券核销与积分消耗、商户入账、按积分规则赠送积分、触发订单交付。
     * 纯钱包支付没有外部回调，直接在本地事务里完成结算。
     *
     * @param salesOrder 已支付的订单
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

        // 钱包支付链路与外部支付回调链路都汇入此处,统一在事务内入队交付事件。
        // 写 Outbox 与订单状态变更必须原子化:Outbox 写失败应回滚订单状态,由调用方重试。
        orderDeliveryService.enqueueDelivery(salesOrder.getOrderNo());
    }

    /**
     * 构建订单支付信息 VO。
     *
     * @param salesOrder 订单实体
     * @return 订单支付信息 VO
     */
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

    /**
     * 确认订单关联的优惠资产（优惠券核销、积分消耗确认）。
     *
     * @param salesOrder 已支付的订单
     */
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

    /**
     * 释放订单关联的优惠资产（优惠券回退、积分预占释放）。
     *
     * @param salesOrder    待取消的订单
     * @param releaseReason 释放原因
     */
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

    /**
     * 根据订单号查询优惠快照列表。
     *
     * @param orderNo 订单号
     * @return 优惠快照列表
     */
    private List<OrderDiscountSnapshot> listDiscountSnapshots(String orderNo) {
        return orderDiscountSnapshotMapper.selectList(new LambdaQueryWrapper<OrderDiscountSnapshot>()
                .eq(OrderDiscountSnapshot::getOrderNo, orderNo));
    }

    /**
     * 查找可复用的未过期支付账单，优先用于重新支付。
     *
     * @param paymentBills 该订单关联的支付账单列表
     * @param salesOrder   订单实体
     * @return 可复用的支付账单，无可用时返回 null
     */
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
        if (paymentBill != null) {
            detailVO.setPaymentBillNo(paymentBill.getBillNo());
            detailVO.setPaymentBillStatus(paymentBill.getPayStatus());
            detailVO.setPaymentBillStatusRemark(paymentBill.getStatusRemark());
            detailVO.setPaymentBillExpireTime(paymentBill.getExpireTime());
        }
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

    /**
     * 处理外部支付回调。
     * <p>
     * 同步支付账单状态，若支付成功则更新订单状态为已支付并执行结算。
     * 幂等处理：已支付订单直接返回。
     *
     * @param paymentBillNo 支付账单号
     * @throws BusinessException 账单或订单不存在、支付未完成时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handlePaymentCallback(String paymentBillNo) {
        PaymentBill bill = paymentBillV1Service.syncBillStatus(paymentBillNo);
        if (bill == null) {
            throw new BusinessException("支付账单不存在: " + paymentBillNo);
        }

        SalesOrder order = salesOrderMapper.selectOne(new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getOrderNo, bill.getBizNo())
                .eq(SalesOrder::getDeleted, 0));
        if (order == null) {
            throw new BusinessException("订单不存在: " + bill.getBizNo());
        }

        if (PayStatusEnum.SUCCESS.name().equals(order.getPayStatus())) {
            return;
        }

        if (PayStatusEnum.SUCCESS.name().equals(bill.getPayStatus())) {
            order.setPayStatus(PayStatusEnum.SUCCESS.name());
            order.setOrderStatus(OrderStatusEnum.PAID.name());
            salesOrderMapper.updateById(order);
            settlePaidOrder(order);
            return;
        }

        throw new BusinessException("支付未完成，继续等待: billNo="
                + paymentBillNo + ", billStatus=" + bill.getPayStatus());
    }

}
