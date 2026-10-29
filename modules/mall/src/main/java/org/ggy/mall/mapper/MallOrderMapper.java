package org.ggy.mall.mapper;

import org.ggy.common.mybatis.core.mapper.BaseMapperPlus;
import org.ggy.mall.domain.MallOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface MallOrderMapper extends BaseMapperPlus<MallOrder, MallOrder> {

    /** 锁定订单行，防止并发取消导致重复回补库存。 */
    @Select("select * from mall_order where id = #{id} for update")
    MallOrder selectByIdForUpdate(@Param("id") Long id);
}
