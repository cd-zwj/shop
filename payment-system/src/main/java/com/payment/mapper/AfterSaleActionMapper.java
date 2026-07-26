package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.AfterSaleAction;
import org.apache.ibatis.annotations.Mapper;

/** 售后处理流水数据访问。 */
@Mapper
public interface AfterSaleActionMapper extends BaseMapper<AfterSaleAction> {
}
