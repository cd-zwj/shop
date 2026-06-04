package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.Store;
import org.apache.ibatis.annotations.Mapper;

/**
 * 门店数据访问接口，用于执行门店区域、位置和服务标签查询。
 */
@Mapper
public interface StoreMapper extends BaseMapper<Store> {
}
