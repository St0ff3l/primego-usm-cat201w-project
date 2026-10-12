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

    /**
     * Renders the merchant creation form.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        User user = (User) session.getAttribute("user");
        if (user.getRole() != Role.ADMIN) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied");
            return;
        }

        req.getRequestDispatcher("/admin/merchant_create.jsp").forward(req, resp);
    }

    /**
     * Processes the creation of a new merchant account.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        boolean jsonRequest = isJsonRequest(req);
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            if (jsonRequest) {
                writeJsonMessage(resp, HttpServletResponse.SC_UNAUTHORIZED, "Please log in to create a merchant account.");
                return;
            }
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        User currentUser = (User) session.getAttribute("user");
        if (currentUser.getRole() != Role.ADMIN) {
            if (jsonRequest) {
                writeJsonMessage(resp, HttpServletResponse.SC_FORBIDDEN, "Only administrators can create merchant accounts.");
                return;
            }
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied");
            return;
        }

        String username = req.getParameter("username");
        String password = req.getParameter("password");
        String contactInfo = req.getParameter("contactInfo");

        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            if (jsonRequest) {
                writeJsonMessage(resp, HttpServletResponse.SC_BAD_REQUEST, "Username and password are required.");
                return;
            }
            req.setAttribute("error", "Username and password are required.");
            req.getRequestDispatcher("/admin/merchant_create.jsp").forward(req, resp);
            return;
        }

        if (userDAO.findByUsername(username) != null) {
            if (jsonRequest) {
                writeJsonMessage(resp, HttpServletResponse.SC_CONFLICT, "Username already exists.");
                return;
            }
            req.setAttribute("error", "Username already exists.");
            req.getRequestDispatcher("/admin/merchant_create.jsp").forward(req, resp);
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
            if (jsonRequest) {
                writeJsonMessage(resp, HttpServletResponse.SC_OK, "success");
                return;
            }
            resp.sendRedirect(req.getContextPath() + "/admin/dashboard?tab=users");
        } catch (SQLException e) {
            e.printStackTrace();
            if (jsonRequest) {
                writeJsonMessage(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
                return;
            }
            req.setAttribute("error", "Database error: " + e.getMessage());
            req.getRequestDispatcher("/admin/merchant_create.jsp").forward(req, resp);
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
