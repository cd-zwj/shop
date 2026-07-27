package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.RechargeOrderV1;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 充值订单 V1 Mapper
 * <p>对应表：recharge_order_v1，V1 版本充值订单的兼容记录</p>
 *
 * @author payment-system
 */
@Mapper
public interface RechargeOrderV1Mapper extends BaseMapper<RechargeOrderV1> {

    /** 充值成功与失败共享的并发裁决锁。调用方必须处于事务中。 */
    @Select("""
            SELECT * FROM recharge_order_v1
            WHERE recharge_no = #{rechargeNo} AND deleted = 0
            FOR UPDATE
            """)
    RechargeOrderV1 selectByRechargeNoForUpdate(@Param("rechargeNo") String rechargeNo);

    /** 渠道明确失败时推进仍待支付的充值业务单。 */
    @Update("""
            UPDATE recharge_order_v1
            SET biz_status = 'FAILED', update_time = NOW()
            WHERE recharge_no = #{rechargeNo} AND deleted = 0 AND biz_status = 'WAIT_PAY'
            """)
    int failIfPending(@Param("rechargeNo") String rechargeNo);
}
