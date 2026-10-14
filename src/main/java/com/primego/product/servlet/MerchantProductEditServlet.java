package com.primego.product.servlet;

import com.primego.common.util.JsonUtil;
import com.primego.product.dao.CategoryDAO;
import com.primego.product.dao.ProductDAO;
import com.primego.product.dao.ProductImageDAO;
import com.primego.product.model.Category;
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

@WebServlet("/api/merchant/product-edit")
public class MerchantProductEditServlet extends HttpServlet {
    private final ProductDAO productDAO = new ProductDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final ProductImageDAO imageDAO = new ProductImageDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            writeFailure(resp, HttpServletResponse.SC_UNAUTHORIZED, "Please log in to edit this product.");
            return;
        }
        if (user.getRole() != Role.MERCHANT) {
            writeFailure(resp, HttpServletResponse.SC_FORBIDDEN, "Only merchants can edit products.");
            return;
        }

        int productId;
        try {
            productId = Integer.parseInt(req.getParameter("id"));
            if (productId < 1) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            writeFailure(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid product ID.");
            return;
        }

        try {
            ProductDTO product = productDAO.getProductById(productId);
            if (product == null) {
                writeFailure(resp, HttpServletResponse.SC_NOT_FOUND, "Product not found.");
                return;
            }
            if (!product.belongsToMerchant(user.getId())) {
                writeFailure(resp, HttpServletResponse.SC_FORBIDDEN, "Product does not belong to this merchant.");
                return;
            }

            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.setHeader("Cache-Control", "no-store");
            resp.getWriter().write(productEditJson(product, categoryDAO.findAll(), imageDAO.getImagesByProductId(productId)));
        } catch (RuntimeException e) {
            writeFailure(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Unable to load this product.");
        }
    }

    static String productEditJson(ProductDTO product, List<Category> categories, List<ProductImage> images) {
        StringBuilder json = new StringBuilder("{\"code\":200,\"msg\":\"success\",\"data\":{\"product\":{")
            .append("\"productId\":").append(product.getProductId())
            .append(",\"categoryId\":").append(product.getCategoryId())
            .append(",\"productName\":").append(JsonUtil.quote(product.getProductName()))
            .append(",\"productDescription\":").append(JsonUtil.quote(product.getProductDescription()))
            .append(",\"productPrice\":").append(product.getProductPrice() == null ? "null" : product.getProductPrice().toPlainString())
            .append(",\"productStockQuantity\":").append(product.getProductStockQuantity())
            .append(",\"contactWhatsapp\":").append(JsonUtil.quote(product.getContactWhatsapp()))
            .append(",\"productStatus\":").append(JsonUtil.quote(product.getProductStatus())).append("},\"categories\":[");
        for (int i = 0; i < categories.size(); i++) {
            if (i > 0) json.append(',');
            Category category = categories.get(i);
            json.append("{\"id\":").append(category.getCategoryId())
                .append(",\"name\":").append(JsonUtil.quote(category.getCategoryName())).append('}');
        }
        json.append("],\"images\":[");
        for (int i = 0; i < images.size(); i++) {
            if (i > 0) json.append(',');
            ProductImage image = images.get(i);
            json.append("{\"id\":").append(image.getImageId())
                .append(",\"url\":").append(JsonUtil.quote(image.getImageUrl()))
                .append(",\"isPrimary\":").append(image.isImageIsPrimary()).append('}');
        }
        return json.append("]}}").toString();
    }

    private void writeFailure(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        resp.getWriter().write("{\"code\":" + status + ",\"msg\":" + JsonUtil.quote(message) + "}");
    }
}
