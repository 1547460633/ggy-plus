package org.ggy.web.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthToken;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthRequest;
import me.zhyd.oauth.request.AuthWechatMiniProgramRequest;
import org.ggy.common.core.constant.SystemConstants;
import org.ggy.common.core.domain.model.LoginUser;
import org.ggy.common.core.enums.DeviceType;
import org.ggy.common.core.exception.ServiceException;
import org.ggy.common.satoken.utils.LoginHelper;
import org.ggy.system.domain.SysSocial;
import org.ggy.system.domain.SysUser;
import org.ggy.system.domain.vo.SysUserVo;
import org.ggy.system.mapper.SysSocialMapper;
import org.ggy.system.mapper.SysUserMapper;
import org.ggy.web.domain.vo.WechatMiniProgramLoginVo;
import org.ggy.web.domain.vo.WechatMiniProgramUserVo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 微信小程序登录服务。
 *
 * @author ggy
 */
@Service
@RequiredArgsConstructor
public class WechatMiniProgramLoginService {

    private static final String SOCIAL_SOURCE = "wechat_mini_program";
    private static final String CLIENT_KEY = "miniapp";

    private final SysUserMapper userMapper;
    private final SysSocialMapper socialMapper;
    private final SysLoginService loginService;

    @Value("${wechat.mini-program.app-id:}")
    private String appId;

    @Value("${wechat.mini-program.app-secret:}")
    private String appSecret;

    @Value("${wechat.mini-program.token-timeout:2592000}")
    private long tokenTimeout;

    /**
     * 使用微信临时凭证完成登录；首次登录自动创建平台用户与绑定记录。
     *
     * @param code wx.login 返回的临时凭证
     * @return 登录结果
     */
    @Transactional(rollbackFor = Exception.class)
    public WechatMiniProgramLoginVo login(String code) {
        validateConfiguration();
        AuthToken wechatToken = exchangeCode(code);
        String openid = wechatToken.getOpenId();
        if (StrUtil.isBlank(openid)) {
            throw new ServiceException("微信未返回用户标识，请重试");
        }

        SysUserVo user = findOrCreateUser(openid, wechatToken.getUnionId());
        if (SystemConstants.DISABLE.equals(user.getStatus())) {
            throw new ServiceException("当前账号已被停用");
        }

        LoginUser loginUser = loginService.buildLoginUser(user);
        loginUser.setClientKey(CLIENT_KEY);
        loginUser.setDeviceType(DeviceType.XCX.getDevice());
        SaLoginParameter parameter = new SaLoginParameter()
            .setDeviceType(DeviceType.XCX.getDevice())
            .setTimeout(tokenTimeout)
            .setExtra(LoginHelper.CLIENT_KEY, CLIENT_KEY);
        LoginHelper.login(loginUser, parameter);

        WechatMiniProgramUserVo userVo = new WechatMiniProgramUserVo();
        userVo.setId(user.getUserId());
        userVo.setNickname(user.getNickName());

        WechatMiniProgramLoginVo result = new WechatMiniProgramLoginVo();
        result.setToken(StpUtil.getTokenValue());
        result.setExpireIn(StpUtil.getTokenTimeout());
        result.setUser(userVo);
        return result;
    }

    private void validateConfiguration() {
        if (StrUtil.isBlank(appId) || StrUtil.isBlank(appSecret)) {
            throw new ServiceException("未配置微信小程序 appId 或 appSecret");
        }
    }

    private AuthToken exchangeCode(String code) {
        AuthRequest request = new AuthWechatMiniProgramRequest(AuthConfig.builder()
            .clientId(appId)
            .clientSecret(appSecret)
            .ignoreCheckRedirectUri(true)
            .ignoreCheckState(true)
            .build());
        AuthCallback callback = new AuthCallback();
        callback.setCode(code);
        AuthResponse<AuthUser> response = request.login(callback);
        if (!response.ok() || response.getData() == null || response.getData().getToken() == null) {
            String message = response.getMsg();
            throw new ServiceException(StrUtil.isBlank(message) ? "微信登录凭证校验失败" : message);
        }
        return response.getData().getToken();
    }

    private SysUserVo findOrCreateUser(String openid, String unionId) {
        String authId = buildAuthId(openid);
        SysSocial social = socialMapper.selectOne(new LambdaQueryWrapper<SysSocial>()
            .eq(SysSocial::getAuthId, authId));
        if (social != null) {
            SysUserVo user = userMapper.selectVoById(social.getUserId());
            if (user == null) {
                throw new ServiceException("微信账号绑定的用户不存在");
            }
            return user;
        }

        SysUser newUser = new SysUser();
        newUser.setUserName("wx_" + IdUtil.fastSimpleUUID().substring(0, 20));
        newUser.setNickName("微信用户");
        newUser.setUserType("sys_user");
        newUser.setStatus(SystemConstants.NORMAL);
        newUser.setDelFlag(SystemConstants.NORMAL);
        newUser.setRemark("微信小程序自动注册");
        userMapper.insert(newUser);

        SysSocial newSocial = new SysSocial();
        newSocial.setUserId(newUser.getUserId());
        newSocial.setAuthId(authId);
        newSocial.setSource(SOCIAL_SOURCE);
        newSocial.setOpenId(openid);
        newSocial.setUnionId(unionId);
        newSocial.setUserName(newUser.getUserName());
        newSocial.setNickName(newUser.getNickName());
        socialMapper.insert(newSocial);
        return userMapper.selectVoById(newUser.getUserId());
    }

    private String buildAuthId(String openid) {
        return SOCIAL_SOURCE + ":" + appId + ":" + openid;
    }
}
