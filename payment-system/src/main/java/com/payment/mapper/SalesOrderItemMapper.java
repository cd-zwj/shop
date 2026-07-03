package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.SalesOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
     * 根据订单 ID 查询订单明细列表，按主键升序排列。
     *
     * @param orderId 订单 ID
     * @return 订单明细列表
     */
    @Select("SELECT * FROM sales_order_item WHERE order_id = #{orderId} ORDER BY id ASC")
    List<SalesOrderItem> selectByOrderId(Long orderId);

    /** 批量插入订单项（单条 SQL 多行 VALUES） */
    int insertBatch(@Param("list") List<SalesOrderItem> items);
}
