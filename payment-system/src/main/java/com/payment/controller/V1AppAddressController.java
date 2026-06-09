package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.payment.common.Result;
import com.payment.dto.UserShippingAddressDTO;
import com.payment.entity.UserShippingAddress;
import com.payment.service.UserShippingAddressService;
import com.payment.util.PlatformSessionHelper;
import com.payment.vo.AddressVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * v1 用户端收货地址接口。
 */
@RestController
@RequestMapping("/v1/app/addresses")
@RequiredArgsConstructor
public class V1AppAddressController {

    private final UserShippingAddressService addressService;

    @SaCheckLogin
    @GetMapping
    public Result<List<AddressVO>> listAddresses() {
        List<UserShippingAddress> addresses = addressService.list(PlatformSessionHelper.getPlatformUserId());
        return Result.success(addresses.stream().map(AddressVO::from).collect(Collectors.toList()));
    }

    @SaCheckLogin
    @PostMapping
    public Result<AddressVO> createAddress(@Valid @RequestBody UserShippingAddressDTO dto) {
        UserShippingAddress address = addressService.create(PlatformSessionHelper.getPlatformUserId(), dto);
        return Result.success(AddressVO.from(address));
    }

    @SaCheckLogin
    @PutMapping("/{id}")
    public Result<AddressVO> updateAddress(@PathVariable @Min(value = 1, message = "ID必须大于0") Long id,
                                           @Valid @RequestBody UserShippingAddressDTO dto) {
        UserShippingAddress address = addressService.update(PlatformSessionHelper.getPlatformUserId(), id, dto);
        return Result.success(AddressVO.from(address));
    }

    @SaCheckLogin
    @DeleteMapping("/{id}")
    public Result<Void> deleteAddress(@PathVariable @Min(value = 1, message = "ID必须大于0") Long id) {
        addressService.delete(PlatformSessionHelper.getPlatformUserId(), id);
        return Result.success();
    }

    @SaCheckLogin
    @PutMapping("/{id}/default")
    public Result<AddressVO> setDefaultAddress(@PathVariable @Min(value = 1, message = "ID必须大于0") Long id) {
        UserShippingAddress address = addressService.setDefault(PlatformSessionHelper.getPlatformUserId(), id);
        return Result.success(AddressVO.from(address));
    }
}
