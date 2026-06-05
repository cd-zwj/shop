package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.dto.AppCatalogSearchTenantVO;
import com.payment.entity.Tenant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

@Mapper
public interface TenantMapper extends BaseMapper<Tenant> {

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

