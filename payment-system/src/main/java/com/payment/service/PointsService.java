package com.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.dto.ExchangeProductDTO;
import com.payment.dto.PointsRuleDTO;
import com.payment.entity.ExchangeProduct;
import com.payment.entity.PointsLog;
import com.payment.entity.PointsRule;

import java.math.BigDecimal;
import java.util.List;

/**
 * 积分服务接口
 */
public interface PointsService {
    
    /**
     * 获取商家积分规则
     * @param tenantId 租户ID
     * @return 积分规则
     */
    PointsRule getPointsRule(Long tenantId);
    
    /**
     * 设置商家积分规则
     * @param dto 积分规则DTO
     */
    void setPointsRule(PointsRuleDTO dto);
    
    /**
     * 计算订单积分
     * @param amount 订单金额
     * @param tenantId 租户ID
     * @return 积分数量
     */
    Integer calculatePoints(BigDecimal amount, Long tenantId);
    
    /**
     * 发放积分
     * @param userId 用户ID
     * @param points 积分数量
     * @param reason 原因
     * @param orderNo 订单号
     */
    void grantPoints(Long userId, Integer points, String reason, String orderNo);
    
    /**
     * 扣减积分
     * @param userId 用户ID
     * @param points 积分数量
     * @param reason 原因
     */
    void deductPoints(Long userId, Integer points, String reason);
    
    /**
     * 查询用户积分余额
     * @param userId 用户ID
     * @param tenantId 租户ID
     * @return 积分余额
     */
    Integer getUserPoints(Long userId, Long tenantId);
    
    /**
     * 积分明细
     * @param userId 用户ID
     * @param tenantId 租户ID
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 积分明细分页
     */
    Page<PointsLog> listPointsLogs(Long userId, Long tenantId, Integer pageNum, Integer pageSize);
    
    /**
     * 积分兑换商品
     * @param userId 用户ID
     * @param exchangeProductId 兑换商品ID
     * @return 兑换订单号
     */
    String exchangeProduct(Long userId, Long exchangeProductId);
    
    /**
     * 设置积分兑换商品
     * @param dto 兑换商品DTO
     */
    void setExchangeProduct(ExchangeProductDTO dto);
    
    /**
     * 积分兑换商品列表
     * @param tenantId 租户ID
     * @return 兑换商品列表
     */
    List<ExchangeProduct> listExchangeProducts(Long tenantId);
    
    /**
     * 更新积分兑换商品
     * @param id 兑换商品ID
     * @param dto 兑换商品DTO
     */
    void updateExchangeProduct(Long id, ExchangeProductDTO dto);
    
    /**
     * 删除积分兑换商品
     * @param id 兑换商品ID
     */
    void deleteExchangeProduct(Long id);

    /**
     * 退款回退积分（积分兑换商品退款时调用）
     * @param userId 用户ID
     * @param tenantId 租户ID
     * @param points 回退积分数量
     * @param orderNo 关联订单号
     * @param reason 原因
     */
    void refundPoints(Long userId, Long tenantId, Integer points, String orderNo, String reason);
}
