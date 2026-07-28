package com.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.dto.AppCreateOrderDTO;
import com.payment.dto.OrderPaymentVO;
import com.payment.dto.SalesOrderDetailVO;
import com.payment.entity.SalesOrder;
import com.payment.entity.SalesOrderItem;
import com.payment.enums.PaymentChannelCodeEnum;
import com.payment.vo.SalesOrderListVO;

import java.util.List;

/**
 * C 端用户订单服务接口。
 *
 * <p>负责用户下单、支付、查询、取消等订单全生命周期管理，
 * 承接 {@code V1AppOrderController} 的业务逻辑。</p>
 */
public interface AppOrderService {

    /**
     * 创建订单并返回支付信息。
     *
     * <p>根据商品和购物车信息生成销售订单，校验库存并锁定，
     * 同时创建支付账单返回待支付信息。</p>
     *
     * @param platformUserId 平台用户ID
     * @param dto            创建订单请求 DTO（含商品明细、地址、优惠券等）
     * @return 订单支付信息 VO（含支付单号、支付链接等）
     * @throws com.payment.common.exception.BusinessException 库存不足或商品下架时抛出
     */
    OrderPaymentVO createOrder(Long platformUserId, AppCreateOrderDTO dto);

    /**
     * 分页查询当前用户的订单列表。
     *
     * @param platformUserId 平台用户ID
     * @param current        当前页码
     * @param size           每页条数
     * @return 订单分页结果
     */
    Page<SalesOrder> listOrders(Long platformUserId, Integer current, Integer size);

    /**
     * 根据订单号查询当前用户的订单。
     *
     * @param platformUserId 平台用户ID
     * @param orderNo        订单编号
     * @return 订单实体，不存在时返回 {@code null}
     */
    SalesOrder getByOrderNo(Long platformUserId, String orderNo);

    /**
     * 获取订单详情（含订单项、支付信息等）。
     *
     * @param platformUserId 平台用户ID
     * @param orderNo        订单编号
     * @return 订单详情 VO
     * @throws com.payment.common.exception.BusinessException 订单不存在时抛出
     */
    SalesOrderDetailVO getOrderDetail(Long platformUserId, String orderNo);

    /**
     * 获取商户视角的订单详情（用于商户端查看订单）。
     *
     * @param tenantId       租户ID
     * @param platformUserId 平台用户ID
     * @param orderNo        订单编号
     * @return 订单详情 VO
     * @throws com.payment.common.exception.BusinessException 订单不存在或不属于该租户时抛出
     */
    SalesOrderDetailVO getMerchantOrderDetail(Long tenantId, Long platformUserId, String orderNo);

    /**
     * 分页查询商户下的订单列表（支持按状态和关键字筛选）。
     *
     * @param tenantId   租户ID
     * @param current    当前页码
     * @param size       每页条数
     * @param orderStatus 订单状态筛选（可为null）
     * @param payStatus  支付状态筛选（可为null）
     * @param keyword    关键字搜索（可为null，匹配订单号等）
     * @return 订单分页结果
     */
    Page<SalesOrder> listMerchantOrders(Long tenantId, Integer current, Integer size, String orderStatus, String payStatus, String keyword);

    /**
     * 分页查询商户订单列表，支持按履约状态筛选。
     *
     * @param tenantId          租户ID
     * @param current           当前页码
     * @param size              每页条数
     * @param orderStatus       订单状态筛选（可为null）
     * @param payStatus         支付状态筛选（可为null）
     * @param keyword           关键字搜索（可为null，匹配订单号等）
     * @param fulfillmentStatus 履约状态分组：PENDING / COMPLETED / ABNORMAL（可为null）
     * @param deliveryStatus    单个交付状态筛选（可为null）
     * @return 订单分页结果
     */
    Page<SalesOrder> listMerchantOrders(Long tenantId,
                                        Integer current,
                                        Integer size,
                                        String orderStatus,
                                        String payStatus,
                                        String keyword,
                                        String fulfillmentStatus,
                                        String deliveryStatus);

    /**
     * 分页查询商户订单列表视图，附带订单项交付汇总和展示文案。
     *
     * @param tenantId          租户ID
     * @param current           当前页码
     * @param size              每页条数
     * @param orderStatus       订单状态筛选（可为null）
     * @param payStatus         支付状态筛选（可为null）
     * @param keyword           关键字搜索（可为null，匹配订单号等）
     * @param fulfillmentStatus 履约状态分组：PENDING / COMPLETED / ABNORMAL（可为null）
     * @param deliveryStatus    单个交付状态筛选（可为null）
     * @return 订单列表视图分页结果
     */
    Page<SalesOrderListVO> listMerchantOrderViews(Long tenantId,
                                                  Long platformUserId,
                                                  Integer current,
                                                  Integer size,
                                                  Long storeId,
                                                  String orderStatus,
                                                  String payStatus,
                                                  String keyword,
                                                  String fulfillmentStatus,
                                                  String deliveryStatus);

    /**
     * 查询指定订单的订单项列表。
     *
     * @param platformUserId 平台用户ID
     * @param orderNo        订单编号
     * @return 订单项列表
     */
    List<SalesOrderItem> listOrderItems(Long platformUserId, String orderNo);

    /**
     * 订单重新支付（用户选择新的支付渠道发起支付）。
     *
     * @param platformUserId    平台用户ID
     * @param orderNo           订单编号
     * @param paymentChannelCode 支付渠道编码
     * @return 订单支付信息 VO
     * @throws com.payment.common.exception.BusinessException 订单状态不允许重新支付时抛出
     */
    OrderPaymentVO repayOrder(Long platformUserId, String orderNo, PaymentChannelCodeEnum paymentChannelCode);

    /**
     * 取消订单（释放库存锁定，更新订单状态为已取消）。
     *
     * @param platformUserId 平台用户ID
     * @param orderNo        订单编号
     * @throws com.payment.common.exception.BusinessException 订单状态不允许取消时抛出
     */
    void cancelOrder(Long platformUserId, String orderNo);

    /** 批量关闭已过期且尚未支付的订单，并释放其锁定库存和支付资产。 */
    int expireUnpaidOrders();

    /**
     * 处理支付回调：根据 PaymentBill 的状态更新关联 SalesOrder。
     *
     * @param paymentBillNo 支付单号
     */
    void handlePaymentCallback(String paymentBillNo);
}
