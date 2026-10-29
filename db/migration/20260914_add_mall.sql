-- 商城：商品、购物车、订单、订单明细与库存流水。执行前请先在目标库备份。
CREATE TABLE IF NOT EXISTS mall_product (
  id bigint NOT NULL COMMENT '商品ID',
  name varchar(100) NOT NULL COMMENT '商品名称',
  cover_url varchar(500) DEFAULT NULL COMMENT '封面图',
  description varchar(500) DEFAULT NULL COMMENT '商品描述',
  price decimal(10,2) NOT NULL COMMENT '售价',
  stock int NOT NULL DEFAULT 0 COMMENT '当前库存',
  status tinyint NOT NULL DEFAULT 0 COMMENT '0上架 1下架',
  sort int NOT NULL DEFAULT 0 COMMENT '排序',
  create_dept bigint DEFAULT NULL, create_by bigint DEFAULT NULL, create_time datetime DEFAULT NULL,
  update_by bigint DEFAULT NULL, update_time datetime DEFAULT NULL,
  PRIMARY KEY (id), KEY idx_mall_product_status_sort (status, sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商城商品';

CREATE TABLE IF NOT EXISTS mall_cart (
  id bigint NOT NULL, user_id bigint NOT NULL, product_id bigint NOT NULL, quantity int NOT NULL,
  create_dept bigint DEFAULT NULL, create_by bigint DEFAULT NULL, create_time datetime DEFAULT NULL,
  update_by bigint DEFAULT NULL, update_time datetime DEFAULT NULL,
  PRIMARY KEY (id), UNIQUE KEY uk_mall_cart_user_product (user_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商城购物车';

CREATE TABLE IF NOT EXISTS mall_order (
  id bigint NOT NULL, order_no varchar(40) NOT NULL, user_id bigint NOT NULL,
  total_amount decimal(10,2) NOT NULL, status tinyint NOT NULL DEFAULT 0 COMMENT '0待付款 1待发货 2已完成 3已取消',
  cancel_time datetime DEFAULT NULL,
  create_dept bigint DEFAULT NULL, create_by bigint DEFAULT NULL, create_time datetime DEFAULT NULL,
  update_by bigint DEFAULT NULL, update_time datetime DEFAULT NULL,
  PRIMARY KEY (id), UNIQUE KEY uk_mall_order_no (order_no), KEY idx_mall_order_user_time (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商城订单';

CREATE TABLE IF NOT EXISTS mall_order_item (
  id bigint NOT NULL, order_id bigint NOT NULL, product_id bigint NOT NULL, product_name varchar(100) NOT NULL,
  cover_url varchar(500) DEFAULT NULL, product_price decimal(10,2) NOT NULL, quantity int NOT NULL,
  subtotal_amount decimal(10,2) NOT NULL,
  create_dept bigint DEFAULT NULL, create_by bigint DEFAULT NULL, create_time datetime DEFAULT NULL,
  update_by bigint DEFAULT NULL, update_time datetime DEFAULT NULL,
  PRIMARY KEY (id), KEY idx_mall_order_item_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商城订单明细';

CREATE TABLE IF NOT EXISTS mall_stock_log (
  id bigint NOT NULL, product_id bigint NOT NULL, change_quantity int NOT NULL,
  before_stock int NOT NULL, after_stock int NOT NULL, business_type varchar(20) NOT NULL,
  business_no varchar(50) DEFAULT NULL, remark varchar(255) DEFAULT NULL,
  create_dept bigint DEFAULT NULL, create_by bigint DEFAULT NULL, create_time datetime DEFAULT NULL,
  update_by bigint DEFAULT NULL, update_time datetime DEFAULT NULL,
  PRIMARY KEY (id), KEY idx_mall_stock_log_product_time (product_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商城库存流水';

-- 演示商品（图片字段可在后台维护为 OSS 地址）。
INSERT INTO mall_product (id, name, description, price, stock, status, sort, create_time, update_time)
VALUES
  (202609140001, '精选手冲咖啡豆', '中浅烘焙，花香与柑橘风味', 68.00, 100, 0, 1, NOW(), NOW()),
  (202609140002, '简约保温随行杯', '316 不锈钢内胆，便携 420ml', 89.00, 80, 0, 2, NOW(), NOW()),
  (202609140003, '柔软针织抱枕', '亲肤针织面料，为生活留一份松弛', 59.00, 120, 0, 3, NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description), price = VALUES(price), stock = VALUES(stock), status = VALUES(status), sort = VALUES(sort), update_time = NOW();
