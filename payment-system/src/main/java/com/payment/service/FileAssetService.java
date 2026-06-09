package com.payment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.payment.entity.FileAsset;
import com.payment.vo.FileAssetVO;

import java.util.List;

/**
 * 文件资产管理服务接口
 */
public interface FileAssetService extends IService<FileAsset> {

    /**
     * 记录一次文件上传到数据库
     */
    FileAssetVO recordUpload(Long tenantId, Long userId, String fileName,
                             String filePath, String md5, Long size, String contentType);

    /**
     * 按租户分页查询文件列表
     */
    List<FileAssetVO> listByTenant(Long tenantId, int page, int size);

    /**
     * 标记文件为已删除
     */
    void markDeleted(Long id);
}
