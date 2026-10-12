package com.primego.user.servlet;

import com.primego.order.model.Order;
import com.primego.user.model.Role;
import com.primego.user.model.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminDashboardServletTest {
    @Test
    void userJsonOmitsPasswordsAndEscapesUserText() {
        User user = new User();
        user.setId(12);
        user.setUsername("Admin \"One\"");
        user.setPassword("must-not-leak");
        user.setRole(Role.ADMIN);
        user.setStatus(1);
        user.setCreatedAt(Timestamp.valueOf("2026-10-12 10:30:00"));

        String json = AdminDashboardServlet.usersJson(List.of(user));

        assertTrue(json.contains("\"username\":\"Admin \\\"One\\\"\""));
        assertTrue(json.contains("\"role\":\"ADMIN\""));
        assertFalse(json.contains("must-not-leak"));
        assertFalse(json.contains("password"));
    }

    @Test
    void transactionJsonKeepsMoneyPrecisionAndTrackingOnlyWhenRequested() {
        Order order = new Order();
        order.setOrdersId(41);
        order.setCustomerId(12);
        order.setTotalAmount(new BigDecimal("31.40"));
        order.setCreatedAt(Timestamp.valueOf("2026-10-12 10:30:00"));
        order.setTrackingNumber("OUT-456");

        String regular = AdminDashboardServlet.ordersJson(List.of(order), false);
        String shipped = AdminDashboardServlet.ordersJson(List.of(order), true);

        assertTrue(regular.contains("\"totalAmount\":\"31.40\""));
        assertFalse(regular.contains("trackingNumber"));
        assertTrue(shipped.contains("\"trackingNumber\":\"OUT-456\""));
    }
}
