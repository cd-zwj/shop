package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.common.BusinessException;
import com.payment.dto.RecoveredPlatformAccountVO;
import com.payment.entity.PlatformUser;
import com.payment.enums.EmailCodeSceneEnum;
import com.payment.mapper.PlatformUserMapper;
import com.payment.service.EmailCodeService;
import com.payment.service.PlatformEmailAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 平台邮箱账号服务实现类，用于实现平台邮箱账号相关业务逻辑。
 */
@Service
@RequiredArgsConstructor
public class PlatformEmailAccountServiceImpl implements PlatformEmailAccountService {

    private final PlatformUserMapper platformUserMapper;
    private final EmailCodeService emailCodeService;
    private final PasswordEncoder passwordEncoder;

    /**
     * 向已完成邮箱绑定且未禁用的用户发送登录验证码。
     */
    @Override
    public void sendLoginCode(String email) {
        PlatformUser user = requireVerifiedUserByEmail(email);
        if (user.getStatus() == null || user.getStatus() == 0) {
            throw new BusinessException("用户已禁用");
        }
        emailCodeService.sendCode(user.getEmail(), EmailCodeSceneEnum.LOGIN);
    }

    /**
     * 向已完成邮箱绑定的用户发送账号找回和密码重置验证码。
     */
    @Override
    public void sendRecoverCode(String email) {
        PlatformUser user = requireVerifiedUserByEmail(email);
        emailCodeService.sendCode(user.getEmail(), EmailCodeSceneEnum.RECOVER);
    }

    /**
     * 校验目标邮箱可绑定后，向该邮箱发送绑定验证码。
     */
    @Override
    public void sendBindCode(Long platformUserId, String email) {
        PlatformUser currentUser = requireUserById(platformUserId);
        String normalizedEmail = normalizeEmail(email);
        ensureEmailCanBind(currentUser, normalizedEmail);
        emailCodeService.sendCode(normalizedEmail, EmailCodeSceneEnum.BIND);
    }

    /**
     * 校验绑定验证码并把目标邮箱写入当前平台用户。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlatformUser bindEmail(Long platformUserId, String email, String emailCode) {
        PlatformUser currentUser = requireUserById(platformUserId);
        String normalizedEmail = normalizeEmail(email);
        ensureEmailCanBind(currentUser, normalizedEmail);
        emailCodeService.validateCode(normalizedEmail, emailCode, EmailCodeSceneEnum.BIND, true);
        currentUser.setEmail(normalizedEmail);
        currentUser.setEmailVerified(1);
        platformUserMapper.updateById(currentUser);
        return sanitizeUser(currentUser);
    }

    /**
     * 校验找回验证码并返回邮箱对应的账号信息。
     */
    @Override
    public RecoveredPlatformAccountVO recoverAccount(String email, String emailCode) {
        PlatformUser user = requireVerifiedUserByEmail(email);
        emailCodeService.validateCode(user.getEmail(), emailCode, EmailCodeSceneEnum.RECOVER, true);
        return new RecoveredPlatformAccountVO(user.getUsername());
    }

    /**
     * 校验找回验证码后使用 BCrypt 重置用户密码。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(String email, String emailCode, String newPassword) {
        PlatformUser user = requireVerifiedUserByEmail(email);
        emailCodeService.validateCode(user.getEmail(), emailCode, EmailCodeSceneEnum.RECOVER, true);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        platformUserMapper.updateById(user);
    }

    /**
     * 确保邮箱未被当前账号或其他账号重复绑定。
     */
    private void ensureEmailCanBind(PlatformUser currentUser, String normalizedEmail) {
        if (!StringUtils.hasText(normalizedEmail)) {
            throw new BusinessException("邮箱不能为空");
        }
        if (normalizedEmail.equals(currentUser.getEmail()) && currentUser.getEmailVerified() != null && currentUser.getEmailVerified() == 1) {
            throw new BusinessException("该邮箱已绑定当前账号");
        }

        PlatformUser existingUser = platformUserMapper.selectOne(new LambdaQueryWrapper<PlatformUser>()
                .eq(PlatformUser::getEmail, normalizedEmail)
                .last("limit 1"));
        if (existingUser != null && !existingUser.getId().equals(currentUser.getId())) {
            throw new BusinessException("该邮箱已被其他账号使用");
        }
    }

    /**
     * 根据邮箱查询已完成邮箱验证的平台用户。
     */
    private PlatformUser requireVerifiedUserByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        PlatformUser platformUser = platformUserMapper.selectOne(new LambdaQueryWrapper<PlatformUser>()
                .eq(PlatformUser::getEmail, normalizedEmail)
                .eq(PlatformUser::getDeleted, 0)
                .last("limit 1"));
        if (platformUser == null || platformUser.getEmailVerified() == null || platformUser.getEmailVerified() == 0) {
            throw new BusinessException("邮箱或验证码错误");
        }
        return platformUser;
    }

    /**
     * 根据平台用户 ID 查询未删除的用户。
     */
    private PlatformUser requireUserById(Long platformUserId) {
        PlatformUser currentUser = platformUserMapper.selectById(platformUserId);
        if (currentUser == null || currentUser.getDeleted() != null && currentUser.getDeleted() == 1) {
            throw new BusinessException("平台用户不存在");
        }
        return currentUser;
    }

    /**
     * 返回前移除不应下发给前端的密码哈希。
     */
    private PlatformUser sanitizeUser(PlatformUser platformUser) {
        platformUser.setPasswordHash(null);
        return platformUser;
    }

    /**
     * 规范化邮箱。
     */
    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        return email.trim().toLowerCase();
    }
}
