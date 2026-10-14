package com.primego.product.servlet;

import com.primego.common.util.JsonUtil;
import com.primego.product.dao.ProductDAO;
import com.primego.product.dao.ProductImageDAO;
import com.primego.product.model.ProductDTO;
import com.primego.product.model.ProductImage;
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

@WebServlet("/admin/product/review")
public class AdminProductReviewDetailServlet extends HttpServlet {

    private final ProductDAO productDAO = new ProductDAO();
    private final ProductImageDAO imageDAO = new ProductImageDAO();

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

        String idStr = req.getParameter("productId");
        if (idStr == null) {
            if (jsonRequest) {
                writeFailure(resp, HttpServletResponse.SC_BAD_REQUEST, "Product ID is required.");
                return;
            }
            resp.sendRedirect(req.getContextPath() + "/admin/product/review/list");
            return;
        }

        int productId;
        try {
            productId = Integer.parseInt(idStr);
            if (productId < 1) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            if (jsonRequest) {
                writeFailure(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid product ID.");
                return;
            }
            resp.sendRedirect(req.getContextPath() + "/admin/product/review/list");
            return;
        }

        ProductDTO product = productDAO.getProductById(productId);
        if (jsonRequest) {
            if (product == null) {
                writeFailure(resp, HttpServletResponse.SC_NOT_FOUND, "Product not found.");
            } else {
                writeSuccess(resp, "\"product\":" + productJson(product)
                    + ",\"images\":" + imagesJson(imageDAO.getImagesByProductId(productId)));
            }
            return;
        }

        List<ProductImage> images = imageDAO.getImagesByProductId(productId);

        req.setAttribute("product", product);
        req.setAttribute("images", images);

        resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, "The Vue app renders this page.");
    }

    static String productJson(ProductDTO product) {
        return "{\"productId\":" + product.getProductId()
            + ",\"productName\":" + JsonUtil.quote(product.getProductName())
            + ",\"auditStatus\":" + JsonUtil.quote(product.getAuditStatus())
            + ",\"auditMessage\":" + JsonUtil.quote(product.getAuditMessage())
            + ",\"merchantName\":" + JsonUtil.quote(product.getMerchantName())
            + ",\"categoryName\":" + JsonUtil.quote(product.getCategoryName())
            + ",\"productPrice\":" + JsonUtil.quote(product.getProductPrice() == null ? null : product.getProductPrice().toPlainString())
            + ",\"productStockQuantity\":" + product.getProductStockQuantity()
            + ",\"productDescription\":" + JsonUtil.quote(product.getProductDescription()) + '}';
    }

    static String imagesJson(List<ProductImage> images) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < images.size(); i++) {
            if (i > 0) json.append(',');
            ProductImage image = images.get(i);
            json.append("{\"imageId\":").append(image.getImageId())
                .append(",\"imageUrl\":").append(JsonUtil.quote(image.getImageUrl())).append('}');
        }
        return json.append(']').toString();
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

