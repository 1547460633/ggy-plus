package org.ggy.mall.mapper;

import org.ggy.common.mybatis.core.mapper.BaseMapperPlus;
import org.ggy.mall.domain.MallCart;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface MallCartMapper extends BaseMapperPlus<MallCart, MallCart> {

    /** 锁定当前用户全部购物车项，避免重复结算同一批商品。 */
    @Select("select * from mall_cart where user_id = #{userId} order by product_id for update")
    List<MallCart> selectByUserIdForUpdate(@Param("userId") Long userId);
}
