package com.payment.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.common.BusinessException;
import com.payment.dto.ScanRequestDTO;
import com.payment.dto.ScanResponseDTO;
import com.payment.entity.PaymentOrder;
import com.payment.entity.Product;
import com.payment.mapper.PosSessionMapper;
import com.payment.mapper.ProductMapper;
import com.payment.service.ScanService;
import com.payment.util.RedisUtils;
import com.payment.util.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 扫码收银服务实现
 */
@Slf4j
@Service
public class ScanServiceImpl implements ScanService {

    private static final String PRODUCT_CACHE_PREFIX = "product:";
    private static final String CART_PREFIX = "cart:";
    private static final int CART_EXPIRE_MINUTES = 30;
    private static final long PRODUCT_CACHE_JITTER_SECONDS = 300L;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private PosSessionMapper posSessionMapper;

    @Autowired
    private com.payment.mapper.PaymentOrderMapper paymentOrderMapper;

    @Override
    public ScanResponseDTO handleScan(ScanRequestDTO request) {
        ScanResponseDTO response = new ScanResponseDTO();

        try {
            Long tenantId = TenantContextHolder.getTenantId();
            if (tenantId == null) {
                response.setStatus("ERROR");
                response.setMessage("租户信息不存在");
                return response;
            }

            Product product = findProductByCode(request.getProductCode(), tenantId);
            if (product == null) {
                response.setStatus("NOT_FOUND");
                response.setMessage("商品不存在");
                response.setProductCode(request.getProductCode());
                return response;
            }

            if (product.getStatus() == null || product.getStatus() != 1) {
                response.setStatus("UNAVAILABLE");
                response.setMessage("商品已下架");
                response.setProductCode(request.getProductCode());
                response.setProductId(product.getId());
                response.setProductName(product.getName());
                return response;
            }

            Integer quantity = request.getQuantity() != null ? request.getQuantity() : 1;
            String sessionId = generateSessionId(request.getDeviceId(), tenantId);
            addToCart(sessionId, product, quantity);

            List<Map<String, Object>> cartItems = getCart(sessionId);
            BigDecimal cartTotal = calculateCartTotal(cartItems);

            response.setStatus("SUCCESS");
            response.setMessage("商品已添加到购物车");
            response.setProductCode(product.getProductCode());
            response.setProductId(product.getId());
            response.setProductName(product.getName());
            response.setProductImage(product.getImageUrl());
            response.setPrice(product.getPrice());
            response.setStock(null);

            Map<String, Object> cartData = new HashMap<>();
            cartData.put("cartTotal", cartItems.size());
            cartData.put("cartAmount", cartTotal);
            response.setCartData(cartData);
        } catch (Exception e) {
            log.error("处理扫码请求失败", e);
            response.setStatus("ERROR");
            response.setMessage("处理失败：" + e.getMessage());
        }

        return response;
    }

    @Override
    public Product findProductByCode(String productCode, Long tenantId) {
        String cacheKey = PRODUCT_CACHE_PREFIX + tenantId + ":" + productCode;
        return redisUtils.queryWithMutex(
                cacheKey,
                Product.class,
                Duration.ofMinutes(30),
                Duration.ofMinutes(2),
                PRODUCT_CACHE_JITTER_SECONDS,
                () -> {
                    LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
                    queryWrapper.eq(Product::getProductCode, productCode)
                            .eq(Product::getTenantId, tenantId)
                            .eq(Product::getDeleted, 0);
                    return productMapper.selectOne(queryWrapper);
                }
        );
    }

    @Override
    public void addToCart(String sessionId, Product product, Integer quantity) {
        String cartKey = CART_PREFIX + sessionId;
        Map<String, String> cartMap = redisUtils.hashEntries(cartKey);

        String productKey = "product_" + product.getId();
        Map<String, Object> cartItem;
        if (cartMap.containsKey(productKey)) {
            cartItem = JSON.parseObject(cartMap.get(productKey), Map.class);
            Integer currentQty = ((Number) cartItem.get("quantity")).intValue();
            cartItem.put("quantity", currentQty + quantity);
        } else {
            cartItem = new HashMap<>();
            cartItem.put("productId", product.getId());
            cartItem.put("productCode", product.getProductCode());
            cartItem.put("productName", product.getName());
            cartItem.put("price", product.getPrice());
            cartItem.put("quantity", quantity);
            cartItem.put("imageUrl", product.getImageUrl());
        }

        redisUtils.hashPut(cartKey, productKey, JSON.toJSONString(cartItem));
        redisUtils.expire(cartKey, CART_EXPIRE_MINUTES, TimeUnit.MINUTES);
        log.info("商品已添加到购物车，sessionId: {}, productId: {}, quantity: {}", sessionId, product.getId(), quantity);
    }

    @Override
    public void addToCart(String sessionId, Long productId, Integer quantity, Long tenantId) {
        Product product = productMapper.selectById(productId);
        if (product == null || product.getDeleted() == 1) {
            throw new BusinessException("商品不存在");
        }
        if (!product.getTenantId().equals(tenantId)) {
            throw new BusinessException("商品不属于当前商家");
        }
        if (product.getStatus() == null || product.getStatus() != 1) {
            throw new BusinessException("商品已下架");
        }
        if (quantity <= 0) {
            throw new BusinessException("商品数量必须大于0");
        }
        addToCart(sessionId, product, quantity);
    }

    @Override
    public void removeFromCart(String sessionId, Long productId) {
        redisUtils.hashRemove(CART_PREFIX + sessionId, "product_" + productId);
        log.info("商品已从购物车移除，sessionId: {}, productId: {}", sessionId, productId);
    }

    @Override
    public void updateCartQuantity(String sessionId, Long productId, Integer quantity) {
        if (quantity <= 0) {
            throw new BusinessException("商品数量必须大于0");
        }

        String cartKey = CART_PREFIX + sessionId;
        String productKey = "product_" + productId;
        String itemJson = redisUtils.hashGet(cartKey, productKey);
        if (itemJson == null) {
            throw new BusinessException("购物车中不存在该商品");
        }

        Map<String, Object> cartItem = JSON.parseObject(itemJson, Map.class);
        cartItem.put("quantity", quantity);
        redisUtils.hashPut(cartKey, productKey, JSON.toJSONString(cartItem));
        redisUtils.expire(cartKey, CART_EXPIRE_MINUTES, TimeUnit.MINUTES);

        log.info("购物车商品数量已更新，sessionId: {}, productId: {}, quantity: {}", sessionId, productId, quantity);
    }

    @Override
    public List<Map<String, Object>> getCart(String sessionId) {
        Map<String, String> cartMap = redisUtils.hashEntries(CART_PREFIX + sessionId);
        List<Map<String, Object>> cartItems = new ArrayList<>();
        for (String value : cartMap.values()) {
            cartItems.add(JSON.parseObject(value, Map.class));
        }
        return cartItems;
    }

    @Override
    public void clearCart(String sessionId) {
        redisUtils.delete(CART_PREFIX + sessionId);
        log.info("购物车已清空，sessionId: {}", sessionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentOrder createPosOrder(String sessionId, Long tenantId) {
        List<Map<String, Object>> cartItems = getCart(sessionId);
        if (cartItems.isEmpty()) {
            throw new BusinessException("购物车为空，无法创建订单");
        }

        BigDecimal totalAmount = calculateCartTotal(cartItems);
        PaymentOrder order = new PaymentOrder();
        order.setTenantId(tenantId);
        order.setOrderNo(generateOrderNo());
        order.setAmount(totalAmount);
        order.setPayAmount(BigDecimal.ZERO);
        order.setOrderStatus("PENDING");
        order.setPayStatus("PENDING");
        order.setSubject("POS收银订单");
        order.setBody("POS收银订单 - " + cartItems.size() + "件商品");
        order.setCreateTime(LocalDateTime.now());
        order.setExpireTime(LocalDateTime.now().plusMinutes(30));
        paymentOrderMapper.insert(order);

        clearCart(sessionId);
        log.info("POS订单创建成功，订单号: {}, 金额: {}", order.getOrderNo(), totalAmount);
        return order;
    }

    private String generateSessionId(String deviceId, Long tenantId) {
        return tenantId + "_" + deviceId;
    }

    private String generateOrderNo() {
        return "POS" + System.currentTimeMillis() + (int) (Math.random() * 1000);
    }

    private BigDecimal calculateCartTotal(List<Map<String, Object>> cartItems) {
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> item : cartItems) {
            BigDecimal price = new BigDecimal(item.get("price").toString());
            Integer quantity = ((Number) item.get("quantity")).intValue();
            total = total.add(price.multiply(new BigDecimal(quantity)));
        }
        return total;
    }
}
