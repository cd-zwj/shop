package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.BusinessException;
import com.payment.dto.CardKeyDeliveryDTO;
import com.payment.dto.V1MerchantCardKeySummaryVO;
import com.payment.dto.V1MerchantCardKeyUploadDTO;
import com.payment.dto.V1MerchantCardKeyVO;
import com.payment.entity.CardKeyPool;
import com.payment.entity.Product;
import com.payment.enums.CardKeyStatusEnum;
import com.payment.enums.ProductTypeEnum;
import com.payment.mapper.CardKeyPoolMapper;
import com.payment.mapper.ProductMapper;
import com.payment.service.CardKeyPoolService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 卡密池管理服务实现类。
 * <p>
 * 负责虚拟商品（游戏卡密、礼品卡等）的卡密库存全生命周期管理，包括：
 * <ul>
 *   <li>商户批量上传卡密到库存池</li>
 *   <li>分页查询卡密列表及状态汇总</li>
 *   <li>订单交付时以 CAS 乐观方式锁定卡密，保障并发安全</li>
 *   <li>退款或取消时归还已使用的卡密</li>
 * </ul>
 * </p>
 */
@Service
@RequiredArgsConstructor
public class CardKeyPoolServiceImpl implements CardKeyPoolService {

    /** 锁定卡密时的最大重试次数，用于处理 CAS 并发冲突 */
    private static final int MAX_LOCK_RETRY = 3;

    private final CardKeyPoolMapper cardKeyPoolMapper;
    private final ProductMapper productMapper;
    private final V1MerchantSupportService v1MerchantSupportService;

    /**
     * 分页查询商户某商品下的卡密列表。
     *
     * @param tenantId       租户ID
     * @param platformUserId 当前操作用户ID，用于校验商户员工身份
     * @param productId      商品ID，必须为卡密类型商品
     * @param current        当前页码
     * @param size           每页条数
     * @param status         可选状态筛选（AVAILABLE / USED / RETURNED / DISABLED）
     * @return 分页后的卡密视图对象
     */
    @Override
    public Page<V1MerchantCardKeyVO> listMerchantCardKeys(Long tenantId, Long platformUserId, Long productId,
                                                          Integer current, Integer size, String status) {
        v1MerchantSupportService.requireEmployee(tenantId, platformUserId);
        requireCardKeyProduct(tenantId, productId);

        Page<CardKeyPool> page = cardKeyPoolMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<CardKeyPool>()
                .eq(CardKeyPool::getTenantId, tenantId)
                .eq(CardKeyPool::getProductId, productId)
                .eq(CardKeyPool::getDeleted, 0)
                .eq(status != null && !status.isBlank(), CardKeyPool::getStatus, normalizeStatus(status))
                .orderByDesc(CardKeyPool::getCreateTime));

        Page<V1MerchantCardKeyVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return result;
    }

    @Override
    public V1MerchantCardKeySummaryVO getMerchantSummary(Long tenantId, Long platformUserId, Long productId) {
        v1MerchantSupportService.requireEmployee(tenantId, platformUserId);
        requireCardKeyProduct(tenantId, productId);
        return buildSummary(tenantId, productId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public V1MerchantCardKeySummaryVO uploadMerchantCardKeys(Long tenantId, Long platformUserId, Long productId,
                                                             V1MerchantCardKeyUploadDTO dto) {
        v1MerchantSupportService.requireEmployee(tenantId, platformUserId);
        requireCardKeyProduct(tenantId, productId);

        List<String> codes = normalizeCodes(dto == null ? null : dto.getCodes());
        if (codes.isEmpty()) {
            throw new BusinessException("卡密列表不能为空");
        }
        Set<String> existingCodes = cardKeyPoolMapper.selectList(new LambdaQueryWrapper<CardKeyPool>()
                        .eq(CardKeyPool::getTenantId, tenantId)
                        .eq(CardKeyPool::getProductId, productId)
                        .eq(CardKeyPool::getDeleted, 0)
                        .in(CardKeyPool::getCardCode, codes))
                .stream()
                .map(CardKeyPool::getCardCode)
                .collect(java.util.stream.Collectors.toSet());
        if (!existingCodes.isEmpty()) {
            throw new BusinessException("卡密已存在: " + existingCodes.iterator().next());
        }

        LocalDateTime now = LocalDateTime.now();
        for (String code : codes) {
            CardKeyPool cardKey = new CardKeyPool();
            cardKey.setTenantId(tenantId);
            cardKey.setProductId(productId);
            cardKey.setCardCode(code);
            cardKey.setStatus(CardKeyStatusEnum.AVAILABLE.name());
            cardKey.setDeleted(0);
            cardKey.setCreateTime(now);
            cardKey.setUpdateTime(now);
            cardKeyPoolMapper.insert(cardKey);
        }
        return buildSummary(tenantId, productId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CardKeyDeliveryDTO lockForDelivery(Long tenantId, Long productId, String orderNo, Long orderItemId) {
        requireCardKeyProduct(tenantId, productId);
        for (int i = 0; i < MAX_LOCK_RETRY; i++) {
            CardKeyPool candidate = cardKeyPoolMapper.selectOne(new LambdaQueryWrapper<CardKeyPool>()
                    .eq(CardKeyPool::getTenantId, tenantId)
                    .eq(CardKeyPool::getProductId, productId)
                    .eq(CardKeyPool::getStatus, CardKeyStatusEnum.AVAILABLE.name())
                    .eq(CardKeyPool::getDeleted, 0)
                    .orderByAsc(CardKeyPool::getId)
                    .last("LIMIT 1"));
            if (candidate == null) {
                throw new BusinessException("卡密库存不足");
            }

            LocalDateTime now = LocalDateTime.now();
            int updated = cardKeyPoolMapper.update(null, new LambdaUpdateWrapper<CardKeyPool>()
                    .eq(CardKeyPool::getId, candidate.getId())
                    .eq(CardKeyPool::getStatus, CardKeyStatusEnum.AVAILABLE.name())
                    .set(CardKeyPool::getStatus, CardKeyStatusEnum.USED.name())
                    .set(CardKeyPool::getOrderNo, orderNo)
                    .set(CardKeyPool::getOrderItemId, orderItemId)
                    .set(CardKeyPool::getUsedTime, now)
                    .set(CardKeyPool::getUpdateTime, now));
            if (updated > 0) {
                return new CardKeyDeliveryDTO(candidate.getId(), candidate.getCardCode());
            }
        }
        throw new BusinessException("卡密库存锁定失败，请重试");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnByOrderItem(Long tenantId, Long orderItemId, String reason) {
        if (orderItemId == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        cardKeyPoolMapper.update(null, new LambdaUpdateWrapper<CardKeyPool>()
                .eq(CardKeyPool::getTenantId, tenantId)
                .eq(CardKeyPool::getOrderItemId, orderItemId)
                .eq(CardKeyPool::getStatus, CardKeyStatusEnum.USED.name())
                .set(CardKeyPool::getStatus, CardKeyStatusEnum.RETURNED.name())
                .set(CardKeyPool::getReturnedTime, now)
                .set(CardKeyPool::getReturnReason, reason)
                .set(CardKeyPool::getUpdateTime, now));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnByCardKeyId(Long tenantId, Long cardKeyId, String reason) {
        if (cardKeyId == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        cardKeyPoolMapper.update(null, new LambdaUpdateWrapper<CardKeyPool>()
                .eq(CardKeyPool::getTenantId, tenantId)
                .eq(CardKeyPool::getId, cardKeyId)
                .eq(CardKeyPool::getStatus, CardKeyStatusEnum.USED.name())
                .set(CardKeyPool::getStatus, CardKeyStatusEnum.RETURNED.name())
                .set(CardKeyPool::getReturnedTime, now)
                .set(CardKeyPool::getReturnReason, reason)
                .set(CardKeyPool::getUpdateTime, now));
    }

    private Product requireCardKeyProduct(Long tenantId, Long productId) {
        Product product = productMapper.selectOne(new LambdaQueryWrapper<Product>()
                .eq(Product::getId, productId)
                .eq(Product::getTenantId, tenantId)
                .eq(Product::getDeleted, 0));
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (!ProductTypeEnum.CARD_KEY.name().equals(product.getProductType())) {
            throw new BusinessException("该商品不是卡密商品");
        }
        return product;
    }

    private List<String> normalizeCodes(List<String> rawCodes) {
        if (rawCodes == null) {
            return List.of();
        }
        Set<String> codes = new LinkedHashSet<>();
        for (String raw : rawCodes) {
            if (raw != null && !raw.trim().isEmpty()) {
                codes.add(raw.trim());
            }
        }
        return List.copyOf(codes);
    }

    private String normalizeStatus(String status) {
        try {
            return CardKeyStatusEnum.valueOf(status.trim().toUpperCase()).name();
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("不支持的卡密状态: " + status);
        }
    }

    private V1MerchantCardKeySummaryVO buildSummary(Long tenantId, Long productId) {
        List<CardKeyPool> rows = cardKeyPoolMapper.selectList(new LambdaQueryWrapper<CardKeyPool>()
                .eq(CardKeyPool::getTenantId, tenantId)
                .eq(CardKeyPool::getProductId, productId)
                .eq(CardKeyPool::getDeleted, 0));
        V1MerchantCardKeySummaryVO summary = new V1MerchantCardKeySummaryVO();
        summary.setProductId(productId);
        summary.setAvailableCount(count(rows, CardKeyStatusEnum.AVAILABLE));
        summary.setUsedCount(count(rows, CardKeyStatusEnum.USED));
        summary.setReturnedCount(count(rows, CardKeyStatusEnum.RETURNED));
        summary.setDisabledCount(count(rows, CardKeyStatusEnum.DISABLED));
        summary.setTotalCount(rows.size());
        return summary;
    }

    private Integer count(List<CardKeyPool> rows, CardKeyStatusEnum status) {
        return (int) rows.stream().filter(row -> status.name().equals(row.getStatus())).count();
    }

    private V1MerchantCardKeyVO toVO(CardKeyPool cardKey) {
        V1MerchantCardKeyVO vo = new V1MerchantCardKeyVO();
        vo.setId(cardKey.getId());
        vo.setTenantId(cardKey.getTenantId());
        vo.setProductId(cardKey.getProductId());
        vo.setCardCode(cardKey.getCardCode());
        vo.setStatus(cardKey.getStatus());
        vo.setOrderNo(cardKey.getOrderNo());
        vo.setOrderItemId(cardKey.getOrderItemId());
        vo.setUsedTime(cardKey.getUsedTime());
        vo.setReturnedTime(cardKey.getReturnedTime());
        vo.setReturnReason(cardKey.getReturnReason());
        vo.setCreateTime(cardKey.getCreateTime());
        vo.setUpdateTime(cardKey.getUpdateTime());
        return vo;
    }
}
