package com.payment.service;

import com.payment.dto.ScanRequestDTO;
import com.payment.dto.ScanResponseDTO;
import com.payment.entity.PaymentOrder;
import com.payment.entity.Product;

import java.util.List;
import java.util.Map;

/**
 * 扫码收银服务接口。
 *
 * @deprecated POS 收银模块已下线，代码保留用于参考。
 *             关联文件：ScanServiceImpl、ScanConsumer、NettyServer、PosController。
 */
@Deprecated
public interface ScanService {

    /**
     * 处理扫码请求。
     *
     * <p>解析扫码内容（条形码/二维码），匹配商品并返回收银信息。</p>
     *
     * @param request 扫码请求 DTO
     * @return 扫码响应 DTO（含商品信息或错误提示）
     */
    ScanResponseDTO handleScan(ScanRequestDTO request);

    /**
     * 查询商品信息（优先从 Redis 缓存查询，缓存未命中时查询 MySQL）。
     *
     * @param productCode 商品编码
     * @param tenantId    租户ID
     * @return 商品信息，不存在时返回 {@code null}
     */
    Product findProductByCode(String productCode, Long tenantId);

    /**
     * 添加商品到购物车（通过商品对象）。
     *
     * @param sessionId 会话ID
     * @param product   商品信息
     * @param quantity  数量
     */
    void addToCart(String sessionId, Product product, Integer quantity);

    /**
     * 添加商品到购物车（通过商品ID）。
     *
     * @param sessionId 会话ID
     * @param productId 商品ID
     * @param quantity  数量
     * @param tenantId  租户ID
     */
    void addToCart(String sessionId, Long productId, Integer quantity, Long tenantId);

    /**
     * 从购物车移除商品。
     *
     * @param sessionId 会话ID
     * @param productId 商品ID
     */
    void removeFromCart(String sessionId, Long productId);

    /**
     * 更新购物车中商品的数量。
     *
     * @param sessionId 会话ID
     * @param productId 商品ID
     * @param quantity  新数量
     */
    void updateCartQuantity(String sessionId, Long productId, Integer quantity);

    /**
     * 获取购物车内容。
     *
     * @param sessionId 会话ID
     * @return 购物车商品列表（每项含商品信息和数量）
     */
    List<Map<String, Object>> getCart(String sessionId);

    /**
     * 清空购物车。
     *
     * @param sessionId 会话ID
     */
    void clearCart(String sessionId);

    /**
     * 创建 POS 收银订单。
     *
     * @param sessionId 会话ID（关联购物车）
     * @param tenantId  租户ID
     * @return 创建的支付订单
     */
    PaymentOrder createPosOrder(String sessionId, Long tenantId);
}
