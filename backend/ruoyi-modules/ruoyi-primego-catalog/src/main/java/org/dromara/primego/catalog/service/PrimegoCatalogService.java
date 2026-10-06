package org.dromara.primego.catalog.service;

import org.dromara.primego.catalog.domain.query.ProductQuery;
import org.dromara.primego.catalog.domain.vo.CategoryVo;
import org.dromara.primego.catalog.domain.vo.ProductVo;
import org.dromara.primego.catalog.domain.vo.ProductPageVo;

import java.util.List;

public interface PrimegoCatalogService {
    List<CategoryVo> listCategories();

    ProductPageVo listProducts(ProductQuery query);

    ProductVo getProduct(Long productId);
}
