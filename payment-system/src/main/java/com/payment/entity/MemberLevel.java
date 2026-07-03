package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员等级实体，对应数据库表 member_level。
 * 定义租户下各会员等级的名称、成长值阈值、折扣权益等信息。
 * 用户成长值达到升级阈值时自动升级，低于保级阈值时可能降级。
 */
@Data
@TableName("member_level")
public class MemberLevel implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID，多租户隔离标识 */
    private Long tenantId;

    /** 等级编号，如 LV0、LV1、VIP 等，全局唯一标识 */
    private String levelNo;

    /** 等级名称，如"普通会员"、"黄金会员"、"钻石会员" */
    private String levelName;

    /** 等级排序值，数值越大等级越高 */
    private Integer levelRank;

    /** 升级所需成长值阈值，成长值达到此值时触发升级 */
    private Integer upgradeGrowth;

    /** 降级成长值阈值，成长值低于此值时触发降级 */
    private Integer downgradeGrowth;

    /** 等级有效期（天数），到期后需重新评估等级 */
    private Integer levelValidityDays;

    /** 该等级享有的折扣率，如 0.95 表示九五折 */
    private BigDecimal discountRate;

    /** 权益详情，JSON 格式存储，如生日礼包、免邮次数等 */
    private String benefitJson;

    /** 状态：1-启用，0-禁用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后更新时间 */
    private LocalDateTime updateTime;
}
