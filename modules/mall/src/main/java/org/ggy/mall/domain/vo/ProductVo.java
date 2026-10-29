package org.ggy.mall.domain.vo;

import lombok.Data;
import java.math.BigDecimal;

/** 商品展示对象。 */
@Data
public class ProductVo {
    private Long id;
    private String name;
    private String coverUrl;
    private String description;
    private BigDecimal price;
    private Integer stock;
}
