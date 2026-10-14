package com.primego.user.servlet;

import com.primego.user.dao.UserDAO;
import com.primego.user.model.Role;
import com.primego.user.model.User;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Servlet handling user login.
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    /**
     * Processes login credentials.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 1. Set request encoding to prevent character encoding issues
        req.setCharacterEncoding("UTF-8");
        boolean jsonRequest = req.getHeader("Accept") != null && req.getHeader("Accept").contains("application/json");
        if (!jsonRequest) {
            resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, "Submit credentials through the Vue app.");
            return;
        }

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        User user = userDAO.validateCredentials(username, password);

        // From src/main/java/com/primego/user/servlet/LoginServlet.java
        if (user != null) {
            // Create Session
            HttpSession session = req.getSession();
            session.setAttribute("user", user);

            String redirectUrl = user.getRole() == Role.MERCHANT
                ? "/merchant/merchant_dashboard.jsp"
                : "/";
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write("{\"redirectUrl\":\"" + redirectUrl + "\"}");

        } else {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write("{\"error\":\"Invalid username or password\"}");
        }
    }
}
