package org.ggy.web.domain.vo;

import lombok.Data;

/**
 * 微信小程序登录用户信息
 *
 * @author ggy
 */
@Data
public class WechatMiniProgramUserVo {

    /** 用户ID */
    private Long id;

    /** 用户昵称 */
    private String nickname;
}
