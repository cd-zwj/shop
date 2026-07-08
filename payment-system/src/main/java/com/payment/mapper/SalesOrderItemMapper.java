package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.dto.ProductSalesRankDTO;
import com.payment.entity.SalesOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 销售订单明细数据访问接口，提供订单商品明细表（sales_order_item）的 CRUD 与自定义查询。
 */
@Mapper
public interface SalesOrderItemMapper extends BaseMapper<SalesOrderItem> {

    /**
     * 根据订单编号查询订单明细列表，按主键升序排列。
     *
     * @param orderNo 订单编号
     * @return 订单明细列表
     */
    @Select("SELECT * FROM sales_order_item WHERE order_no = #{orderNo} ORDER BY id ASC")
    List<SalesOrderItem> selectByOrderNo(String orderNo);

    /**
     * 根据订单编号列表查询订单明细列表。
     *
     * @param orderNos 订单编号列表
     * @return 订单明细列表
     */
    List<SalesOrderItem> selectByOrderNos(@Param("orderNos") List<String> orderNos);

    /**
     * 根据订单 ID 查询订单明细列表，按主键升序排列。
     *
     * @param orderId 订单 ID
     * @return 订单明细列表
     */
    @Select("SELECT * FROM sales_order_item WHERE order_id = #{orderId} ORDER BY id ASC")
    List<SalesOrderItem> selectByOrderId(Long orderId);

    /** 批量插入订单项（单条 SQL 多行 VALUES） */
    int insertBatch(@Param("list") List<SalesOrderItem> items);

    /**
     * 基于 V1 销售订单查询商品销量排行。
     *
     * @param tenantId  租户 ID
     * @param startTime 开始时间（含）
     * @param endTime   结束时间（不含）
     * @param limit     返回数量上限
     * @return 按销售数量和销售额降序排列的商品排行
     */
    @Select("""
            SELECT soi.product_id AS productId,
                   CAST(soi.product_id AS CHAR) AS productCode,
                   soi.product_name AS productName,
                   NULL AS productImage,
                   COALESCE(SUM(soi.quantity), 0) AS salesQuantity,
                   COALESCE(SUM(soi.subtotal), 0) AS salesAmount
            FROM sales_order_item soi
            INNER JOIN sales_order so ON soi.order_no = so.order_no
            WHERE soi.tenant_id = #{tenantId}
              AND so.tenant_id = #{tenantId}
              AND so.deleted = 0
              AND so.pay_status IN ('SUCCESS', 'PAID')
              AND so.create_time >= #{startTime}
              AND so.create_time < #{endTime}
            GROUP BY soi.product_id, soi.product_name
            ORDER BY salesQuantity DESC, salesAmount DESC
            LIMIT #{limit}
            """)
    List<ProductSalesRankDTO> selectV1ProductSalesRank(@Param("tenantId") Long tenantId,
                                                       @Param("startTime") LocalDateTime startTime,
                                                       @Param("endTime") LocalDateTime endTime,
                                                       @Param("limit") Integer limit);
}
