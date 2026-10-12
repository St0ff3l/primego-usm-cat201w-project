package com.primego.product.servlet;

import com.primego.common.util.JsonUtil;
import com.primego.product.dao.CategoryDAO;
import com.primego.product.dao.ProductDAO;
import com.primego.product.dao.ProductImageDAO;
import com.primego.product.model.Category;
import com.primego.product.model.ProductDTO;
import com.primego.product.model.ProductImage;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/api/catalog/*")
public class LegacyCatalogServlet extends HttpServlet {
    private final ProductDAO productDAO = new ProductDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final ProductImageDAO imageDAO = new ProductImageDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");

        try {
            String path = req.getPathInfo();
            if ("/categories".equals(path)) {
                writeSuccess(resp, categoriesJson());
            } else if ("/products".equals(path)) {
                writeSuccess(resp, productsJson(req));
            } else if (path != null && path.startsWith("/products/")) {
                writeSuccess(resp, productJson(parseProductId(path.substring("/products/".length())), true));
            } else {
                writeFailure(resp, HttpServletResponse.SC_NOT_FOUND, "Catalog endpoint not found");
            }
        } catch (IllegalArgumentException e) {
            writeFailure(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private String categoriesJson() {
        StringBuilder json = new StringBuilder("[");
        List<Category> categories = categoryDAO.findAll();
        for (int i = 0; i < categories.size(); i++) {
            if (i > 0) json.append(',');
            Category category = categories.get(i);
            json.append("{\"id\":").append(category.getCategoryId())
                .append(",\"name\":").append(JsonUtil.quote(category.getCategoryName())).append('}');
        }
        return json.append(']').toString();
    }

    private String productsJson(HttpServletRequest req) {
        String keyword = req.getParameter("keyword");
        if (keyword != null) keyword = keyword.trim();
        if (keyword != null && keyword.length() > 100) throw new IllegalArgumentException("keyword must be at most 100 characters");

        Integer categoryId = parseInteger(req.getParameter("categoryId"), "categoryId");
        Double minPrice = parsePrice(req.getParameter("minPrice"), "minPrice");
        Double maxPrice = parsePrice(req.getParameter("maxPrice"), "maxPrice");
        if (categoryId != null && categoryId < 1) throw new IllegalArgumentException("categoryId must be positive");
        if (minPrice != null && maxPrice != null && minPrice > maxPrice) throw new IllegalArgumentException("minPrice must not exceed maxPrice");

        int pageNum = parseInteger(req.getParameter("pageNum"), "pageNum", 1);
        int pageSize = parseInteger(req.getParameter("pageSize"), "pageSize", 100);
        if (pageNum < 1 || pageSize < 1 || pageSize > 100) throw new IllegalArgumentException("Invalid page range");

        boolean hasFilter = (keyword != null && !keyword.isEmpty()) || categoryId != null || minPrice != null || maxPrice != null;
        List<ProductDTO> matches = hasFilter
            ? productDAO.searchProductsWithFilter(blankToNull(keyword), categoryId, minPrice, maxPrice)
            : productDAO.getAllProducts();
        List<ProductDTO> products = new ArrayList<>();
        if (matches != null) {
            boolean inStockOnly = Boolean.parseBoolean(req.getParameter("inStockOnly"));
            for (ProductDTO product : matches) {
                if (!inStockOnly || product.getProductStockQuantity() > 0) products.add(product);
            }
        }

        int start = Math.min((pageNum - 1) * pageSize, products.size());
        int end = Math.min(start + pageSize, products.size());
        StringBuilder rows = new StringBuilder("[");
        for (int i = start; i < end; i++) {
            if (i > start) rows.append(',');
            rows.append(productJson(products.get(i), false));
        }
        rows.append(']');
        return "{\"rows\":" + rows + ",\"total\":" + products.size()
            + ",\"pageNum\":" + pageNum + ",\"pageSize\":" + pageSize + "}";
    }

    private String productJson(int productId, boolean includeImages) {
        ProductDTO product = productDAO.getProductById(productId);
        if (product == null) throw new IllegalArgumentException("Product not found");
        return productJson(product, includeImages);
    }

    private String productJson(ProductDTO product, boolean includeImages) {
        StringBuilder json = new StringBuilder("{")
            .append("\"id\":").append(product.getProductId())
            .append(",\"categoryId\":").append(product.getCategoryId())
            .append(",\"name\":").append(JsonUtil.quote(product.getProductName()))
            .append(",\"description\":").append(JsonUtil.quote(product.getProductDescription()))
            .append(",\"price\":").append(decimal(product.getProductPrice()))
            .append(",\"stockQuantity\":").append(product.getProductStockQuantity())
            .append(",\"categoryName\":").append(JsonUtil.quote(product.getCategoryName()))
            .append(",\"merchantName\":").append(JsonUtil.quote(product.getMerchantName()))
            .append(",\"imageUrl\":").append(JsonUtil.quote(product.getPrimaryImageUrl()))
            .append(",\"productStatus\":").append(JsonUtil.quote(product.getProductStatus()));
        if (includeImages) {
            json.append(",\"contactWhatsapp\":").append(JsonUtil.quote(product.getContactWhatsapp()))
                .append(",\"imageUrls\":[");
            List<ProductImage> images = imageDAO.getImagesByProductId(product.getProductId());
            for (int i = 0; i < images.size(); i++) {
                if (i > 0) json.append(',');
                json.append(JsonUtil.quote(images.get(i).getImageUrl()));
            }
            json.append(']');
        }
        return json.append('}').toString();
    }

    private int parseProductId(String value) {
        try {
            int id = Integer.parseInt(value);
            if (id > 0) return id;
        } catch (NumberFormatException ignored) {
            // Report malformed IDs through the same client error response.
        }
        throw new IllegalArgumentException("Invalid product ID");
    }

    private Integer parseInteger(String value, String name) {
        return value == null || value.isEmpty() ? null : parseInteger(value, name, 0);
    }

    private int parseInteger(String value, String name, int defaultValue) {
        if (value == null || value.isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " must be an integer");
        }
    }

    private Double parsePrice(String value, String name) {
        if (value == null || value.isEmpty()) return null;
        try {
            double price = Double.parseDouble(value);
            if (Double.isFinite(price) && price >= 0) return price;
        } catch (NumberFormatException ignored) {
            // Report malformed values through the client error response below.
        }
        throw new IllegalArgumentException(name + " must be a non-negative number");
    }

    private String blankToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    private String decimal(BigDecimal value) {
        return value == null ? "null" : value.toPlainString();
    }

    private void writeSuccess(HttpServletResponse resp, String data) throws IOException {
        resp.getWriter().write("{\"code\":200,\"msg\":\"success\",\"data\":" + data + "}");
    }

    private void writeFailure(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        resp.getWriter().write("{\"code\":" + status + ",\"msg\":" + JsonUtil.quote(message) + "}");
    }
}
