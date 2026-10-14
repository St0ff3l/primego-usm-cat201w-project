package com.primego.order.servlet;

import com.primego.order.model.Order;
import com.primego.order.model.OrderItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MerchantOrderServletTest {
    @Test
    void serializesOrderFieldsAndEscapesCustomerProvidedText() {
        Order order = new Order();
        order.setOrdersId(41);
        order.setTotalAmount(new BigDecimal("31.40"));
        order.setOrderStatus("RETURN_REQUESTED");
        order.setAddress("Unit 3\nCall first");
        order.setCreatedAt(Timestamp.valueOf("2026-10-12 10:30:00"));
        order.setRefundReason("Seal \"broken\"");
        order.setRejectionCount(1);
        order.setMerchantRejectReason("Item was used");
        order.setRefundStatus("RETURN_SHIPPED");
        order.setRefundType("RETURN_AND_REFUND");
        order.setReturnAddress("Seller's store");
        order.setReturnTrackingNumber("RT-123");
        order.setTrackingNumber("OUT-456");

        OrderItem item = new OrderItem();
        item.setProductName("Tea");
        item.setQuantity(2);
        item.setSubtotal(new BigDecimal("31.40"));
        item.setProductImageUrl("assets/tea.jpg");
        order.setOrderItems(List.of(item));

        String json = MerchantOrderServlet.orderJson(order);

        assertTrue(json.startsWith("{\"id\":41,\"totalAmount\":\"31.40\""));
        assertTrue(json.contains("\"address\":\"Unit 3\\nCall first\""));
        assertTrue(json.contains("\"refundReason\":\"Seal \\\"broken\\\"\""));
        assertTrue(json.contains("\"returnTrackingNumber\":\"RT-123\""));
        assertTrue(json.endsWith("\"orderItems\":[{\"productName\":\"Tea\",\"quantity\":2,\"subtotal\":\"31.40\",\"productImageUrl\":\"assets/tea.jpg\"}]}"));
    }

    @Test
    void serializesAnEmptyOrderList() {
        assertEquals("[]", MerchantOrderServlet.ordersJson(List.of()));
    }
}
