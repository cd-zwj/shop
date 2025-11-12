package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 扫码记录实体
 */
@Data
@TableName("scan_record")
public class ScanRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long tenantId;
    
    private String deviceId;
    
    private String productCode;
    
    private Long productId;
    
    /**
     * 扫码状态：SUCCESS-成功，NOT_FOUND-商品不存在，ERROR-错误
     */
    private String scanStatus;
    
    private String errorMessage;
    
    private LocalDateTime createTime;
}

