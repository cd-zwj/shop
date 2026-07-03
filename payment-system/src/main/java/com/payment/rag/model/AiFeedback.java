package com.payment.rag.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_feedback")
public class AiFeedback {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String sessionId;

    private Integer messageIndex;

    private String userId;

    private String feedbackType;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
