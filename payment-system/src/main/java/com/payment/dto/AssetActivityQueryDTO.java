package com.payment.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 资产动态查询参数。
 */
@Data
public class AssetActivityQueryDTO {
    @Size(max = 4, message = "资产类型最多选择4种")
    private List<String> types;

    @Min(value = 1, message = "商户ID必须大于0")
    private Long tenantId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime from;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime to;

    @Pattern(regexp = "^[A-Za-z0-9_-]*$", message = "游标参数错误")
    @Size(max = 256, message = "游标参数错误")
    private String cursor;

    @Min(value = 1, message = "每页条数必须在1到50之间")
    @Max(value = 50, message = "每页条数必须在1到50之间")
    private Integer size;
}
