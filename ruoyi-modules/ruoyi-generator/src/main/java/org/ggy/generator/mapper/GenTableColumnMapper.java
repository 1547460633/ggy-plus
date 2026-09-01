package org.ggy.generator.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.ggy.common.mybatis.core.mapper.BaseMapperPlus;
import org.ggy.generator.domain.GenTableColumn;

/**
 * 业务字段 数据层
 *
 * @ggy
 */
@InterceptorIgnore(dataPermission = "true", tenantLine = "true")
public interface GenTableColumnMapper extends BaseMapperPlus<GenTableColumn, GenTableColumn> {

}
