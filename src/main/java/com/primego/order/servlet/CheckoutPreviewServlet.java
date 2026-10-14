package com.primego.order.servlet;

import com.primego.common.util.JsonUtil;
import com.primego.order.dao.CartDAO;
import com.primego.order.model.Cart;
import com.primego.order.model.CartItem;
import com.primego.product.dao.ProductDAO;
import com.primego.product.model.ProductDTO;
import com.primego.user.dao.ProfileDAO;
import com.primego.user.model.CustomerProfile;
import com.primego.user.model.User;
import com.primego.user.model.UserAddress;
import com.primego.wallet.dao.WalletDAO;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@WebServlet("/api/checkout")
public class CheckoutPreviewServlet extends HttpServlet {
    private final CartDAO cartDAO = new CartDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final ProfileDAO profileDAO = new ProfileDAO();
    private final WalletDAO walletDAO = new WalletDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");

        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            writeFailure(resp, HttpServletResponse.SC_UNAUTHORIZED, "Please log in to checkout.");
            return;
        }

        try {
            writeSuccess(resp, checkoutJson(req, session, user));
        } catch (IllegalArgumentException e) {
            writeFailure(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private String checkoutJson(HttpServletRequest req, HttpSession session, User user) {
        String productIdValue = req.getParameter("productId");
        boolean buyNow = productIdValue != null && !productIdValue.isEmpty();
        List<CartItem> items = new ArrayList<>();
        Set<Integer> merchantIds = new HashSet<>();
        Set<Integer> selectedIds = new LinkedHashSet<>();
        int productId = 0;
        int buyNowQuantity = 1;
        BigDecimal subtotal = BigDecimal.ZERO;

        if (buyNow) {
            productId = positiveInteger(productIdValue, "Invalid product ID.");
            String quantityValue = req.getParameter("quantity");
            if (quantityValue != null && !quantityValue.isEmpty()) {
                buyNowQuantity = positiveInteger(quantityValue, "Quantity must be a positive integer.");
            }
            ProductDTO product = productDAO.getProductById(productId);
            if (product == null || product.getProductPrice() == null || buyNowQuantity > product.getProductStockQuantity()) {
                throw new IllegalArgumentException("This product is no longer available in the selected quantity.");
            }
            items.add(new CartItem(product, buyNowQuantity));
        } else {
            String[] requestedIds = req.getParameterValues("selectedProductIds");
            if (requestedIds != null && requestedIds.length > 0) {
                for (String value : requestedIds) selectedIds.add(positiveInteger(value, "Invalid cart item."));
            }

            Cart cart = cartDAO.getCartByUserId(user.getId());
            session.setAttribute("cart", cart);
            Set<Integer> cartIds = new HashSet<>();
            for (CartItem item : cart.getItems()) {
                if (item.getProduct() != null) cartIds.add(item.getProduct().getProductId());
            }
            if (!selectedIds.isEmpty() && !cartIds.containsAll(selectedIds)) {
                throw new IllegalArgumentException("Some selected cart items are no longer available.");
            }

            for (CartItem item : cart.getItems()) {
                ProductDTO product = item.getProduct();
                if (product == null || (!selectedIds.isEmpty() && !selectedIds.contains(product.getProductId()))) continue;
                if (item.getQuantity() < 1 || item.getQuantity() > product.getProductStockQuantity() || product.getProductPrice() == null) {
                    throw new IllegalArgumentException("A selected item is no longer available in the requested quantity.");
                }
                items.add(item);
            }
        }

        if (items.isEmpty()) throw new IllegalArgumentException("There are no items to checkout.");

        StringBuilder itemJson = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            CartItem item = items.get(i);
            ProductDTO product = item.getProduct();
            if (i > 0) itemJson.append(',');
            itemJson.append("{\"product\":{\"id\":").append(product.getProductId())
                .append(",\"name\":").append(JsonUtil.quote(product.getProductName()))
                .append(",\"price\":").append(product.getProductPrice().toPlainString())
                .append(",\"stockQuantity\":").append(product.getProductStockQuantity())
                .append(",\"imageUrl\":").append(JsonUtil.quote(product.getPrimaryImageUrl()))
                .append("},\"quantity\":").append(item.getQuantity())
                .append(",\"totalPrice\":").append(item.getTotalPrice().toPlainString()).append('}');
            subtotal = subtotal.add(item.getTotalPrice());
            merchantIds.add(product.getMerchantId());
        }
        itemJson.append(']');

        int merchantCount = Math.max(merchantIds.size(), 1);
        BigDecimal shippingFee = new BigDecimal("15.00").multiply(BigDecimal.valueOf(merchantCount));
        CustomerProfile profile = profileDAO.getCustomerProfile(user.getId());
        UserAddress address = profile == null ? null : profile.getDefaultAddress();

        return "{\"isBuyNow\":" + buyNow
            + ",\"productId\":" + (buyNow ? Integer.toString(productId) : "null")
            + ",\"buyNowQuantity\":" + buyNowQuantity
            + ",\"items\":" + itemJson
            + ",\"subtotal\":" + subtotal.toPlainString()
            + ",\"merchantCount\":" + merchantCount
            + ",\"shippingFee\":" + shippingFee.toPlainString()
            + ",\"walletBalance\":" + walletDAO.getBalance(user.getId()).toPlainString()
            + ",\"hasPin\":" + (profile != null && profile.getPaymentPin() != null && !profile.getPaymentPin().isEmpty())
            + ",\"defaultAddress\":" + addressJson(address) + '}';
    }

    private int positiveInteger(String value, String message) {
        try {
            int number = Integer.parseInt(value);
            if (number > 0) return number;
        } catch (NumberFormatException ignored) {
            // Return the same client error for malformed or non-positive values.
        }
        throw new IllegalArgumentException(message);
    }

    private String addressJson(UserAddress address) {
        if (address == null) return "null";
        return "{\"recipientName\":" + JsonUtil.quote(address.getRecipientName())
            + ",\"phone\":" + JsonUtil.quote(address.getPhone())
            + ",\"fullAddress\":" + JsonUtil.quote(address.getFullAddress()) + '}';
    }

    private void writeSuccess(HttpServletResponse resp, String data) throws IOException {
        resp.getWriter().write("{\"code\":200,\"msg\":\"success\",\"data\":" + data + '}');
    }

    private void writeFailure(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        resp.getWriter().write("{\"code\":" + status + ",\"msg\":" + JsonUtil.quote(message) + '}');
    }
}
