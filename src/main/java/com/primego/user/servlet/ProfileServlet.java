package com.primego.user.servlet;

import com.primego.common.util.JsonUtil;
import com.primego.user.dao.AddressDAO;
import com.primego.user.dao.ProfileDAO;
import com.primego.user.dao.UserDAO;
import com.primego.user.model.CustomerProfile;
import com.primego.user.model.AdminProfile;
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
 * Servlet handling user profile viewing and updates.
 * Supports different views for Customer, Merchant, and Admin.
 */
@WebServlet("/profile")
public class ProfileServlet extends HttpServlet {

    private ProfileDAO profileDAO = new ProfileDAO();
    private UserDAO userDAO = new UserDAO();
    // Removed OrderDAO, no longer needed for order handling

    /**
     * Renders the user profile page based on role.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        boolean jsonRequest = isJsonRequest(req);
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            if (jsonRequest) {
                writeFailure(resp, HttpServletResponse.SC_UNAUTHORIZED, "Please log in to view your profile.");
                return;
            }
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        User user = (User) session.getAttribute("user");
        int userId = user.getId();

        switch (user.getRole()) {
            case CUSTOMER:
                CustomerProfile customerProfile = profileDAO.getCustomerProfile(userId);
                List<com.primego.user.model.UserAddress> addresses = new AddressDAO().getAddressesByUserId(userId);
                req.setAttribute("profile", customerProfile);
                // Fetch addresses for the Addresses tab
                req.setAttribute("addresses", addresses);

                // Removed: Order retrieval logic (Handle Order Filtering)
                // Now handled by CustomerOrderServlet (/customer/orders)

                if (jsonRequest) writeProfileJson(req, resp, user, customerProfile, addresses);
                else resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, "The Vue app renders this page.");
                break;
            case MERCHANT:
                if (jsonRequest) {
                    writeFailure(resp, HttpServletResponse.SC_FORBIDDEN, "Customer profile is not available for this account.");
                    return;
                }
                resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, "The Vue app renders this page.");
                break;
            case ADMIN:
                if (jsonRequest) {
                    resp.setContentType("application/json");
                    resp.setCharacterEncoding("UTF-8");
                    resp.setHeader("Cache-Control", "no-store");
                    resp.getWriter().write(adminProfileJson(user, profileDAO.getAdminProfile(userId)));
                    return;
                }
                resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, "The Vue app renders this page.");
                break;
        }
    }

    /**
     * Handles profile updates (info, password, PIN).
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        boolean jsonRequest = isJsonRequest(req);
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            if (jsonRequest) {
                writeFailure(resp, HttpServletResponse.SC_UNAUTHORIZED, "Please log in to update your profile.");
                return;
            }
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        User user = (User) session.getAttribute("user");
        if (jsonRequest && user.getRole() != Role.CUSTOMER) {
            writeFailure(resp, HttpServletResponse.SC_FORBIDDEN, "Customer profile is not available for this account.");
            return;
        }
        String action = req.getParameter("action");

        if ("updateCustomerProfile".equals(action)
                && (isBlank(req.getParameter("firstName")) || isBlank(req.getParameter("lastName"))
                    || isBlank(req.getParameter("phoneNumber")))) {
            req.setAttribute("message", "Please complete your name and phone number.");
            req.setAttribute("messageType", "error");
            if (jsonRequest) {
                writeProfileJson(req, resp, user);
            } else {
                doGet(req, resp);
            }
            return;
        }

        // 1. Update personal profile
        if ("updateCustomerProfile".equals(action)) {
            String newUsername = req.getParameter("username");

            // Name
            String firstName = req.getParameter("firstName");
            String lastName = req.getParameter("lastName");
            String fullName = (firstName + " " + lastName).trim();

            // Phone
            String phoneAreaCode = req.getParameter("phoneAreaCode");
            String phoneNumber = req.getParameter("phoneNumber");
            String phone = (phoneAreaCode + " " + phoneNumber).trim();

            // Update Username (in users table)
            boolean usernameUpdated = true;
            if (newUsername != null && !newUsername.trim().isEmpty() && !newUsername.equals(user.getUsername())) {
                if (userDAO.updateUsername(user.getId(), newUsername)) {
                    user.setUsername(newUsername); // Update session object
                } else {
                    usernameUpdated = false; // Failed (likely duplicate)
                }
            }

            // Update Profile Details (in customer_profiles table)
            CustomerProfile profile = new CustomerProfile(
                    user.getId(),
                    fullName,
                    "", // Email ignored in update
                    phone,
                    null,
                    null);

            boolean profileUpdated = profileDAO.updateCustomerProfile(profile);

            if (usernameUpdated && profileUpdated) {
                req.setAttribute("message", "Profile updated successfully!");
                req.setAttribute("messageType", "success");
            } else if (!usernameUpdated) {
                req.setAttribute("message", "Failed to update username. It might be taken.");
                req.setAttribute("messageType", "error");
            } else {
                req.setAttribute("message", "Failed to update profile details.");
                req.setAttribute("messageType", "error");
            }
        }
        // 2. Change password
        else if ("changePassword".equals(action)) {
            String oldPassword = req.getParameter("oldPassword");
            String newPassword = req.getParameter("newPassword");
            String confirmPassword = req.getParameter("confirmPassword");

            if (newPassword != null && !newPassword.isEmpty() && newPassword.equals(confirmPassword)) {
                // Validate old password
                User validUser = userDAO.validateCredentials(user.getUsername(), oldPassword);
                if (validUser != null) {
                    // Update to new password
                    if (userDAO.updatePassword(user.getId(), newPassword)) {
                        req.setAttribute("settingsMessage", "Password changed successfully!");
                        req.setAttribute("settingsMessageType", "success");
                    } else {
                        req.setAttribute("settingsMessage", "Failed to update password. Database error.");
                        req.setAttribute("settingsMessageType", "error");
                    }
                } else {
                    req.setAttribute("settingsMessage", "Incorrect old password.");
                    req.setAttribute("settingsMessageType", "error");
                }
            } else {
                req.setAttribute("settingsMessage", "New passwords do not match.");
                req.setAttribute("settingsMessageType", "error");
            }
        }
        // 3. Update/Set Payment PIN
        else if ("updatePin".equals(action)) {
            String pin1 = req.getParameter("pin1");
            String pin2 = req.getParameter("pin2");
            String pin3 = req.getParameter("pin3");
            String pin4 = req.getParameter("pin4");
            String pin5 = req.getParameter("pin5");
            String pin6 = req.getParameter("pin6");

            String newPin = pin1 + pin2 + pin3 + pin4 + pin5 + pin6;

            String confirmPin1 = req.getParameter("confirmPin1");
            String confirmPin2 = req.getParameter("confirmPin2");
            String confirmPin3 = req.getParameter("confirmPin3");
            String confirmPin4 = req.getParameter("confirmPin4");
            String confirmPin5 = req.getParameter("confirmPin5");
            String confirmPin6 = req.getParameter("confirmPin6");

            String confirmPin = confirmPin1 + confirmPin2 + confirmPin3 + confirmPin4 + confirmPin5 + confirmPin6;

            if (newPin.matches("\\d{6}") && newPin.equals(confirmPin)) {
                CustomerProfile currentProfile = profileDAO.getCustomerProfile(user.getId());
                boolean isUpdate = currentProfile != null && currentProfile.getPaymentPin() != null
                        && !currentProfile.getPaymentPin().isEmpty();

                boolean authorized = true;
                if (isUpdate) {
                    // Check old PIN
                    String oldPin1 = req.getParameter("oldPin1");
                    String oldPin2 = req.getParameter("oldPin2");
                    String oldPin3 = req.getParameter("oldPin3");
                    String oldPin4 = req.getParameter("oldPin4");
                    String oldPin5 = req.getParameter("oldPin5");
                    String oldPin6 = req.getParameter("oldPin6");
                    String oldPin = oldPin1 + oldPin2 + oldPin3 + oldPin4 + oldPin5 + oldPin6;

                    if (!com.primego.common.util.PasswordUtil.checkPassword(oldPin, currentProfile.getPaymentPin())) {
                        authorized = false;
                        req.setAttribute("settingsMessage", "Incorrect current PIN.");
                        req.setAttribute("settingsMessageType", "error");
                    }
                }

                if (authorized) {
                    String hashedPin = com.primego.common.util.PasswordUtil.hashPassword(newPin);
                    if (profileDAO.updatePaymentPin(user.getId(), hashedPin)) {
                        req.setAttribute("settingsMessage", "Payment PIN updated successfully!");
                        req.setAttribute("settingsMessageType", "success");
                    } else {
                        req.setAttribute("settingsMessage", "Failed to update PIN. Database error.");
                        req.setAttribute("settingsMessageType", "error");
                    }
                }
            } else {
                req.setAttribute("settingsMessage", "New PINs do not match or are incomplete.");
                req.setAttribute("settingsMessageType", "error");
            }
        }

        // Removed: confirmReceipt logic
        // Logic moved to CustomerOrderServlet

        if (jsonRequest) writeProfileJson(req, resp, user);
        else resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, "The Vue app renders this page.");
    }

    private boolean isJsonRequest(HttpServletRequest req) {
        return req.getHeader("Accept") != null && req.getHeader("Accept").contains("application/json");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void writeProfileJson(HttpServletRequest req, HttpServletResponse resp, User user) throws IOException {
        writeProfileJson(req, resp, user, profileDAO.getCustomerProfile(user.getId()),
            new AddressDAO().getAddressesByUserId(user.getId()));
    }

    private void writeProfileJson(HttpServletRequest req, HttpServletResponse resp, User user,
                                  CustomerProfile profile, List<UserAddress> addresses) throws IOException {
        HttpSession session = req.getSession(false);

        Object message = req.getAttribute("message");
        Object messageType = req.getAttribute("messageType");
        if (message == null && session != null) message = session.getAttribute("message");
        if (messageType == null && session != null) messageType = session.getAttribute("messageType");
        if (session != null) {
            session.removeAttribute("message");
            session.removeAttribute("messageType");
        }

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        StringBuilder json = new StringBuilder("{\"code\":200,\"msg\":\"success\",\"data\":{\"username\":")
            .append(JsonUtil.quote(user.getUsername())).append(",\"profile\":").append(profileJson(profile))
            .append(",\"addresses\":").append(addressesJson(addresses))
            .append(",\"message\":").append(JsonUtil.quote(message == null ? null : message.toString()))
            .append(",\"messageType\":").append(JsonUtil.quote(messageType == null ? null : messageType.toString()))
            .append(",\"settingsMessage\":").append(JsonUtil.quote((String) req.getAttribute("settingsMessage")))
            .append(",\"settingsMessageType\":").append(JsonUtil.quote((String) req.getAttribute("settingsMessageType")))
            .append("}}");
        resp.getWriter().write(json.toString());
    }

    private String profileJson(CustomerProfile profile) {
        if (profile == null) return "null";
        return "{\"firstName\":" + JsonUtil.quote(profile.getFirstName())
            + ",\"lastName\":" + JsonUtil.quote(profile.getLastName())
            + ",\"fullName\":" + JsonUtil.quote(profile.getFullName())
            + ",\"email\":" + JsonUtil.quote(profile.getEmail())
            + ",\"phone\":" + JsonUtil.quote(profile.getPhone())
            + ",\"phoneAreaCode\":" + JsonUtil.quote(profile.getPhoneAreaCode())
            + ",\"phoneNumberOnly\":" + JsonUtil.quote(profile.getPhoneNumberOnly())
            + ",\"formattedAddress\":" + JsonUtil.quote(profile.getFormattedAddress())
            + ",\"hasPaymentPin\":" + (profile.getPaymentPin() != null && !profile.getPaymentPin().isEmpty()) + '}';
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

    static String adminProfileJson(User user, AdminProfile profile) {
        String profileJson = profile == null ? "null" : "{\"department\":" + JsonUtil.quote(profile.getDepartment())
            + ",\"level\":" + profile.getLevel() + "}";
        return "{\"code\":200,\"msg\":\"success\",\"data\":{\"username\":"
            + JsonUtil.quote(user.getUsername()) + ",\"profile\":" + profileJson + "}}";
    }

    private void writeFailure(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        resp.getWriter().write("{\"code\":" + status + ",\"msg\":" + JsonUtil.quote(message) + "}");
    }
}
