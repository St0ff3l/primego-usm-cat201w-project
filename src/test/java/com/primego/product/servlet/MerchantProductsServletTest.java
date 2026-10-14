package com.primego.product.servlet;

import com.primego.product.model.ProductDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MerchantProductsServletTest {
    @Test
    void serializesMerchantProductFieldsAndEscapesNames() {
        ProductDTO product = new ProductDTO();
        product.setProductId(12);
        product.setProductName("Box \"A\"");
        product.setCategoryName("Food");
        product.setProductPrice(new BigDecimal("19.90"));
        product.setProductStockQuantity(0);
        product.setProductStatus("OFF_SALE");
        product.setAuditStatus("APPROVED");
        product.setPrimaryImageUrl("assets/images/products/a.jpg");

        assertEquals("[{\"productId\":12,\"productName\":\"Box \\\"A\\\"\",\"categoryName\":\"Food\",\"productPrice\":19.90,\"productStockQuantity\":0,\"productStatus\":\"OFF_SALE\",\"auditStatus\":\"APPROVED\",\"primaryImageUrl\":\"assets/images/products/a.jpg\"}]",
            MerchantProductsServlet.productsJson(List.of(product)));
    }
}
