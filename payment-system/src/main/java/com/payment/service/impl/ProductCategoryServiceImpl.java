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
 * 商品分类服务实现类，负责商品分类树的 CRUD 操作。
 * <p>支持按租户查询分类树、创建分类、更新分类及逻辑删除分类，
 * 并通过 {@link #buildTree} 将平铺列表组装为层级树形结构。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCategoryServiceImpl
        extends ServiceImpl<ProductCategoryMapper, ProductCategory>
        implements ProductCategoryService {

    /**
     * 查询指定租户的商品分类树。
     * <p>根据租户 ID 查询所有未删除的分类记录，按排序字段升序排列，
     * 然后将平铺列表组装为以 parentId=0 为根节点的树形结构返回。</p>
     *
     * @param tenantId 租户 ID，为 null 时查询平台级（无租户）分类
     * @return 商品分类树形结构列表
     */
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

    /**
     * 创建商品分类。
     * <p>初始化删除标记、创建/更新时间、状态和排序等默认值后插入数据库，
     * 再根据生成的主键重新查询并返回完整的分类 VO。</p>
     *
     * @param category 待创建的商品分类实体
     * @return 新创建的商品分类 VO 对象
     */
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

    /**
     * 更新商品分类信息。
     * <p>根据 ID 查询已存在的分类，若不存在或已逻辑删除则抛出异常。
     * 仅更新请求中非空的字段（部分更新语义），最后刷新更新时间并返回最新数据。</p>
     *
     * @param id       待更新的分类 ID
     * @param category 包含待更新字段的分类实体
     * @return 更新后的商品分类 VO 对象
     * @throws BusinessException 当分类不存在或已被删除时抛出
     */
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

    /**
     * 逻辑删除商品分类。
     * <p>根据 ID 查询已存在的分类，若不存在或已逻辑删除则抛出异常。
     * 将删除标记置为 1、状态置为 0，并更新修改时间。</p>
     *
     * @param id 待删除的分类 ID
     * @throws BusinessException 当分类不存在或已被删除时抛出
     */
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

    /**
     * 将平铺的分类列表组装为树形结构。
     * <p>按 parentId 分组后，以指定的 parentId 作为根节点，
     * 递归挂载子节点，最终按 sortOrder 升序排列根节点列表。</p>
     *
     * @param flatList 平铺的分类 VO 列表
     * @param parentId 根节点的父级 ID（通常为 0L）
     * @return 组装完成的树形分类列表
     */
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
