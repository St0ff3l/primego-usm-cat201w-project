package org.dromara.primego.catalog.service.impl;

import lombok.RequiredArgsConstructor;
import org.dromara.primego.catalog.domain.query.ProductQuery;
import org.dromara.primego.catalog.domain.vo.CategoryVo;
import org.dromara.primego.catalog.domain.vo.ProductVo;
import org.dromara.primego.catalog.domain.vo.ProductPageVo;
import org.dromara.primego.catalog.mapper.PrimegoCatalogMapper;
import org.dromara.primego.catalog.service.PrimegoCatalogService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PrimegoCatalogServiceImpl implements PrimegoCatalogService {

    private final PrimegoCatalogMapper catalogMapper;

    @Override
    @Cacheable(cacheNames = "primego:catalog:categories#5m#10m#100#1", key = "'active'")
    public List<CategoryVo> listCategories() {
        return catalogMapper.selectActiveCategories();
    }

    @Override
    @Cacheable(cacheNames = "primego:catalog:products#30s#2m#500#1", key = "#query.cacheKey")
    public ProductPageVo listProducts(ProductQuery query) {
        long total = catalogMapper.countProducts(query);
        List<ProductVo> rows = catalogMapper.selectProducts(query);
        return new ProductPageVo(rows, total, query.getPageNum(), query.getPageSize());
    }

    @Override
    @Cacheable(cacheNames = "primego:catalog:product#30s#2m#1000#1", key = "#productId", condition = "#productId != null")
    public ProductVo getProduct(Long productId) {
        return catalogMapper.selectProductById(productId);
    }
}
