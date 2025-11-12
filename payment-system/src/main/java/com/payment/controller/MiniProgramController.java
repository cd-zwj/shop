package com.payment.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.annotation.RequireAuth;
import com.payment.common.Result;
import com.payment.dto.*;
import com.payment.entity.PaymentOrder;
import com.payment.entity.Product;
import com.payment.entity.User;
import com.payment.service.*;

import com.payment.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;

/**
 * 微信小程序用户端控制器
 */

@RestController
@RequestMapping("/miniprogram")
public class MiniProgramController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ProductService productService;
    
    @Autowired
    private ProductSearchService productSearchService;
    
    @Autowired
    private PaymentOrderService paymentOrderService;
    
    @Autowired
    private PointsService pointsService;
    
    @Autowired
    private RechargeService rechargeService;
    
    // ==================== 用户认证接口 ====================
    

    @PostMapping("/auth/login")
    public Result<MiniProgramUserVO> wechatLogin(@Valid @RequestBody WechatLoginDTO dto) {
        MiniProgramUserVO userVO = userService.wechatLogin(dto);
        return Result.success(userVO);
    }

    @GetMapping("/auth/userinfo")
    @RequireAuth
    public Result<User> getUserInfo() {
        Long userId = UserContext.getCurrentUserId();
        User user = userService.getById(userId);
        // 清空密码字段
        user.setPassword(null);
        return Result.success(user);
    }
    
    // ==================== 商品浏览接口 ====================
    
     

    @GetMapping("/product/list")
    public Result<IPage<Product>> getProductList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sortBy) {
        Page<Product> page = new Page<>(current, size);
        IPage<Product> productPage = productService.listProducts(page, category, sortBy);
        return Result.success(productPage);
    }
    

    @GetMapping("/product/detail/{id}")
    public Result<Product> getProductDetail(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        return Result.success(product);
    }
    

    @GetMapping("/product/search")
    public Result<List<Product>> searchProducts(
            @RequestParam String keyword,
            @RequestParam(required = false) Long tenantId) {
        List<Product> products = productSearchService.searchProducts(keyword, tenantId);
        return Result.success(products);
    }
    
    // ==================== 订单接口 ====================

    @PostMapping("/order/create")
    @RequireAuth
    public Result<PaymentOrder> createOrder(@Valid @RequestBody CreateOrderDTO dto) {
        Long userId = UserContext.getCurrentUserId();
        PaymentOrder order = paymentOrderService.createOrder(userId, dto);
        return Result.success(order);
    }

    @GetMapping("/order/list")
    @RequireAuth
    public Result<IPage<PaymentOrder>> getOrderList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String orderStatus) {
        Long userId = UserContext.getCurrentUserId();
        Page<PaymentOrder> page = new Page<>(current, size);
        IPage<PaymentOrder> orderPage = paymentOrderService.listUserOrders(userId, page, orderStatus);
        return Result.success(orderPage);
    }

    @GetMapping("/order/detail/{orderNo}")
    @RequireAuth
    public Result<PaymentOrder> getOrderDetail(@PathVariable String orderNo) {
        PaymentOrder order = paymentOrderService.getOrderByNo(orderNo);
        return Result.success(order);
    }
    
    // ==================== 支付接口 ====================
    

    @PostMapping("/pay/create")
    @RequireAuth
    public Result<PayResponseDTO> createPayment(@RequestParam String orderNo) {
        Long userId = UserContext.getCurrentUserId();
        PayResponseDTO payResponse = paymentOrderService.pay(userId, orderNo);
        return Result.success(payResponse);
    }
    

    @GetMapping("/pay/status")
    @RequireAuth
    public Result<String> getPaymentStatus(@RequestParam String orderNo) {
        PaymentOrder order = paymentOrderService.getOrderByNo(orderNo);
        return Result.success(order.getPayStatus());
    }
    
    // ==================== 积分接口 ====================

    @GetMapping("/points/balance")
    @RequireAuth
    public Result<Integer> getPointsBalance() {
        Long userId = UserContext.getCurrentUserId();
        Long tenantId = UserContext.getCurrentTenantId();
        Integer points = pointsService.getUserPoints(userId, tenantId);
        return Result.success(points);
    }
    

    @GetMapping("/points/exchange/list")
    @RequireAuth
    public Result<List<com.payment.entity.ExchangeProduct>> getExchangeProductList() {
        Long tenantId = UserContext.getCurrentTenantId();
        List<com.payment.entity.ExchangeProduct> products = pointsService.listExchangeProducts(tenantId);
        return Result.success(products);
    }
    
   
    @PostMapping("/points/exchange")
    @RequireAuth
    public Result<PaymentOrder> exchangeProduct(@RequestParam Long exchangeProductId) {
        Long userId = UserContext.getCurrentUserId();
        String orderNo = pointsService.exchangeProduct(userId, exchangeProductId);
        PaymentOrder order = paymentOrderService.getOrderByNo(orderNo);
        return Result.success(order);
    }
    
    // ==================== 充值接口 ====================
    

    @GetMapping("/recharge/rules")
    @RequireAuth
    public Result<List<com.payment.entity.RechargeRule>> getRechargeRules() {
        Long tenantId = UserContext.getCurrentTenantId();
        List<com.payment.entity.RechargeRule> rules = rechargeService.getRechargeRules(tenantId);
        return Result.success(rules);
    }

    @PostMapping("/recharge/create")
    @RequireAuth
    public Result<com.payment.entity.RechargeOrder> createRechargeOrder(@Valid @RequestBody CreateRechargeOrderDTO dto) {
        Long userId = UserContext.getCurrentUserId();
        com.payment.entity.RechargeOrder order = rechargeService.createRechargeOrder(userId, dto.getRuleId());
        return Result.success(order);
    }

    @GetMapping("/recharge/balance")
    @RequireAuth
    public Result<BigDecimal> getUserBalance() {
        Long userId = UserContext.getCurrentUserId();
        Long tenantId = UserContext.getCurrentTenantId();
        BigDecimal balance = rechargeService.getUserBalance(userId, tenantId);
        return Result.success(balance);
    }
}
