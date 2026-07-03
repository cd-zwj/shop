package com.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.dto.ExchangeProductDTO;
import com.payment.dto.PointsRuleDTO;
import com.payment.entity.ExchangeProduct;
import com.payment.entity.PointsLog;
import com.payment.entity.PointsRule;

import java.math.BigDecimal;
import java.util.List;

/**
 * 积分服务接口。
 *
 * <p>提供用户积分的全生命周期管理能力，包括积分规则配置、积分发放与扣减、
 * 积分余额查询、积分明细查看、积分兑换商品管理以及退款积分回退等。
 * 积分规则按租户维度独立配置。</p>
 */
public interface PointsService {

    /**
     * 获取商家积分规则。
     *
     * @param tenantId 租户ID
     * @return 积分规则实体，不存在时返回null
     */
    PointsRule getPointsRule(Long tenantId);

    /**
     * 设置商家积分规则。
     *
     * @param dto 积分规则DTO，包含积分兑换比例、有效期等配置
     * @throws com.payment.common.exception.BusinessException 当参数校验失败时抛出
     */
    void setPointsRule(PointsRuleDTO dto);

    /**
     * 根据订单金额计算应发放的积分数量。
     *
     * @param amount   订单金额
     * @param tenantId 租户ID（用于查询该租户的积分兑换比例）
     * @return 计算得出的积分数量
     */
    Integer calculatePoints(BigDecimal amount, Long tenantId);

    /**
     * 发放积分给用户。
     *
     * @param userId  用户ID
     * @param points  积分数量（正整数）
     * @param reason  发放原因说明
     * @param orderNo 关联订单号（可空）
     */
    void grantPoints(Long userId, Integer points, String reason, String orderNo);

    /**
     * 扣减用户积分。
     *
     * @param userId 用户ID
     * @param points 扣减积分数量（正整数）
     * @param reason 扣减原因说明
     * @throws com.payment.common.exception.BusinessException 当积分余额不足时抛出
     */
    void deductPoints(Long userId, Integer points, String reason);

    /**
     * 查询用户积分余额。
     *
     * @param userId   用户ID
     * @param tenantId 租户ID
     * @return 积分余额
     */
    Integer getUserPoints(Long userId, Long tenantId);

    /**
     * 分页查询用户积分明细（收支记录）。
     *
     * @param userId   用户ID
     * @param tenantId 租户ID
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 积分明细分页数据
     */
    Page<PointsLog> listPointsLogs(Long userId, Long tenantId, Integer pageNum, Integer pageSize);

    /**
     * 使用积分兑换商品。
     *
     * <p>扣减用户积分并生成兑换订单，积分不足时抛出异常。</p>
     *
     * @param userId            用户ID
     * @param exchangeProductId 兑换商品ID
     * @return 兑换订单号
     * @throws com.payment.common.exception.BusinessException 当积分不足或兑换商品不存在时抛出
     */
    String exchangeProduct(Long userId, Long exchangeProductId);

    /**
     * 设置积分兑换商品。
     *
     * @param dto 兑换商品DTO，包含商品名称、所需积分、库存等
     */
    void setExchangeProduct(ExchangeProductDTO dto);

    /**
     * 查询积分兑换商品列表。
     *
     * @param tenantId 租户ID
     * @return 兑换商品列表
     */
    List<ExchangeProduct> listExchangeProducts(Long tenantId);

    /**
     * 更新积分兑换商品信息。
     *
     * @param id  兑换商品ID
     * @param dto 更新后的兑换商品数据
     */
    void updateExchangeProduct(Long id, ExchangeProductDTO dto);

    /**
     * 删除积分兑换商品。
     *
     * @param id 兑换商品ID
     */
    void deleteExchangeProduct(Long id);

    /**
     * 退款时回退积分（积分兑换商品退款场景调用）。
     *
     * @param userId   用户ID
     * @param tenantId 租户ID
     * @param points   回退积分数量（正整数）
     * @param orderNo  关联订单号
     * @param reason   回退原因
     */
    void refundPoints(Long userId, Long tenantId, Integer points, String orderNo, String reason);
}
