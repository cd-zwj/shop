package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.payment.dto.ChatRequest;
import com.payment.dto.SalesAnalysisRequest;
import com.payment.util.AiUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/ai")
@Slf4j
public class AiConrtoller {
    @Autowired
    private AiUtil aiUtil;

    @SaCheckPermission("ai:analysis")
    @PostMapping("/merchant/sales-analysis")
    public Flux<String> analyzeSales(@RequestBody SalesAnalysisRequest  request){
        log.info("商家"+request.getMerchantId()+"的"+request.getStartDate()+"到"+request.getEndDate()+"的"+request.getAnalysisType()+"销售数据"+request.getMerchantId()+"的订单分析开始");
       return aiUtil.analyzeSales(request.getMerchantId(), request.getStartDate(), request.getEndDate(), request.getAnalysisType());
    }

    @SaCheckPermission("ai:chat")
    @GetMapping("/recommend/products")
    public Flux<String> recommendProducts(@PathVariable("userId") Long userId, @PathVariable("limit") Integer limit, @PathVariable("scene") String scene){
        log.info("用户"+userId+"的"+scene+"场景下的商品推荐开始");
        return aiUtil.recommendProducts(String.valueOf(userId), limit, scene);
    }

    @SaCheckPermission("ai:chat")
    @PostMapping("/chat")
    public Flux<String> chat(@RequestBody ChatRequest request){
        log.info("用户"+request.getUserId()+"的"+request.getMessage()+"的聊天开始");
       return aiUtil.chat(request.getUserId().toString(), request.getMessage(), request.getContext().toString());
    }
}
