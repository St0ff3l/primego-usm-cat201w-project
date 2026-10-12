package com.primego.order.servlet;

import com.primego.common.util.JsonUtil;
import com.primego.order.dao.OrderDAO;
import com.primego.order.model.Order;
import com.primego.order.model.OrderItem;
import com.primego.user.model.Role;
import com.primego.user.model.User;
import com.primego.wallet.dao.WalletDAO;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

// Route: /merchant/order/order_management
@WebServlet("/merchant/order/order_management")
public class MerchantOrderServlet extends HttpServlet {

    private final OrderDAO orderDAO = new OrderDAO();
    private final WalletDAO walletDAO = new WalletDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        User user = (User) (session != null ? session.getAttribute("user") : null);
        boolean jsonRequest = isJsonRequest(req);

        if (user == null || user.getRole() != Role.MERCHANT) {
            if (jsonRequest) {
                writeFailure(resp, user == null ? HttpServletResponse.SC_UNAUTHORIZED : HttpServletResponse.SC_FORBIDDEN,
                    "Only merchants can view orders.");
                return;
            }
            resp.sendRedirect(req.getContextPath() + "/public/login.jsp");
            return;
        }

        String action = req.getParameter("action");
        if ("toReturnAddress".equals(action)) {
            int orderId;
            try {
                orderId = Integer.parseInt(req.getParameter("orderId"));
                if (orderId < 1) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                if (jsonRequest) {
                    writeFailure(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid order id.");
                    return;
                }
                session.setAttribute("message", "Invalid order id.");
                session.setAttribute("messageType", "error");
                resp.sendRedirect(req.getContextPath() + "/merchant/order/order_management?filter=return");
                return;
            }

            Order order = orderDAO.getOrderByIdForMerchant(orderId, user.getId());
            if (jsonRequest) {
                if (order == null) writeFailure(resp, HttpServletResponse.SC_NOT_FOUND, "Order not found or not accessible.");
                else writeSuccess(resp, "\"order\":" + orderJson(order));
                return;
            }

            req.setAttribute("order", order);
            req.getRequestDispatcher("/merchant/order/return_address.jsp").forward(req, resp);
            return;
        }

        List<Order> orders = orderDAO.getOrdersByMerchantId(user.getId());
        if (jsonRequest) {
            writeSuccess(resp, "\"orders\":" + ordersJson(orders));
            return;
        }

        req.setAttribute("orders", orders);
        req.getRequestDispatcher("/merchant/order/order_management.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String action = req.getParameter("action");
        boolean jsonRequest = isJsonRequest(req);

        HttpSession session = req.getSession(false);
        User merchant = (User) (session != null ? session.getAttribute("user") : null);

        if (merchant == null || merchant.getRole() != Role.MERCHANT) {
            if (jsonRequest) {
                writeFailure(resp, merchant == null ? HttpServletResponse.SC_UNAUTHORIZED : HttpServletResponse.SC_FORBIDDEN,
                    "Only merchants can update orders.");
                return;
            }
            resp.sendRedirect(req.getContextPath() + "/public/login.jsp");
            return;
        }

        // Persist the merchant's return address for a specific order.
        if ("saveReturnAddress".equals(action)) {
            String orderIdStr = req.getParameter("orderId");
            String returnAddress = req.getParameter("returnAddress");

            if (orderIdStr == null || orderIdStr.trim().isEmpty()) {
                session.setAttribute("message", "Missing order id.");
                session.setAttribute("messageType", "error");
                respond(req, resp, "/merchant/order/order_management?filter=return");
                return;
            }

            try {
                int orderId = Integer.parseInt(orderIdStr);
                if (returnAddress == null || returnAddress.trim().isEmpty()) {
                    session.setAttribute("message", "Please enter return address.");
                    session.setAttribute("messageType", "error");
                    respond(req, resp, "/merchant/order/order_management?action=toReturnAddress&orderId=" + orderId);
                    return;
                }

                if (orderDAO.getOrderByIdForMerchant(orderId, merchant.getId()) == null) {
                    session.setAttribute("message", "Order not found or not accessible.");
                    session.setAttribute("messageType", "error");
                } else if (orderDAO.agreeReturn(orderId, returnAddress.trim())) {
                    session.setAttribute("message", "Return address saved. Waiting for customer to ship back.");
                    session.setAttribute("messageType", "success");
                } else {
                    session.setAttribute("message", "Failed to save return address. Please try again.");
                    session.setAttribute("messageType", "error");
                }
            } catch (NumberFormatException e) {
                session.setAttribute("message", "Invalid order id.");
                session.setAttribute("messageType", "error");
            }

            respond(req, resp, "/merchant/order/order_management?filter=return");
            return;
        }

        // 1) Shipping flow
        if ("shipOrder".equals(action)) {
            String orderIdStr = req.getParameter("orderId");
            String trackingNumber = req.getParameter("trackingNumber");

            if (orderIdStr == null || trackingNumber == null || trackingNumber.trim().isEmpty()) {
                session.setAttribute("message", "Missing order id or tracking number.");
                session.setAttribute("messageType", "error");
            } else {
                try {
                    int orderId = Integer.parseInt(orderIdStr);
                    if (orderDAO.getOrderByIdForMerchant(orderId, merchant.getId()) == null) {
                        session.setAttribute("message", "Order not found or not accessible.");
                        session.setAttribute("messageType", "error");
                    } else if (orderDAO.shipOrder(orderId, trackingNumber.trim())) {
                        session.setAttribute("message", "Order shipped successfully!");
                        session.setAttribute("messageType", "success");
                    } else {
                        session.setAttribute("message", "Failed to ship order. Please try again.");
                        session.setAttribute("messageType", "error");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    session.setAttribute("message", "System error while shipping order.");
                    session.setAttribute("messageType", "error");
                }
            }
        }

        // 2) Refund/return flow
        else if ("processRefund".equals(action)) {
            String orderIdStr = req.getParameter("orderId");
            String decision = req.getParameter("decision");
            String merchantReason = req.getParameter("merchantReason");
            String refundType = req.getParameter("refundType");

            if (orderIdStr == null || orderIdStr.trim().isEmpty() || decision == null || decision.trim().isEmpty()) {
                session.setAttribute("message", "Missing order id or decision.");
                session.setAttribute("messageType", "error");
            } else {
                try {
                    int orderId = Integer.parseInt(orderIdStr);
                    Order order = orderDAO.getOrderByIdForMerchant(orderId, merchant.getId());

                    if (order == null) {
                        session.setAttribute("message", "Order not found.");
                        session.setAttribute("messageType", "error");
                    } else {
                        // Normalize refund type.
                        String rt = (refundType == null || refundType.trim().isEmpty()) ? "MONEY_ONLY" : refundType.trim();
                        if (!"MONEY_ONLY".equals(rt) && !"RETURN_AND_REFUND".equals(rt)) {
                            rt = "MONEY_ONLY";
                        }

                        // For MONEY_ONLY, approving the request means refunding immediately.
                        if ("MONEY_ONLY".equals(rt) && "agree_return".equals(decision)) {
                            decision = "confirm_return_receipt";
                        }

                        // Case A: merchant agrees to a return (first step for RETURN_AND_REFUND).
                        if ("agree_return".equals(decision)) {
                            // For RETURN_AND_REFUND, a return address is required.
                            if ("RETURN_AND_REFUND".equals(rt)) {
                                respond(req, resp, "/merchant/order/order_management?action=toReturnAddress&orderId=" + orderId);
                                return;
                            } else {
                                // MONEY_ONLY does not require a return address.
                                boolean ok = orderDAO.agreeReturn(orderId);
                                if (ok) {
                                    session.setAttribute("message", "Request Accepted. Waiting for customer to return items.");
                                    session.setAttribute("messageType", "success");
                                } else {
                                    session.setAttribute("message", "Failed to accept return request. Please try again.");
                                    session.setAttribute("messageType", "error");
                                }
                            }
                        }

                        // Case B: merchant confirms receipt of returned items (final step, triggers refund).
                        else if ("confirm_return_receipt".equals(decision)) {
                            // Refund from platform escrow back to the customer.
                            boolean refundSuccess = walletDAO.refundFromEscrowToCustomer(
                                    order.getCustomerId(),
                                    order.getTotalAmount()
                            );

                            if (refundSuccess) {
                                // Update refund status to approved/refunded.
                                boolean ok = orderDAO.approveRefundStatus(orderId);
                                if (ok) {
                                    session.setAttribute("message", "Refund processed successfully.");
                                    session.setAttribute("messageType", "success");
                                } else {
                                    session.setAttribute("message", "Refund sent but failed to update order status.");
                                    session.setAttribute("messageType", "error");
                                }
                            } else {
                                session.setAttribute("message", "Refund failed. Please try again.");
                                session.setAttribute("messageType", "error");
                            }
                        }

                        // Case C: merchant rejects the request.
                        else if ("reject".equals(decision)) {
                            if (merchantReason == null || merchantReason.trim().isEmpty()) {
                                session.setAttribute("message", "Please provide a rejection reason.");
                                session.setAttribute("messageType", "error");
                            } else {
                                boolean ok = orderDAO.rejectRefund(orderId, merchantReason);
                                if (ok) {
                                    session.setAttribute("message", "Refund Rejected.");
                                    session.setAttribute("messageType", "warning");
                                } else {
                                    session.setAttribute("message", "Failed to reject refund. Please try again.");
                                    session.setAttribute("messageType", "error");
                                }
                            }
                        } else {
                            session.setAttribute("message", "Invalid decision: " + decision);
                            session.setAttribute("messageType", "error");
                        }
                    }

                } catch (NumberFormatException e) {
                    session.setAttribute("message", "Invalid order id.");
                    session.setAttribute("messageType", "error");
                } catch (Exception e) {
                    e.printStackTrace();
                    session.setAttribute("message", "System Error processing refund.");
                    session.setAttribute("messageType", "error");
                }
            }
        }

        respond(req, resp, "/merchant/order/order_management?filter=return");
    }

    private boolean isJsonRequest(HttpServletRequest req) {
        String accept = req.getHeader("Accept");
        return accept != null && accept.contains("application/json");
    }

    private void respond(HttpServletRequest req, HttpServletResponse resp, String redirect) throws IOException {
        if (!isJsonRequest(req)) {
            resp.sendRedirect(req.getContextPath() + redirect);
            return;
        }

        HttpSession session = req.getSession(false);
        String message = session == null ? null : (String) session.getAttribute("message");
        String messageType = session == null ? null : (String) session.getAttribute("messageType");
        if (session != null) {
            session.removeAttribute("message");
            session.removeAttribute("messageType");
        }
        writeSuccess(resp, "\"message\":" + JsonUtil.quote(message)
            + ",\"messageType\":" + JsonUtil.quote(messageType)
            + ",\"redirect\":" + JsonUtil.quote(redirect));
    }

    static String ordersJson(List<Order> orders) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < orders.size(); i++) {
            if (i > 0) json.append(',');
            json.append(orderJson(orders.get(i)));
        }
        return json.append(']').toString();
    }

    static String orderJson(Order order) {
        StringBuilder json = new StringBuilder("{\"id\":").append(order.getOrdersId())
            .append(",\"totalAmount\":").append(decimalJson(order.getTotalAmount()))
            .append(",\"orderStatus\":").append(JsonUtil.quote(order.getOrderStatus()))
            .append(",\"address\":").append(JsonUtil.quote(order.getAddress()))
            .append(",\"createdAt\":").append(JsonUtil.quote(order.getCreatedAt() == null ? null : order.getCreatedAt().toString()))
            .append(",\"refundReason\":").append(JsonUtil.quote(order.getRefundReason()))
            .append(",\"rejectionCount\":").append(order.getRejectionCount())
            .append(",\"merchantRejectReason\":").append(JsonUtil.quote(order.getMerchantRejectReason()))
            .append(",\"refundStatus\":").append(JsonUtil.quote(order.getRefundStatus()))
            .append(",\"refundType\":").append(JsonUtil.quote(order.getRefundType()))
            .append(",\"returnAddress\":").append(JsonUtil.quote(order.getReturnAddress()))
            .append(",\"returnTrackingNumber\":").append(JsonUtil.quote(order.getReturnTrackingNumber()))
            .append(",\"trackingNumber\":").append(JsonUtil.quote(order.getTrackingNumber()))
            .append(",\"orderItems\":[");
        List<OrderItem> items = order.getOrderItems();
        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                if (i > 0) json.append(',');
                OrderItem item = items.get(i);
                json.append("{\"productName\":").append(JsonUtil.quote(item.getProductName()))
                    .append(",\"quantity\":").append(item.getQuantity())
                    .append(",\"subtotal\":").append(decimalJson(item.getSubtotal()))
                    .append(",\"productImageUrl\":").append(JsonUtil.quote(item.getProductImageUrl())).append('}');
            }
        }
        return json.append("]}").toString();
    }

    private static String decimalJson(BigDecimal value) {
        return value == null ? "null" : JsonUtil.quote(value.toPlainString());
    }

    private void writeSuccess(HttpServletResponse resp, String data) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        resp.getWriter().write("{\"code\":200,\"msg\":\"success\",\"data\":{" + data + "}}");
    }

    private void writeFailure(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        resp.getWriter().write("{\"code\":" + status + ",\"msg\":" + JsonUtil.quote(message) + "}");
    }
}
