package com.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.dto.AdminDashboardOverviewVO;
import com.payment.dto.AdminTrendVO;
import com.payment.dto.AdminOrderListVO;
import com.payment.dto.AdminPaymentBillVO;
import com.payment.dto.AdminPlatformUserVO;
import com.payment.dto.AdminRechargeOrderVO;
import com.payment.dto.AdminTradeOverviewVO;
import com.payment.dto.MerchantDTO;
import com.payment.dto.MerchantDetailVO;
import com.payment.dto.MerchantListVO;
import com.payment.dto.SalesOrderDetailVO;
import com.payment.dto.UserPermissionDTO;
import com.payment.dto.UserPermissionVO;
import com.payment.dto.V1AdminSessionVO;
import com.payment.dto.WithdrawalVO;
import com.payment.entity.Permission;
import com.payment.entity.Tenant;

import java.util.List;
import java.util.Map;

/**
 * 平台管理端服务接口。
 *
 * <p>提供平台管理员所需的全部管理能力，包括管理员登录认证、数据看板、
 * 平台用户管理、商户管理、提现审批、权限管理、订单查看、支付账单查询和充值订单查看等。
 * 此接口是管理端Controller的统一业务入口。</p>
 */
public interface V1AdminService {

    /**
     * 管理员登录。
     *
     * @param username 用户名
     * @param password 密码
     * @return 登录成功后的Token字符串
     * @throws com.payment.common.exception.BusinessException 当用户名或密码错误时抛出
     */
    String login(String username, String password);

    /**
     * 获取当前管理员的会话信息。
     *
     * @return 管理员会话视图对象，包含用户ID、角色、权限等
     */
    V1AdminSessionVO getAdminSession();

    /**
     * 获取当前管理员的个人信息。
     *
     * @return 管理员信息键值对，包含用户名、头像、角色等
     */
    Map<String, Object> getAdminInfo();

    /**
     * 获取平台数据看板概览（商户数、用户数、交易额、订单数等核心指标）。
     *
     * @return 数据看板概览视图对象
     */
    AdminDashboardOverviewVO getDashboardOverview();

    /**
     * 获取平台趋势数据（按时间维度的交易额、订单量等趋势）。
     *
     * @param startDate   起始日期（格式：yyyy-MM-dd）
     * @param endDate     截止日期（格式：yyyy-MM-dd）
     * @param granularity 粒度：day / week / month
     * @return 趋势数据视图对象
     */
    AdminTrendVO getTrend(String startDate, String endDate, String granularity);

    /**
     * 获取平台交易概览（支付成功率、退款率、各渠道占比等）。
     *
     * @return 交易概览视图对象
     */
    AdminTradeOverviewVO getTradeOverview();

    /**
     * 分页查询平台用户列表。
     *
     * @param current 页码
     * @param size    每页条数
     * @param keyword 用户名/手机号模糊搜索（可空）
     * @param status  用户状态过滤：1-正常 / 0-禁用（可空）
     * @return 平台用户分页数据
     */
    Page<AdminPlatformUserVO> listPlatformUsers(Integer current, Integer size, String keyword, Integer status);

    /**
     * 查询平台用户详情。
     *
     * @param userId 平台用户ID
     * @return 用户详情视图对象
     * @throws com.payment.common.exception.BusinessException 当用户不存在时抛出
     */
    AdminPlatformUserVO getPlatformUserDetail(Long userId);

    /**
     * 启用平台用户。
     *
     * @param userId 平台用户ID
     */
    void enablePlatformUser(Long userId);

    /**
     * 禁用平台用户。
     *
     * @param userId 平台用户ID
     */
    void disablePlatformUser(Long userId);

    /**
     * 分页查询商户列表。
     *
     * @param current 页码
     * @param size    每页条数
     * @param name    商户名称模糊搜索（可空）
     * @param status  商户状态过滤（可空）
     * @return 商户列表分页数据
     */
    Page<MerchantListVO> listMerchants(Integer current, Integer size, String name, Integer status);

    /**
     * 查询商户详情。
     *
     * @param tenantId 租户ID
     * @return 商户详情视图对象
     * @throws com.payment.common.exception.BusinessException 当商户不存在时抛出
     */
    MerchantDetailVO getMerchantDetail(Long tenantId);

    /**
     * 创建商户。
     *
     * @param dto 商户信息DTO
     * @return 创建成功的租户实体
     */
    Tenant createMerchant(MerchantDTO dto);

    /**
     * 更新商户信息。
     *
     * @param tenantId 租户ID
     * @param dto      更新后的商户信息
     */
    void updateMerchant(Long tenantId, MerchantDTO dto);

    /**
     * 启用商户。
     *
     * @param tenantId 租户ID
     */
    void enableMerchant(Long tenantId);

    /**
     * 禁用商户。
     *
     * @param tenantId 租户ID
     */
    void disableMerchant(Long tenantId);

    /**
     * 分页查询提现申请列表。
     *
     * @param current      页码
     * @param size         每页条数
     * @param merchantName 商户名称模糊搜索（可空）
     * @param status       提现状态过滤：pending / approved / rejected（可空）
     * @param startDate    起始日期（可空）
     * @param endDate      截止日期（可空）
     * @return 提现申请分页数据
     */
    Page<WithdrawalVO> listWithdrawals(Integer current, Integer size, String merchantName, Integer status, String startDate, String endDate);

    /**
     * 审批通过提现申请。
     *
     * @param withdrawalId 提现申请ID
     * @param approverId   审批人ID
     * @throws com.payment.common.exception.BusinessException 当申请不存在或状态异常时抛出
     */
    void approveWithdrawal(Long withdrawalId, Long approverId);

    /**
     * 驳回提现申请。
     *
     * @param withdrawalId 提现申请ID
     * @param approverId   审批人ID
     * @param reason       驳回原因
     */
    void rejectWithdrawal(Long withdrawalId, Long approverId, String reason);

    /**
     * 查询平台权限列表（按模块分组）。
     *
     * @return 权限列表，key为模块名，value为该模块下的权限列表
     */
    Map<String, List<Permission>> listPermissions();

    /**
     * 查询指定用户已分配的权限。
     *
     * @param userId 平台用户ID
     * @return 用户权限视图对象，包含角色和权限列表
     */
    UserPermissionVO getUserPermissions(Long userId);

    /**
     * 设置用户权限（全量覆盖）。
     *
     * @param userId 用户ID
     * @param dto    权限配置，包含角色ID和权限ID列表
     */
    void setUserPermissions(Long userId, UserPermissionDTO dto);

    /**
     * 移除用户的单条权限。
     *
     * @param userId       用户ID
     * @param permissionId 权限ID
     */
    void removeUserPermission(Long userId, Long permissionId);

    /**
     * 分页查询订单列表。
     *
     * @param current     页码
     * @param size        每页条数
     * @param orderNo     订单号模糊搜索（可空）
     * @param orderStatus 订单状态过滤（可空）
     * @param payStatus   支付状态过滤（可空）
     * @param tenantId    租户ID过滤（可空）
     * @return 订单列表分页数据
     */
    Page<AdminOrderListVO> listOrders(Integer current, Integer size, String orderNo, String orderStatus, String payStatus, Long tenantId);

    /**
     * 查询订单详情。
     *
     * @param orderNo 订单号
     * @return 订单详情视图对象
     * @throws com.payment.common.exception.BusinessException 当订单不存在时抛出
     */
    SalesOrderDetailVO getOrderDetail(String orderNo);

    /**
     * 分页查询支付账单列表。
     *
     * @param current    页码
     * @param size       每页条数
     * @param bizType    业务类型过滤（可空）
     * @param payStatus  支付状态过滤（可空）
     * @param channelCode 支付渠道过滤（可空）
     * @return 支付账单分页数据
     */
    Page<AdminPaymentBillVO> listPaymentBills(Integer current, Integer size, String bizType, String payStatus, String channelCode);

    /**
     * 分页查询充值订单列表。
     *
     * @param current    页码
     * @param size       每页条数
     * @param walletType 钱包类型过滤：unified / merchant（可空）
     * @param bizStatus  业务状态过滤（可空）
     * @param tenantId   租户ID过滤（可空）
     * @return 充值订单分页数据
     */
    Page<AdminRechargeOrderVO> listRechargeOrders(Integer current, Integer size, String walletType, String bizStatus, Long tenantId);
}
