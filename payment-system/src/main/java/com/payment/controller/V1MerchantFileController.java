package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.payment.common.PageResult;
import com.payment.common.Result;
import com.payment.common.TenantContextHolder;
import com.payment.config.AuthStpKit;
import com.payment.service.FileUploadApplicationService;
import com.payment.util.PlatformSessionHelper;
import com.payment.vo.FileAssetVO;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1/merchant/tenants/{tenantId}/files")
@SaCheckLogin(type = AuthStpKit.MERCHANT_TYPE)
@RequiredArgsConstructor
public class V1MerchantFileController {

    private final FileUploadApplicationService fileUploadApplicationService;

    @GetMapping("/check-exists")
    public Result<Map<String, Object>> checkFileExists(
            @PathVariable Long tenantId,
            @RequestParam("fileMd5") String fileMd5,
            @RequestParam("fileName") String fileName) {
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

    @PostMapping("/upload")
    public Result<String> uploadFile(
            @PathVariable Long tenantId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "fileMd5", required = false) String fileMd5) {
        Long currentTenantId = TenantContextHolder.getTenantId();
        Long userId = PlatformSessionHelper.getPlatformUserId();
        return Result.success(fileUploadApplicationService.uploadFile(file, fileMd5, currentTenantId, userId));
    }

    @PostMapping("/upload-chunk")
    public Result<Map<String, Object>> uploadFileChunk(
            @PathVariable Long tenantId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("fileId") String fileId,
            @RequestParam("chunkNumber") int chunkNumber,
            @RequestParam("totalChunks") int totalChunks,
            @RequestParam("fileMd5") String fileMd5) {
        return Result.success(fileUploadApplicationService.uploadFileChunk(file, fileId, chunkNumber, totalChunks, fileMd5));
    }

    @GetMapping("/upload-progress")
    public Result<Map<String, Object>> getUploadProgress(
            @PathVariable Long tenantId,
            @RequestParam("fileId") String fileId) {
        return Result.success(fileUploadApplicationService.getUploadProgress(fileId));
    }

    @SaCheckPermission(type = AuthStpKit.MERCHANT_TYPE, value = "file:list")
    @GetMapping
    public Result<PageResult<FileAssetVO>> listFiles(
            @PathVariable Long tenantId,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer current,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数必须大于0") Integer size) {
        return Result.success(fileUploadApplicationService.listFiles(TenantContextHolder.getTenantId(), current, size));
    }
}
