package com.payment.util;

import com.payment.config.MinioConfig;
import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * MinIO工具类 - 支持分片上传
 */
@Slf4j
@Component
public class MinioUtil {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioConfig minioConfig;

    @Autowired
    private RedisUtils redisUtils;

    /**
     * 简单上传文件（适用于小文件）
     * 
     * 功能说明：
     * 1. 如果传入了fileMd5，使用MD5作为文件名（实现秒传和去重）
     * 2. 如果没有传入fileMd5，生成随机UUID作为文件名
     * 3. 上传成功后返回预签名URL（有效期由配置决定）
     * 
     * @param file 要上传的文件
     * @param fileMd5 文件的MD5值（可选），用于实现秒传功能
     * @return 预签名URL，客户端可以通过这个URL访问文件
     */
    public String uploadFile(MultipartFile file, String fileMd5) {
        try {
            ensureBucketExists();
            String fileName;
            if (fileMd5 != null && !fileMd5.isEmpty()) {
                fileName = fileMd5 + getFileExtension(file.getOriginalFilename());
            } else {
                fileName = generateFileName(file.getOriginalFilename());
            }
            String objectName = minioConfig.getPathPrefix() + fileName;
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            return getPresignedUrl(objectName, minioConfig.getUrlExpiryDays());
        } catch (Exception e) {
            log.error("上传文件到MinIO失败", e);
            throw new RuntimeException("上传文件失败，请稍后重试");
        }
    }

    /**
     * 简单上传文件的重载方法（不传MD5）
     * 
     * @param file 要上传的文件
     * @return 预签名URL
     */
    public String uploadFile(MultipartFile file) {
        return uploadFile(file, null);
    }

    /**
     * 检查文件是否已存在（秒传功能）
     * 
     * 工作流程：
     * 1. 根据MD5和文件名构造对象名称
     * 2. 调用MinIO的statObject检查文件是否存在
     * 3. 如果存在，直接返回预签名URL（秒传成功）
     * 4. 如果不存在，返回null（需要正常上传）
     * 
     * 使用场景：
     * - 前端在上传前先调用此方法检查
     * - 如果返回URL则秒传成功，无需上传
     * - 如果返回null则需要调用上传接口
     * 
     * @param fileMd5 文件的MD5值
     * @param fileName 原始文件名（用于提取扩展名）
     * @return 如果文件存在返回预签名URL，否则返回null
     */
    public String checkFileExists(String fileMd5, String fileName) {
        try {
            String objectName = minioConfig.getPathPrefix() + fileMd5 + getFileExtension(fileName);
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .build()
            );
            log.info("文件已存在，秒传: md5={}, fileName={}", fileMd5, fileName);
            return getPresignedUrl(objectName, minioConfig.getUrlExpiryDays());
        } catch (Exception e) {
            log.debug("文件不存在，需要上传: md5={}, fileName={}", fileMd5, fileName);
            return null;
        }
    }

    /**
     * 生成MinIO对象的预签名URL
     * 
     * 预签名URL的作用：
     * - 提供临时访问权限，无需公开bucket
     * - URL包含签名信息，防止被篡改
     * - 有过期时间，提高安全性
     * 
     * @param objectName MinIO中的对象名称（包含路径前缀）
     * @param expiryDays URL有效期（天）
     * @return 预签名URL
     */
    public String getPresignedUrl(String objectName, int expiryDays) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .expiry(expiryDays, TimeUnit.DAYS)
                            .build()
            );
        } catch (Exception e) {
            log.error("生成预签名URL失败: objectName={}", objectName, e);
            throw new RuntimeException("生成预签名URL失败，请稍后重试");
        }
    }

    /**
     * 从文件名中提取扩展名
     * 
     * @param fileName 文件名
     * @return 扩展名（包含点号，如 ".jpg"），如果没有扩展名返回空字符串
     */
    private String getFileExtension(String fileName) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf("."));
        }
        return "";
    }

    /**
     * 上传文件分片
     * 
     * 分片上传流程：
     * 1. 将分片上传到MinIO（临时对象名：fileId_part{chunkNumber}）
     * 2. 更新Redis中的上传进度
     * 3. 当所有分片上传完成时，自动触发合并操作
     * 
     * 并发安全：
     * - 使用Redisson分布式锁保证进度更新的原子性
     * - 防止多个分片同时触发合并操作
     * 
     * @param file 分片文件
     * @param fileId 文件唯一标识（通常是文件的MD5值）
     * @param chunkNumber 当前分片编号（从1开始）
     * @param totalChunks 总分片数
     * @param fileMd5 完整文件的MD5值（用于合并后校验）
     * @return 包含上传结果的Map
     */
    public Map<String, Object> uploadFileChunk(MultipartFile file, String fileId, int chunkNumber, int totalChunks, String fileMd5) {
        try {
            ensureBucketExists();
            String md5Key = "upload:md5:" + fileId;
            redisUtils.set(md5Key, fileMd5, 1, TimeUnit.DAYS);
            String chunkObjectName = fileId + "_part" + chunkNumber;
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(chunkObjectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            updateUploadProgress(fileId, chunkNumber, totalChunks, fileMd5);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "分片上传成功");
            response.put("chunkNumber", chunkNumber);
            response.put("fileId", fileId);
            response.put("uploadedChunks", getUploadedChunks(fileId));
            return response;
        } catch (Exception e) {
            log.error("上传文件分片失败: fileId={}, chunkNumber={}", fileId, chunkNumber, e);
            throw new RuntimeException("上传文件分片失败，请稍后重试");
        }
    }

    /**
     * 更新文件上传进度（使用分布式锁保证并发安全）
     * 
     * 核心逻辑：
     * 1. 获取分布式锁（防止并发问题）
     * 2. 记录当前分片已上传（Redis Set）
     * 3. 检查是否所有分片都已上传
     * 4. 如果全部上传完成，触发文件合并
     * 5. 合并成功后清理Redis中的临时数据
     * 
     * 为什么需要分布式锁：
     * - 多个分片可能同时上传完成
     * - 需要保证只有一个线程检测到"全部完成"并触发合并
     * - 避免重复合并或并发冲突
     * 
     * Redis数据结构：
     * - upload:progress:{fileId} -> Set<分片编号>
     * - upload:total:{fileId} -> 总分片数
     * - upload:md5:{fileId} -> 文件MD5值
     * 
     * @param fileId 文件唯一标识
     * @param chunkNumber 当前分片编号
     * @param totalChunks 总分片数
     * @param fileMd5 文件MD5值
     */
    private void updateUploadProgress(String fileId, int chunkNumber, int totalChunks, String fileMd5) {
        String lockKey = "lock:upload:progress:" + fileId;
        RLock lock = redisUtils.getLock(lockKey);
        
        try {
            // 尝试获取锁，最多等待10秒，锁自动释放时间30秒
            if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
                try {
                    String progressKey = "upload:progress:" + fileId;
                    String totalChunksKey = "upload:total:" + fileId;
                    String md5Key = "upload:md5:" + fileId;
                    
                    redisUtils.setAdd(progressKey, String.valueOf(chunkNumber));
                    redisUtils.set(totalChunksKey, String.valueOf(totalChunks), 1, TimeUnit.DAYS);
                    redisUtils.set(md5Key, fileMd5, 1, TimeUnit.DAYS);
                    redisUtils.expire(progressKey, 1, TimeUnit.DAYS);
                    
                    int uploadedCount = redisUtils.setSize(progressKey);
                    if (uploadedCount == totalChunks) {
                        try {
                            mergeFileChunks(fileId, totalChunks, fileMd5);
                            redisUtils.delete(progressKey, totalChunksKey, md5Key);
                        } catch (Exception e) {
                            log.error("合并文件分片失败: fileId={}", fileId, e);
                            throw new RuntimeException("合并文件分片失败，请稍后重试");
                        }
                    }
                } finally {
                    lock.unlock();
                }
            } else {
                log.error("获取上传进度锁超时: fileId={}", fileId);
                throw new RuntimeException("更新上传进度失败，请稍后重试");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取上传进度锁被中断: fileId={}", fileId, e);
            throw new RuntimeException("更新上传进度失败", e);
        } catch (Exception e) {
            log.error("更新上传进度失败: fileId={}", fileId, e);
            throw new RuntimeException("更新上传进度失败", e);
        }
    }

    /**
     * 获取已上传的分片列表
     * 
     * @param fileId 文件唯一标识
     * @return 已上传的分片编号列表（已排序）
     */
    public List<Integer> getUploadedChunks(String fileId) {
        String progressKey = "upload:progress:" + fileId;
        Set<String> uploadedChunks = redisUtils.setMembers(progressKey);
        if (uploadedChunks == null || uploadedChunks.isEmpty()) {
            return new ArrayList<>();
        }
        return uploadedChunks.stream().map(Integer::parseInt).sorted().toList();
    }

    /**
     * 查询文件上传进度
     * 
     * 返回信息：
     * - 上传进度百分比
     * - 已上传分片数
     * - 总分片数
     * - 如果上传完成，返回文件的预签名URL
     * 
     * @param fileId 文件唯一标识
     * @return 包含进度信息的Map
     */
    public Map<String, Object> getUploadProgress(String fileId) {
        List<Integer> uploadedChunksList = getUploadedChunks(fileId);
        int uploadedCount = uploadedChunksList.size();
        String totalChunksKey = "upload:total:" + fileId;
        String totalChunksStr = redisUtils.get(totalChunksKey);
        int totalChunks = totalChunksStr != null ? Integer.parseInt(totalChunksStr) : 0;
        int progressPercent = totalChunks > 0 ? (uploadedCount * 100 / totalChunks) : 0;
        String urlKey = "upload:url:" + fileId;
        String fileUrl = redisUtils.get(urlKey);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("progress", progressPercent);
        response.put("message", progressPercent == 100 ? "上传完成" : "正在处理");
        response.put("uploadedChunks", uploadedCount);
        response.put("totalChunks", totalChunks);
        response.put("fileId", fileId);
        if (progressPercent == 100 && fileUrl != null) {
            response.put("fileUrl", fileUrl);
        }
        return response;
    }

    /**
     * 合并文件分片
     * 
     * 合并流程：
     * 1. 构建所有分片的引用列表
     * 2. 调用MinIO的composeObject合并分片
     * 3. 下载合并后的文件并计算MD5
     * 4. 校验MD5是否与前端传递的一致
     * 5. 如果校验成功：
     *    - 生成预签名URL并保存到Redis
     *    - 删除所有临时分片文件
     * 6. 如果校验失败：
     *    - 删除合并后的文件
     *    - 抛出异常
     * 
     * MD5校验的重要性：
     * - 确保文件在传输过程中没有损坏
     * - 防止分片顺序错误或分片丢失
     * - 保证文件完整性
     * 
     * @param fileId 文件唯一标识
     * @param totalChunks 总分片数
     * @param expectedMd5 前端传递的文件MD5值
     * @throws Exception 合并失败或MD5校验失败时抛出异常
     */
    private void mergeFileChunks(String fileId, int totalChunks, String expectedMd5) throws Exception {
        List<ComposeSource> parts = new ArrayList<>();
        for (int i = 1; i <= totalChunks; i++) {
            String partName = fileId + "_part" + i;
            ComposeSource part = ComposeSource.builder().bucket(minioConfig.getBucketName()).object(partName).build();
            parts.add(part);
        }
        String mergedObjectName = minioConfig.getPathPrefix() + fileId;
        minioClient.composeObject(ComposeObjectArgs.builder().bucket(minioConfig.getBucketName()).object(mergedObjectName).sources(parts).build());
        log.info("文件分片合并完成，开始校验MD5: fileId={}", fileId);
        String actualMd5 = calculateMergedFileMD5(mergedObjectName);
        if (!expectedMd5.equalsIgnoreCase(actualMd5)) {
            log.error("文件MD5校验失败: fileId={}, expected={}, actual={}", fileId, expectedMd5, actualMd5);
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(minioConfig.getBucketName()).object(mergedObjectName).build());
            throw new RuntimeException("文件MD5校验失败，上传的文件可能已损坏");
        }
        log.info("文件MD5校验成功: fileId={}, md5={}", fileId, actualMd5);
        String presignedUrl = getPresignedUrl(mergedObjectName, minioConfig.getUrlExpiryDays());
        String urlKey = "upload:url:" + fileId;
        redisUtils.set(urlKey, presignedUrl, minioConfig.getUrlExpiryDays(), TimeUnit.DAYS);
        for (int i = 1; i <= totalChunks; i++) {
            String partName = fileId + "_part" + i;
            try {
                minioClient.removeObject(RemoveObjectArgs.builder().bucket(minioConfig.getBucketName()).object(partName).build());
            } catch (Exception e) {
                log.warn("删除临时分片文件失败: {}", partName, e);
            }
        }
        log.info("文件分片合并成功并通过MD5校验: fileId={}, totalChunks={}", fileId, totalChunks);
    }

    /**
     * 计算合并后文件的MD5值
     * 
     * 实现方式：
     * 1. 从MinIO下载文件流
     * 2. 边读取边计算MD5
     * 3. 返回16进制格式的MD5字符串
     * 
     * 注意：使用流式处理，避免大文件占用过多内存
     * 
     * @param objectName MinIO中的对象名称
     * @return MD5值（32位16进制字符串）
     * @throws Exception 读取文件或计算MD5失败时抛出异常
     */
    private String calculateMergedFileMD5(String objectName) throws Exception {
        try (InputStream stream = minioClient.getObject(GetObjectArgs.builder().bucket(minioConfig.getBucketName()).object(objectName).build())) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = stream.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        }
    }

    /**
     * 删除MinIO中的文件
     * 
     * 注意：传入的是对象名称，不是URL
     * 如果需要从预签名URL中提取对象名称，使用 extractObjectNameFromUrl() 方法
     * 
     * @param objectName MinIO中的对象名称（如：uploads/abc123.jpg）
     */
    public void deleteFile(String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(minioConfig.getBucketName()).object(objectName).build());
            log.info("删除MinIO文件成功: {}", objectName);
        } catch (Exception e) {
            log.error("删除MinIO文件失败: {}", objectName, e);
            throw new RuntimeException("删除文件失败，请稍后重试");
        }
    }

    /**
     * 从预签名URL中提取对象名称
     * 
     * 使用场景：
     * - 当你只有预签名URL，需要删除文件时
     * - 先调用此方法提取对象名称
     * - 再调用 deleteFile(objectName) 删除
     * 
     * 处理逻辑：
     * 1. 去掉URL中的查询参数（?后面的部分）
     * 2. 去掉endpoint和bucket前缀
     * 3. 得到纯粹的对象名称
     * 
     * 示例：
     * 输入：http://localhost:9000/payment-system/uploads/abc.jpg?X-Amz-Algorithm=...
     * 输出：uploads/abc.jpg
     * 
     * @param presignedUrl 预签名URL
     * @return 对象名称
     */
    public String extractObjectNameFromUrl(String presignedUrl) {
        try {
            String url = presignedUrl.split("\\?")[0];
            String bucketPrefix = minioConfig.getEndpoint() + "/" + minioConfig.getBucketName() + "/";
            return url.replace(bucketPrefix, "");
        } catch (Exception e) {
            log.error("从URL提取对象名称失败: {}", presignedUrl, e);
            throw new RuntimeException("从URL提取对象名称失败");
        }
    }

    /**
     * 确保MinIO的bucket存在，如果不存在则自动创建
     * 
     * 调用时机：在每次上传文件前调用
     * 
     * @throws RuntimeException 检查或创建bucket失败时抛出异常
     */
    private void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioConfig.getBucketName()).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioConfig.getBucketName()).build());
                log.info("创建MinIO bucket成功: {}", minioConfig.getBucketName());
            }
        } catch (Exception e) {
            log.error("检查或创建bucket失败", e);
            throw new RuntimeException("检查或创建bucket失败", e);
        }
    }

    /**
     * 生成随机文件名
     * 
     * 格式：UUID + 原始文件扩展名
     * 示例：550e8400-e29b-41d4-a716-446655440000.jpg
     * 
     * @param originalFilename 原始文件名
     * @return 随机生成的文件名
     */
    private String generateFileName(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID() + extension;
    }
}
