package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.ScanRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 扫码记录数据访问接口，提供扫码记录表（scan_record）的 CRUD 操作。
 * 记录用户扫码支付/核销等扫码行为的详细信息。
 */
@Mapper
public interface ScanRecordMapper extends BaseMapper<ScanRecord> {
}

