package org.dromara.primego.catalog.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class ProductPageVo {
    private List<ProductVo> rows;
    private long total;
    private int pageNum;
    private int pageSize;

    public ProductPageVo(List<ProductVo> rows, long total, int pageNum, int pageSize) {
        this.rows = rows;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }
}
