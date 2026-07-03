package com.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.dto.WalletAccountVO;
import com.payment.dto.WalletLogVO;

import java.math.BigDecimal;

/**
 * 统一钱包服务接口。
 * <p>
 * 管理平台级统一钱包账户，提供余额查询、入账（credit）和扣款（debit）操作。
 * 统一钱包与商户钱包互补，支持多种支付策略组合（如 UNIFIED_ONLY、MERCHANT_THEN_UNIFIED 等）。
 * 所有余额变动均采用乐观锁机制保障资金安全。
 */
public interface UnifiedWalletService {

    /**
     * 查询用户的统一钱包账户信息。
     *
     * @param platformUserId 平台用户 ID
     * @return 钱包账户视图对象，包含余额、冻结金额等信息
     */
    WalletAccountVO getWallet(Long platformUserId);

    /**
     * 分页查询用户的钱包流水记录。
     *
     * @param platformUserId 平台用户 ID
     * @param current        当前页码（从 1 开始）
     * @param size           每页数量
     * @return 钱包流水视图分页结果
     */
    Page<WalletLogVO> listLogs(Long platformUserId, Integer current, Integer size);

    /**
     * 向用户统一钱包入账（增加余额）。
     *
     * @param platformUserId 平台用户 ID
     * @param amount         入账金额（单位：元，必须大于 0）
     * @param bizType        业务类型（如 RECHARGE、REFUND 等）
     * @param bizNo          业务单号
     * @param remark         备注说明
     */
    void credit(Long platformUserId, BigDecimal amount, String bizType, String bizNo, String remark);

    /**
     * 从用户统一钱包扣款（减少余额）。
     *
     * @param platformUserId 平台用户 ID
     * @param amount         扣款金额（单位：元，必须大于 0）
     * @param bizType        业务类型
     * @param bizNo          业务单号
     * @param remark         备注说明
     */
    void debit(Long platformUserId, BigDecimal amount, String bizType, String bizNo, String remark);
}
