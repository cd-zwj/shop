package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("compensation_task")
public class CompensationTask implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskNo;
    private String bizType;
    private String bizNo;
    private String taskStatus;
    private String remark;
    private Integer retryCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
