package com.primego.user.servlet;

import com.primego.common.util.JsonUtil;
import com.primego.user.dao.UserDAO;
import com.primego.user.model.MerchantProfile;
import com.primego.user.model.Role;
import com.primego.user.model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Servlet for creating new merchant accounts by admin.
 */
@WebServlet("/admin/merchant/create")
public class MerchantCreateServlet extends HttpServlet {

    private UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    /**
     * Processes the creation of a new merchant account.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        boolean jsonRequest = isJsonRequest(req);
        if (!jsonRequest) {
            resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, "Submit this form through the Vue app.");
            return;
        }
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            writeJsonMessage(resp, HttpServletResponse.SC_UNAUTHORIZED, "Please log in to create a merchant account.");
            return;
        }

        User currentUser = (User) session.getAttribute("user");
        if (currentUser.getRole() != Role.ADMIN) {
            writeJsonMessage(resp, HttpServletResponse.SC_FORBIDDEN, "Only administrators can create merchant accounts.");
            return;
        }

        String username = req.getParameter("username");
        String password = req.getParameter("password");
        String contactInfo = req.getParameter("contactInfo");

        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            writeJsonMessage(resp, HttpServletResponse.SC_BAD_REQUEST, "Username and password are required.");
            return;
        }

        if (userDAO.findByUsername(username) != null) {
            writeJsonMessage(resp, HttpServletResponse.SC_CONFLICT, "Username already exists.");
            return;
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(password); // Will be hashed in DAO
        newUser.setRole(Role.MERCHANT);
        newUser.setStatus(1); // Active

        // Store Name is optional/not required at creation now
        MerchantProfile profile = new MerchantProfile(0, null, null, contactInfo); // ID set in DAO

        try {
            userDAO.createMerchant(newUser, profile);
            session.setAttribute("globalMessage", "Merchant account created successfully!");
            session.setAttribute("globalMessageType", "success");
            writeJsonMessage(resp, HttpServletResponse.SC_OK, "success");
        } catch (SQLException e) {
            e.printStackTrace();
            writeJsonMessage(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    static String resultJson(int code, String message) {
        return "{\"code\":" + code + ",\"msg\":" + JsonUtil.quote(message) + "}";
    }

    private boolean isJsonRequest(HttpServletRequest req) {
        String accept = req.getHeader("Accept");
        return accept != null && accept.contains("application/json");
    }

    private void writeJsonMessage(HttpServletResponse resp, int code, String message) throws IOException {
        resp.setStatus(code);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        resp.getWriter().write(resultJson(code, message));
    }
}
