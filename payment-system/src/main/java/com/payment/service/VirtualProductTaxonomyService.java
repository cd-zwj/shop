package com.payment.service;

import com.payment.dto.VirtualProductCategoryUpsertDTO;
import com.payment.dto.VirtualProductCategoryVO;
import com.payment.dto.VirtualProductTypeUpsertDTO;
import com.payment.dto.VirtualProductTypeVO;

import java.util.List;

public interface VirtualProductTaxonomyService {
    List<VirtualProductTypeVO> listTypes(Long tenantId, Long platformUserId, Integer status);

    VirtualProductTypeVO createType(Long tenantId, Long platformUserId, VirtualProductTypeUpsertDTO dto);

    VirtualProductTypeVO updateType(Long tenantId, Long platformUserId, Long id, VirtualProductTypeUpsertDTO dto);

    void deleteType(Long tenantId, Long platformUserId, Long id);

    List<VirtualProductCategoryVO> listCategories(Long tenantId, Long platformUserId, Long typeId, Integer status);

    VirtualProductCategoryVO createCategory(Long tenantId, Long platformUserId, VirtualProductCategoryUpsertDTO dto);

    VirtualProductCategoryVO updateCategory(Long tenantId, Long platformUserId, Long id, VirtualProductCategoryUpsertDTO dto);

    void deleteCategory(Long tenantId, Long platformUserId, Long id);
}
