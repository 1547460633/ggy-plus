package org.ggy.common.translation.core.impl;

import lombok.AllArgsConstructor;
import org.ggy.common.core.service.DeptService;
import org.ggy.common.translation.annotation.TranslationType;
import org.ggy.common.translation.constant.TransConstant;
import org.ggy.common.translation.core.TranslationInterface;

/**
 * 部门翻译实现
 *
 * @ggy
 */
@AllArgsConstructor
@TranslationType(type = TransConstant.DEPT_ID_TO_NAME)
public class DeptNameTranslationImpl implements TranslationInterface<String> {

    private final DeptService deptService;

    @Override
    public String translation(Object key, String other) {
        if (key instanceof String ids) {
            return deptService.selectDeptNameByIds(ids);
        } else if (key instanceof Long id) {
            return deptService.selectDeptNameByIds(id.toString());
        }
        return null;
    }
}
