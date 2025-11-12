package com.payment.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 商品创建/更新DTO
 */
@Data
public class ProductDTO {
    private Long id;
    
    @NotBlank(message = "商品编码不能为空")
    private String productCode;
    
    @NotBlank(message = "商品名称不能为空")
    private String name;
    
    @NotNull(message = "单价不能为空")
    private BigDecimal price;
    
    private String unit;
    
    private String category;
    
    private String description;
    
    private Integer status;
    
    private MultipartFile image; // 商品图片
}

