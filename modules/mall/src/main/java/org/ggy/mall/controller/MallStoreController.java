package org.ggy.mall.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ggy.common.core.domain.R;
import org.ggy.common.idempotent.annotation.RepeatSubmit;
import org.ggy.common.log.annotation.Log;
import org.ggy.common.log.enums.BusinessType;
import org.ggy.mall.domain.MallProduct;
import org.ggy.mall.domain.bo.CartItemBo;
import org.ggy.mall.domain.bo.ProductBo;
import org.ggy.mall.domain.bo.StockAdjustBo;
import org.ggy.mall.domain.vo.CartItemVo;
import org.ggy.mall.domain.vo.OrderVo;
import org.ggy.mall.domain.vo.ProductVo;
import org.ggy.mall.service.MallStoreService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 商城用户端和后台管理接口。 */
@SaCheckLogin
@RestController
@RequiredArgsConstructor
@RequestMapping("/mall")
public class MallStoreController {
    private final MallStoreService mallStoreService;

    @GetMapping("/products")
    public R<List<ProductVo>> products() { return R.ok(mallStoreService.listProducts()); }

    @GetMapping("/cart")
    public R<List<CartItemVo>> cart() { return R.ok(mallStoreService.listCart()); }

    @PostMapping("/cart")
    @RepeatSubmit
    public R<Void> addCart(@Valid @RequestBody CartItemBo bo) { mallStoreService.addCart(bo); return R.ok(); }

    @PutMapping("/cart/{cartId}")
    public R<Void> updateCart(@PathVariable Long cartId, @Valid @RequestBody CartItemBo bo) { mallStoreService.updateCart(cartId, bo); return R.ok(); }

    @DeleteMapping("/cart/{cartId}")
    public R<Void> deleteCart(@PathVariable Long cartId) { mallStoreService.removeCart(cartId); return R.ok(); }

    @PostMapping("/orders/checkout")
    @RepeatSubmit
    public R<OrderVo> checkout() { return R.ok(mallStoreService.checkout()); }

    @GetMapping("/orders")
    public R<List<OrderVo>> orders() { return R.ok(mallStoreService.listOrders()); }

    @GetMapping("/orders/{orderId}")
    public R<OrderVo> order(@PathVariable Long orderId) { return R.ok(mallStoreService.getOrder(orderId)); }

    @PostMapping("/orders/{orderId}/cancel")
    @RepeatSubmit
    public R<Void> cancelOrder(@PathVariable Long orderId) { mallStoreService.cancelOrder(orderId); return R.ok(); }

    /**
     * 支付订单。当前为模拟支付确认，真实支付应由第三方支付回调驱动状态更新。
     */
    @PostMapping("/orders/{orderId}/pay")
    @RepeatSubmit
    public R<OrderVo> payOrder(@PathVariable Long orderId) { return R.ok(mallStoreService.payOrder(orderId)); }

    @SaCheckPermission("mall:product:list")
    @GetMapping("/manage/products")
    public R<List<MallProduct>> manageProducts() { return R.ok(mallStoreService.listManageProducts()); }

    @Log(title = "商城商品", businessType = BusinessType.INSERT)
    @SaCheckPermission("mall:product:add")
    @PostMapping("/manage/products")
    public R<Long> createProduct(@Valid @RequestBody ProductBo bo) { return R.ok(mallStoreService.createProduct(bo)); }

    @Log(title = "商城商品", businessType = BusinessType.UPDATE)
    @SaCheckPermission("mall:product:edit")
    @PutMapping("/manage/products/{id}")
    public R<Void> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductBo bo) { mallStoreService.updateProduct(id, bo); return R.ok(); }

    @Log(title = "商城库存", businessType = BusinessType.UPDATE)
    @SaCheckPermission("mall:stock:adjust")
    @PostMapping("/manage/products/{id}/stock-adjustments")
    public R<Void> adjustStock(@PathVariable Long id, @Valid @RequestBody StockAdjustBo bo) { mallStoreService.adjustStock(id, bo); return R.ok(); }
}
