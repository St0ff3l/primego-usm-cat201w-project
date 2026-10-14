package org.dromara.primego.catalog.domain.query;

import lombok.Getter;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Read-only storefront filters. */
@Getter
public class ProductQuery {

    public static final int MAX_KEYWORD_LENGTH = 100;
    public static final int DEFAULT_PAGE_SIZE = 24;
    public static final int MAX_PAGE_SIZE = 100;

    private final String keyword;
    private final Long categoryId;
    private final BigDecimal minPrice;
    private final BigDecimal maxPrice;
    private final Boolean inStockOnly;
    private final int pageNum;
    private final int pageSize;

    public ProductQuery(String keyword, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice, Boolean inStockOnly,
                        Integer pageNum, Integer pageSize) {
        String normalizedKeyword = keyword == null ? null : keyword.trim();
        this.keyword = normalizedKeyword == null || normalizedKeyword.isEmpty() ? null : normalizedKeyword;
        this.categoryId = categoryId;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.inStockOnly = inStockOnly;
        this.pageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        this.pageSize = pageSize == null || pageSize < 1
            ? DEFAULT_PAGE_SIZE
            : Math.min(pageSize, MAX_PAGE_SIZE);
    }

    public long getOffset() {
        return (long) (pageNum - 1) * pageSize;
    }

    /** Stable key component for Redis query caching. */
    public String getCacheKey() {
        String encodedKeyword = keyword == null ? "" : URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        return encodedKeyword + "|" + value(categoryId) + "|" + value(minPrice) + "|" + value(maxPrice)
            + "|" + value(inStockOnly) + "|" + pageNum + "|" + pageSize;
    }

    private String value(Object value) {
        return value == null ? "" : value.toString();
    }
}
