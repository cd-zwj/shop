package com.payment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.annotation.RequireAuth;
import com.payment.common.Result;
import com.payment.dto.ExchangeProductDTO;
import com.payment.dto.PointsRuleDTO;
import com.payment.entity.ExchangeProduct;
import com.payment.entity.PointsLog;
import com.payment.entity.PointsRule;
import com.payment.service.PointsService;
import com.payment.util.TenantContextHolder;
import com.payment.util.UserContextHolder;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 积分管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/points")
 
 
public class PointsController {
    
    @Autowired
    private PointsService pointsService;
    
    /**
     * 获取商家积分规则（商家端）
     */
    @RequireAuth
    @GetMapping("/rule")
    public Result<PointsRule> getPointsRule() {
        Long tenantId = TenantContextHolder.getTenantId();
        PointsRule rule = pointsService.getPointsRule(tenantId);
        return Result.success(rule);
    }
    
    /**
     * 设置商家积分规则（商家端）
     */
    @RequireAuth
    @PostMapping("/rule")
    public Result<Void> setPointsRule(@Validated @RequestBody PointsRuleDTO dto) {
        pointsService.setPointsRule(dto);
        return Result.success();
    }
    
    /**
     * 查询用户积分余额（用户端）
     */
    @RequireAuth
    @GetMapping("/balance")
    public Result<Map<String, Object>> getUserPoints() {
        Long userId = UserContextHolder.getUserId();
        Long tenantId = TenantContextHolder.getTenantId();
        
        Integer points = pointsService.getUserPoints(userId, tenantId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("points", points);
        result.put("userId", userId);
        result.put("tenantId", tenantId);
        
        return Result.success(result);
    }
    
    /**
     * 查询积分明细（用户端）
     */
    @RequireAuth
    @GetMapping("/logs")
    public Result<Page<PointsLog>> listPointsLogs(
               @RequestParam(defaultValue = "1") Integer pageNum,
               @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = UserContextHolder.getUserId();
        Long tenantId = TenantContextHolder.getTenantId();
        
        Page<PointsLog> page = pointsService.listPointsLogs(userId, tenantId, pageNum, pageSize);
        return Result.success(page);
    }
    
    /**
     * 积分兑换商品列表（用户端）
     */
    @RequireAuth
    @GetMapping("/exchange/products")
    public Result<List<ExchangeProduct>> listExchangeProducts() {
        Long tenantId = TenantContextHolder.getTenantId();
        List<ExchangeProduct> products = pointsService.listExchangeProducts(tenantId);
        return Result.success(products);
    }
    
    /**
     * 积分兑换商品（用户端）
     */
    @RequireAuth
    @PostMapping("/exchange/{exchangeProductId}")
    public Result<Map<String, String>> exchangeProduct(
               @PathVariable Long exchangeProductId) {
        Long userId = UserContextHolder.getUserId();
        String orderNo = pointsService.exchangeProduct(userId, exchangeProductId);
        
        Map<String, String> result = new HashMap<>();
        result.put("orderNo", orderNo);
        result.put("message", "兑换成功");
        
        return Result.success(result);
    }
    
    /**
     * 添加积分兑换商品（商家端）
     */
    @RequireAuth
    @PostMapping("/exchange/product")
    public Result<Void> setExchangeProduct(@Validated @RequestBody ExchangeProductDTO dto) {
        pointsService.setExchangeProduct(dto);
        return Result.success();
    }
    
    /**
     * 更新积分兑换商品（商家端）
     */
    @RequireAuth
    @PutMapping("/exchange/product/{id}")
    public Result<Void> updateExchangeProduct(
               @PathVariable Long id,
            @Validated @RequestBody ExchangeProductDTO dto) {
        pointsService.updateExchangeProduct(id, dto);
        return Result.success();
    }
    
    /**
     * 删除积分兑换商品（商家端）
     */
    @RequireAuth
    @DeleteMapping("/exchange/product/{id}")
    public Result<Void> deleteExchangeProduct(   @PathVariable Long id) {
        pointsService.deleteExchangeProduct(id);
        return Result.success();
    }
}
