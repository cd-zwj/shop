package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.BusinessException;
import com.payment.dto.V1MerchantStoreUpsertDTO;
import com.payment.dto.V1MerchantStoreVO;
import com.payment.entity.Store;
import com.payment.mapper.StoreMapper;
import com.payment.service.V1MerchantStoreService;
import com.payment.util.BizNoGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 商户端门店管理服务实现类。
 * <p>提供门店的增删改查、状态变更等操作，
 * 所有操作前均校验当前用户是否为该租户的有效员工。</p>
 */
@Service
@RequiredArgsConstructor
public class V1MerchantStoreServiceImpl implements V1MerchantStoreService {

    private static final String DEFAULT_STORE_TYPE = "DIRECT";

    private final StoreMapper storeMapper;
    private final V1MerchantSupportService v1MerchantSupportService;

    /**
     * 分页查询当前租户的门店列表，支持关键字搜索和状态过滤。
     *
     * @param tenantId       租户ID
     * @param platformUserId 平台用户ID
     * @param current        当前页码
     * @param size           每页条数
     * @param keyword        搜索关键字（匹配门店名称、编号、联系电话）
     * @param status         门店状态过滤，null表示不过滤
     * @return 分页结果
     */
    @Override
    public Page<V1MerchantStoreVO> listStores(Long tenantId, Long platformUserId, Integer current, Integer size,
                                              String keyword, Integer status) {
        v1MerchantSupportService.requireEmployee(tenantId, platformUserId);

        Page<Store> page = storeMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<Store>()
                .eq(Store::getTenantId, tenantId)
                .eq(Store::getDeleted, 0)
                .eq(status != null, Store::getStatus, status)
                .and(StringUtils.hasText(keyword), q -> q.like(Store::getStoreName, keyword)
                        .or()
                        .like(Store::getStoreNo, keyword)
                        .or()
                        .like(Store::getContactPhone, keyword))
                .orderByDesc(Store::getCreateTime));

        Page<V1MerchantStoreVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toStoreVO).toList());
        return result;
    }

    /**
     * 获取单个门店详情。
     *
     * @param tenantId       租户ID
     * @param platformUserId 平台用户ID
     * @param storeId        门店ID
     * @return 门店详情
     * @throws BusinessException 门店不存在或不属于当前租户时抛出异常
     */
    @Override
    public V1MerchantStoreVO getStore(Long tenantId, Long platformUserId, Long storeId) {
        v1MerchantSupportService.requireEmployee(tenantId, platformUserId);
        return toStoreVO(getTenantStore(tenantId, storeId));
    }

    /**
     * 新增门店。门店编号可选，未指定时自动生成。编号在租户内唯一。
     *
     * @param tenantId       租户ID
     * @param platformUserId 平台用户ID
     * @param dto            门店创建参数
     * @return 新建门店详情
     * @throws BusinessException 门店编号重复时抛出异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public V1MerchantStoreVO createStore(Long tenantId, Long platformUserId, V1MerchantStoreUpsertDTO dto) {
        v1MerchantSupportService.requireEmployee(tenantId, platformUserId);

        String storeNo = resolveStoreNo(dto.getStoreNo());
        ensureStoreNoAvailable(tenantId, storeNo, null);

        Store store = new Store();
        store.setTenantId(tenantId);
        store.setStoreNo(storeNo);
        applyUpsert(store, dto);
        store.setDeleted(0);
        store.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        storeMapper.insert(store);
        return toStoreVO(storeMapper.selectById(store.getId()));
    }

    /**
     * 更新门店信息。若修改了门店编号，会校验编号在租户内的唯一性。
     *
     * @param tenantId       租户ID
     * @param platformUserId 平台用户ID
     * @param storeId        门店ID
     * @param dto            门店更新参数
     * @return 更新后的门店详情
     * @throws BusinessException 门店不存在或编号重复时抛出异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public V1MerchantStoreVO updateStore(Long tenantId, Long platformUserId, Long storeId, V1MerchantStoreUpsertDTO dto) {
        v1MerchantSupportService.requireEmployee(tenantId, platformUserId);

        Store store = getTenantStore(tenantId, storeId);
        String storeNo = resolveStoreNo(dto.getStoreNo(), store.getStoreNo());
        ensureStoreNoAvailable(tenantId, storeNo, storeId);

        store.setStoreNo(storeNo);
        applyUpsert(store, dto);
        if (dto.getStatus() != null) {
            store.setStatus(dto.getStatus());
        }
        storeMapper.updateById(store);
        return toStoreVO(storeMapper.selectById(storeId));
    }

    /**
     * 更新门店状态（启用/停用）。状态值只能为0（停用）或1（启用）。
     *
     * @param tenantId       租户ID
     * @param platformUserId 平台用户ID
     * @param storeId        门店ID
     * @param status         目标状态，0-停用 1-启用
     * @return 更新后的门店详情
     * @throws BusinessException 状态值非法或门店不存在时抛出异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public V1MerchantStoreVO updateStoreStatus(Long tenantId, Long platformUserId, Long storeId, Integer status) {
        v1MerchantSupportService.requireEmployee(tenantId, platformUserId);
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("状态只能为0或1");
        }

        Store store = getTenantStore(tenantId, storeId);
        store.setStatus(status);
        storeMapper.updateById(store);
        return toStoreVO(storeMapper.selectById(storeId));
    }

    /**
     * 删除门店（逻辑删除）。将门店标记为已删除并停用。
     *
     * @param tenantId       租户ID
     * @param platformUserId 平台用户ID
     * @param storeId        门店ID
     * @throws BusinessException 门店不存在时抛出异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStore(Long tenantId, Long platformUserId, Long storeId) {
        v1MerchantSupportService.requireEmployee(tenantId, platformUserId);
        Store store = getTenantStore(tenantId, storeId);
        store.setDeleted(1);
        store.setStatus(0);
        storeMapper.updateById(store);
    }

    private Store getTenantStore(Long tenantId, Long storeId) {
        Store store = storeMapper.selectOne(new LambdaQueryWrapper<Store>()
                .eq(Store::getId, storeId)
                .eq(Store::getTenantId, tenantId)
                .eq(Store::getDeleted, 0));
        if (store == null) {
            throw new BusinessException("门店不存在");
        }
        return store;
    }

    private void ensureStoreNoAvailable(Long tenantId, String storeNo, Long excludeStoreId) {
        Store existing = storeMapper.selectOne(new LambdaQueryWrapper<Store>()
                .eq(Store::getTenantId, tenantId)
                .eq(Store::getStoreNo, storeNo)
                .eq(Store::getDeleted, 0)
                .ne(excludeStoreId != null, Store::getId, excludeStoreId));
        if (existing != null) {
            throw new BusinessException("门店编号已存在");
        }
    }

    private void applyUpsert(Store store, V1MerchantStoreUpsertDTO dto) {
        store.setStoreName(dto.getStoreName());
        store.setStoreType(StringUtils.hasText(dto.getStoreType()) ? dto.getStoreType().trim() : DEFAULT_STORE_TYPE);
        store.setContactName(dto.getContactName());
        store.setContactPhone(dto.getContactPhone());
        store.setProvince(dto.getProvince());
        store.setCity(dto.getCity());
        store.setDistrict(dto.getDistrict());
        store.setAddress(dto.getAddress());
        store.setLongitude(dto.getLongitude());
        store.setLatitude(dto.getLatitude());
        store.setBusinessHours(dto.getBusinessHours());
        store.setServiceTags(dto.getServiceTags());
    }

    private String resolveStoreNo(String raw) {
        if (StringUtils.hasText(raw)) {
            return raw.trim();
        }
        return BizNoGenerator.generate("ST");
    }

    private String resolveStoreNo(String raw, String fallback) {
        if (StringUtils.hasText(raw)) {
            return raw.trim();
        }
        return fallback;
    }

    private V1MerchantStoreVO toStoreVO(Store store) {
        V1MerchantStoreVO vo = new V1MerchantStoreVO();
        vo.setId(store.getId());
        vo.setStoreNo(store.getStoreNo());
        vo.setTenantId(store.getTenantId());
        vo.setStoreName(store.getStoreName());
        vo.setStoreType(store.getStoreType());
        vo.setContactName(store.getContactName());
        vo.setContactPhone(store.getContactPhone());
        vo.setProvince(store.getProvince());
        vo.setCity(store.getCity());
        vo.setDistrict(store.getDistrict());
        vo.setAddress(store.getAddress());
        vo.setLongitude(store.getLongitude());
        vo.setLatitude(store.getLatitude());
        vo.setRating(store.getRating());
        vo.setBusinessHours(store.getBusinessHours());
        vo.setServiceTags(store.getServiceTags());
        vo.setStatus(store.getStatus());
        vo.setCreateTime(store.getCreateTime());
        vo.setUpdateTime(store.getUpdateTime());
        return vo;
    }
}
