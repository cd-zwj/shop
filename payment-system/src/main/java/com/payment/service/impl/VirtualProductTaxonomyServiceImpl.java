package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.common.BusinessException;
import com.payment.constant.MerchantPermission;
import com.payment.dto.VirtualProductCategoryUpsertDTO;
import com.payment.dto.VirtualProductCategoryVO;
import com.payment.dto.VirtualProductTypeUpsertDTO;
import com.payment.dto.VirtualProductTypeVO;
import com.payment.entity.VirtualProductCategory;
import com.payment.entity.VirtualProductType;
import com.payment.enums.ProductTypeEnum;
import com.payment.mapper.VirtualProductCategoryMapper;
import com.payment.mapper.VirtualProductTypeMapper;
import com.payment.service.VirtualProductTaxonomyService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VirtualProductTaxonomyServiceImpl implements VirtualProductTaxonomyService {

    private static final EnumSet<ProductTypeEnum> ALLOWED_DELIVERY_STRATEGIES = EnumSet.of(
            ProductTypeEnum.VIRTUAL,
            ProductTypeEnum.CARD_KEY,
            ProductTypeEnum.SERVICE,
            ProductTypeEnum.SUBSCRIPTION
    );

    private final VirtualProductTypeMapper typeMapper;
    private final VirtualProductCategoryMapper categoryMapper;
    private final V1MerchantSupportService v1MerchantSupportService;

    @Override
    public List<VirtualProductTypeVO> listTypes(Long tenantId, Long platformUserId, Integer status) {
        v1MerchantSupportService.requirePermission(tenantId, platformUserId, MerchantPermission.PRODUCT_MANAGE);
        return typeMapper.selectList(new LambdaQueryWrapper<VirtualProductType>()
                        .eq(VirtualProductType::getTenantId, tenantId)
                        .eq(VirtualProductType::getDeleted, 0)
                        .eq(status != null, VirtualProductType::getStatus, status)
                        .orderByAsc(VirtualProductType::getSortOrder)
                        .orderByDesc(VirtualProductType::getCreateTime))
                .stream()
                .map(this::toTypeVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VirtualProductTypeVO createType(Long tenantId, Long platformUserId, VirtualProductTypeUpsertDTO dto) {
        v1MerchantSupportService.requirePermission(tenantId, platformUserId, MerchantPermission.PRODUCT_MANAGE);
        String typeCode = normalizeRequired(dto.getTypeCode(), "类型编码不能为空");
        ensureTypeCodeAvailable(tenantId, typeCode, null);

        VirtualProductType type = new VirtualProductType();
        type.setTenantId(tenantId);
        type.setTypeCode(typeCode);
        applyType(type, dto);
        type.setDeleted(0);
        typeMapper.insert(type);
        return toTypeVO(typeMapper.selectById(type.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VirtualProductTypeVO updateType(Long tenantId, Long platformUserId, Long id, VirtualProductTypeUpsertDTO dto) {
        v1MerchantSupportService.requirePermission(tenantId, platformUserId, MerchantPermission.PRODUCT_MANAGE);
        VirtualProductType type = getTenantType(tenantId, id, false);
        String typeCode = normalizeRequired(dto.getTypeCode(), "类型编码不能为空");
        ensureTypeCodeAvailable(tenantId, typeCode, id);

        type.setTypeCode(typeCode);
        applyType(type, dto);
        typeMapper.updateById(type);
        return toTypeVO(typeMapper.selectById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteType(Long tenantId, Long platformUserId, Long id) {
        v1MerchantSupportService.requirePermission(tenantId, platformUserId, MerchantPermission.PRODUCT_MANAGE);
        VirtualProductType type = getTenantType(tenantId, id, false);
        type.setDeleted(1);
        type.setStatus(0);
        typeMapper.updateById(type);
    }

    @Override
    public List<VirtualProductCategoryVO> listCategories(Long tenantId, Long platformUserId, Long typeId, Integer status) {
        v1MerchantSupportService.requirePermission(tenantId, platformUserId, MerchantPermission.PRODUCT_MANAGE);
        return categoryMapper.selectList(new LambdaQueryWrapper<VirtualProductCategory>()
                        .eq(VirtualProductCategory::getTenantId, tenantId)
                        .eq(VirtualProductCategory::getDeleted, 0)
                        .eq(typeId != null, VirtualProductCategory::getTypeId, typeId)
                        .eq(status != null, VirtualProductCategory::getStatus, status)
                        .orderByAsc(VirtualProductCategory::getSortOrder)
                        .orderByDesc(VirtualProductCategory::getCreateTime))
                .stream()
                .map(this::toCategoryVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VirtualProductCategoryVO createCategory(Long tenantId, Long platformUserId, VirtualProductCategoryUpsertDTO dto) {
        v1MerchantSupportService.requirePermission(tenantId, platformUserId, MerchantPermission.PRODUCT_MANAGE);
        getTenantType(tenantId, dto.getTypeId(), false);
        validateParentCategory(tenantId, dto.getTypeId(), dto.getParentId(), null);
        String categoryCode = normalizeRequired(dto.getCategoryCode(), "分类编码不能为空");
        ensureCategoryCodeAvailable(tenantId, categoryCode, null);

        VirtualProductCategory category = new VirtualProductCategory();
        category.setTenantId(tenantId);
        category.setCategoryCode(categoryCode);
        applyCategory(category, dto);
        category.setDeleted(0);
        categoryMapper.insert(category);
        return toCategoryVO(categoryMapper.selectById(category.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VirtualProductCategoryVO updateCategory(Long tenantId, Long platformUserId, Long id, VirtualProductCategoryUpsertDTO dto) {
        v1MerchantSupportService.requirePermission(tenantId, platformUserId, MerchantPermission.PRODUCT_MANAGE);
        VirtualProductCategory category = getTenantCategory(tenantId, id, false);
        getTenantType(tenantId, dto.getTypeId(), false);
        validateParentCategory(tenantId, dto.getTypeId(), dto.getParentId(), id);
        String categoryCode = normalizeRequired(dto.getCategoryCode(), "分类编码不能为空");
        ensureCategoryCodeAvailable(tenantId, categoryCode, id);

        category.setCategoryCode(categoryCode);
        applyCategory(category, dto);
        categoryMapper.updateById(category);
        return toCategoryVO(categoryMapper.selectById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long tenantId, Long platformUserId, Long id) {
        v1MerchantSupportService.requirePermission(tenantId, platformUserId, MerchantPermission.PRODUCT_MANAGE);
        VirtualProductCategory category = getTenantCategory(tenantId, id, false);
        category.setDeleted(1);
        category.setStatus(0);
        categoryMapper.updateById(category);
    }

    private void applyType(VirtualProductType type, VirtualProductTypeUpsertDTO dto) {
        type.setTypeName(normalizeRequired(dto.getTypeName(), "类型名称不能为空"));
        type.setDeliveryStrategy(resolveDeliveryStrategy(dto.getDeliveryStrategy()).name());
        type.setDescription(dto.getDescription());
        type.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        type.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
    }

    private void applyCategory(VirtualProductCategory category, VirtualProductCategoryUpsertDTO dto) {
        category.setTypeId(dto.getTypeId());
        category.setCategoryName(normalizeRequired(dto.getCategoryName(), "分类名称不能为空"));
        category.setParentId(dto.getParentId() == null ? 0L : dto.getParentId());
        category.setDescription(dto.getDescription());
        category.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        category.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
    }

    private ProductTypeEnum resolveDeliveryStrategy(String raw) {
        ProductTypeEnum strategy;
        try {
            strategy = ProductTypeEnum.valueOf(normalizeRequired(raw, "交付策略不能为空").toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("不支持的交付策略: " + raw);
        }
        if (!ALLOWED_DELIVERY_STRATEGIES.contains(strategy)) {
            throw new BusinessException("交付策略只能是 VIRTUAL/CARD_KEY/SERVICE/SUBSCRIPTION");
        }
        return strategy;
    }

    private VirtualProductType getTenantType(Long tenantId, Long typeId, boolean activeOnly) {
        if (typeId == null) {
            throw new BusinessException("虚拟商品类型不能为空");
        }
        VirtualProductType type = typeMapper.selectOne(new LambdaQueryWrapper<VirtualProductType>()
                .eq(VirtualProductType::getId, typeId)
                .eq(VirtualProductType::getTenantId, tenantId)
                .eq(VirtualProductType::getDeleted, 0)
                .eq(activeOnly, VirtualProductType::getStatus, 1));
        if (type == null) {
            throw new BusinessException(activeOnly ? "虚拟商品类型不存在或已停用" : "虚拟商品类型不存在");
        }
        return type;
    }

    private VirtualProductCategory getTenantCategory(Long tenantId, Long categoryId, boolean activeOnly) {
        if (categoryId == null) {
            throw new BusinessException("虚拟商品分类不能为空");
        }
        VirtualProductCategory category = categoryMapper.selectOne(new LambdaQueryWrapper<VirtualProductCategory>()
                .eq(VirtualProductCategory::getId, categoryId)
                .eq(VirtualProductCategory::getTenantId, tenantId)
                .eq(VirtualProductCategory::getDeleted, 0)
                .eq(activeOnly, VirtualProductCategory::getStatus, 1));
        if (category == null) {
            throw new BusinessException(activeOnly ? "虚拟商品分类不存在或已停用" : "虚拟商品分类不存在");
        }
        return category;
    }

    private void validateParentCategory(Long tenantId, Long typeId, Long parentId, Long selfId) {
        if (parentId == null || parentId == 0L) {
            return;
        }
        if (parentId.equals(selfId)) {
            throw new BusinessException("父分类不能选择自身");
        }
        VirtualProductCategory parent = getTenantCategory(tenantId, parentId, false);
        if (!parent.getTypeId().equals(typeId)) {
            throw new BusinessException("父分类必须属于同一虚拟商品类型");
        }
    }

    private void ensureTypeCodeAvailable(Long tenantId, String typeCode, Long excludeId) {
        VirtualProductType existing = typeMapper.selectOne(new LambdaQueryWrapper<VirtualProductType>()
                .eq(VirtualProductType::getTenantId, tenantId)
                .eq(VirtualProductType::getTypeCode, typeCode)
                .eq(VirtualProductType::getDeleted, 0)
                .ne(excludeId != null, VirtualProductType::getId, excludeId));
        if (existing != null) {
            throw new BusinessException("虚拟商品类型编码已存在");
        }
    }

    private void ensureCategoryCodeAvailable(Long tenantId, String categoryCode, Long excludeId) {
        VirtualProductCategory existing = categoryMapper.selectOne(new LambdaQueryWrapper<VirtualProductCategory>()
                .eq(VirtualProductCategory::getTenantId, tenantId)
                .eq(VirtualProductCategory::getCategoryCode, categoryCode)
                .eq(VirtualProductCategory::getDeleted, 0)
                .ne(excludeId != null, VirtualProductCategory::getId, excludeId));
        if (existing != null) {
            throw new BusinessException("虚拟商品分类编码已存在");
        }
    }

    private String normalizeRequired(String raw, String message) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException(message);
        }
        return raw.trim();
    }

    private VirtualProductTypeVO toTypeVO(VirtualProductType type) {
        VirtualProductTypeVO vo = new VirtualProductTypeVO();
        BeanUtils.copyProperties(type, vo);
        return vo;
    }

    private VirtualProductCategoryVO toCategoryVO(VirtualProductCategory category) {
        VirtualProductCategoryVO vo = new VirtualProductCategoryVO();
        BeanUtils.copyProperties(category, vo);
        return vo;
    }
}
