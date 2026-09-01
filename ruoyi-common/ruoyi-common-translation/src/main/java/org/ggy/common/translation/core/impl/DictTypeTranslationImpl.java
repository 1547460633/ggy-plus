package org.ggy.common.translation.core.impl;

import lombok.AllArgsConstructor;
import org.ggy.common.core.service.DictService;
import org.ggy.common.core.utils.StringUtils;
import org.ggy.common.translation.annotation.TranslationType;
import org.ggy.common.translation.constant.TransConstant;
import org.ggy.common.translation.core.TranslationInterface;

/**
 * 字典翻译实现
 *
 * @ggy
 */
@AllArgsConstructor
@TranslationType(type = TransConstant.DICT_TYPE_TO_LABEL)
public class DictTypeTranslationImpl implements TranslationInterface<String> {

    private final DictService dictService;

    @Override
    public String translation(Object key, String other) {
        if (key instanceof String dictValue && StringUtils.isNotBlank(other)) {
            return dictService.getDictLabel(other, dictValue);
        }
        return null;
    }
}
