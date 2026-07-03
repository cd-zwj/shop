package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.dto.ProductSalesRankDTO;
import com.payment.dto.SalesTrendDTO;
import com.payment.entity.PaymentOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付订单 Mapper
 * <p>对应表：payment_order，存储 C 端用户提交的支付订单主记录</p>
 *
 * @author payment-system
 */
@Mapper
public interface PaymentOrderMapper extends BaseMapper<PaymentOrder> {

    /**
     * 统计指定时间范围内的销售额
     *
     * @param tenantId  租户 ID
     * @param startTime 开始时间（含）
     * @param endTime   结束时间（不含）
     * @return 已支付订单的销售总额，无数据时返回 0
     */
    @Select("SELECT COALESCE(SUM(pay_amount), 0) FROM payment_order " +
            "WHERE tenant_id = #{tenantId} AND order_status = 'PAID' " +
            "AND pay_time >= #{startTime} AND pay_time < #{endTime} AND deleted = 0")
    BigDecimal sumSalesAmount(@Param("tenantId") Long tenantId, 
                              @Param("startTime") LocalDateTime startTime, 
                              @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计指定时间范围内的订单数量
     *
     * @param tenantId  租户 ID
     * @param startTime 开始时间（含）
     * @param endTime   结束时间（不含）
     * @return 已支付订单的数量
     */
    @Select("SELECT COUNT(*) FROM payment_order " +
            "WHERE tenant_id = #{tenantId} AND order_status = 'PAID' " +
            "AND pay_time >= #{startTime} AND pay_time < #{endTime} AND deleted = 0")
    Integer countOrders(@Param("tenantId") Long tenantId, 
                       @Param("startTime") LocalDateTime startTime, 
                       @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查询销售趋势数据（按日期分组）
     *
     * @param tenantId  租户 ID
     * @param startTime 开始时间（含）
     * @param endTime   结束时间（不含）
     * @return 按日期升序排列的销售趋势列表，每天包含销售金额与订单数
     */
    @Select("SELECT DATE_FORMAT(pay_time, '%Y-%m-%d') as date, " +
            "COALESCE(SUM(pay_amount), 0) as salesAmount, " +
            "COUNT(*) as orderCount " +
            "FROM payment_order " +
            "WHERE tenant_id = #{tenantId} AND order_status = 'PAID' " +
            "AND pay_time >= #{startTime} AND pay_time < #{endTime} AND deleted = 0 " +
            "GROUP BY DATE_FORMAT(pay_time, '%Y-%m-%d') " +
            "ORDER BY date ASC")
    List<SalesTrendDTO> selectSalesTrend(@Param("tenantId") Long tenantId, 
                                         @Param("startTime") LocalDateTime startTime, 
                                         @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查询商品销售排行
     *
     * @param tenantId  租户 ID
     * @param startTime 开始时间（含）
     * @param endTime   结束时间（不含）
     * @param limit     返回的排行数量上限
     * @return 按销售数量降序排列的商品销售排行列表
     */
    @Select("SELECT oi.product_id as productId, " +
            "oi.product_code as productCode, " +
            "oi.product_name as productName, " +
            "oi.product_image as productImage, " +
            "SUM(oi.quantity) as salesQuantity, " +
            "SUM(oi.subtotal) as salesAmount " +
            "FROM order_item oi " +
            "INNER JOIN payment_order po ON oi.order_no = po.order_no " +
            "WHERE oi.tenant_id = #{tenantId} AND po.order_status = 'PAID' " +
            "AND po.pay_time >= #{startTime} AND po.pay_time < #{endTime} " +
            "AND po.deleted = 0 " +
            "GROUP BY oi.product_id, oi.product_code, oi.product_name, oi.product_image " +
            "ORDER BY salesQuantity DESC " +
            "LIMIT #{limit}")
    List<ProductSalesRankDTO> selectProductSalesRank(@Param("tenantId") Long tenantId, 
                                                     @Param("startTime") LocalDateTime startTime, 
                                                     @Param("endTime") LocalDateTime endTime, 
                                                     @Param("limit") Integer limit);
}

