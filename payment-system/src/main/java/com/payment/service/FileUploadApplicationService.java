package com.payment.service;

import com.payment.common.BusinessException;
import com.payment.common.PageResult;
import com.payment.util.MinioUtil;
import com.payment.vo.FileAssetVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadApplicationService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "application/pdf"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "pdf"
    );

    private final MinioUtil minioUtil;
    private final FileAssetService fileAssetService;

    public String checkFileExists(String fileMd5, String fileName) {
        return minioUtil.checkFileExists(fileMd5, fileName);
    }

    public String uploadFile(MultipartFile file, String fileMd5, Long tenantId, Long userId) {
        validateFileType(file);
        if (fileMd5 != null && !fileMd5.isEmpty()) {
            String existingUrl = minioUtil.checkFileExists(fileMd5, file.getOriginalFilename());
            if (existingUrl != null) {
                log.info("文件秒传成功: md5={}", fileMd5);
                recordFileAssetOrCleanup(file, tenantId, userId, existingUrl, fileMd5);
                return existingUrl;
            }
        }

        String fileUrl = minioUtil.uploadFile(file, fileMd5);
        recordFileAssetOrCleanup(file, tenantId, userId, fileUrl, fileMd5);
        return fileUrl;
    }

    public Map<String, Object> uploadFileChunk(MultipartFile file, String fileId, int chunkNumber, int totalChunks, String fileMd5) {
        validateFileType(file);
        return minioUtil.uploadFileChunk(file, fileId, chunkNumber, totalChunks, fileMd5);
    }

    public Map<String, Object> getUploadProgress(String fileId) {
        return minioUtil.getUploadProgress(fileId);
    }

    public PageResult<FileAssetVO> listFiles(Long tenantId, int current, int size) {
        return fileAssetService.listByTenant(tenantId, current, size);
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

    private void recordFileAssetOrCleanup(MultipartFile file, Long tenantId, Long userId, String fileUrl, String md5) {
        try {
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
