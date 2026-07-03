package com.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.dto.WalletAccountVO;
import com.payment.dto.WalletLogVO;

import java.math.BigDecimal;

/**
 * 商户钱包服务接口。
 * <p>
 * 管理商户级别的钱包账户，提供余额查询、入账（credit）和扣款（debit）操作。
 * 商户钱包与统一钱包互补，支持 MERCHANT_ONLY、MERCHANT_THEN_UNIFIED 等支付策略。
 * 每个商户在每个租户下拥有独立的钱包账户，余额变动采用乐观锁保障安全。
 */
public interface MerchantWalletService {

    /**
     * 查询商户钱包账户信息。
     *
     * @param tenantId       租户 ID
     * @param platformUserId 平台用户 ID（商户所属用户）
     * @return 钱包账户视图对象，包含余额、冻结金额等信息
     */
    WalletAccountVO getWallet(Long tenantId, Long platformUserId);

    /**
     * 分页查询商户钱包流水记录。
     *
     * @param tenantId       租户 ID
     * @param platformUserId 平台用户 ID
     * @param current        当前页码（从 1 开始）
     * @param size           每页数量
     * @return 钱包流水视图分页结果
     */
    Page<WalletLogVO> listLogs(Long tenantId, Long platformUserId, Integer current, Integer size);

    /**
     * 向商户钱包入账（增加余额）。
     *
     * @param tenantId       租户 ID
     * @param platformUserId 平台用户 ID
     * @param amount         入账金额（单位：元，必须大于 0）
     * @param bizType        业务类型（如 ORDER_SETTLE、REFUND 等）
     * @param bizNo          业务单号
     * @param remark         备注说明
     */
    void credit(Long tenantId, Long platformUserId, BigDecimal amount, String bizType, String bizNo, String remark);

    /**
     * 从商户钱包扣款（减少余额）。
     *
     * @param tenantId       租户 ID
     * @param platformUserId 平台用户 ID
     * @param amount         扣款金额（单位：元，必须大于 0）
     * @param bizType        业务类型（如 WITHDRAWAL、COUPON 等）
     * @param bizNo          业务单号
     * @param remark         备注说明
     */
    void debit(Long tenantId, Long platformUserId, BigDecimal amount, String bizType, String bizNo, String remark);
}
