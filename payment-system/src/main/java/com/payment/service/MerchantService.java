package com.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.payment.dto.MerchantDTO;
import com.payment.dto.MerchantDetailVO;
import com.payment.dto.MerchantQueryDTO;
import com.payment.entity.Tenant;

/**
 * 商家管理服务接口
 */
public interface MerchantService extends IService<Tenant> {
    
    /**
     * 创建商家（租户）
     */
    Tenant createMerchant(MerchantDTO dto);
    
    /**
     * 更新商家信息
     */
    void updateMerchant(Long tenantId, MerchantDTO dto);
    
    /**
     * 启用商家
     */
    void enableMerchant(Long tenantId);
    
    /**
     * 禁用商家
     */
    void disableMerchant(Long tenantId);
    
    /**
     * 商家列表（分页）
     */
    Page<Tenant> listMerchants(MerchantQueryDTO query);
    
    /**
     * 商家详情
     */
    MerchantDetailVO getMerchantDetail(Long tenantId);
    
    /**
     * 获取平台数据概览
     */
    java.util.Map<String, Object> getDashboardStats();
    
    /**
     * 获取商家注册趋势
     */
    java.util.Map<String, Object> getMerchantTrend();
    
    /**
     * 获取平台销售趋势
     */
    java.util.Map<String, Object> getSalesTrend();
}
