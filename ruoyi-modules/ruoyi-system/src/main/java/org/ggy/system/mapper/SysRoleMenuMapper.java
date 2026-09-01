package org.ggy.system.mapper;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.ggy.common.mybatis.core.mapper.BaseMapperPlus;
import org.ggy.system.domain.SysRoleMenu;

import java.util.List;

/**
 * 角色与菜单关联表 数据层
 *
 * @ggy
 */
public interface SysRoleMenuMapper extends BaseMapperPlus<SysRoleMenu, SysRoleMenu> {

    /**
     * 根据菜单ID串删除关联关系
     *
     * @param menuIds 菜单ID串
     * @return 结果
     */
    default int deleteByMenuIds(List<Long> menuIds) {
        return this.delete(new LambdaUpdateWrapper<SysRoleMenu>().in(SysRoleMenu::getMenuId, menuIds));
    }

}
