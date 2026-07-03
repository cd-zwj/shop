package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.common.BusinessException;
import com.payment.common.ResultCode;
import com.payment.config.AuthStpKit;
import com.payment.dto.PlatformRegisterDTO;
import com.payment.entity.PlatformUser;
import com.payment.enums.PlatformLoginTypeEnum;
import com.payment.mapper.PlatformUserMapper;
import com.payment.service.PlatformIdentityService;
import com.payment.service.login.PlatformLoginHandler;
import com.payment.service.login.PlatformLoginRequest;
import com.payment.util.AuthLoginIdHelper;
import com.payment.util.BizNoGenerator;
import com.payment.util.PlatformSessionHelper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 平台统一用户认证服务。
 */
@Service
public class PlatformIdentityServiceImpl implements PlatformIdentityService {

    private final PlatformUserMapper platformUserMapper;
    private final Map<PlatformLoginTypeEnum, PlatformLoginHandler> loginHandlerMap;
    private final PasswordEncoder passwordEncoder;

    public PlatformIdentityServiceImpl(PlatformUserMapper platformUserMapper, List<PlatformLoginHandler> loginHandlers, PasswordEncoder passwordEncoder) {
        this.platformUserMapper = platformUserMapper;
        this.passwordEncoder = passwordEncoder;
        Map<PlatformLoginTypeEnum, PlatformLoginHandler> handlerMap = new EnumMap<>(PlatformLoginTypeEnum.class);
        for (PlatformLoginHandler loginHandler : loginHandlers) {
            handlerMap.put(loginHandler.supports(), loginHandler);
        }
        this.loginHandlerMap = Map.copyOf(handlerMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlatformUser register(PlatformRegisterDTO dto) {
        assertUniqueRegistrationFields(dto);

        PlatformUser platformUser = new PlatformUser();
        platformUser.setUserNo(BizNoGenerator.generate("PU"));
        platformUser.setUsername(dto.getUsername());
        platformUser.setPhone(dto.getPhone());
        platformUser.setEmail(dto.getEmail());
        platformUser.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        platformUser.setStatus(1);
        platformUser.setDeleted(0);
        try {
            platformUserMapper.insert(platformUser);
        } catch (DuplicateKeyException e) {
            throw toRegistrationConflict(e);
        }
        platformUser.setPasswordHash(null);
        return platformUser;
    }

    @Override
    public String login(PlatformLoginRequest request) {
        PlatformUser platformUser = authenticate(request);
        return createPlatformSession(platformUser);
    }

    @Override
    public PlatformUser authenticate(PlatformLoginRequest request) {
        PlatformLoginHandler loginHandler = loginHandlerMap.get(request.loginType());
        if (loginHandler == null) {
            throw new BusinessException("暂不支持该登录方式");
        }
        return loginHandler.authenticate(request);
    }

    @Override
    public PlatformUser getCurrentUser() {
        Long platformUserId = PlatformSessionHelper.getPlatformUserId();
        PlatformUser platformUser = platformUserMapper.selectById(platformUserId);
        if (platformUser == null || platformUser.getDeleted() == 1) {
            throw new BusinessException("平台用户不存在");
        }
        platformUser.setPasswordHash(null);
        return platformUser;
    }

    String createPlatformSession(PlatformUser platformUser) {
        AuthStpKit.PLATFORM.login(AuthLoginIdHelper.platform(platformUser.getId()));
        AuthStpKit.PLATFORM.getSession().set("platformUserId", platformUser.getId());
        AuthStpKit.PLATFORM.getSession().set("platformUsername", platformUser.getUsername());
        return AuthStpKit.PLATFORM.getTokenValue();
    }

    private void assertUniqueRegistrationFields(PlatformRegisterDTO dto) {
        if (existsByField(PlatformUser::getUsername, dto.getUsername())) {
            throw registrationConflict("用户名已存在");
        }
        if (StringUtils.hasText(dto.getPhone()) && existsByField(PlatformUser::getPhone, dto.getPhone())) {
            throw registrationConflict("手机号已注册");
        }
        if (StringUtils.hasText(dto.getEmail()) && existsByField(PlatformUser::getEmail, dto.getEmail())) {
            throw registrationConflict("邮箱已注册");
        }
    }

    private boolean existsByField(com.baomidou.mybatisplus.core.toolkit.support.SFunction<PlatformUser, ?> column, String value) {
        return platformUserMapper.selectOne(new LambdaQueryWrapper<PlatformUser>()
                .eq(column, value)
                .eq(PlatformUser::getDeleted, 0)) != null;
    }

    private BusinessException toRegistrationConflict(DuplicateKeyException e) {
        String message = e.getMostSpecificCause() != null
                ? e.getMostSpecificCause().getMessage()
                : e.getMessage();
        if (message != null) {
            if (message.contains("uk_phone")) {
                return registrationConflict("手机号已注册");
            }
            if (message.contains("uk_email")) {
                return registrationConflict("邮箱已注册");
            }
            if (message.contains("uk_username") || message.contains("uk_platform_username")) {
                return registrationConflict("用户名已存在");
            }
        }
        return registrationConflict("用户已存在");
    }

    private BusinessException registrationConflict(String message) {
        return new BusinessException(ResultCode.USER_ALREADY_EXISTS.getCode(), message);
    }
}
