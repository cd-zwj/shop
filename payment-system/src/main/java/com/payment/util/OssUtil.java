package com.payment.util;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectRequest;
import com.payment.config.OssConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

/**
 * 阿里云OSS工具类
 */
@Slf4j
@Component
public class OssUtil {
    
    @Autowired
    private OssConfig ossConfig;
    
    /**
     * 上传文件到OSS
     * @param file 文件
     * @return 文件URL
     */
    public String uploadFile(MultipartFile file) {
       synchronized (OssUtil.class){
           OSS ossClient = null;
           try {
               // 创建OSS客户端
               ossClient = new OSSClientBuilder().build(
                       ossConfig.getEndpoint(),
                       ossConfig.getAccessKeyId(),
                       ossConfig.getAccessKeySecret()
               );

               // 生成文件名
               String originalFilename = file.getOriginalFilename();
               String extension = "";
               if (originalFilename != null && originalFilename.contains(".")) {
                   extension = originalFilename.substring(originalFilename.lastIndexOf("."));
               }
               String fileName = ossConfig.getPathPrefix() + UUID.randomUUID() + extension;

               // 上传文件
               InputStream inputStream = file.getInputStream();
               PutObjectRequest putObjectRequest = new PutObjectRequest(
                       ossConfig.getBucketName(),
                       fileName,
                       inputStream
               );
               ossClient.putObject(putObjectRequest);

               // 返回文件URL
               return ossConfig.getDomain() + "/" + fileName;
           } catch (Exception e) {
               log.error("上传文件到OSS失败", e);
               throw new RuntimeException("上传文件失败：" + e.getMessage());
           } finally {
               if (ossClient != null) {
                   ossClient.shutdown();
               }
           }
       }
    }
    
    /**
     * 删除OSS文件
     * @param fileUrl 文件URL
     */
    public void deleteFile(String fileUrl) {
        OSS ossClient = null;
        try {
            ossClient = new OSSClientBuilder().build(
                    ossConfig.getEndpoint(),
                    ossConfig.getAccessKeyId(),
                    ossConfig.getAccessKeySecret()
            );
            
            // 从URL中提取文件路径
            String fileName = fileUrl.replace(ossConfig.getDomain() + "/", "");
            ossClient.deleteObject(ossConfig.getBucketName(), fileName);
        } catch (Exception e) {
            log.error("删除OSS文件失败", e);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
}

