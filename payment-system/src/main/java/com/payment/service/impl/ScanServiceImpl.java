package com.payment.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.common.BusinessException;
import com.payment.dto.ScanRequestDTO;
import com.payment.dto.ScanResponseDTO;
import com.payment.entity.PaymentOrder;
import com.payment.entity.PosSession;
import com.payment.entity.Product;
import com.payment.mapper.PosSessionMapper;
import com.payment.mapper.ProductMapper;
import com.payment.service.ScanService;
import com.payment.util.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 扫码收银服务实现
 */
@Slf4j
@Service
public class ScanServiceImpl implements ScanService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private ProductMapper productMapper;
    
    @Autowired
    private PosSessionMapper posSessionMapper;
    
    @Autowired
    private com.payment.mapper.PaymentOrderMapper paymentOrderMapper;
    
    private static final String PRODUCT_CACHE_PREFIX = "product:";
    private static final String CART_PREFIX = "cart:";
    private static final int CART_EXPIRE_MINUTES = 30;
    
    @Override
    public ScanResponseDTO handleScan(ScanRequestDTO request) {
        ScanResponseDTO response = new ScanResponseDTO();
        
        try {
            // 获取租户ID
            Long tenantId = TenantContextHolder.getTenantId();
            if (tenantId == null) {
                response.setStatus("ERROR");
                response.setMessage("租户信息不存在");
                return response;
            }
            
            // 查询商品（先Redis后MySQL）
            Product product = findProductByCode(request.getProductCode(), tenantId);
            
            if (product == null) {
                response.setStatus("NOT_FOUND");
                response.setMessage("商品不存在");
                response.setProductCode(request.getProductCode());
                return response;
            }
            
            // 检查商品状态
            if (product.getStatus() == null || product.getStatus() != 1) {
                response.setStatus("UNAVAILABLE");
                response.setMessage("商品已下架");
                response.setProductCode(request.getProductCode());
                response.setProductId(product.getId());
                response.setProductName(product.getName());
                return response;
            }
            
            // 添加到购物车
            Integer quantity = request.getQuantity() != null ? request.getQuantity() : 1;
            String sessionId = generateSessionId(request.getDeviceId(), tenantId);
            addToCart(sessionId, product, quantity);
            
            // 获取购物车信息
            List<Map<String, Object>> cartItems = getCart(sessionId);
            BigDecimal cartTotal = calculateCartTotal(cartItems);
            
            // 构建成功响应
            response.setStatus("SUCCESS");
            response.setMessage("商品已添加到购物车");
            response.setProductCode(product.getProductCode());
            response.setProductId(product.getId());
            response.setProductName(product.getName());
            response.setProductImage(product.getImageUrl());
            response.setPrice(product.getPrice());
            response.setStock(null); // 可以从库存表查询
            
            // 添加购物车信息
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
        // 先从Redis缓存查询
        String cacheKey = PRODUCT_CACHE_PREFIX + tenantId + ":" + productCode;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        
        if (cached != null) {
            log.info("从Redis缓存获取商品：{}", productCode);
            return JSON.parseObject(cached.toString(), Product.class);
        }
        
        // Redis未命中，从MySQL查询
        log.info("从MySQL查询商品：{}", productCode);
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getProductCode, productCode)
                   .eq(Product::getTenantId, tenantId)
                   .eq(Product::getDeleted, 0);
        
        Product product = productMapper.selectOne(queryWrapper);
        
        // 写入Redis缓存（30分钟过期）
        if (product != null) {
            redisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(product), 30, TimeUnit.MINUTES);
            log.info("商品信息已写入Redis缓存：{}", productCode);
        }
        
        return product;
    }
    
    @Override
    public void addToCart(String sessionId, Product product, Integer quantity) {
        String cartKey = CART_PREFIX + sessionId;
        
        // 获取当前购物车中的商品
        Map<Object, Object> cartMap = redisTemplate.opsForHash().entries(cartKey);
        
        String productKey = "product_" + product.getId();
        Map<String, Object> cartItem;
        
        if (cartMap.containsKey(productKey)) {
            // 商品已存在，增加数量
            String itemJson = (String) cartMap.get(productKey);
            cartItem = JSON.parseObject(itemJson, Map.class);
            Integer currentQty = (Integer) cartItem.get("quantity");
            cartItem.put("quantity", currentQty + quantity);
        } else {
            // 新商品，添加到购物车
            cartItem = new HashMap<>();
            cartItem.put("productId", product.getId());
            cartItem.put("productCode", product.getProductCode());
            cartItem.put("productName", product.getName());
            cartItem.put("price", product.getPrice());
            cartItem.put("quantity", quantity);
            cartItem.put("imageUrl", product.getImageUrl());
        }
        
        // 保存到Redis Hash
        redisTemplate.opsForHash().put(cartKey, productKey, JSON.toJSONString(cartItem));
        
        // 设置过期时间（30分钟，每次操作都刷新）
        redisTemplate.expire(cartKey, CART_EXPIRE_MINUTES, TimeUnit.MINUTES);
        
        log.info("商品已添加到购物车，sessionId: {}, productId: {}, quantity: {}", 
                sessionId, product.getId(), quantity);
    }
    
    @Override
    public void addToCart(String sessionId, Long productId, Integer quantity, Long tenantId) {
        // 查询商品信息
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
        
        // 调用原有的添加方法
        addToCart(sessionId, product, quantity);
    }
    
    @Override
    public void removeFromCart(String sessionId, Long productId) {
        String cartKey = CART_PREFIX + sessionId;
        String productKey = "product_" + productId;
        
        redisTemplate.opsForHash().delete(cartKey, productKey);
        
        log.info("商品已从购物车移除，sessionId: {}, productId: {}", sessionId, productId);
    }
    
    @Override
    public void updateCartQuantity(String sessionId, Long productId, Integer quantity) {
        if (quantity <= 0) {
            throw new BusinessException("商品数量必须大于0");
        }
        
        String cartKey = CART_PREFIX + sessionId;
        String productKey = "product_" + productId;
        
        // 获取购物车商品
        Object itemObj = redisTemplate.opsForHash().get(cartKey, productKey);
        
        if (itemObj == null) {
            throw new BusinessException("购物车中不存在该商品");
        }
        
        // 更新数量
        Map<String, Object> cartItem = JSON.parseObject(itemObj.toString(), Map.class);
        cartItem.put("quantity", quantity);
        
        // 保存回Redis
        redisTemplate.opsForHash().put(cartKey, productKey, JSON.toJSONString(cartItem));
        
        // 刷新过期时间
        redisTemplate.expire(cartKey, CART_EXPIRE_MINUTES, TimeUnit.MINUTES);
        
        log.info("购物车商品数量已更新，sessionId: {}, productId: {}, quantity: {}", 
                sessionId, productId, quantity);
    }
    
    @Override
    public List<Map<String, Object>> getCart(String sessionId) {
        String cartKey = CART_PREFIX + sessionId;
        Map<Object, Object> cartMap = redisTemplate.opsForHash().entries(cartKey);
        
        List<Map<String, Object>> cartItems = new ArrayList<>();
        for (Object value : cartMap.values()) {
            Map<String, Object> item = JSON.parseObject(value.toString(), Map.class);
            cartItems.add(item);
        }
        
        return cartItems;
    }
    
    @Override
    public void clearCart(String sessionId) {
        String cartKey = CART_PREFIX + sessionId;
        redisTemplate.delete(cartKey);
        log.info("购物车已清空，sessionId: {}", sessionId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentOrder createPosOrder(String sessionId, Long tenantId) {
        // 获取购物车
        List<Map<String, Object>> cartItems = getCart(sessionId);
        
        if (cartItems.isEmpty()) {
            throw new BusinessException("购物车为空，无法创建订单");
        }
        
        // 计算订单总金额
        BigDecimal totalAmount = calculateCartTotal(cartItems);
        
        // 创建订单
        PaymentOrder order = new PaymentOrder();
        order.setTenantId(tenantId);
        order.setOrderNo(generateOrderNo());
        order.setAmount(totalAmount);
        order.setPayAmount(BigDecimal.ZERO);
        order.setOrderStatus("PENDING"); // 待支付
        order.setPayStatus("PENDING"); // 待支付
        order.setSubject("POS收银订单");
        order.setBody("POS收银订单 - " + cartItems.size() + "件商品");
        order.setCreateTime(LocalDateTime.now());
        order.setExpireTime(LocalDateTime.now().plusMinutes(30)); // 30分钟后过期
        
        // 保存订单到数据库
        paymentOrderMapper.insert(order);
        
        // 清空购物车
        clearCart(sessionId);
        
        log.info("POS订单创建成功，订单号: {}, 金额: {}", order.getOrderNo(), totalAmount);
        
        return order;
    }
    
    /**
     * 生成会话ID
     */
    private String generateSessionId(String deviceId, Long tenantId) {
        return tenantId + "_" + deviceId;
    }
    
    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        return "POS" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }
    
    /**
     * 计算购物车总金额
     */
    private BigDecimal calculateCartTotal(List<Map<String, Object>> cartItems) {
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> item : cartItems) {
            BigDecimal price = new BigDecimal(item.get("price").toString());
            Integer quantity = (Integer) item.get("quantity");
            total = total.add(price.multiply(new BigDecimal(quantity)));
        }
        return total;
    }
}
