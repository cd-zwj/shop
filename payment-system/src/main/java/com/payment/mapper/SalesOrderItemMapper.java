package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.SalesOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SalesOrderItemMapper extends BaseMapper<SalesOrderItem> {

    @Select("SELECT * FROM sales_order_item WHERE order_no = #{orderNo} ORDER BY id ASC")
    List<SalesOrderItem> selectByOrderNo(String orderNo);

    @Select("SELECT * FROM sales_order_item WHERE order_id = #{orderId} ORDER BY id ASC")
    List<SalesOrderItem> selectByOrderId(Long orderId);

    /** 批量插入订单项（单条 SQL 多行 VALUES） */
    int insertBatch(@Param("list") List<SalesOrderItem> items);
}
