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

@Service
@RequiredArgsConstructor
public class V1MerchantStoreServiceImpl implements V1MerchantStoreService {

    private static final String DEFAULT_STORE_TYPE = "DIRECT";

    private final StoreMapper storeMapper;
    private final V1MerchantSupportService v1MerchantSupportService;

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

    @Override
    public V1MerchantStoreVO getStore(Long tenantId, Long platformUserId, Long storeId) {
        v1MerchantSupportService.requireEmployee(tenantId, platformUserId);
        return toStoreVO(getTenantStore(tenantId, storeId));
    }

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
