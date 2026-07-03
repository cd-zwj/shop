package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 扫码记录实体。
 * 对应 scan_record 表，记录用户通过扫码识别商品的行为。
 * 用于扫码统计、商品追溯和异常扫码监控，支持多租户数据隔离。
 */
@Data
@TableName("scan_record")
public class ScanRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户 ID，标识扫码记录所属的商户租户 */
    private Long tenantId;

    /** 设备标识，记录扫码来源设备 */
    private String deviceId;

    /** 商品编码，扫码获取的商品条码或二维码内容 */
    private String productCode;

    /** 商品 ID，关联的系统内部商品主键 */
    private Long productId;

    /**
     * 扫码状态：SUCCESS-成功，NOT_FOUND-商品不存在，ERROR-错误
     */
    private String scanStatus;

    /** 错误信息，扫码失败时记录原因 */
    private String errorMessage;

    /** 扫码时间 */
    private LocalDateTime createTime;
}
