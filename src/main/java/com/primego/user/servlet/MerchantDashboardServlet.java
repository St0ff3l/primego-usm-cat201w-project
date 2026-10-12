package com.primego.user.servlet;

import com.primego.common.util.JsonUtil;
import com.primego.order.dao.OrderDAO;
import com.primego.order.model.Order;
import com.primego.product.dao.ProductDAO;
import com.primego.product.model.ProductDTO;
import com.primego.user.model.Role;
import com.primego.user.model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/api/merchant/dashboard")
public class MerchantDashboardServlet extends HttpServlet {
    private final OrderDAO orderDAO = new OrderDAO();
    private final ProductDAO productDAO = new ProductDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            writeFailure(resp, HttpServletResponse.SC_UNAUTHORIZED, "Please log in to view the seller center.");
            return;
        }
        if (user.getRole() != Role.MERCHANT && user.getRole() != Role.ADMIN) {
            writeFailure(resp, HttpServletResponse.SC_FORBIDDEN, "The seller center is not available for this account.");
            return;
        }

        try {
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.setHeader("Cache-Control", "no-store");
            resp.getWriter().write(dashboardJson(user,
                orderDAO.getOrdersByMerchantId(user.getId()),
                productDAO.getProductsByMerchantId(user.getId())));
        } catch (RuntimeException e) {
            writeFailure(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Unable to load seller center data.");
        }
    }

    static String dashboardJson(User user, List<Order> orders, List<ProductDTO> products) {
        int toShipCount = 0;
        int shippedCount = 0;
        int completedCount = 0;
        for (Order order : orders) {
            String status = order.getOrderStatus();
            if ("PAID".equalsIgnoreCase(status)) toShipCount++;
            else if ("SHIPPED".equalsIgnoreCase(status)) shippedCount++;
            else if ("COMPLETED".equalsIgnoreCase(status)) completedCount++;
        }

        int lowStockCount = 0;
        List<ProductDTO> lowStockItems = new ArrayList<>();
        for (ProductDTO product : products) {
            if (product.getProductStockQuantity() <= 5) {
                lowStockCount++;
                if (product.getProductName() != null && !product.getProductName().trim().isEmpty()) {
                    lowStockItems.add(product);
                }
            }
        }

        StringBuilder json = new StringBuilder("{\"code\":200,\"msg\":\"success\",\"data\":{\"username\":")
            .append(JsonUtil.quote(user.getUsername()))
            .append(",\"toShipCount\":").append(toShipCount)
            .append(",\"shippedCount\":").append(shippedCount)
            .append(",\"completedCount\":").append(completedCount)
            .append(",\"lowStockCount\":").append(lowStockCount)
            .append(",\"lowStockItems\":[");
        int previewCount = Math.min(5, lowStockItems.size());
        for (int i = 0; i < previewCount; i++) {
            if (i > 0) json.append(',');
            ProductDTO product = lowStockItems.get(i);
            json.append("{\"productId\":").append(product.getProductId())
                .append(",\"name\":").append(JsonUtil.quote(product.getProductName().trim()))
                .append(",\"stock\":").append(product.getProductStockQuantity()).append('}');
        }
        return json.append("],\"hasMoreLowStock\":").append(lowStockItems.size() > 5).append("}}").toString();
    }

    private void writeFailure(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        resp.getWriter().write("{\"code\":" + status + ",\"msg\":" + JsonUtil.quote(message) + "}");
    }
}
