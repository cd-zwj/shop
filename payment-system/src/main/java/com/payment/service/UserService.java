package com.payment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.payment.dto.LoginDTO;
import com.payment.dto.MiniProgramUserVO;
import com.payment.dto.WechatLoginDTO;
import com.payment.entity.User;

/**
 * 用户服务接口
 */
public interface UserService extends IService<User> {
    
    /**
     * 用户登录
     */
    String login(LoginDTO dto);

    String loginadmin(LoginDTO dto);
    
    /**
     * 根据用户名查询用户
     */
    User getByUsername(String username);
    
    /**
     * 注册用户
     */
    User register(User user);
    
    /**
     * 微信小程序登录
     */
    MiniProgramUserVO wechatLogin(WechatLoginDTO dto);
}

