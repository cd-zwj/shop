package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.PageResult;
import com.payment.common.Result;
import com.payment.dto.MerchantBalanceVO;
import com.payment.dto.MerchantDTO;
import com.payment.dto.MerchantDetailVO;
import com.payment.dto.MerchantListVO;
import com.payment.entity.MerchantBalance;
import com.payment.entity.Tenant;
import org.springframework.beans.BeanUtils;
import com.payment.service.V1AdminService;
import com.payment.service.WithdrawalService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 平台管理端 - 商户管理控制器。
 * <p>提供商户的查询、新增、编辑、启用/禁用和余额查看等功能，接口路径前缀 /v1/admin/merchants。</p>
 * <p>需 admin 角色并具备相应商户管理权限。</p>
 */
@RestController
@RequestMapping("/v1/admin/merchants")
@RequiredArgsConstructor
public class V1AdminMerchantController {

    private final V1AdminService v1AdminService;
    private final WithdrawalService withdrawalService;

    /**
     * 分页查询商户列表。
     * <p>支持按商户名称和状态进行筛选。</p>
     * <p>GET /v1/admin/merchants</p>
     *
     * @param current 页码，默认 1
     * @param size    每页条数，默认 10
     * @param name    商户名称关键字（可选）
     * @param status  商户状态（可选）
     * @return 商户列表分页数据
     */
    @SaCheckPermission(type = "admin", value = "admin:merchant:list")
    @GetMapping
    public Result<PageResult<MerchantListVO>> listMerchants(@RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于0") Integer current,
                                                             @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数必须大于0") Integer size,
                                                             @RequestParam(required = false) String name,
                                                             @RequestParam(required = false) Integer status) {
        Page<MerchantListVO> page = v1AdminService.listMerchants(current, size, name, status);
        return Result.success(PageResult.from(page));
    }

    /**
     * 获取商户详情。
     * <p>根据租户ID查询商户的详细信息。</p>
     * <p>GET /v1/admin/merchants/{tenantId}</p>
     *
     * @param tenantId 商户租户ID
     * @return 商户详情（MerchantDetailVO）
     */
    @SaCheckPermission(type = "admin", value = "admin:merchant:detail")
    @GetMapping("/{tenantId}")
    public Result<MerchantDetailVO> getMerchantDetail(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId) {
        return Result.success(v1AdminService.getMerchantDetail(tenantId));
    }

    /**
     * 创建商户。
     * <p>由平台管理员新增一个商户（租户），返回创建后的商户详情。</p>
     * <p>POST /v1/admin/merchants</p>
     *
     * @param dto 商户创建信息（MerchantDTO）
     * @return 创建成功的商户详情
     */
    @SaCheckPermission(type = "admin", value = "admin:merchant:create")
    @PostMapping
    public Result<MerchantDetailVO> createMerchant(@Valid @RequestBody MerchantDTO dto) {
        Tenant tenant = v1AdminService.createMerchant(dto);
        MerchantDetailVO vo = new MerchantDetailVO();
        BeanUtils.copyProperties(tenant, vo);
        return Result.success(vo);
    }

    /**
     * 更新商户信息。
     * <p>修改指定商户的基本信息。</p>
     * <p>PUT /v1/admin/merchants/{tenantId}</p>
     *
     * @param tenantId 商户租户ID
     * @param dto      商户更新信息
     * @return 操作结果
     */
    @SaCheckPermission(type = "admin", value = "admin:merchant:update")
    @PutMapping("/{tenantId}")
    public Result<Void> updateMerchant(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId, @Valid @RequestBody MerchantDTO dto) {
        v1AdminService.updateMerchant(tenantId, dto);
        return Result.success();
    }

    /**
     * 启用商户。
     * <p>将指定商户的状态设置为启用，恢复正常运营。</p>
     * <p>PUT /v1/admin/merchants/{tenantId}/enable</p>
     *
     * @param tenantId 商户租户ID
     * @return 操作结果
     */
    @SaCheckPermission(type = "admin", value = "admin:merchant:enable")
    @PutMapping("/{tenantId}/enable")
    public Result<Void> enableMerchant(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId) {
        v1AdminService.enableMerchant(tenantId);
        return Result.success();
    }

    /**
     * 禁用商户。
     * <p>将指定商户的状态设置为禁用，暂停其运营。</p>
     * <p>PUT /v1/admin/merchants/{tenantId}/disable</p>
     *
     * @param tenantId 商户租户ID
     * @return 操作结果
     */
    @SaCheckPermission(type = "admin", value = "admin:merchant:disable")
    @PutMapping("/{tenantId}/disable")
    public Result<Void> disableMerchant(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId) {
        v1AdminService.disableMerchant(tenantId);
        return Result.success();
    }

    /**
     * 查询商户余额。
     * <p>获取指定商户的钱包余额信息。</p>
     * <p>GET /v1/admin/merchants/{tenantId}/balance</p>
     *
     * @param tenantId 商户租户ID
     * @return 商户余额信息（MerchantBalanceVO），如无余额记录则返回 null
     */
    @SaCheckPermission(type = "admin", value = "admin:merchant:balance")
    @GetMapping("/{tenantId}/balance")
    public Result<MerchantBalanceVO> getMerchantBalance(@PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId) {
        MerchantBalance balance = withdrawalService.getMerchantBalance(tenantId);
        if (balance == null) {
            return Result.success(null);
        }
        MerchantBalanceVO vo = new MerchantBalanceVO();
        BeanUtils.copyProperties(balance, vo);
        return Result.success(vo);
    }
}
