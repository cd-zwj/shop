package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payment.common.BusinessException;
import com.payment.entity.ProductCategory;
import com.payment.mapper.ProductCategoryMapper;
import com.payment.service.ProductCategoryService;
import com.payment.vo.ProductCategoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商品分类服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCategoryServiceImpl
        extends ServiceImpl<ProductCategoryMapper, ProductCategory>
        implements ProductCategoryService {

    @Override
    public List<ProductCategoryVO> listTreeByTenant(Long tenantId) {
        LambdaQueryWrapper<ProductCategory> wrapper = new LambdaQueryWrapper<ProductCategory>()
                .eq(tenantId != null, ProductCategory::getTenantId, tenantId)
                .isNull(tenantId == null, ProductCategory::getTenantId)
                .eq(ProductCategory::getDeleted, 0)
                .orderByAsc(ProductCategory::getSortOrder);

        List<ProductCategoryVO> flatList = baseMapper.selectList(wrapper)
                .stream()
                .map(ProductCategoryVO::from)
                .toList();

        return buildTree(flatList, 0L);
    }

    @Override
    public ProductCategoryVO create(ProductCategory category) {
        category.setDeleted(0);
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());
        if (category.getStatus() == null) {
            category.setStatus(1);
        }
        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
        baseMapper.insert(category);
        return ProductCategoryVO.from(baseMapper.selectById(category.getId()));
    }

    @Override
    public ProductCategoryVO update(Long id, ProductCategory category) {
        ProductCategory existing = baseMapper.selectById(id);
        if (existing == null || existing.getDeleted() == 1) {
            throw new BusinessException("分类不存在");
        }
        if (category.getName() != null) {
            existing.setName(category.getName());
        }
        if (category.getParentId() != null) {
            existing.setParentId(category.getParentId());
        }
        if (category.getSortOrder() != null) {
            existing.setSortOrder(category.getSortOrder());
        }
        if (category.getIcon() != null) {
            existing.setIcon(category.getIcon());
        }
        if (category.getStatus() != null) {
            existing.setStatus(category.getStatus());
        }
        existing.setUpdateTime(LocalDateTime.now());
        baseMapper.updateById(existing);
        return ProductCategoryVO.from(baseMapper.selectById(id));
    }

    @Override
    public void delete(Long id) {
        ProductCategory existing = baseMapper.selectById(id);
        if (existing == null || existing.getDeleted() == 1) {
            throw new BusinessException("分类不存在");
        }
        existing.setDeleted(1);
        existing.setStatus(0);
        existing.setUpdateTime(LocalDateTime.now());
        baseMapper.updateById(existing);
    }

    private List<ProductCategoryVO> buildTree(List<ProductCategoryVO> flatList, Long parentId) {
        Map<Long, List<ProductCategoryVO>> grouped = flatList.stream()
                .collect(Collectors.groupingBy(vo -> vo.getParentId() == null ? 0L : vo.getParentId()));

        List<ProductCategoryVO> roots = grouped.getOrDefault(parentId, new ArrayList<>());
        for (ProductCategoryVO root : roots) {
            root.setChildren(grouped.getOrDefault(root.getId(), new ArrayList<>()));
        }
        roots.sort(Comparator.comparingInt(vo -> vo.getSortOrder() == null ? 0 : vo.getSortOrder()));
        return roots;
    }
}
