package com.payment.dto;

import com.payment.entity.MerchantBalance;
import com.payment.entity.Tenant;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 商家详情VO
 */
@Data
public class MerchantDetailVO {
    
    /**
     * 租户ID
     */
    private Long id;
    
    /**
     * 租户编码
     */
    private String tenantCode;
    
    /**
     * 租户名称
     */
    private String name;
    
    /**
     * 联系人
     */
    private String contactName;
    
    /**
     * 联系电话
     */
    private String contactPhone;
    
    /**
     * 地址
     */
    private String address;
    
    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 商品数量
     */
    private Long productCount;
    
    /**
     * 订单数量
     */
    private Long orderCount;
    
    /**
     * 销售额
     */
    private BigDecimal totalSales;
    
    /**
     * 获取商家基本信息
     */
    public Tenant getMerchant() {
        Tenant tenant = new Tenant();
        tenant.setId(this.id);
        tenant.setTenantCode(this.tenantCode);
        tenant.setName(this.name);
        tenant.setContact(this.contactName);
        tenant.setPhone(this.contactPhone);
        tenant.setAddress(this.address);
        tenant.setStatus(this.status);
        tenant.setCreateTime(this.createTime);
        return tenant;
    }
    
    /**
     * 获取统计信息
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("productCount", this.productCount);
        stats.put("orderCount", this.orderCount);
        stats.put("totalSales", this.totalSales);
        return stats;
    }
    
    /**
     * 获取余额信息
     */
    public MerchantBalance getBalance() {
        MerchantBalance balance = new MerchantBalance();
        balance.setTenantId(this.id);
        balance.setBalance(BigDecimal.ZERO);
        balance.setFrozenBalance(BigDecimal.ZERO);
        balance.setTotalIncome(this.totalSales != null ? this.totalSales : BigDecimal.ZERO);
        balance.setTotalWithdrawal(BigDecimal.ZERO);
        return balance;
    }
}
