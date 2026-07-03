package com.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.dto.CardKeyDeliveryDTO;
import com.payment.dto.V1MerchantCardKeySummaryVO;
import com.payment.dto.V1MerchantCardKeyUploadDTO;
import com.payment.dto.V1MerchantCardKeyVO;

/**
 * 卡密池服务接口。
 *
 * <p>提供虚拟商品（如充值卡、激活码等）的卡密管理能力，包括批量导入、库存查询、
 * 订单发货时锁定卡密、退款时归还卡密等功能。卡密池按商户+商品维度隔离。</p>
 */
public interface CardKeyPoolService {

    /**
     * 分页查询商户的卡密列表。
     *
     * @param tenantId       商户（租户）ID
     * @param platformUserId 当前登录用户ID（用于权限校验）
     * @param productId      商品ID（可空，为空则查询全部商品的卡密）
     * @param current        页码
     * @param size           每页条数
     * @param status         卡密状态过滤：available / locked / used（可空）
     * @return 卡密分页数据
     */
    Page<V1MerchantCardKeyVO> listMerchantCardKeys(Long tenantId, Long platformUserId, Long productId,
                                                   Integer current, Integer size, String status);

    /**
     * 获取商户卡密池汇总信息（可用数、已锁定数、已使用数等）。
     *
     * @param tenantId       商户ID
     * @param platformUserId 当前登录用户ID
     * @param productId      商品ID（可空）
     * @return 卡密汇总视图对象
     */
    V1MerchantCardKeySummaryVO getMerchantSummary(Long tenantId, Long platformUserId, Long productId);

    /**
     * 批量上传卡密到卡密池。
     *
     * <p>上传的卡密自动关联到指定商户和商品，重复卡密将被跳过。</p>
     *
     * @param tenantId       商户ID
     * @param platformUserId 当前登录用户ID
     * @param productId      关联商品ID
     * @param dto            上传数据，包含卡密列表
     * @return 上传结果汇总（成功数、跳过数等）
     * @throws com.payment.common.exception.BusinessException 当商品不存在或参数校验失败时抛出
     */
    V1MerchantCardKeySummaryVO uploadMerchantCardKeys(Long tenantId, Long platformUserId, Long productId,
                                                      V1MerchantCardKeyUploadDTO dto);

    /**
     * 为订单发货锁定一张可用卡密。
     *
     * <p>从卡密池中选取最早入库的可用卡密，将其状态更新为锁定，
     * 关联到指定订单项。无可用卡密时抛出异常。</p>
     *
     * @param tenantId    租户ID
     * @param productId   商品ID
     * @param orderNo     订单号
     * @param orderItemId 订单项ID
     * @return 锁定的卡密交付信息（卡号、密码等）
     * @throws com.payment.common.exception.BusinessException 当无可用卡密时抛出
     */
    CardKeyDeliveryDTO lockForDelivery(Long tenantId, Long productId, String orderNo, Long orderItemId);

    /**
     * 根据订单项归还已锁定的卡密（退款场景）。
     *
     * <p>将关联到指定订单项的卡密状态从锁定恢复为可用，并记录归还原因。</p>
     *
     * @param tenantId    租户ID
     * @param orderItemId 订单项ID
     * @param reason      归还原因
     */
    void returnByOrderItem(Long tenantId, Long orderItemId, String reason);

    /**
     * 根据卡密ID归还已锁定的卡密。
     *
     * @param tenantId  租户ID
     * @param cardKeyId 卡密ID
     * @param reason    归还原因
     */
    void returnByCardKeyId(Long tenantId, Long cardKeyId, String reason);
}
