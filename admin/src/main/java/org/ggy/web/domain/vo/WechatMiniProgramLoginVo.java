package org.ggy.web.domain.vo;

import lombok.Data;

/**
 * 微信小程序登录结果
 *
 * @author ggy
 */
@Data
public class WechatMiniProgramLoginVo {

    /** Sa-Token 访问令牌 */
    private String token;

    /** 令牌剩余有效期，单位：秒 */
    private Long expireIn;

    /** 当前登录用户 */
    private WechatMiniProgramUserVo user;
}
