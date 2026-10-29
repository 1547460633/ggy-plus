package org.ggy.mall.mapper;

import org.ggy.common.mybatis.core.mapper.BaseMapperPlus;
import org.ggy.mall.domain.MallProduct;
import org.apache.ibatis.annotations.Select;

public interface MallProductMapper extends BaseMapperPlus<MallProduct, MallProduct> {

    /** 锁定库存行，和下单、取消订单、人工调整处于同一事务。 */
    @Select("select * from mall_product where id = #{id} for update")
    MallProduct selectByIdForUpdate(Long id);
}
