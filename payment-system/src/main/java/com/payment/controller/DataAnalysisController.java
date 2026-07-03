package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.payment.common.Result;
import com.payment.config.AuthStpKit;
import com.payment.dto.AnalysisRequestDTO;
import com.payment.entity.DataAnalysisResult;
import com.payment.service.DataAnalysisService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 数据分析控制器
 * <p>
 * 数据分析功能待 V1 迁移。
 *
 * @deprecated 待 V1 迁移完成后移除
 */
@Deprecated
@Profile({"dev", "test"})
@RestController
@RequestMapping("/analysis")
@SaCheckLogin(type = AuthStpKit.MERCHANT_TYPE)
public class DataAnalysisController {
    
    @Autowired
    private DataAnalysisService dataAnalysisService;

    @PostMapping("/analyze")
    public Result<DataAnalysisResult> analyze(@Valid @RequestBody AnalysisRequestDTO request) {
        DataAnalysisResult result = dataAnalysisService.analyze(request);
        return Result.success(result);
    }
    

    @GetMapping("/result/{id}")
    public Result<DataAnalysisResult> getResult(@PathVariable Long id) {
        DataAnalysisResult result = dataAnalysisService.getAnalysisResult(id);
        return Result.success(result);
    }
    

    @GetMapping("/list")
    public Result<com.baomidou.mybatisplus.extension.plugins.pagination.Page<DataAnalysisResult>> getAnalysisList(
            @RequestParam(required = false) String analysisType,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        return Result.success(dataAnalysisService.getAnalysisList(analysisType, current, size));
    }
}

