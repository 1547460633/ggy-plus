package org.ggy.common.translation.core.impl;

import cn.hutool.core.convert.Convert;
import lombok.AllArgsConstructor;
import org.ggy.common.core.service.UserService;
import org.ggy.common.translation.annotation.TranslationType;
import org.ggy.common.translation.constant.TransConstant;
import org.ggy.common.translation.core.TranslationInterface;

/**
 * 用户名翻译实现
 *
 * @ggy
 */
@AllArgsConstructor
@TranslationType(type = TransConstant.USER_ID_TO_NAME)
public class UserNameTranslationImpl implements TranslationInterface<String> {

    private final UserService userService;

    @Override
    public String translation(Object key, String other) {
        return userService.selectUserNameById(Convert.toLong(key));
    }
}
