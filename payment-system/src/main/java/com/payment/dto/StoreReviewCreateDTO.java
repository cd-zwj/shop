package com.payment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 用户提交门店评价。 */
@Data
public class StoreReviewCreateDTO {
    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低为1星")
    @Max(value = 5, message = "评分最高为5星")
    private Integer rating;

    @Size(max = 1000, message = "评价内容不能超过1000个字符")
    private String content;

    @Size(max = 6, message = "最多上传6张评价图片")
    private List<@Size(max = 500, message = "图片地址不合法") String> imageUrls;
}
