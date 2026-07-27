package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.PaymentBill;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 支付账单 Mapper
 * <p>对应表：payment_bill，记录支付流水的分账账单信息</p>
 *
 * @author payment-system
 */
@Mapper
public interface PaymentBillMapper extends BaseMapper<PaymentBill> {

    /**
     * 条件更新账单为已支付：仅 WAIT_PAY/PAYING -> SUCCESS，避免无状态条件下
     * 与"关闭"互相无条件覆盖（渠道迟到回调 vs 超时/取消关闭并发）。
     *
     * @return 受影响行数；0 表示当前账单非待支付（可能已成功/已关闭）。
     */
    @Update("""
            UPDATE payment_bill
            SET pay_status = 'SUCCESS', callback_status = #{callbackStatus},
                third_party_bill_no = #{thirdPartyBillNo}, status_remark = #{statusRemark},
                extension_json = #{extensionJson}, update_time = NOW()
            WHERE bill_no = #{billNo}
              AND pay_status IN ('WAIT_PAY', 'PAYING')
            """)
    int markPaidIfPending(@Param("billNo") String billNo,
                          @Param("callbackStatus") String callbackStatus,
                          @Param("thirdPartyBillNo") String thirdPartyBillNo,
                          @Param("statusRemark") String statusRemark,
                          @Param("extensionJson") String extensionJson);

    /** 渠道明确失败时，仅将仍在支付中的账单置为失败。 */
    @Update("""
            UPDATE payment_bill
            SET pay_status = 'FAILED', callback_status = #{callbackStatus},
                status_remark = #{statusRemark}, extension_json = #{extensionJson},
                update_time = NOW()
            WHERE bill_no = #{billNo}
              AND pay_status IN ('WAIT_PAY', 'PAYING')
            """)
    int markFailedIfPending(@Param("billNo") String billNo,
                            @Param("callbackStatus") String callbackStatus,
                            @Param("statusRemark") String statusRemark,
                            @Param("extensionJson") String extensionJson);

    /**
     * 已关闭或明确失败账单收到渠道迟到成功时，仅记录真实资金事实 -> SUCCESS；
     * 此更新绝不发布订单支付事件，后续由退款/人工审核策略处理。
     */
    @Update("""
            UPDATE payment_bill
            SET pay_status = 'SUCCESS', callback_status = #{callbackStatus},
                third_party_bill_no = #{thirdPartyBillNo}, status_remark = #{statusRemark},
                update_time = NOW()
            WHERE bill_no = #{billNo} AND pay_status IN ('CLOSED', 'FAILED')
            """)
    int markLatePaidIfClosed(@Param("billNo") String billNo,
                             @Param("callbackStatus") String callbackStatus,
                             @Param("thirdPartyBillNo") String thirdPartyBillNo,
                             @Param("statusRemark") String statusRemark);

    /**
     * 条件关闭账单：仅 WAIT_PAY/PAYING -> CLOSED，避免覆盖已成功账单
     * （迟到支付成功需保留 SUCCESS 资金事实，由迟到回调策略驱动退款）。
     *
     * @return 受影响行数；0 表示账单已成功或其他不可关闭状态。
     */
    @Update("""
            UPDATE payment_bill
            SET pay_status = 'CLOSED', status_remark = #{statusRemark},
                extension_json = #{extensionJson},
                update_time = NOW()
            WHERE bill_no = #{billNo}
              AND pay_status IN ('WAIT_PAY', 'PAYING')
            """)
    int closeIfPending(@Param("billNo") String billNo,
                      @Param("statusRemark") String statusRemark,
                      @Param("extensionJson") String extensionJson);
}
