package com.payment.vo;

import com.payment.entity.FileAsset;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件资产视图对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileAssetVO {

    private Long id;
    private Long tenantId;
    private Long platformUserId;
    private String fileName;
    private String filePath;
    private String fileMd5;
    private Long fileSize;
    private String contentType;
    private String storageType;
    private Integer status;
    private String createTime;

    public static FileAssetVO from(FileAsset asset) {
        if (asset == null) {
            return null;
        }
        return FileAssetVO.builder()
                .id(asset.getId())
                .tenantId(asset.getTenantId())
                .platformUserId(asset.getPlatformUserId())
                .fileName(asset.getFileName())
                .filePath(asset.getFilePath())
                .fileMd5(asset.getFileMd5())
                .fileSize(asset.getFileSize())
                .contentType(asset.getContentType())
                .storageType(asset.getStorageType())
                .status(asset.getStatus())
                .createTime(VoConverterUtil.formatTime(asset.getCreateTime()))
                .build();
    }
}
