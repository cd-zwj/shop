package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.common.BusinessException;
import com.payment.dto.AppAccountSecurityVO;
import com.payment.dto.AppChangePasswordDTO;
import com.payment.entity.PlatformAuthProvider;
import com.payment.entity.PlatformUser;
import com.payment.entity.PlatformUserAuth;
import com.payment.mapper.PlatformAuthProviderMapper;
import com.payment.mapper.PlatformUserAuthMapper;
import com.payment.mapper.PlatformUserMapper;
import com.payment.service.AppAccountSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户端账号安全服务实现类，用于汇总账号绑定状态和处理密码修改。
 */
@Service
@RequiredArgsConstructor
public class AppAccountSecurityServiceImpl implements AppAccountSecurityService {

    private final PlatformUserMapper platformUserMapper;
    private final PlatformAuthProviderMapper platformAuthProviderMapper;
    private final PlatformUserAuthMapper platformUserAuthMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 获取账号安全摘要。
     */
    @Override
    public AppAccountSecurityVO getSecuritySummary(Long platformUserId) {
        PlatformUser user = requireUser(platformUserId);
        List<PlatformAuthProvider> providers = platformAuthProviderMapper.selectList(new LambdaQueryWrapper<PlatformAuthProvider>()
                .eq(PlatformAuthProvider::getStatus, 1)
                .orderByAsc(PlatformAuthProvider::getSortOrder)
                .orderByAsc(PlatformAuthProvider::getId));
        List<PlatformUserAuth> auths = platformUserAuthMapper.selectList(new LambdaQueryWrapper<PlatformUserAuth>()
                .eq(PlatformUserAuth::getPlatformUserId, platformUserId));
        Set<Long> boundProviderIds = auths.stream()
                .map(PlatformUserAuth::getProviderId)
                .collect(Collectors.toSet());

        AppAccountSecurityVO result = new AppAccountSecurityVO();
        result.setPhone(binding(StringUtils.hasText(user.getPhone()), maskPhone(user.getPhone())));
        result.setEmail(binding(StringUtils.hasText(user.getEmail()) && Integer.valueOf(1).equals(user.getEmailVerified()), maskEmail(user.getEmail())));

        AppAccountSecurityVO.PasswordSecurityVO password = new AppAccountSecurityVO.PasswordSecurityVO();
        password.setSet(StringUtils.hasText(user.getPasswordHash()));
        result.setPassword(password);

        result.setThirdPartyBindings(providers.stream().map(provider -> {
            AppAccountSecurityVO.ThirdPartyBindingVO binding = new AppAccountSecurityVO.ThirdPartyBindingVO();
            binding.setProviderId(provider.getId());
            binding.setProviderCode(provider.getProviderCode());
            binding.setProviderName(provider.getProviderName());
            binding.setBound(boundProviderIds.contains(provider.getId()));
            return binding;
        }).toList());
        return result;
    }

    /**
     * 修改密码。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long platformUserId, AppChangePasswordDTO dto) {
        PlatformUser user = requireUser(platformUserId);
        if (!StringUtils.hasText(user.getPasswordHash())
                || !passwordEncoder.matches(dto.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException("原密码不正确");
        }

        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        platformUserMapper.updateById(user);
    }

    private PlatformUser requireUser(Long platformUserId) {
        PlatformUser user = platformUserMapper.selectById(platformUserId);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted())) {
            throw new BusinessException("平台用户不存在");
        }
        return user;
    }

    private AppAccountSecurityVO.SecurityBindingVO binding(boolean bound, String maskedValue) {
        AppAccountSecurityVO.SecurityBindingVO binding = new AppAccountSecurityVO.SecurityBindingVO();
        binding.setBound(bound);
        binding.setMaskedValue(bound ? maskedValue : null);
        return binding;
    }

    private String maskPhone(String phone) {
        return phone != null && phone.length() >= 7 ? phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4) : phone;
    }

    private String maskEmail(String email) {
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@", 2);
        String prefix = parts[0].length() <= 2 ? parts[0] : parts[0].substring(0, 2);
        return prefix + "***@" + parts[1];
    }
}
