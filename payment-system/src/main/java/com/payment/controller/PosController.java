package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.payment.common.Result;
import com.payment.config.AuthStpKit;
import com.payment.entity.PaymentOrder;
import com.payment.service.ScanService;
import com.payment.util.TenantContextHolder;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * POS 收银控制器 — 已下线，代码保留用于参考。
 * <p>
 * 当前状态（2026-06-12）：@Deprecated + @Profile({"dev","test"}) 双标记，
 * 生产环境不会加载。前端已无任何 POS 路由/页面/菜单入口。
 * 零外部代码引用，仅自身引用 ScanService。
 *
 * @deprecated 已下线，待 V1 迁移完成后移除
 */
@Deprecated
@Profile({"dev", "test"})
@Slf4j
@RestController
@RequestMapping("/pos")
public class PosController {

    @Autowired
    private ScanService scanService;

    /**
     * 添加商品到购物车
     */
    @SaCheckPermission(type = AuthStpKit.MERCHANT_TYPE, value = "pos:checkout")
    @PostMapping("/cart/{sessionId}/add")
    public Result<Void> addToCart(
            @PathVariable String sessionId,
            @RequestBody Map<String, Object> request) {
        try {
            Long productId = Long.valueOf(request.get("productId").toString());
            Integer quantity = Integer.valueOf(request.get("quantity").toString());
            
            Long tenantId = TenantContextHolder.getTenantId();
            if (tenantId == null) {
                return Result.error("租户信息不存在");
            }
            
            scanService.addToCart(sessionId, productId, quantity, tenantId);
            return Result.success( "商品已添加到购物车",null);
        } catch (Exception e) {
            log.error("添加商品到购物车失败", e);
            return Result.error("添加商品失败，请稍后重试");
        }
    }
    
    /**
     * 移除购物车中的商品
     */
    @SaCheckPermission(type = AuthStpKit.MERCHANT_TYPE, value = "pos:checkout")
    @DeleteMapping("/cart/{sessionId}/remove/{productId}")
    public Result<Void> removeFromCart(
            @PathVariable String sessionId,
            @PathVariable Long productId) {
        try {
            scanService.removeFromCart(sessionId, productId);
            return Result.success("商品已从购物车移除",null);
        } catch (Exception e) {
            log.error("移除购物车商品失败", e);
            return Result.error("移除商品失败，请稍后重试");
        }
    }
    
    /**
     * 更新购物车商品数量
     */
    @SaCheckPermission(type = AuthStpKit.MERCHANT_TYPE, value = "pos:checkout")
    @PutMapping("/cart/{sessionId}/update")
    public Result<Void> updateCartQuantity(
            @PathVariable String sessionId,
            @RequestBody Map<String, Object> request) {
        try {
            Long productId = Long.valueOf(request.get("productId").toString());
            Integer quantity = Integer.valueOf(request.get("quantity").toString());
            
            scanService.updateCartQuantity(sessionId, productId, quantity);
            return Result.success("购物车已更新",null );
        } catch (Exception e) {
            log.error("更新购物车失败", e);
            return Result.error("更新购物车失败，请稍后重试");
        }
    }
    
    /**
     * 查询购物车
     */
    @SaCheckPermission(type = AuthStpKit.MERCHANT_TYPE, value = "pos:checkout")
    @GetMapping("/cart/{sessionId}")
    public Result<List<Map<String, Object>>> getCart(@PathVariable String sessionId) {
        try {
            List<Map<String, Object>> cart = scanService.getCart(sessionId);
            return Result.success(cart);
        } catch (Exception e) {
            log.error("查询购物车失败", e);
            return Result.error("查询购物车失败，请稍后重试");
        }
    }
    
    /**
     * 清空购物车
     */
    @SaCheckPermission(type = AuthStpKit.MERCHANT_TYPE, value = "pos:checkout")
    @DeleteMapping("/cart/{sessionId}")
    public Result<Void> clearCart(@PathVariable String sessionId) {
        try {
            scanService.clearCart(sessionId);
            return Result.success("购物车已清空",null);
        } catch (Exception e) {
            log.error("清空购物车失败", e);
            return Result.error("清空购物车失败，请稍后重试");
        }
    }
    
    /**
     * 结账（创建订单）
     */
    @SaCheckPermission(type = AuthStpKit.MERCHANT_TYPE, value = "pos:checkout")
    @PostMapping("/checkout/{sessionId}")
    public Result<PaymentOrder> checkout(@PathVariable String sessionId) {
        try {
            Long tenantId = TenantContextHolder.getTenantId();
            if (tenantId == null) {
                return Result.error("租户信息不存在");
            }
            PaymentOrder order = scanService.createPosOrder(sessionId, tenantId);
            return Result.success("订单创建成功",order);
        } catch (Exception e) {
            log.error("结账失败", e);
            return Result.error("结账失败，请稍后重试");
        }
    }
}
