package com.primego.product.servlet;

import com.primego.common.util.JsonUtil;
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
import java.util.Date;
import java.util.List;

@WebServlet("/admin/product/review/list")
public class AdminProductReviewListServlet extends HttpServlet {

    private final ProductDAO productDAO = new ProductDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        boolean jsonRequest = isJsonRequest(req);
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            if (jsonRequest) {
                writeFailure(resp, HttpServletResponse.SC_UNAUTHORIZED, "Please log in to review products.");
                return;
            }
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        User user = (User) session.getAttribute("user");
        if (user.getRole() != Role.ADMIN) {
            if (jsonRequest) {
                writeFailure(resp, HttpServletResponse.SC_FORBIDDEN, "Only administrators can review products.");
                return;
            }
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied");
            return;
        }

        String filter = req.getParameter("filter");
        List<ProductDTO> products;

        if ("history".equals(filter)) {
            products = productDAO.getReviewedProducts();
            req.setAttribute("currentFilter", "history");
        } else {
            products = productDAO.getProductsPendingReview();
            req.setAttribute("currentFilter", "pending");
        }

        if (jsonRequest) {
            String filterName = "history".equals(filter) ? "history" : "pending";
            writeSuccess(resp, "\"currentFilter\":" + JsonUtil.quote(filterName)
                + ",\"products\":" + productsJson(products));
            return;
        }

        req.setAttribute("products", products);

        req.getRequestDispatcher("/admin/product/review_list.jsp").forward(req, resp);
    }

    static String productsJson(List<ProductDTO> products) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < products.size(); i++) {
            if (i > 0) json.append(',');
            ProductDTO product = products.get(i);
            json.append("{\"productId\":").append(product.getProductId())
                .append(",\"productName\":").append(JsonUtil.quote(product.getProductName()))
                .append(",\"merchantName\":").append(JsonUtil.quote(product.getMerchantName()))
                .append(",\"categoryName\":").append(JsonUtil.quote(product.getCategoryName()))
                .append(",\"auditStatus\":").append(JsonUtil.quote(product.getAuditStatus()))
                .append(",\"productCreatedAt\":").append(dateJson(product.getProductCreatedAt()))
                .append(",\"productUpdatedAt\":").append(dateJson(product.getProductUpdatedAt()))
                .append(",\"hasBeenApproved\":").append(product.isHasBeenApproved()).append('}');
        }
        return json.append(']').toString();
    }

    private static String dateJson(Date date) {
        return JsonUtil.quote(date == null ? null : date.toString());
    }

    private boolean isJsonRequest(HttpServletRequest req) {
        String accept = req.getHeader("Accept");
        return accept != null && accept.contains("application/json");
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
