package org.ggy.mall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.ggy.common.core.exception.ServiceException;
import org.ggy.common.mybatis.utils.IdGeneratorUtil;
import org.ggy.common.satoken.utils.LoginHelper;
import org.ggy.mall.domain.*;
import org.ggy.mall.domain.bo.CartItemBo;
import org.ggy.mall.domain.bo.ProductBo;
import org.ggy.mall.domain.bo.StockAdjustBo;
import org.ggy.mall.domain.vo.CartItemVo;
import org.ggy.mall.domain.vo.OrderItemVo;
import org.ggy.mall.domain.vo.OrderVo;
import org.ggy.mall.domain.vo.ProductVo;
import org.ggy.mall.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** 商城用户端商品、购物车、库存和订单业务。 */
@Service
@RequiredArgsConstructor
public class MallStoreService {

    private static final int ON_SHELF = 0;
    private static final int PENDING_PAYMENT = 0;
    private static final int PENDING_DELIVERY = 1;
    private static final int CANCELED = 3;

    private final MallProductMapper productMapper;
    private final MallCartMapper cartMapper;
    private final MallOrderMapper orderMapper;
    private final MallOrderItemMapper orderItemMapper;
    private final MallStockLogMapper stockLogMapper;

    /** 获取上架商品。 */
    public List<ProductVo> listProducts() {
        return productMapper.selectList(new LambdaQueryWrapper<MallProduct>()
                .eq(MallProduct::getStatus, ON_SHELF)
                .orderByAsc(MallProduct::getSort)
                .orderByDesc(MallProduct::getCreateTime))
            .stream().map(this::toProductVo).toList();
    }

    /** 获取当前用户购物车。 */
    public List<CartItemVo> listCart() {
        Long userId = LoginHelper.getUserId();
        List<MallCart> carts = cartMapper.selectList(new LambdaQueryWrapper<MallCart>()
            .eq(MallCart::getUserId, userId).orderByDesc(MallCart::getUpdateTime));
        List<CartItemVo> result = new ArrayList<>();
        for (MallCart cart : carts) {
            MallProduct product = productMapper.selectById(cart.getProductId());
            if (product == null) {
                continue;
            }
            CartItemVo vo = new CartItemVo();
            vo.setId(cart.getId());
            vo.setProductId(product.getId());
            vo.setName(product.getName());
            vo.setCoverUrl(product.getCoverUrl());
            vo.setPrice(product.getPrice());
            vo.setStock(product.getStock());
            vo.setQuantity(cart.getQuantity());
            vo.setAvailable(product.getStatus() == ON_SHELF && product.getStock() > 0);
            result.add(vo);
        }
        return result;
    }

    /** 加入购物车；同商品自动合并数量。 */
    @Transactional(rollbackFor = Exception.class)
    public void addCart(CartItemBo bo) {
        Long userId = LoginHelper.getUserId();
        MallProduct product = requireOnShelfProduct(bo.getProductId());
        MallCart cart = cartMapper.selectOne(new LambdaQueryWrapper<MallCart>()
            .eq(MallCart::getUserId, userId).eq(MallCart::getProductId, bo.getProductId()));
        int quantity = bo.getQuantity() + (cart == null ? 0 : cart.getQuantity());
        validateCartQuantity(product, quantity);
        if (cart == null) {
            cart = new MallCart();
            cart.setUserId(userId);
            cart.setProductId(bo.getProductId());
            cart.setQuantity(quantity);
            cartMapper.insert(cart);
        } else {
            cart.setQuantity(quantity);
            cartMapper.updateById(cart);
        }
    }

    /** 修改购物车数量。 */
    @Transactional(rollbackFor = Exception.class)
    public void updateCart(Long cartId, CartItemBo bo) {
        MallCart cart = requireUserCart(cartId);
        if (!cart.getProductId().equals(bo.getProductId())) {
            throw new ServiceException("购物车商品不允许替换");
        }
        validateCartQuantity(requireOnShelfProduct(cart.getProductId()), bo.getQuantity());
        cart.setQuantity(bo.getQuantity());
        cartMapper.updateById(cart);
    }

    /** 删除购物车项。 */
    public void removeCart(Long cartId) {
        cartMapper.delete(new LambdaQueryWrapper<MallCart>()
            .eq(MallCart::getId, cartId).eq(MallCart::getUserId, LoginHelper.getUserId()));
    }

    /** 从当前购物车创建订单，逐行锁住库存避免超卖。 */
    @Transactional(rollbackFor = Exception.class)
    public OrderVo checkout() {
        Long userId = LoginHelper.getUserId();
        List<MallCart> carts = cartMapper.selectByUserIdForUpdate(userId);
        if (carts.isEmpty()) {
            throw new ServiceException("购物车为空，不能下单");
        }

        MallOrder order = new MallOrder();
        order.setOrderNo(IdGeneratorUtil.nextIdWithPrefix("M"));
        order.setUserId(userId);
        order.setStatus(PENDING_PAYMENT);
        order.setTotalAmount(BigDecimal.ZERO);
        orderMapper.insert(order);

        BigDecimal total = BigDecimal.ZERO;
        List<OrderItemVo> items = new ArrayList<>();
        for (MallCart cart : carts) {
            MallProduct product = productMapper.selectByIdForUpdate(cart.getProductId());
            if (product == null || product.getStatus() != ON_SHELF) {
                throw new ServiceException("商品已下架，请刷新购物车后重试");
            }
            if (product.getStock() < cart.getQuantity()) {
                throw new ServiceException("商品「{}」库存不足", product.getName());
            }
            int beforeStock = product.getStock();
            product.setStock(beforeStock - cart.getQuantity());
            productMapper.updateById(product);
            saveStockLog(product.getId(), -cart.getQuantity(), beforeStock, product.getStock(), "ORDER", order.getOrderNo(), "订单扣减库存");

            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity()));
            MallOrderItem item = new MallOrderItem();
            item.setOrderId(order.getId());
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setCoverUrl(product.getCoverUrl());
            item.setProductPrice(product.getPrice());
            item.setQuantity(cart.getQuantity());
            item.setSubtotalAmount(subtotal);
            orderItemMapper.insert(item);

            OrderItemVo itemVo = toOrderItemVo(item);
            items.add(itemVo);
            total = total.add(subtotal);
        }
        order.setTotalAmount(total);
        orderMapper.updateById(order);
        cartMapper.delete(new LambdaQueryWrapper<MallCart>().eq(MallCart::getUserId, userId));
        return toOrderVo(order, items);
    }

    /** 当前用户订单列表。 */
    public List<OrderVo> listOrders() {
        return orderMapper.selectList(new LambdaQueryWrapper<MallOrder>()
                .eq(MallOrder::getUserId, LoginHelper.getUserId()).orderByDesc(MallOrder::getCreateTime))
            .stream().map(this::toOrderVo).toList();
    }

    /** 当前用户订单详情。 */
    public OrderVo getOrder(Long orderId) {
        MallOrder order = requireUserOrder(orderId);
        return toOrderVo(order);
    }

    /** 未付款订单取消后回补库存。 */
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId) {
        MallOrder order = orderMapper.selectByIdForUpdate(orderId);
        if (order == null || !LoginHelper.getUserId().equals(order.getUserId())) {
            throw new ServiceException("订单不存在");
        }
        if (order.getStatus() != PENDING_PAYMENT) {
            throw new ServiceException("当前订单状态不能取消");
        }
        List<MallOrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<MallOrderItem>()
            .eq(MallOrderItem::getOrderId, orderId).orderByAsc(MallOrderItem::getProductId));
        for (MallOrderItem item : items) {
            MallProduct product = productMapper.selectByIdForUpdate(item.getProductId());
            if (product == null) {
                throw new ServiceException("订单商品不存在，无法回补库存");
            }
            int beforeStock = product.getStock();
            product.setStock(beforeStock + item.getQuantity());
            productMapper.updateById(product);
            saveStockLog(product.getId(), item.getQuantity(), beforeStock, product.getStock(), "CANCEL", order.getOrderNo(), "取消订单回补库存");
        }
        order.setStatus(CANCELED);
        order.setCancelTime(new java.util.Date());
        orderMapper.updateById(order);
    }

    /**
     * 支付确认：当前为站内模拟支付，确认后订单进入待发货状态。
     * 后续接入微信支付时，应由支付回调在验签成功后调用同等的状态流转逻辑。
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderVo payOrder(Long orderId) {
        MallOrder order = orderMapper.selectByIdForUpdate(orderId);
        if (order == null || !LoginHelper.getUserId().equals(order.getUserId())) {
            throw new ServiceException("订单不存在");
        }
        if (order.getStatus() != PENDING_PAYMENT) {
            throw new ServiceException("当前订单状态不能支付");
        }
        order.setStatus(PENDING_DELIVERY);
        orderMapper.updateById(order);
        return toOrderVo(order);
    }

    /** 后台商品列表。 */
    public List<MallProduct> listManageProducts() {
        return productMapper.selectList(new LambdaQueryWrapper<MallProduct>()
            .orderByAsc(MallProduct::getSort).orderByDesc(MallProduct::getCreateTime));
    }

    /** 后台新增商品。 */
    @Transactional(rollbackFor = Exception.class)
    public Long createProduct(ProductBo bo) {
        MallProduct product = new MallProduct();
        fillProduct(product, bo);
        productMapper.insert(product);
        saveStockLog(product.getId(), product.getStock(), 0, product.getStock(), "CREATE", null, "新建商品初始库存");
        return product.getId();
    }

    /** 后台更新商品，并将库存变化写入流水。 */
    @Transactional(rollbackFor = Exception.class)
    public void updateProduct(Long id, ProductBo bo) {
        MallProduct product = productMapper.selectByIdForUpdate(id);
        if (product == null) throw new ServiceException("商品不存在");
        int beforeStock = product.getStock();
        fillProduct(product, bo);
        product.setId(id);
        productMapper.updateById(product);
        int change = product.getStock() - beforeStock;
        if (change != 0) saveStockLog(id, change, beforeStock, product.getStock(), "EDIT", null, "编辑商品库存");
    }

    /** 后台人工调库存。 */
    @Transactional(rollbackFor = Exception.class)
    public void adjustStock(Long productId, StockAdjustBo bo) {
        if (bo.getChangeQuantity() == 0) throw new ServiceException("调整数量不能为 0");
        MallProduct product = productMapper.selectByIdForUpdate(productId);
        if (product == null) throw new ServiceException("商品不存在");
        int beforeStock = product.getStock();
        int afterStock = beforeStock + bo.getChangeQuantity();
        if (afterStock < 0) throw new ServiceException("库存不足，不能调减");
        product.setStock(afterStock);
        productMapper.updateById(product);
        saveStockLog(productId, bo.getChangeQuantity(), beforeStock, afterStock, "ADJUST", null, bo.getRemark());
    }

    private MallProduct requireOnShelfProduct(Long productId) {
        MallProduct product = productMapper.selectById(productId);
        if (product == null || product.getStatus() != ON_SHELF) throw new ServiceException("商品不存在或已下架");
        return product;
    }

    private MallCart requireUserCart(Long cartId) {
        MallCart cart = cartMapper.selectById(cartId);
        if (cart == null || !LoginHelper.getUserId().equals(cart.getUserId())) throw new ServiceException("购物车项不存在");
        return cart;
    }

    private MallOrder requireUserOrder(Long orderId) {
        MallOrder order = orderMapper.selectById(orderId);
        if (order == null || !LoginHelper.getUserId().equals(order.getUserId())) throw new ServiceException("订单不存在");
        return order;
    }

    private void validateCartQuantity(MallProduct product, Integer quantity) {
        if (product.getStock() < quantity) throw new ServiceException("商品库存不足");
    }

    private void fillProduct(MallProduct product, ProductBo bo) {
        product.setName(bo.getName());
        product.setCoverUrl(bo.getCoverUrl());
        product.setDescription(bo.getDescription());
        product.setPrice(bo.getPrice());
        product.setStock(bo.getStock());
        product.setStatus(bo.getStatus());
        product.setSort(bo.getSort() == null ? 0 : bo.getSort());
    }

    private void saveStockLog(Long productId, int change, int before, int after, String type, String businessNo, String remark) {
        MallStockLog log = new MallStockLog();
        log.setProductId(productId);
        log.setChangeQuantity(change);
        log.setBeforeStock(before);
        log.setAfterStock(after);
        log.setBusinessType(type);
        log.setBusinessNo(businessNo);
        log.setRemark(remark);
        stockLogMapper.insert(log);
    }

    private ProductVo toProductVo(MallProduct product) {
        ProductVo vo = new ProductVo();
        vo.setId(product.getId()); vo.setName(product.getName()); vo.setCoverUrl(product.getCoverUrl());
        vo.setDescription(product.getDescription()); vo.setPrice(product.getPrice()); vo.setStock(product.getStock());
        return vo;
    }

    private OrderVo toOrderVo(MallOrder order) {
        List<OrderItemVo> items = orderItemMapper.selectList(new LambdaQueryWrapper<MallOrderItem>()
                .eq(MallOrderItem::getOrderId, order.getId()))
            .stream().map(this::toOrderItemVo).toList();
        return toOrderVo(order, items);
    }

    private OrderVo toOrderVo(MallOrder order, List<OrderItemVo> items) {
        OrderVo vo = new OrderVo();
        vo.setId(order.getId()); vo.setOrderNo(order.getOrderNo()); vo.setTotalAmount(order.getTotalAmount());
        vo.setStatus(order.getStatus()); vo.setCreateTime(order.getCreateTime()); vo.setItems(items);
        return vo;
    }

    private OrderItemVo toOrderItemVo(MallOrderItem item) {
        OrderItemVo vo = new OrderItemVo();
        vo.setProductId(item.getProductId()); vo.setProductName(item.getProductName()); vo.setCoverUrl(item.getCoverUrl());
        vo.setProductPrice(item.getProductPrice()); vo.setQuantity(item.getQuantity()); vo.setSubtotalAmount(item.getSubtotalAmount());
        return vo;
    }
}
