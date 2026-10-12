package com.primego.order.servlet;

import com.primego.common.util.JsonUtil;
import com.primego.order.dao.OrderDAO;
import com.primego.order.model.Order;
import com.primego.order.model.OrderItem;
import com.primego.product.dao.ProductDAO;
import com.primego.product.model.ProductDTO;
import com.primego.user.model.User;
import com.primego.wallet.dao.WalletDAO;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

@WebServlet("/customer/orders")
public class CustomerOrderServlet extends HttpServlet {

    private final OrderDAO orderDAO = new OrderDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final WalletDAO walletDAO = new WalletDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            if (isJsonRequest(req)) {
                writeFailure(resp, HttpServletResponse.SC_UNAUTHORIZED, "Please log in to view your orders.");
                return;
            }
            resp.sendRedirect(req.getContextPath() + "/public/login.jsp");
            return;
        }

        User user = (User) session.getAttribute("user");
        int userId = user.getId();

        // Action parameter used for special navigation/flows.
        String action = req.getParameter("action");

        // Forward to the refund application page (intermediate step).
        if ("toRefundPage".equals(action)) {
            String orderIdStr = req.getParameter("orderId");
            if (orderIdStr != null) {
                try {
                    int orderId = Integer.parseInt(orderIdStr);
                    Order order = orderDAO.getOrderById(orderId);

                    // Security check: only allow viewing the current user's order, and only when COMPLETED.
                    if (order != null && order.getCustomerId() == userId && "COMPLETED".equals(order.getOrderStatus())) {
                        req.setAttribute("refundOrder", order);
                        req.getRequestDispatcher("/customer/order/refund_application.jsp").forward(req, resp);
                        return; // Stop processing after forward.
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
            // Fallback to the orders list on invalid input or failed validation.
            resp.sendRedirect(req.getContextPath() + "/customer/orders");
            return;
        }

        // Default behavior: display the orders list.
        String status = (String) req.getAttribute("jsonOrderStatus");
        if (status == null) status = req.getParameter("status");
        List<Order> orders;

        // Special filter for after-sales/returns-related orders.
        if ("RETURNS".equals(status)) {
            // Include all after-sales orders (including rejected SHIPPED orders).
            orders = orderDAO.getReturnOrdersByUserId(userId);
            req.setAttribute("currentStatus", "RETURNS");
        } else if (status != null && !status.isEmpty() && !"ALL".equals(status)) {
            orders = orderDAO.getOrdersByUserIdAndStatus(userId, status);
            req.setAttribute("currentStatus", status);
        } else {
            orders = orderDAO.getOrdersByUserId(userId);
            req.setAttribute("currentStatus", "ALL");
        }

        if (isJsonRequest(req)) {
            writeOrders(resp, session, orders, (String) req.getAttribute("currentStatus"));
            return;
        }
        req.setAttribute("orderList", orders);
        req.getRequestDispatcher("/customer/order/my_order.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            if (isJsonRequest(req)) {
                writeFailure(resp, HttpServletResponse.SC_UNAUTHORIZED, "Please log in to update your orders.");
                return;
            }
            resp.sendRedirect(req.getContextPath() + "/public/login.jsp");
            return;
        }

        String action = req.getParameter("action");
        String status = req.getParameter("status");
        if (status == null || status.trim().isEmpty()) {
            status = "ALL";
        }

        if ("cancelOrder".equals(action)) {
            handleCancelOrder(req);
            respondAfterAction(req, resp, status);
            return;
        }

        if ("confirmReceipt".equals(action)) {
            handleConfirmReceipt(req);
            respondAfterAction(req, resp, status);
            return;
        }

        // Return/refund request (SHIPPED only). Support both action names.
        if ("processRefundRequest".equals(action) || "requestRefund".equals(action)) {
            handleReturnRequest(req);
            respondAfterAction(req, resp, status);
            return;
        }

        // Buyer confirmation that the return shipment has been sent.
        if ("confirmReturnShipped".equals(action)) {
            String orderIdStr = req.getParameter("orderId");
            String returnTrackingNumber = req.getParameter("returnTrackingNumber");

            if (orderIdStr != null) {
                try {
                    int orderId = Integer.parseInt(orderIdStr);
                    Order order = orderDAO.getOrderById(orderId);

                    if (order == null || order.getCustomerId() != ((User) session.getAttribute("user")).getId()) {
                        session.setAttribute("message", "Order not found.");
                        session.setAttribute("messageType", "error");
                    } else if (!"WAITING_RETURN".equals(order.getRefundStatus())) {
                        session.setAttribute("message", "This order is not waiting for return shipment.");
                        session.setAttribute("messageType", "error");
                    } else if (returnTrackingNumber == null || returnTrackingNumber.trim().isEmpty()) {
                        session.setAttribute("message", "Please enter return tracking number.");
                        session.setAttribute("messageType", "error");
                    } else if (orderDAO.buyerConfirmShipped(orderId, returnTrackingNumber.trim())) {
                        session.setAttribute("message", "Return shipment submitted. Waiting for merchant to receive.");
                        session.setAttribute("messageType", "success");
                    } else {
                        session.setAttribute("message", "Failed to submit return tracking number.");
                        session.setAttribute("messageType", "error");
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    session.setAttribute("message", "Invalid order id.");
                    session.setAttribute("messageType", "error");
                }
            } else {
                session.setAttribute("message", "Missing order id.");
                session.setAttribute("messageType", "error");
            }
            // Redirect back to the Returns list.
            respondAfterAction(req, resp, "RETURNS");
            return;
        }

        respondAfterAction(req, resp, status);
    }

    private boolean isJsonRequest(HttpServletRequest req) {
        return req.getHeader("Accept") != null && req.getHeader("Accept").contains("application/json");
    }

    private void respondAfterAction(HttpServletRequest req, HttpServletResponse resp, String status)
            throws ServletException, IOException {
        if (isJsonRequest(req)) {
            req.setAttribute("jsonOrderStatus", status);
            doGet(req, resp);
        } else {
            resp.sendRedirect(req.getContextPath() + "/customer/orders?status=" + status);
        }
    }

    private void writeOrders(HttpServletResponse resp, HttpSession session, List<Order> orders, String status)
            throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");

        StringBuilder json = new StringBuilder("{\"code\":200,\"msg\":\"success\",\"data\":{\"currentStatus\":")
            .append(JsonUtil.quote(status)).append(",\"message\":")
            .append(JsonUtil.quote((String) session.getAttribute("message"))).append(",\"messageType\":")
            .append(JsonUtil.quote((String) session.getAttribute("messageType"))).append(",\"orders\":[");

        Object message = session.getAttribute("message");
        if (message != null) session.removeAttribute("message");
        if (session.getAttribute("messageType") != null) session.removeAttribute("messageType");

        for (int i = 0; i < orders.size(); i++) {
            if (i > 0) json.append(',');
            Order order = orders.get(i);
            json.append("{\"id\":").append(order.getOrdersId())
                .append(",\"totalAmount\":").append(decimal(order.getTotalAmount()))
                .append(",\"orderStatus\":").append(JsonUtil.quote(order.getOrderStatus()))
                .append(",\"address\":").append(JsonUtil.quote(order.getAddress()))
                .append(",\"createdAt\":").append(JsonUtil.quote(order.getCreatedAt() == null ? null : order.getCreatedAt().toString()))
                .append(",\"trackingNumber\":").append(JsonUtil.quote(order.getTrackingNumber()))
                .append(",\"rejectionCount\":").append(order.getRejectionCount())
                .append(",\"merchantRejectReason\":").append(JsonUtil.quote(order.getMerchantRejectReason()))
                .append(",\"refundStatus\":").append(JsonUtil.quote(order.getRefundStatus()))
                .append(",\"refundType\":").append(JsonUtil.quote(order.getRefundType()))
                .append(",\"returnAddress\":").append(JsonUtil.quote(order.getReturnAddress()))
                .append(",\"returnTrackingNumber\":").append(JsonUtil.quote(order.getReturnTrackingNumber()))
                .append(",\"canReturn\":").append(canReturn(order))
                .append(",\"items\":[");
            List<OrderItem> items = order.getOrderItems();
            if (items != null) {
                for (int j = 0; j < items.size(); j++) {
                    if (j > 0) json.append(',');
                    OrderItem item = items.get(j);
                    json.append("{\"productId\":").append(item.getProductId())
                        .append(",\"productName\":").append(JsonUtil.quote(item.getProductName()))
                        .append(",\"quantity\":").append(item.getQuantity())
                        .append(",\"subtotal\":").append(decimal(item.getSubtotal()))
                        .append(",\"productImageUrl\":").append(JsonUtil.quote(item.getProductImageUrl())).append('}');
                }
            }
            json.append("]}");
        }
        json.append("]}}");
        resp.getWriter().write(json.toString());
    }

    private boolean canReturn(Order order) {
        if (!"SHIPPED".equals(order.getOrderStatus())) return false;
        long baseTime = order.getCreatedAt() == null ? System.currentTimeMillis() : order.getCreatedAt().getTime();
        return (System.currentTimeMillis() - baseTime) / (1000L * 60 * 60 * 24) <= 7;
    }

    private String decimal(java.math.BigDecimal value) {
        return value == null ? "null" : value.toPlainString();
    }

    private void writeFailure(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        resp.getWriter().write("{\"code\":" + status + ",\"msg\":" + JsonUtil.quote(message) + "}");
    }

    private void handleCancelOrder(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            return;
        }

        User user = (User) session.getAttribute("user");
        int userId = user.getId();

        String orderIdStr = req.getParameter("orderId");
        if (orderIdStr == null) {
            session.setAttribute("message", "Missing order id.");
            session.setAttribute("messageType", "error");
            return;
        }

        try {
            int orderId = Integer.parseInt(orderIdStr);
            Order order = orderDAO.getOrderById(orderId);

            if (order == null || order.getCustomerId() != userId) {
                session.setAttribute("message", "Order not found.");
                session.setAttribute("messageType", "error");
                return;
            }

            if (!"PAID".equals(order.getOrderStatus())) {
                session.setAttribute("message", "Only PAID orders can be cancelled.");
                session.setAttribute("messageType", "error");
                return;
            }

            // Only updates the order status here; refund/restock handling is not performed in this servlet.
            boolean success = orderDAO.updateOrderStatus(orderId, "CANCELLED");
            if (success) {
                session.setAttribute("message", "Order cancelled successfully.");
                session.setAttribute("messageType", "success");
            } else {
                session.setAttribute("message", "Failed to cancel order.");
                session.setAttribute("messageType", "error");
            }
        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute("message", "Failed to cancel order.");
            session.setAttribute("messageType", "error");
        }
    }

    // SHIPPED -> RETURN_REQUESTED
    private void handleReturnRequest(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            return;
        }

        User user = (User) session.getAttribute("user");
        int userId = user.getId();

        String orderIdStr = req.getParameter("orderId");
        String reason = req.getParameter("reason");
        String refundType = req.getParameter("refundType");

        if (orderIdStr == null) {
            session.setAttribute("message", "Missing order id.");
            session.setAttribute("messageType", "error");
            return;
        }

        try {
            int orderId = Integer.parseInt(orderIdStr);
            Order order = orderDAO.getOrderById(orderId);

            if (order == null || order.getCustomerId() != userId) {
                session.setAttribute("message", "Order not found.");
                session.setAttribute("messageType", "error");
                return;
            }

            if (!"SHIPPED".equals(order.getOrderStatus())) {
                session.setAttribute("message", "Only SHIPPED orders can request a return.");
                session.setAttribute("messageType", "error");
                return;
            }

            // 7-day return window (uses createdAt as a fallback; shipped time is preferred if available).
            long now = System.currentTimeMillis();
            long baseTime = (order.getCreatedAt() != null) ? order.getCreatedAt().getTime() : now;
            long daysDiff = (now - baseTime) / (1000L * 60 * 60 * 24);
            if (daysDiff > 7) {
                session.setAttribute("message", "Sorry, return period expired.");
                session.setAttribute("messageType", "error");
                return;
            }

            boolean success = orderDAO.requestRefund(orderId, reason, user.getId(), refundType);
            if (success) {
                session.setAttribute("message", "Return request submitted successfully!");
                session.setAttribute("messageType", "success");
            } else {
                session.setAttribute("message", "Failed to submit request.");
                session.setAttribute("messageType", "error");
            }
        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute("message", "Failed to submit request.");
            session.setAttribute("messageType", "error");
        }
    }

    private void handleConfirmReceipt(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            return;
        }

        User user = (User) session.getAttribute("user");
        int userId = user.getId();

        String orderIdStr = req.getParameter("orderId");
        if (orderIdStr == null) {
            session.setAttribute("message", "Missing order id.");
            session.setAttribute("messageType", "error");
            return;
        }

        try {
            int orderId = Integer.parseInt(orderIdStr);
            Order order = orderDAO.getOrderById(orderId);

            if (order == null || order.getCustomerId() != userId) {
                session.setAttribute("message", "Order not found.");
                session.setAttribute("messageType", "error");
                return;
            }

            if (!"SHIPPED".equals(order.getOrderStatus())) {
                session.setAttribute("message", "Only shipped orders can be confirmed.");
                session.setAttribute("messageType", "error");
                return;
            }

            // SHIPPED -> COMPLETED
            if (!order.getOrderItems().isEmpty()) {
                int productId = order.getOrderItems().get(0).getProductId();
                ProductDTO product = productDAO.getProductById(productId);
                if (product != null) {
                    walletDAO.creditMerchantBalance(product.getMerchantId(), order.getTotalAmount());
                    orderDAO.updateOrderStatus(orderId, "COMPLETED");
                    session.setAttribute("message", "Order confirmed!");
                    session.setAttribute("messageType", "success");
                    return;
                }
            }

            session.setAttribute("message", "Failed to confirm order.");
            session.setAttribute("messageType", "error");
        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute("message", "Failed to confirm order.");
            session.setAttribute("messageType", "error");
        }
    }
}
