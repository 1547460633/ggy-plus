package org.ggy.web.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 微信小程序登录请求对象
 *
 * @author ggy
 */
@Data
public class WechatMiniProgramLoginBo {

    /**
     * 小程序调用 wx.login 获取的临时凭证
     */
    @NotBlank(message = "微信登录凭证不能为空")
    private String code;
}
