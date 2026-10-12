package com.primego.user.servlet;

import com.primego.common.util.JsonUtil;
import com.primego.user.dao.AddressDAO;
import com.primego.user.model.Role;
import com.primego.user.model.User;
import com.primego.user.model.UserAddress;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

/**
 * Servlet handling user address management (CRUD).
 * Supports adding, updating, deleting, and setting default addresses.
 */
@WebServlet("/user/address")
public class AddressServlet extends HttpServlet {
    private AddressDAO addressDAO = new AddressDAO();

    /**
     * Handles GET requests to list addresses.
     * Often used via AJAX or internal includes.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        User user = (User) session.getAttribute("user");

        String action = req.getParameter("action");
        if ("list".equals(action)) {
            // Gson not available, skip JSON response for now or implement manual JSON if
            // needed
            // For now, we rely on JSP rendering via ProfileServlet
            resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, "JSON list not supported without Gson");
        } else {
            // Default could be redirecting to list page or something, but we mainly use
            // JSON via AJAX or JSP include
            List<UserAddress> addresses = addressDAO.getAddressesByUserId(user.getId());
            req.setAttribute("addresses", addresses);
        }
    }

    /**
     * Handles POST requests for address actions: add, update, delete, setDefault.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        boolean jsonRequest = isJsonRequest(req);
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            if (jsonRequest) {
                writeFailure(resp, HttpServletResponse.SC_UNAUTHORIZED, "Please log in to manage addresses.");
                return;
            }
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        User user = (User) session.getAttribute("user");
        if (jsonRequest && user.getRole() != Role.CUSTOMER) {
            writeFailure(resp, HttpServletResponse.SC_FORBIDDEN, "Customer addresses are not available for this account.");
            return;
        }

        String action = req.getParameter("action");
        String message = "";
        String messageType = "error";

        if (("add".equals(action) || "update".equals(action))
                && (isBlank(req.getParameter("recipientName")) || isBlank(req.getParameter("phone"))
                    || isBlank(req.getParameter("detail")))) {
            if (jsonRequest) {
                writeFailure(resp, HttpServletResponse.SC_BAD_REQUEST, "Please complete the recipient, phone and address.");
                return;
            }
        }

        if ("add".equals(action)) {
            UserAddress addr = new UserAddress();
            addr.setUserId(user.getId());
            addr.setRecipientName(req.getParameter("recipientName"));
            addr.setPhone(req.getParameter("phone"));
            addr.setProvince(req.getParameter("province"));
            addr.setCity(req.getParameter("city"));
            addr.setDistrict(req.getParameter("district"));
            addr.setDetail(req.getParameter("detail"));
            addr.setDefaultAddress("true".equals(req.getParameter("isDefault")));

            if (addressDAO.addAddress(addr)) {
                message = "Address added successfully!";
                messageType = "success";
            } else {
                message = "Failed to add address.";
            }

        } else if ("update".equals(action)) {
            Integer parsedId = parseAddressId(req, resp, jsonRequest);
            if (parsedId == null) return;
            int id = parsedId;
            UserAddress addr = addressDAO.getAddressById(id);
            if (addr != null && addr.getUserId() == user.getId()) {
                addr.setRecipientName(req.getParameter("recipientName"));
                addr.setPhone(req.getParameter("phone"));
                addr.setProvince(req.getParameter("province"));
                addr.setCity(req.getParameter("city"));
                addr.setDistrict(req.getParameter("district"));
                addr.setDetail(req.getParameter("detail"));
                addr.setDefaultAddress("true".equals(req.getParameter("isDefault")));

                if (addressDAO.updateAddress(addr)) {
                    message = "Address updated successfully!";
                    messageType = "success";
                } else {
                    message = "Failed to update address.";
                }
            } else {
                message = "Address not found or unauthorized.";
            }

        } else if ("delete".equals(action)) {
            Integer parsedId = parseAddressId(req, resp, jsonRequest);
            if (parsedId == null) return;
            int id = parsedId;
            UserAddress addr = addressDAO.getAddressById(id);
            if (addr != null && addr.getUserId() == user.getId()) {
                if (addressDAO.deleteAddress(id)) {
                    message = "Address deleted successfully!";
                    messageType = "success";
                } else {
                    message = "Failed to delete address.";
                }
            } else {
                message = "Address not found or unauthorized.";
            }

        } else if ("setDefault".equals(action)) {
            Integer parsedId = parseAddressId(req, resp, jsonRequest);
            if (parsedId == null) return;
            int id = parsedId;
            if (addressDAO.setDefaultAddress(user.getId(), id)) {
                message = "Default address set successfully!";
                messageType = "success";
            } else {
                message = "Failed to set default address.";
            }
        }

        if (jsonRequest) {
            writeAddressResult(resp, user.getId(), message, messageType);
            return;
        }

        // Redirect back to profile address tab
        req.getSession().setAttribute("message", message);
        req.getSession().setAttribute("messageType", messageType);
        // We use a query param 'tab=addresses' to help frontend switch tab
        // automatically
        resp.sendRedirect(req.getContextPath() + "/profile?tab=addresses");
    }

    private boolean isJsonRequest(HttpServletRequest req) {
        return req.getHeader("Accept") != null && req.getHeader("Accept").contains("application/json");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private Integer parseAddressId(HttpServletRequest req, HttpServletResponse resp, boolean jsonRequest)
            throws IOException {
        try {
            int id = Integer.parseInt(req.getParameter("addressId"));
            if (id > 0) return id;
        } catch (NumberFormatException ignored) {
            // Malformed IDs are returned as a client error for the Vue API path.
        }
        if (jsonRequest) {
            writeFailure(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid address ID.");
            return null;
        }
        throw new NumberFormatException("Invalid address ID.");
    }

    private void writeAddressResult(HttpServletResponse resp, int userId, String message, String messageType)
            throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        resp.getWriter().write("{\"code\":200,\"msg\":\"success\",\"data\":{\"addresses\":"
            + addressesJson(addressDAO.getAddressesByUserId(userId))
            + ",\"message\":" + JsonUtil.quote(message)
            + ",\"messageType\":" + JsonUtil.quote(messageType) + "}}");
    }

    private String addressesJson(List<UserAddress> addresses) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < addresses.size(); i++) {
            if (i > 0) json.append(',');
            UserAddress address = addresses.get(i);
            json.append("{\"id\":").append(address.getId())
                .append(",\"recipientName\":").append(JsonUtil.quote(address.getRecipientName()))
                .append(",\"phone\":").append(JsonUtil.quote(address.getPhone()))
                .append(",\"province\":").append(JsonUtil.quote(address.getProvince()))
                .append(",\"city\":").append(JsonUtil.quote(address.getCity()))
                .append(",\"district\":").append(JsonUtil.quote(address.getDistrict()))
                .append(",\"detail\":").append(JsonUtil.quote(address.getDetail()))
                .append(",\"fullAddress\":").append(JsonUtil.quote(address.getFullAddress()))
                .append(",\"defaultAddress\":").append(address.isDefaultAddress()).append('}');
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
