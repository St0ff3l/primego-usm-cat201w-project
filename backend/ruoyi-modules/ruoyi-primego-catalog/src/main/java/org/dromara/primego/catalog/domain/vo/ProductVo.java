package org.dromara.primego.catalog.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductVo {
    private Long id;
    private Long categoryId;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
    private String categoryName;
    private String merchantName;
    private String imageUrl;
}
