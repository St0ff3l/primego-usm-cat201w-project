package org.dromara.primego.catalog.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.dromara.primego.catalog.domain.query.ProductQuery;
import org.dromara.primego.catalog.domain.vo.CategoryVo;
import org.dromara.primego.catalog.domain.vo.ProductVo;

import java.util.List;

@Mapper
public interface PrimegoCatalogMapper {
    List<CategoryVo> selectActiveCategories();

    List<ProductVo> selectProducts(@Param("query") ProductQuery query);

    long countProducts(@Param("query") ProductQuery query);

    ProductVo selectProductById(@Param("productId") Long productId);
}
