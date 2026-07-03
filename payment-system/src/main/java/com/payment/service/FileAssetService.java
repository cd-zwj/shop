package com.payment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.payment.entity.FileAsset;
import com.payment.vo.FileAssetVO;

import java.util.List;

/**
 * 文件资产管理服务接口。
 *
 * <p>负责文件上传记录的持久化管理和查询，支持按租户隔离的文件列表检索和软删除。
 * 实际文件存储由 MinIO / 阿里云 OSS 负责，本服务仅管理文件元数据。
 * 继承 MyBatis-Plus 的 {@link IService} 获得通用 CRUD 能力。</p>
 */
public interface FileAssetService extends IService<FileAsset> {

    /**
     * 记录一次文件上传到数据库。
     *
     * <p>文件已由 Controller 层上传至对象存储后，将元数据（文件名、路径、MD5、大小等）持久化。</p>
     *
     * @param tenantId    租户ID
     * @param userId      上传用户ID
     * @param fileName    原始文件名
     * @param filePath    对象存储路径
     * @param md5         文件 MD5 哈希值
     * @param size        文件大小（字节）
     * @param contentType MIME 类型
     * @return 文件资产 VO
     */
    FileAssetVO recordUpload(Long tenantId, Long userId, String fileName,
                             String filePath, String md5, Long size, String contentType);

    /**
     * 按租户分页查询文件列表。
     *
     * @param tenantId 租户ID
     * @param page     当前页码
     * @param size     每页条数
     * @return 文件资产 VO 列表
     */
    List<FileAssetVO> listByTenant(Long tenantId, int page, int size);

    /**
     * 标记文件为已删除（软删除）。
     *
     * <p>仅更新数据库中的删除标记，不删除对象存储中的实际文件。</p>
     *
     * @param id 文件资产ID
     */
    void markDeleted(Long id);
}
