package org.ggy.demo.mapper;

import org.ggy.common.mybatis.annotation.DataColumn;
import org.ggy.common.mybatis.annotation.DataPermission;
import org.ggy.common.mybatis.core.mapper.BaseMapperPlus;
import org.ggy.demo.domain.TestTree;
import org.ggy.demo.domain.vo.TestTreeVo;

/**
 * 测试树表Mapper接口
 *
 * @ggy
 * @date 2021-07-26
 */
@DataPermission({
    @DataColumn(key = "deptName", value = "dept_id"),
    @DataColumn(key = "userName", value = "user_id")
})
public interface TestTreeMapper extends BaseMapperPlus<TestTree, TestTreeVo> {

}
