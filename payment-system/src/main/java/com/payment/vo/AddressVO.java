package com.payment.vo;

import com.payment.entity.UserShippingAddress;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 收货地址视图对象，排除内部字段后返回给前端。
 */
@Data
public class AddressVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String receiverName;
    private String phone;
    private String province;
    private String city;
    private String district;
    private String detail;
    private Integer isDefault;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static AddressVO from(UserShippingAddress entity) {
        if (entity == null) {
            return null;
        }
        AddressVO vo = new AddressVO();
        vo.setId(entity.getId());
        vo.setReceiverName(entity.getReceiverName());
        vo.setPhone(entity.getPhone());
        vo.setProvince(entity.getProvince());
        vo.setCity(entity.getCity());
        vo.setDistrict(entity.getDistrict());
        vo.setDetail(entity.getDetail());
        vo.setIsDefault(entity.getIsDefault());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
