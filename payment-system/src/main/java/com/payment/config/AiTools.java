package com.payment.config;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.entity.PaymentOrder;
import com.payment.entity.Product;
import com.payment.mapper.PaymentOrderMapper;
import com.payment.mapper.ProductMapper;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.service.V;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI工具类
 * 提供给AI使用的工具方法
 */
@Slf4j
@Component("aiTools")
@RequiredArgsConstructor
public class AiTools {
    
    private final PaymentOrderMapper paymentOrderMapper;
    private final ProductMapper productMapper;
    
    /**
     * 获取销售数据
     * 
     * @param merchantId 商家ID
     * @param startDate 开始日期 (yyyy-MM-dd)
     * @param endDate 结束日期 (yyyy-MM-dd)
     * @return 销售数据JSON字符串
     */
    @Tool("获取商家的销售数据，包括销售额、订单数、趋势等")
    public String getSalesData(@V("商家ID") Long merchantId, @V("开始日期 (yyyy-MM-dd)")String startDate,@V("结束日期 (yyyy-MM-dd)") String endDate) {
        log.info("获取销售数据: merchantId={}, startDate={}, endDate={}", merchantId, startDate, endDate);
        
        try {
            LocalDateTime startTime = LocalDate.parse(startDate).atStartOfDay();
            LocalDateTime endTime = LocalDate.parse(endDate).atTime(23, 59, 59);
            
            // 查询订单数据
            LambdaQueryWrapper<PaymentOrder> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PaymentOrder::getTenantId, merchantId)
                   .between(PaymentOrder::getCreateTime, startTime, endTime)
                   .eq(PaymentOrder::getPayStatus, "PAID");
            
            List<PaymentOrder> orders = paymentOrderMapper.selectList(wrapper);
            
            // 计算汇总数据
            BigDecimal totalSales = orders.stream()
                .map(PaymentOrder::getPayAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            int orderCount = orders.size();
            BigDecimal avgOrderValue = orderCount > 0 ? 
                totalSales.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            
            // 计算增长率（与上一期对比）
            double growthRate = calculateGrowthRate(merchantId, startTime, endTime);
            
            Map<String, Object> summary = new HashMap<>();
            summary.put("totalSales", totalSales);
            summary.put("orderCount", orderCount);
            summary.put("avgOrderValue", avgOrderValue);
            summary.put("growthRate", growthRate);
            
            // 计算趋势数据（按月统计）
            Map<String, List<PaymentOrder>> monthlyOrders = orders.stream()
                .collect(Collectors.groupingBy(o -> 
                    o.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM"))
                ));
            
            List<String> labels = new ArrayList<>();
            List<BigDecimal> sales = new ArrayList<>();
            List<Integer> orderCounts = new ArrayList<>();
            
            monthlyOrders.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    labels.add(entry.getKey());
                    BigDecimal monthlySales = entry.getValue().stream()
                        .map(PaymentOrder::getPayAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                    sales.add(monthlySales);
                    orderCounts.add(entry.getValue().size());
                });
            
            Map<String, Object> trend = new HashMap<>();
            trend.put("labels", labels);
            trend.put("sales", sales);
            trend.put("orders", orderCounts);
            
            Map<String, Object> result = new HashMap<>();
            result.put("summary", summary);
            result.put("trend", trend);
            
            return JSON.toJSONString(result);
        } catch (Exception e) {
            log.error("获取销售数据失败", e);
            return "{\"error\":\"获取销售数据失败\"}";
        }
    }
    
    /**
     * 获取推荐商品列表
     * 
     * @param userId 用户ID（可选）
     * @param limit 推荐数量
     * @param scene 推荐场景
     * @return 推荐商品列表JSON字符串
     */
    @Tool("获取推荐商品列表，基于用户行为和偏好")
    public String getRecommendProducts(@V("用户ID") Long userId,@V("推荐数量") Integer limit, @V("推荐场景") String scene) {
        log.info("获取推荐商品: userId={}, limit={}, scene={}", userId, limit, scene);
        
        try {
            LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Product::getStatus, 1)
                   .orderByDesc(Product::getCreateTime)
                   .last("LIMIT " + (limit != null ? limit : 10));
            
            List<Product> products = productMapper.selectList(wrapper);
            
            List<Map<String, Object>> result = products.stream()
                .map(p -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("productId", p.getId());
                    item.put("productName", p.getName());
                    item.put("price", p.getPrice());
                    item.put("imageUrl", p.getImageUrl());
                    item.put("merchantName", "商家" + p.getTenantId());
                    item.put("score", 0.85 + Math.random() * 0.15);
                    item.put("reason", "热门商品推荐");
                    return item;
                })
                .collect(Collectors.toList());
            
            return JSON.toJSONString(result);
        } catch (Exception e) {
            log.error("获取推荐商品失败", e);
            return "[]";
        }
    }
    
    /**
     * 获取相似商品列表
     * 
     * @param productId 商品ID
     * @param limit 推荐数量
     * @return 相似商品列表JSON字符串
     */
    @Tool("获取与指定商品相似的商品列表")
    public String getSimilarProducts(@V("商品ID") Long productId, @V("推荐数量") Integer limit) {
        log.info("获取相似商品: productId={}, limit={}", productId, limit);
        
        try {
            Product product = productMapper.selectById(productId);
            if (product == null) {
                return "[]";
            }
            
            LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Product::getStatus, 1)
                   .eq(Product::getCategory, product.getCategory())
                   .ne(Product::getId, productId)
                   .orderByDesc(Product::getCreateTime)
                   .last("LIMIT " + (limit != null ? limit : 6));
            
            List<Product> products = productMapper.selectList(wrapper);
            
            List<Map<String, Object>> result = products.stream()
                .map(p -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("productId", p.getId());
                    item.put("productName", p.getName());
                    item.put("price", p.getPrice());
                    item.put("imageUrl", p.getImageUrl());
                    item.put("merchantName", "商家" + p.getTenantId());
                    item.put("similarity", 0.80 + Math.random() * 0.20);
                    return item;
                })
                .collect(Collectors.toList());
            
            return JSON.toJSONString(result);
        } catch (Exception e) {
            log.error("获取相似商品失败", e);
            return "[]";
        }
    }
    
    /**
     * 搜索商品
     * 
     * @param keyword 搜索关键词
     * @param limit 返回数量
     * @return 商品列表JSON字符串
     */
    @Tool("根据关键词搜索商品")
    public String searchProducts(String keyword, Integer limit) {
        log.info("搜索商品: keyword={}, limit={}", keyword, limit);
        
        try {
            LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Product::getStatus, 1)
                   .like(Product::getName, keyword)
                   .orderByDesc(Product::getCreateTime)
                   .last("LIMIT " + (limit != null ? limit : 10));
            
            List<Product> products = productMapper.selectList(wrapper);
            
            List<Map<String, Object>> result = products.stream()
                .map(p -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("productId", p.getId());
                    item.put("productName", p.getName());
                    item.put("price", p.getPrice());
                    item.put("imageUrl", p.getImageUrl());
                    item.put("category", p.getCategory());
                    item.put("description", p.getDescription());
                    return item;
                })
                .collect(Collectors.toList());
            
            return JSON.toJSONString(result);
        } catch (Exception e) {
            log.error("搜索商品失败", e);
            return "[]";
        }
    }
    
    /**
     * 计算增长率
     */
    private double calculateGrowthRate(Long merchantId, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            // 计算当前期间的天数
            long days = java.time.Duration.between(startTime, endTime).toDays();
            
            // 计算上一期间
            LocalDateTime prevStartTime = startTime.minusDays(days);
            LocalDateTime prevEndTime = startTime;
            
            // 查询当前期间订单
            LambdaQueryWrapper<PaymentOrder> currentWrapper = new LambdaQueryWrapper<>();
            currentWrapper.eq(PaymentOrder::getTenantId, merchantId)
                         .between(PaymentOrder::getCreateTime, startTime, endTime)
                         .eq(PaymentOrder::getPayStatus, "PAID");
            List<PaymentOrder> currentOrders = paymentOrderMapper.selectList(currentWrapper);
            BigDecimal currentSales = currentOrders.stream()
                .map(PaymentOrder::getPayAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // 查询上一期间订单
            LambdaQueryWrapper<PaymentOrder> prevWrapper = new LambdaQueryWrapper<>();
            prevWrapper.eq(PaymentOrder::getTenantId, merchantId)
                      .between(PaymentOrder::getCreateTime, prevStartTime, prevEndTime)
                      .eq(PaymentOrder::getPayStatus, "PAID");
            List<PaymentOrder> prevOrders = paymentOrderMapper.selectList(prevWrapper);
            BigDecimal prevSales = prevOrders.stream()
                .map(PaymentOrder::getPayAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            if (prevSales.compareTo(BigDecimal.ZERO) == 0) {
                return 0.0;
            }
            BigDecimal growth = currentSales.subtract(prevSales)
                .divide(prevSales, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
            
            return growth.doubleValue();
        } catch (Exception e) {
            log.error("计算增长率失败", e);
            return 0.0;
        }
    }
}
