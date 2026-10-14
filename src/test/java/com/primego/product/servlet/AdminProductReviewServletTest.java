package com.primego.product.servlet;

import com.primego.product.model.ProductDTO;
import com.primego.product.model.ProductImage;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminProductReviewServletTest {
    @Test
    void serializesReviewQueueRowsAndEscapesText() {
        ProductDTO product = new ProductDTO();
        product.setProductId(12);
        product.setProductName("Tea \"Special\"");
        product.setMerchantName("seller");
        product.setCategoryName("Food");
        product.setAuditStatus("PENDING");
        product.setProductCreatedAt(Timestamp.valueOf("2026-10-12 10:00:00"));
        product.setProductUpdatedAt(Timestamp.valueOf("2026-10-12 11:00:00"));
        product.setHasBeenApproved(true);

        String json = AdminProductReviewListServlet.productsJson(List.of(product));

        assertTrue(json.contains("\"productName\":\"Tea \\\"Special\\\"\""));
        assertTrue(json.contains("\"productCreatedAt\":\"2026-10-12 10:00:00.0\""));
        assertTrue(json.contains("\"hasBeenApproved\":true"));
    }

    @Test
    void serializesReviewDetailAndImages() {
        ProductDTO product = new ProductDTO();
        product.setProductId(21);
        product.setProductName("Fresh bread");
        product.setAuditStatus("PENDING");
        product.setAuditMessage("Needs label");
        product.setMerchantName("baker");
        product.setCategoryName("Bakery");
        product.setProductPrice(new BigDecimal("8.50"));
        product.setProductStockQuantity(7);
        product.setProductDescription("Whole grain");

        ProductImage image = new ProductImage();
        image.setImageId(9);
        image.setImageUrl("assets/images/bread.jpg");

        String productJson = AdminProductReviewDetailServlet.productJson(product);
        String imagesJson = AdminProductReviewDetailServlet.imagesJson(List.of(image));

        assertTrue(productJson.contains("\"productPrice\":\"8.50\""));
        assertTrue(productJson.contains("\"productDescription\":\"Whole grain\""));
        assertTrue(imagesJson.equals("[{\"imageId\":9,\"imageUrl\":\"assets/images/bread.jpg\"}]"));
    }
}
