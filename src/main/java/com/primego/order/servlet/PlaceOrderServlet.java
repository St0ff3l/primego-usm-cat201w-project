package com.primego.order.servlet;

import com.primego.order.dao.CartDAO;
import com.primego.order.dao.OrderDAO;
import com.primego.order.model.Cart;
import com.primego.order.model.CartItem;
import com.primego.order.model.Order;
import com.primego.order.model.OrderItem;
import com.primego.common.util.JsonUtil;
import com.primego.product.dao.ProductDAO;
import com.primego.product.model.ProductDTO;
import com.primego.user.model.User;
import com.primego.user.dao.ProfileDAO;
import com.primego.user.model.CustomerProfile;
import com.primego.common.util.PasswordUtil;
import com.primego.wallet.dao.WalletDAO;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@WebServlet("/placeOrder")
public class PlaceOrderServlet extends HttpServlet {

    private OrderDAO orderDAO = new OrderDAO();
    private ProductDAO productDAO = new ProductDAO();
    private ProfileDAO profileDAO = new ProfileDAO();
    private WalletDAO walletDAO = new WalletDAO();
    private CartDAO cartDAO = new CartDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        boolean jsonRequest = request.getHeader("Accept") != null
            && request.getHeader("Accept").contains("application/json");
        HttpSession session = request.getSession();

        // 1) Get the current logged-in user.
        User user = (User) session.getAttribute("user");
        if (user == null) {
            if (jsonRequest) {
                writeJsonFailure(response, HttpServletResponse.SC_UNAUTHORIZED, "Please log in to checkout.");
                return;
            }
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        int userId = user.getId();

        // =========================================================================
        // Step 1: Validate the payment PIN.
        // =========================================================================
        String inputPin = request.getParameter("paymentPin");
        CustomerProfile profile = profileDAO.getCustomerProfile(userId);

        boolean isPinValid = false;
        if (profile != null && profile.getPaymentPin() != null && inputPin != null) {
            isPinValid = PasswordUtil.checkPassword(inputPin, profile.getPaymentPin());
        }

        if (!isPinValid) {
            if (jsonRequest) {
                writeJsonFailure(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid Payment PIN. Please try again.");
                return;
            }
            response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, "Submit checkout through the Vue app.");
            return;
        }

        // =========================================================================
        // Step 2: Prepare grouped items (group by merchantId to support split orders).
        // =========================================================================
        String fullName = request.getParameter("fullName");
        String address = request.getParameter("address");
        String phone = request.getParameter("phone");
        if (jsonRequest && (isBlank(fullName) || isBlank(address) || isBlank(phone))) {
            writeJsonFailure(response, HttpServletResponse.SC_BAD_REQUEST, "Please select a delivery address.");
            return;
        }
        String fullAddress = fullName + ", " + address + ", Phone: " + phone;

        // Group items by merchantId.
        // Key: merchantId, Value: List<OrderItem>
        Map<Integer, List<OrderItem>> merchantItemsMap = new HashMap<>();

        String isBuyNow = request.getParameter("isBuyNow");
        String productIdStr = request.getParameter("productId");
        // Quantity selected on the confirmation page.
        String quantityStr = request.getParameter("quantity");

        String[] selectedIds = request.getParameterValues("selectedProductIds");
        List<String> selectedIdList = selectedIds == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(selectedIds));

        if ("true".equals(isBuyNow) && productIdStr != null) {
            // Scenario A: Buy now
            try {
                int productId = Integer.parseInt(productIdStr);
                int quantity = 1;
                if (quantityStr != null && !quantityStr.isEmpty()) {
                    quantity = Integer.parseInt(quantityStr);
                }

                ProductDTO product = productDAO.getProductById(productId);
                if (jsonRequest && (product == null || product.getProductPrice() == null
                    || quantity < 1 || quantity > product.getProductStockQuantity())) {
                    writeJsonFailure(response, HttpServletResponse.SC_BAD_REQUEST,
                        "This product is no longer available in the selected quantity.");
                    return;
                }
                if (product != null) {
                    int merchantId = product.getMerchantId();
                    merchantItemsMap.putIfAbsent(merchantId, new ArrayList<>());

                    // Create an order item and add it to the merchant group.
                    OrderItem item = createOrderItem(product, quantity);
                    merchantItemsMap.get(merchantId).add(item);
                }
            } catch (NumberFormatException e) {
                if (jsonRequest) {
                    writeJsonFailure(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid product or quantity.");
                    return;
                }
                e.printStackTrace();
            }
        } else {
            // Scenario B: Checkout from cart (iterate cart items and group by merchant).
            Cart cart = (Cart) session.getAttribute("cart");
            Map<String, Integer> quantityOverrides = new HashMap<>();
            if (jsonRequest) {
                cart = cartDAO.getCartByUserId(userId);
                session.setAttribute("cart", cart);

                if (selectedIdList.isEmpty()) {
                    for (CartItem item : cart.getItems()) {
                        if (item.getProduct() != null) selectedIdList.add(String.valueOf(item.getProduct().getProductId()));
                    }
                }
                String[] quantities = request.getParameterValues("selectedQuantities");
                if (quantities == null || quantities.length != selectedIdList.size()
                    || new HashSet<>(selectedIdList).size() != selectedIdList.size()) {
                    writeJsonFailure(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid cart item quantities.");
                    return;
                }
                for (int i = 0; i < selectedIdList.size(); i++) {
                    try {
                        int quantity = Integer.parseInt(quantities[i]);
                        if (quantity < 1) throw new NumberFormatException();
                        quantityOverrides.put(selectedIdList.get(i), quantity);
                    } catch (NumberFormatException e) {
                        writeJsonFailure(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid cart item quantity.");
                        return;
                    }
                }
                Set<String> cartProductIds = new HashSet<>();
                for (CartItem item : cart.getItems()) {
                    if (item.getProduct() != null) cartProductIds.add(String.valueOf(item.getProduct().getProductId()));
                }
                if (!cartProductIds.containsAll(selectedIdList)) {
                    writeJsonFailure(response, HttpServletResponse.SC_BAD_REQUEST, "Some selected cart items are no longer available.");
                    return;
                }
            }
            if (cart != null && !cart.getItems().isEmpty()) {
                for (CartItem cartItem : cart.getItems()) {
                    String pId = String.valueOf(cartItem.getProduct().getProductId());
                    // Process only selected products.
                    if (!selectedIdList.isEmpty() && !selectedIdList.contains(pId)) {
                        continue;
                    }

                    int quantity = quantityOverrides.getOrDefault(pId, cartItem.getQuantity());
                    if (jsonRequest && (cartItem.getProduct().getProductPrice() == null
                        || quantity > cartItem.getProduct().getProductStockQuantity())) {
                        writeJsonFailure(response, HttpServletResponse.SC_BAD_REQUEST,
                            "A selected item is no longer available in the requested quantity.");
                        return;
                    }

                    // Relies on product data in the cart item containing a valid merchantId.
                    int merchantId = cartItem.getProduct().getMerchantId();

                    merchantItemsMap.putIfAbsent(merchantId, new ArrayList<>());

                    // Create an order item and add it to the merchant group.
                    OrderItem item = createOrderItem(cartItem.getProduct(), quantity);
                    merchantItemsMap.get(merchantId).add(item);
                }
            }
        }

        if (merchantItemsMap.isEmpty()) {
            if (jsonRequest) {
                writeJsonFailure(response, HttpServletResponse.SC_BAD_REQUEST, "There are no items to checkout.");
                return;
            }
            response.sendRedirect(request.getContextPath() + "/?error=EmptyOrder");
            return;
        }

        // =========================================================================
        // Step 3: Convert the grouped map into a list of orders and calculate totals.
        // =========================================================================
        List<Order> ordersToCreate = new ArrayList<>();
        BigDecimal grandTotal = BigDecimal.ZERO; // Total amount across all sub-orders.

        for (Map.Entry<Integer, List<OrderItem>> entry : merchantItemsMap.entrySet()) {
            List<OrderItem> items = entry.getValue();

            // 3.1 Calculate subtotal for this sub-order.
            BigDecimal subTotal = BigDecimal.ZERO;
            for (OrderItem item : items) {
                subTotal = subTotal.add(item.getSubtotal());
            }

            // 3.2 Add shipping fee (policy: each sub-order charges its own shipping fee).
            BigDecimal shippingFee = new BigDecimal("15.00");
            BigDecimal orderTotal = subTotal.add(shippingFee);

            // 3.3 Build the sub-order.
            Order order = new Order();
            order.setCustomerId(userId);
            order.setAddress(fullAddress);
            order.setOrderItems(items);
            order.setTotalAmount(orderTotal);

            ordersToCreate.add(order);

            // 3.4 Accumulate to the overall total.
            grandTotal = grandTotal.add(orderTotal);
        }

        // =========================================================================
        // Step 4: Check wallet balance using grandTotal.
        // =========================================================================
        BigDecimal currentBalance = walletDAO.getBalance(userId);
        if (currentBalance.compareTo(grandTotal) < 0) {
            if (jsonRequest) {
                String message = "Insufficient wallet balance. Please top up your wallet.";
                String failureUrl = request.getContextPath() + "/customer/order/payment_failed.jsp?errorMessage="
                    + URLEncoder.encode(message, StandardCharsets.UTF_8);
                writeJsonRedirect(response, HttpServletResponse.SC_PAYMENT_REQUIRED, message, failureUrl);
                return;
            }
            response.sendRedirect(request.getContextPath() + "/customer/order/payment_failed.jsp");
            return;
        }

        // =========================================================================
        // Step 5: Create orders in batch.
        // =========================================================================
        List<Integer> createdIds = orderDAO.createOrdersBatch(ordersToCreate);

        if (createdIds != null && !createdIds.isEmpty()) {
            // Success
            if (!"true".equals(isBuyNow)) {
                // Remove purchased cart items from session and database.
                Cart cart = (Cart) session.getAttribute("cart");
                int cartId = cartDAO.getOrCreateCartId(userId);

                if (cart != null && !selectedIdList.isEmpty()) {
                    for (String id : selectedIdList) {
                        try {
                            int pId = Integer.parseInt(id);
                            cart.removeItem(pId); // Remove from session cart
                            cartDAO.removeItemFromCart(cartId, pId); // Remove from database cart
                        } catch (NumberFormatException e) {
                            // ignore
                        }
                    }
                }
            }

            // Build a comma-separated list of created order ids.
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < createdIds.size(); i++) {
                sb.append(createdIds.get(i));
                if (i < createdIds.size() - 1) {
                    sb.append(",");
                }
            }

            // Redirect to the success page. Parameter name: orderIds
            String successUrl = request.getContextPath() + "/customer/order/payment_success.jsp?orderIds=" + sb;
            if (jsonRequest) {
                writeJsonRedirect(response, HttpServletResponse.SC_OK, "success", successUrl);
            } else {
                response.sendRedirect(successUrl);
            }
        } else {
            if (jsonRequest) {
                writeJsonFailure(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Order processing failed. Please try again.");
                return;
            }
            // Failure
            response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, "Submit checkout through the Vue app.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void writeJsonFailure(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"code\":" + status + ",\"msg\":" + JsonUtil.quote(message) + "}");
    }

    private void writeJsonRedirect(HttpServletResponse response, int status, String message, String redirectUrl) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"code\":" + status + ",\"msg\":" + JsonUtil.quote(message)
            + ",\"data\":{\"redirectUrl\":" + JsonUtil.quote(redirectUrl) + "}}");
    }

    // Helper: convert ProductDTO into OrderItem to avoid duplicating mapping logic.
    private OrderItem createOrderItem(ProductDTO product, int quantity) {
        OrderItem item = new OrderItem();
        item.setProductId(product.getProductId());
        item.setProductName(product.getProductName());
        item.setPrice(product.getProductPrice());
        item.setQuantity(quantity);
        item.setSubtotal(product.getProductPrice().multiply(new BigDecimal(quantity)));
        return item;
    }
}
