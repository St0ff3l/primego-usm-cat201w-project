package com.primego.user.servlet;

import com.primego.order.model.Order;
import com.primego.product.model.ProductDTO;
import com.primego.user.model.User;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MerchantDashboardServletTest {
    @Test
    void summarizesOrderStatusesAndCapsLowStockPreview() {
        User user = new User();
        user.setUsername("Seller");

        List<Order> orders = new ArrayList<>();
        orders.add(order("PAID"));
        orders.add(order("SHIPPED"));
        orders.add(order("COMPLETED"));
        orders.add(order("CANCELLED"));

        List<ProductDTO> products = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            ProductDTO product = new ProductDTO();
            product.setProductId(i);
            product.setProductName("Item " + i);
            product.setProductStockQuantity(5);
            products.add(product);
        }

        String json = MerchantDashboardServlet.dashboardJson(user, orders, products);
        assertTrue(json.contains("\"toShipCount\":1"));
        assertTrue(json.contains("\"shippedCount\":1"));
        assertTrue(json.contains("\"completedCount\":1"));
        assertTrue(json.contains("\"lowStockCount\":6"));
        assertTrue(json.contains("\"productId\":5"));
        assertTrue(!json.contains("\"productId\":6"));
        assertTrue(json.contains("\"hasMoreLowStock\":true"));
    }

    private Order order(String status) {
        Order order = new Order();
        order.setOrderStatus(status);
        return order;
    }
}
