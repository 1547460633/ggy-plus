package org.ggy.common.translation.core.impl;

import lombok.AllArgsConstructor;
import org.ggy.common.core.service.OssService;
import org.ggy.common.translation.annotation.TranslationType;
import org.ggy.common.translation.constant.TransConstant;
import org.ggy.common.translation.core.TranslationInterface;

/**
 * OSS翻译实现
 *
 * @ggy
 */
@AllArgsConstructor
@TranslationType(type = TransConstant.OSS_ID_TO_URL)
public class OssUrlTranslationImpl implements TranslationInterface<String> {

    private final OssService ossService;

    @Override
    public String translation(Object key, String other) {
        if (key instanceof String ids) {
            return ossService.selectUrlByIds(ids);
        } else if (key instanceof Long id) {
            return ossService.selectUrlByIds(id.toString());
        }
        return null;
    }
}
