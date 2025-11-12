package com.payment.service;

import com.payment.dto.ScanRequestDTO;
import com.payment.dto.ScanResponseDTO;
import com.payment.entity.PaymentOrder;
import com.payment.entity.Product;

import java.util.List;
import java.util.Map;

/**
 * 扫码收银服务接口
 */
public interface ScanService {
    
    /**
     * 处理扫码请求
     * @param request 扫码请求
     * @return 扫码响应
     */
    ScanResponseDTO handleScan(ScanRequestDTO request);
    
    /**
     * 查询商品（先Redis后MySQL）
     * @param productCode 商品编码
     * @param tenantId 租户ID
     * @return 商品信息
     */
    Product findProductByCode(String productCode, Long tenantId);
    
    /**
     * 添加到购物车
     * @param sessionId 会话ID
     * @param product 商品
     * @param quantity 数量
     */
    void addToCart(String sessionId, Product product, Integer quantity);
    
    /**
     * 添加到购物车（通过商品ID）
     * @param sessionId 会话ID
     * @param productId 商品ID
     * @param quantity 数量
     * @param tenantId 租户ID
     */
    void addToCart(String sessionId, Long productId, Integer quantity, Long tenantId);
    
    /**
     * 从购物车移除商品
     * @param sessionId 会话ID
     * @param productId 商品ID
     */
    void removeFromCart(String sessionId, Long productId);
    
    /**
     * 更新购物车商品数量
     * @param sessionId 会话ID
     * @param productId 商品ID
     * @param quantity 新数量
     */
    void updateCartQuantity(String sessionId, Long productId, Integer quantity);
    
    /**
     * 获取购物车
     * @param sessionId 会话ID
     * @return 购物车商品列表
     */
    List<Map<String, Object>> getCart(String sessionId);
    
    /**
     * 清空购物车
     * @param sessionId 会话ID
     */
    void clearCart(String sessionId);
    
    /**
     * 创建收银订单
     * @param sessionId 会话ID
     * @param tenantId 租户ID
     * @return 订单信息
     */
    PaymentOrder createPosOrder(String sessionId, Long tenantId);
}
