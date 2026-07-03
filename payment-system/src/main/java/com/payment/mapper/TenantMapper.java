package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.dto.AppCatalogSearchTenantVO;
import com.payment.entity.Tenant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

/**
 * 租户表数据访问接口，提供租户信息的增删改查操作。
 * <p>对应实体表：{@link com.payment.entity.Tenant}</p>
 * <p>包含商户目录搜索分页查询等自定义方法。</p>
 */
@Mapper
public interface TenantMapper extends BaseMapper<Tenant> {

    /**
     * 分页搜索商户目录，支持关键词、分类、区域、评分、距离等多维度筛选和排序。
     *
     * @param page             分页参数
     * @param keyword          搜索关键词（模糊匹配商户名称），可为 null
     * @param category         分类筛选，可为 null
     * @param region           区域筛选，可为 null
     * @param minRating        最低评分，可为 null
     * @param maxDistanceKm     最大距离（公里），可为 null
     * @param longitude        用户经度，可为 null
     * @param latitude         用户纬度，可为 null
     * @param sort             排序方式，可为 null
     * @param hasLocation      是否提供位置信息
     * @param hasDistanceFilter 是否启用距离过滤
     * @param sortByDistance    是否按距离排序
     * @param sortByRating     是否按评分排序
     * @return 商户目录搜索结果分页
     */

    @Select("<script>" +
            "SELECT id, tenant_code, name, address, contact, phone, status " +
            "FROM tenant WHERE status = 1 AND deleted = 0 " +
            "<if test='keyword != null'> AND name LIKE CONCAT('%', #{keyword}, '%') </if>" +
            "</script>")
    Page<AppCatalogSearchTenantVO> selectSearchTenantPage(
            Page<AppCatalogSearchTenantVO> page,
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("region") String region,
            @Param("minRating") BigDecimal minRating,
            @Param("maxDistanceKm") Integer maxDistanceKm,
            @Param("longitude") BigDecimal longitude,
            @Param("latitude") BigDecimal latitude,
            @Param("sort") String sort,
            @Param("hasLocation") boolean hasLocation,
            @Param("hasDistanceFilter") boolean hasDistanceFilter,
            @Param("sortByDistance") boolean sortByDistance,
            @Param("sortByRating") boolean sortByRating
    );
}

