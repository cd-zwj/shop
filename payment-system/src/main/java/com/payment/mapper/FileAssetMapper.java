package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.FileAsset;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件资源数据访问接口，提供文件资源表（file_asset）的 CRUD 操作。
 * 记录上传至 MinIO/OSS 的文件元数据。
 */
@Mapper
public interface FileAssetMapper extends BaseMapper<FileAsset> {
}
