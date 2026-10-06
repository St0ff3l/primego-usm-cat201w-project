package org.dromara.primego.catalog.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.primego.catalog.domain.query.ProductQuery;
import org.dromara.primego.catalog.domain.vo.CategoryVo;
import org.dromara.primego.catalog.domain.vo.ProductVo;
import org.dromara.primego.catalog.domain.vo.ProductPageVo;
import org.dromara.primego.catalog.service.PrimegoCatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/** PrimeGo storefront's public catalog endpoints. */
@SaIgnore
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class PrimegoCatalogController {

    private final PrimegoCatalogService catalogService;

    @GetMapping("/categories")
    public R<List<CategoryVo>> categories() {
        return R.ok(catalogService.listCategories());
    }

    @GetMapping("/products")
    public R<ProductPageVo> products(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(required = false) Boolean inStockOnly,
        @RequestParam(required = false) Integer pageNum,
        @RequestParam(required = false) Integer pageSize
    ) {
        if (keyword != null && keyword.length() > ProductQuery.MAX_KEYWORD_LENGTH) {
            return R.fail(400, "keyword must be at most 100 characters");
        }
        ProductQuery query = new ProductQuery(keyword, categoryId, minPrice, maxPrice, inStockOnly, pageNum, pageSize);
        return R.ok(catalogService.listProducts(query));
    }

    @GetMapping("/products/{productId}")
    public R<ProductVo> product(@PathVariable Long productId) {
        ProductVo product = catalogService.getProduct(productId);
        if (product == null) {
            return R.fail(404, "Product not found");
        }
        return R.ok(product);
    }
}
