package com.payment.dto;

import lombok.Builder;
import lombok.Value;

import java.io.Serializable;

/**
 * 商户工作台待办项展示对象。
 */
@Value
@Builder
public class MerchantWorkbenchTodoItemVO implements Serializable {
    String key;
    String label;
    String description;
    Long count;
    String path;
    String tone;
}
