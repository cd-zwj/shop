package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.dto.AppCatalogProductSearchQueryDTO;
import com.payment.dto.AppCatalogSearchProductVO;
import com.payment.dto.AppCatalogSearchTenantVO;
import com.payment.dto.AppCatalogTenantSearchQueryDTO;
import com.payment.entity.Product;
import com.payment.entity.Tenant;
import com.payment.mapper.ProductMapper;
import com.payment.mapper.TenantMapper;
import com.payment.service.AppCatalogSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户端公开搜索服务实现。
 */
@Service
@RequiredArgsConstructor
public class AppCatalogSearchServiceImpl implements AppCatalogSearchService {

    private static final int DEFAULT_CURRENT = 1;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 50;
    private static final String ALL_CATEGORY = "全部分类";
    private static final String DISTANCE_PLACEHOLDER = "暂无距离";
    private static final String LIKE_ESCAPE_SQL = "\\\\";

    private final ProductMapper productMapper;
    private final TenantMapper tenantMapper;

    /**
     * 搜索用户端可见商品，并批量补齐商品所属商户名称。
     */
    @Override
    public Page<AppCatalogSearchProductVO> searchProducts(AppCatalogProductSearchQueryDTO query) {
        AppCatalogProductSearchQueryDTO safeQuery = query == null ? new AppCatalogProductSearchQueryDTO() : query;
        Page<Product> page = productMapper.selectPage(
                new Page<>(normalizeCurrent(safeQuery.getCurrent()), normalizeSize(safeQuery.getSize())),
                buildProductWrapper(safeQuery)
        );
        Map<Long, Tenant> tenantMap = loadTenantMap(page.getRecords());

        Page<AppCatalogSearchProductVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream()
                .map(product -> toProductVO(product, tenantMap.get(product.getTenantId())))
                .toList());
        return result;
    }

    /**
     * 搜索用户端可见商户；普通查询和门店维度查询都在数据库层分页。
     */
    @Override
    public Page<AppCatalogSearchTenantVO> searchTenants(AppCatalogTenantSearchQueryDTO query) {
        AppCatalogTenantSearchQueryDTO safeQuery = query == null ? new AppCatalogTenantSearchQueryDTO() : query;
        int current = normalizeCurrent(safeQuery.getCurrent());
        int size = normalizeSize(safeQuery.getSize());
        if (shouldUseStoreTenantSearch(safeQuery)) {
            return searchTenantsWithStorePage(safeQuery, current, size);
        }

        Page<Tenant> tenantPage = tenantMapper.selectPage(
                new Page<>(current, size),
                buildTenantWrapper(safeQuery)
        );
        Map<Long, Long> productCountByTenantId = batchCountProducts(tenantPage.getRecords().stream()
                .map(Tenant::getId)
                .filter(Objects::nonNull)
                .toList());

        Page<AppCatalogSearchTenantVO> result = new Page<>(tenantPage.getCurrent(), tenantPage.getSize(), tenantPage.getTotal());
        result.setRecords(tenantPage.getRecords().stream()
                .map(tenant -> toTenantVO(tenant, productCountByTenantId.getOrDefault(tenant.getId(), 0L)))
                .toList());
        return result;
    }

    private Page<AppCatalogSearchTenantVO> searchTenantsWithStorePage(
            AppCatalogTenantSearchQueryDTO query,
            int current,
            int size
    ) {
        String sort = normalizeText(query.getSort());
        boolean hasLocation = hasLocation(query);
        boolean hasDistanceFilter = normalizeMaxDistanceKm(query.getMaxDistanceKm()) != null && hasLocation;
        Page<AppCatalogSearchTenantVO> tenantPage = tenantMapper.selectSearchTenantPage(
                new Page<>(current, size),
                escapeLikeParam(normalizeText(query.getKeyword())),
                escapeLikeParam(normalizeCategory(query.getCategory())),
                escapeLikeParam(normalizeText(query.getRegion())),
                normalizeMinRating(query.getMinRating()),
                normalizeMaxDistanceKm(query.getMaxDistanceKm()),
                query.getLongitude(),
                query.getLatitude(),
                sort,
                hasLocation,
                hasDistanceFilter,
                "distance".equalsIgnoreCase(sort),
                "rating_desc".equalsIgnoreCase(sort)
        );
        Map<Long, Long> productCountByTenantId = batchCountProducts(tenantPage.getRecords().stream()
                .map(AppCatalogSearchTenantVO::getTenantId)
                .filter(Objects::nonNull)
                .toList());
        tenantPage.getRecords().forEach(record ->
                record.setProductCount(productCountByTenantId.getOrDefault(record.getTenantId(), 0L)));
        return tenantPage;
    }

    private LambdaQueryWrapper<Product> buildProductWrapper(AppCatalogProductSearchQueryDTO query) {
        String keyword = normalizeText(query.getKeyword());
        String category = normalizeCategory(query.getCategory());

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getDeleted, 0)
                .eq(Product::getStatus, 1)
                .eq(query.getTenantId() != null && query.getTenantId() > 0, Product::getTenantId, query.getTenantId())
                .eq(StringUtils.hasText(category), Product::getCategory, category);
        applyLikeAny(wrapper, keyword, List.of("name", "product_code", "description"));

        applyProductSort(wrapper, query.getSort());
        return wrapper;
    }

    private LambdaQueryWrapper<Tenant> buildTenantWrapper(AppCatalogTenantSearchQueryDTO query) {
        String keyword = normalizeText(query.getKeyword());

        LambdaQueryWrapper<Tenant> wrapper = new LambdaQueryWrapper<Tenant>()
                .eq(Tenant::getDeleted, 0)
                .eq(Tenant::getStatus, 1);
        applyLikeAny(wrapper, keyword, List.of("name", "address", "contact", "phone"));

        if (!hasStoreSort(query.getSort())) {
            applyTenantSort(wrapper, query.getSort());
        }
        return wrapper;
    }

    private void applyProductSort(LambdaQueryWrapper<Product> wrapper, String sort) {
        String normalizedSort = normalizeText(sort);
        if ("price_asc".equalsIgnoreCase(normalizedSort)) {
            wrapper.orderByAsc(Product::getPrice);
            return;
        }
        if ("price_desc".equalsIgnoreCase(normalizedSort)) {
            wrapper.orderByDesc(Product::getPrice);
            return;
        }
        if ("name_asc".equalsIgnoreCase(normalizedSort)) {
            wrapper.orderByAsc(Product::getName);
            return;
        }
        wrapper.orderByDesc(Product::getCreateTime);
    }

    private void applyTenantSort(LambdaQueryWrapper<Tenant> wrapper, String sort) {
        String normalizedSort = normalizeText(sort);
        if ("name_asc".equalsIgnoreCase(normalizedSort)) {
            wrapper.orderByAsc(Tenant::getName);
            return;
        }
        wrapper.orderByDesc(Tenant::getCreateTime);
    }

    private Map<Long, Tenant> loadTenantMap(List<Product> products) {
        List<Long> tenantIds = products.stream()
                .map(Product::getTenantId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (tenantIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return tenantMapper.selectBatchIds(tenantIds).stream()
                .collect(Collectors.toMap(Tenant::getId, Function.identity(), (left, right) -> left));
    }

    private AppCatalogSearchProductVO toProductVO(Product product, Tenant tenant) {
        AppCatalogSearchProductVO vo = new AppCatalogSearchProductVO();
        vo.setId(product.getId());
        vo.setProductId(product.getId());
        vo.setTenantId(product.getTenantId());
        vo.setTenantName(tenant == null ? null : tenant.getName());
        vo.setTitle(product.getName());
        vo.setName(product.getName());
        vo.setSubtitle(product.getDescription());
        vo.setCategory(product.getCategory());
        vo.setPrice(product.getPrice());
        vo.setRating(null);
        vo.setDistanceLabel(DISTANCE_PLACEHOLDER);
        vo.setCoverImage(product.getImageUrl());
        vo.setStatus(product.getStatus());
        return vo;
    }

    private AppCatalogSearchTenantVO toTenantVO(Tenant tenant, Long productCount) {
        AppCatalogSearchTenantVO vo = new AppCatalogSearchTenantVO();
        vo.setId(tenant.getId());
        vo.setTenantId(tenant.getId());
        vo.setTitle(tenant.getName());
        vo.setName(tenant.getName());
        vo.setSubtitle(tenant.getAddress());
        vo.setAddress(tenant.getAddress());
        vo.setContact(tenant.getContact());
        vo.setPhone(tenant.getPhone());
        vo.setCategory(null);
        vo.setRating(null);
        vo.setDistanceLabel(DISTANCE_PLACEHOLDER);
        vo.setProductCount(productCount == null ? 0L : productCount);
        vo.setStatus(tenant.getStatus());
        return vo;
    }

    /**
     * 批量统计每个商户的上架商品数，避免搜索结果页逐条触发 count 查询。
     */
    private Map<Long, Long> batchCountProducts(List<Long> tenantIds) {
        List<Long> safeTenantIds = tenantIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (safeTenantIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return productMapper.selectMaps(new QueryWrapper<Product>()
                        .select("tenant_id", "COUNT(*) AS product_count")
                        .in("tenant_id", safeTenantIds)
                        .eq("deleted", 0)
                        .eq("status", 1)
                        .groupBy("tenant_id"))
                .stream()
                .collect(Collectors.toMap(
                        row -> toLong(row.get("tenant_id")),
                        row -> toLong(row.get("product_count")),
                        (left, right) -> left
                ));
    }

    private int normalizeCurrent(Integer current) {
        return current == null || current < 1 ? DEFAULT_CURRENT : current;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private String normalizeCategory(String category) {
        String normalized = normalizeText(category);
        if (!StringUtils.hasText(normalized) || ALL_CATEGORY.equals(normalized)) {
            return null;
        }
        return normalized;
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

    private boolean hasStoreSort(String sort) {
        String normalizedSort = normalizeText(sort);
        return "distance".equalsIgnoreCase(normalizedSort) || "rating_desc".equalsIgnoreCase(normalizedSort);
    }

    private boolean shouldUseStoreTenantSearch(AppCatalogTenantSearchQueryDTO query) {
        return StringUtils.hasText(normalizeCategory(query.getCategory()))
                || StringUtils.hasText(normalizeText(query.getRegion()))
                || normalizeMinRating(query.getMinRating()) != null
                || (normalizeMaxDistanceKm(query.getMaxDistanceKm()) != null && hasLocation(query))
                || hasStoreSort(query.getSort());
    }

    private boolean hasLocation(AppCatalogTenantSearchQueryDTO query) {
        return query.getLatitude() != null && query.getLongitude() != null;
    }

    private BigDecimal normalizeMinRating(BigDecimal minRating) {
        if (minRating == null || minRating.compareTo(BigDecimal.ZERO) < 0) {
            return null;
        }
        return minRating.min(new BigDecimal("5.00"));
    }

    private Integer normalizeMaxDistanceKm(Integer maxDistanceKm) {
        return maxDistanceKm == null || maxDistanceKm < 1 ? null : maxDistanceKm;
    }

    /**
     * 生成带 ESCAPE 的模糊匹配条件，确保用户输入的 %、_ 只按普通字符匹配。
     */
    private <T> void applyLikeAny(LambdaQueryWrapper<T> wrapper, String keyword, List<String> columns) {
        String normalizedKeyword = normalizeText(keyword);
        if (!StringUtils.hasText(normalizedKeyword)) {
            return;
        }
        String escapedKeyword = escapeLike(normalizedKeyword);
        wrapper.and(group -> {
            for (int i = 0; i < columns.size(); i++) {
                if (i > 0) {
                    group.or();
                }
                group.apply(columns.get(i) + " LIKE CONCAT('%', {0}, '%') ESCAPE '" + LIKE_ESCAPE_SQL + "'", escapedKeyword);
            }
        });
    }

    /**
     * 转义 SQL LIKE 通配符，配合参数化 apply 使用，避免扩大匹配范围。
     */
    private String escapeLike(String value) {
        return value.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private String escapeLikeParam(String value) {
        return StringUtils.hasText(value) ? escapeLike(value) : null;
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

}
