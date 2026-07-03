package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payment.common.BusinessException;
import com.payment.entity.FileAsset;
import com.payment.mapper.FileAssetMapper;
import com.payment.service.FileAssetService;
import com.payment.vo.FileAssetVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件资产管理服务实现。
 */
@Slf4j
@Service
public class FileAssetServiceImpl
        extends ServiceImpl<FileAssetMapper, FileAsset>
        implements FileAssetService {

    /**
     * 记录文件上传信息（文件名、路径、MD5、大小、类型等）。
     *
     * @param tenantId    租户ID
     * @param userId      上传用户ID
     * @param fileName    文件名
     * @param filePath    文件存储路径
     * @param md5         文件MD5摘要
     * @param size        文件大小（字节）
     * @param contentType 文件MIME类型
     * @return 文件资产视图对象
     * @throws BusinessException 当文件路径为空时抛出
     */
    @Override
    public FileAssetVO recordUpload(Long tenantId, Long userId, String fileName,
                                    String filePath, String md5, Long size, String contentType) {
        if (filePath == null || filePath.isBlank()) {
            throw new BusinessException("文件路径不能为空");
        }
        FileAsset asset = new FileAsset();
        asset.setTenantId(tenantId);
        asset.setPlatformUserId(userId);
        asset.setFileName(fileName);
        asset.setFilePath(filePath);
        asset.setFileMd5(md5);
        asset.setFileSize(size);
        asset.setContentType(contentType);
        asset.setStorageType("MINIO");
        asset.setStatus(1);
        asset.setCreateTime(LocalDateTime.now());
        baseMapper.insert(asset);
        return FileAssetVO.from(baseMapper.selectById(asset.getId()));
    }

    @Override
    public List<FileAssetVO> listByTenant(Long tenantId, int page, int size) {
        if (tenantId == null) {
            throw new BusinessException("租户ID不能为空");
        }
        Page<FileAsset> pageParam = new Page<>(page, size);
        baseMapper.selectPage(pageParam,
                new LambdaQueryWrapper<FileAsset>()
                        .eq(FileAsset::getTenantId, tenantId)
                        .eq(FileAsset::getStatus, 1)
                        .orderByDesc(FileAsset::getCreateTime));
        return pageParam.getRecords().stream()
                .map(FileAssetVO::from)
                .toList();
    }

    @Override
    public void markDeleted(Long id) {
        FileAsset asset = baseMapper.selectById(id);
        if (asset == null) {
            throw new BusinessException("文件记录不存在");
        }
        asset.setStatus(0);
        baseMapper.updateById(asset);
    }
}
