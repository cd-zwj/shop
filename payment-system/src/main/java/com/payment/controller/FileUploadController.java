package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.payment.common.BusinessException;
import com.payment.common.PageResult;
import com.payment.common.Result;
import com.payment.common.TenantContextHolder;
import com.payment.service.FileAssetService;
import com.payment.util.MinioUtil;
import com.payment.util.PlatformSessionHelper;
import com.payment.vo.FileAssetVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 文件上传控制器 - 支持分片上传
 */
@Slf4j
@RestController
@RequestMapping("/api/file")
@Tag(name = "文件上传", description = "文件上传相关接口，支持分片上传")
@SaCheckLogin
public class FileUploadController {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "application/pdf"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "pdf"
    );

    @Autowired
    private MinioUtil minioUtil;

    @Autowired
    private FileAssetService fileAssetService;
    
    /**
     * 检查文件是否存在（秒传）
     */
    @GetMapping("/check-exists")
    @Operation(summary = "检查文件是否存在", description = "用于秒传功能，如果文件已存在则直接返回URL")
    public Result<Map<String, Object>> checkFileExists(
            @RequestParam("fileMd5") String fileMd5,
            @RequestParam("fileName") String fileName) {
        try {
            String fileUrl = minioUtil.checkFileExists(fileMd5, fileName);
            
            Map<String, Object> result = new HashMap<>();
            if (fileUrl != null) {
                // 文件已存在，秒传
                result.put("exists", true);
                result.put("fileUrl", fileUrl);
                result.put("message", "文件已存在，秒传成功");
            } else {
                // 文件不存在，需要上传
                result.put("exists", false);
                result.put("message", "文件不存在，请上传");
            }
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("检查文件是否存在失败: fileMd5={}, fileName={}", fileMd5, fileName, e);
            return Result.error("检查文件失败，请稍后重试");
        }
    }
    
    /**
     * 简单文件上传（小文件）
     */
    @PostMapping("/upload")
    @Operation(summary = "简单文件上传", description = "适用于小文件的直接上传，支持MD5去重")
    public Result<String> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "fileMd5", required = false) String fileMd5) {
        Long tenantId = TenantContextHolder.getTenantId();
        try {
            validateFileType(file);

            // 如果提供了MD5，先检查文件是否存在
            if (fileMd5 != null && !fileMd5.isEmpty()) {
                String existingUrl = minioUtil.checkFileExists(fileMd5, file.getOriginalFilename());
                if (existingUrl != null) {
                    log.info("文件秒传成功: md5={}", fileMd5);
                    recordFileAssetOrCleanup(file, tenantId, existingUrl, fileMd5);
                    return Result.success(existingUrl);
                }
            }

            // 文件不存在，执行上传
            String fileUrl = minioUtil.uploadFile(file, fileMd5);
            recordFileAssetOrCleanup(file, tenantId, fileUrl, fileMd5);
            return Result.success(fileUrl);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return Result.error("文件上传失败，请稍后重试");
        }
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
        try {
            validateFileType(file);

            Map<String, Object> result = minioUtil.uploadFileChunk(file, fileId, chunkNumber, totalChunks, fileMd5);
            return Result.success(result);
        } catch (Exception e) {
            log.error("分片上传失败: fileId={}, chunkNumber={}", fileId, chunkNumber, e);
            return Result.error("分片上传失败，请稍后重试");
        }
    }
    
    /**
     * 获取上传进度
     */
    @GetMapping("/upload-progress")
    @Operation(summary = "获取上传进度", description = "查询文件分片上传进度")
    public Result<Map<String, Object>> getUploadProgress(@RequestParam("fileId") String fileId) {
        try {
            Map<String, Object> progress = minioUtil.getUploadProgress(fileId);
            return Result.success(progress);
        } catch (Exception e) {
            log.error("获取上传进度失败: fileId={}", fileId, e);
            return Result.error("获取上传进度失败，请稍后重试");
        }
    }

    @SaCheckPermission("file:list")
    @GetMapping("/list")
    @Operation(summary = "查询已上传文件列表", description = "按租户分页查询已上传的文件列表，需要登录")
    public Result<PageResult<FileAssetVO>> listFiles(
            @RequestParam(defaultValue = "1") @jakarta.validation.constraints.Min(value = 1, message = "页码必须大于0") Integer current,
            @RequestParam(defaultValue = "10") @jakarta.validation.constraints.Min(value = 1, message = "每页条数必须大于0") Integer size) {
        Long tenantId = TenantContextHolder.getTenantId();
        List<FileAssetVO> records = fileAssetService.listByTenant(tenantId, current, size);
        return Result.success(new PageResult<>(records, (long) records.size(), current, size));
    }

    private void validateFileType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessException("不支持的文件类型，仅支持 JPEG、PNG、GIF、WebP 图片和 PDF 文档");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String extension = StringUtils.getFilenameExtension(originalFilename);
            if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT))) {
                throw new BusinessException("不支持的文件扩展名，仅支持 .jpg、.jpeg、.png、.gif、.webp、.pdf");
            }
        }
    }

    private void recordFileAssetOrCleanup(MultipartFile file, Long tenantId, String fileUrl, String md5) {
        try {
            Long userId = PlatformSessionHelper.getPlatformUserId();
            fileAssetService.recordUpload(
                    tenantId,
                    userId,
                    file.getOriginalFilename(),
                    fileUrl,
                    md5,
                    file.getSize(),
                    file.getContentType());
        } catch (Exception e) {
            log.error("记录文件资产失败，清理已上传文件, fileUrl={}", fileUrl, e);
            try {
                minioUtil.deleteFile(minioUtil.extractObjectNameFromUrl(fileUrl));
            } catch (Exception cleanupEx) {
                log.warn("清理已上传文件失败, fileUrl={}", fileUrl, cleanupEx);
            }
            throw new BusinessException("文件资产记录失败，请稍后重试");
        }
    }
}
