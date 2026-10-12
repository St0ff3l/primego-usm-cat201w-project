package com.primego.product.servlet;

import com.primego.product.model.Category;
import com.primego.product.model.ProductDTO;
import com.primego.product.model.ProductImage;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MerchantProductEditServletTest {
    @Test
    void serializesEditDataAndImageOrder() {
        ProductDTO product = new ProductDTO();
        product.setProductId(12);
        product.setCategoryId(3);
        product.setProductName("Box \"A\"");
        product.setProductDescription("Red\\blue");
        product.setProductPrice(new BigDecimal("19.90"));
        product.setProductStockQuantity(4);
        product.setContactWhatsapp("60123456789");
        product.setProductStatus("OFF_SALE");

        ProductImage image = new ProductImage();
        image.setImageId(23);
        image.setImageUrl("assets/images/products/a.jpg");
        image.setImageIsPrimary(true);

        assertEquals("{\"code\":200,\"msg\":\"success\",\"data\":{\"product\":{\"productId\":12,\"categoryId\":3,\"productName\":\"Box \\\"A\\\"\",\"productDescription\":\"Red\\\\blue\",\"productPrice\":19.90,\"productStockQuantity\":4,\"contactWhatsapp\":\"60123456789\",\"productStatus\":\"OFF_SALE\"},\"categories\":[{\"id\":3,\"name\":\"Electronics\"}],\"images\":[{\"id\":23,\"url\":\"assets/images/products/a.jpg\",\"isPrimary\":true}]}}",
            MerchantProductEditServlet.productEditJson(product, List.of(new Category(3, "Electronics")), List.of(image)));
    }
}
