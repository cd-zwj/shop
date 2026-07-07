package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.payment.common.BusinessException;
import com.payment.common.PageResult;
import com.payment.common.Result;
import com.payment.common.TenantContextHolder;
import com.payment.config.AuthStpKit;
import com.payment.constant.MerchantPermission;
import com.payment.service.FileUploadApplicationService;
import com.payment.service.impl.V1MerchantSupportService;
import com.payment.util.PlatformSessionHelper;
import com.payment.vo.FileAssetVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 文件上传控制器 - 支持分片上传
 */
@Slf4j
@RestController
@RequestMapping("/api/file")
@Tag(name = "文件上传", description = "文件上传相关接口，支持分片上传")
@SaCheckLogin(type = AuthStpKit.MERCHANT_TYPE)
@RequiredArgsConstructor
@Deprecated
public class FileUploadController {

    private final FileUploadApplicationService fileUploadApplicationService;
    private final V1MerchantSupportService v1MerchantSupportService;
    
    /**
     * 检查文件是否存在（秒传）
     */
    @GetMapping("/check-exists")
    @Operation(summary = "检查文件是否存在", description = "用于秒传功能，如果文件已存在则直接返回URL")
    public Result<Map<String, Object>> checkFileExists(
            @RequestParam("fileMd5") String fileMd5,
            @RequestParam("fileName") String fileName) {
        requireProductManageForCurrentTenant();
        String fileUrl = fileUploadApplicationService.checkFileExists(fileMd5, fileName);
        Map<String, Object> result = new HashMap<>();
        result.put("exists", fileUrl != null);
        if (fileUrl != null) {
            result.put("fileUrl", fileUrl);
            result.put("message", "文件已存在，秒传成功");
        } else {
            result.put("message", "文件不存在，请上传");
        }
        return Result.success(result);
    }
    
    /**
     * 简单文件上传（小文件）
     */
    @PostMapping("/upload")
    @Operation(summary = "简单文件上传", description = "适用于小文件的直接上传，支持MD5去重")
    public Result<String> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "fileMd5", required = false) String fileMd5) {
        Long tenantId = requireProductManageForCurrentTenant();
        Long userId = PlatformSessionHelper.getPlatformUserId();
        return Result.success(fileUploadApplicationService.uploadFile(file, fileMd5, tenantId, userId));
    }
    
    /**
     * 分片上传
     * @param file 文件分片
     * @param fileId 文件唯一ID（前端传递）
     * @param chunkNumber 分片编号（从1开始）
     * @param totalChunks 总分片数
     * @param fileMd5 完整文件的MD5值（前端计算，用于合并后校验）
     */
    @PostMapping("/upload-chunk")
    @Operation(summary = "分片上传", description = "上传文件分片，fileId和fileMd5由前端传递")
    public Result<Map<String, Object>> uploadFileChunk(
            @RequestParam("file") MultipartFile file,
            @RequestParam("fileId") String fileId,
            @RequestParam("chunkNumber") int chunkNumber,
            @RequestParam("totalChunks") int totalChunks,
            @RequestParam("fileMd5") String fileMd5) {
        requireProductManageForCurrentTenant();
        return Result.success(fileUploadApplicationService.uploadFileChunk(file, fileId, chunkNumber, totalChunks, fileMd5));
    }
    
    /**
     * 获取上传进度
     */
    @GetMapping("/upload-progress")
    @Operation(summary = "获取上传进度", description = "查询文件分片上传进度")
    public Result<Map<String, Object>> getUploadProgress(@RequestParam("fileId") String fileId) {
        requireProductManageForCurrentTenant();
        return Result.success(fileUploadApplicationService.getUploadProgress(fileId));
    }

    @GetMapping("/list")
    @Operation(summary = "查询已上传文件列表", description = "按租户分页查询已上传的文件列表，需要登录")
    public Result<PageResult<FileAssetVO>> listFiles(
            @RequestParam(defaultValue = "1") @jakarta.validation.constraints.Min(value = 1, message = "页码必须大于0") Integer current,
            @RequestParam(defaultValue = "10") @jakarta.validation.constraints.Min(value = 1, message = "每页条数必须大于0") Integer size) {
        Long tenantId = requireProductManageForCurrentTenant();
        return Result.success(fileUploadApplicationService.listFiles(tenantId, current, size));
    }

    private Long requireProductManageForCurrentTenant() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("租户信息不存在");
        }
        v1MerchantSupportService.requirePermission(
                tenantId,
                PlatformSessionHelper.getPlatformUserId(),
                MerchantPermission.PRODUCT_MANAGE);
        return tenantId;
    }
}
