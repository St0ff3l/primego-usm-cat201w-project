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
import java.util.List;

@WebServlet("/api/merchant/products")
public class MerchantProductsServlet extends HttpServlet {
    private final ProductDAO productDAO = new ProductDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            writeFailure(resp, HttpServletResponse.SC_UNAUTHORIZED, "Please log in to view your products.");
            return;
        }
        if (user.getRole() != Role.MERCHANT && user.getRole() != Role.ADMIN) {
            writeFailure(resp, HttpServletResponse.SC_FORBIDDEN, "Seller products are not available for this account.");
            return;
        }

        try {
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.setHeader("Cache-Control", "no-store");
            resp.getWriter().write("{\"code\":200,\"msg\":\"success\",\"data\":{\"products\":"
                + productsJson(productDAO.getProductsByMerchantId(user.getId())) + "}}");
        } catch (RuntimeException e) {
            writeFailure(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Unable to load your products.");
        }
    }

    static String productsJson(List<ProductDTO> products) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < products.size(); i++) {
            if (i > 0) json.append(',');
            ProductDTO product = products.get(i);
            json.append("{\"productId\":").append(product.getProductId())
                .append(",\"productName\":").append(JsonUtil.quote(product.getProductName()))
                .append(",\"categoryName\":").append(JsonUtil.quote(product.getCategoryName()))
                .append(",\"productPrice\":").append(product.getProductPrice() == null ? "null" : product.getProductPrice().toPlainString())
                .append(",\"productStockQuantity\":").append(product.getProductStockQuantity())
                .append(",\"productStatus\":").append(JsonUtil.quote(product.getProductStatus()))
                .append(",\"auditStatus\":").append(JsonUtil.quote(product.getAuditStatus()))
                .append(",\"primaryImageUrl\":").append(JsonUtil.quote(product.getPrimaryImageUrl())).append('}');
        }
        return json.append(']').toString();
    }

    private void writeFailure(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        resp.getWriter().write("{\"code\":" + status + ",\"msg\":" + JsonUtil.quote(message) + "}");
    }
}
