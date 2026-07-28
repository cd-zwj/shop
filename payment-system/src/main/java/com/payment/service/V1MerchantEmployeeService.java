package com.payment.service;

import com.payment.dto.V1MerchantEmployeeCreateDTO;
import com.payment.dto.V1MerchantEmployeeVO;
import com.payment.dto.V1MerchantEmployeeStoreScopeUpdateDTO;

import java.util.List;

/**
 * 商户端员工与本地角色管理服务。
 */
public interface V1MerchantEmployeeService {

    List<V1MerchantEmployeeVO> listEmployees(Long tenantId, Long operatorPlatformUserId);

    V1MerchantEmployeeVO addEmployee(Long tenantId, Long operatorPlatformUserId, V1MerchantEmployeeCreateDTO dto);

    V1MerchantEmployeeVO updateRole(Long tenantId, Long operatorPlatformUserId, Long employeeId, String employeeRole);

    V1MerchantEmployeeVO updateStatus(Long tenantId, Long operatorPlatformUserId, Long employeeId, Integer status);

    V1MerchantEmployeeVO updateStoreScope(Long tenantId, Long operatorPlatformUserId, Long employeeId,
                                          V1MerchantEmployeeStoreScopeUpdateDTO dto);
}
