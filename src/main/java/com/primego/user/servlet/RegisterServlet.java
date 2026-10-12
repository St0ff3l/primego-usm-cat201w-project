package com.primego.user.servlet;

import com.primego.user.dao.UserDAO;
import com.primego.user.model.Role;
import com.primego.user.model.User;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Servlet handling user registration (sign up).
 */
@WebServlet("/register")
public class RegisterServlet extends HttpServlet {
    private UserDAO userDAO = new UserDAO();

    /**
     * Renders the registration page.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/public/signup.jsp").forward(req, resp);
    }

    /**
     * Processes new user registration.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        boolean jsonRequest = req.getHeader("Accept") != null && req.getHeader("Accept").contains("application/json");
        String username = req.getParameter("username");
        String email = req.getParameter("email");
        String confirmEmail = req.getParameter("confirmEmail");
        String password = req.getParameter("password");
        String confirmPassword = req.getParameter("confirmPassword");

        // Server-side validation
        if (email == null || confirmEmail == null || !email.equals(confirmEmail)) {
            if (jsonRequest) { writeJsonError(resp, "Emails do not match"); return; }
            req.setAttribute("error", "Emails do not match");
            req.getRequestDispatcher("/public/signup.jsp").forward(req, resp);
            return;
        }

        if (password == null || confirmPassword == null || !password.equals(confirmPassword)) {
            if (jsonRequest) { writeJsonError(resp, "Passwords do not match"); return; }
            req.setAttribute("error", "Passwords do not match");
            req.getRequestDispatcher("/public/signup.jsp").forward(req, resp);
            return;
        }

        if (userDAO.findByUsername(username) != null) {
            if (jsonRequest) { writeJsonError(resp, "Username already exists"); return; }
            req.setAttribute("error", "Username already exists");
            req.getRequestDispatcher("/public/signup.jsp").forward(req, resp);
            return;
        }

        // Create new user (Default role: CUSTOMER)
        User newUser = new User(username, password, Role.CUSTOMER);
        try {
            userDAO.createUser(newUser, email);
            if (jsonRequest) {
                resp.setContentType("application/json");
                resp.setCharacterEncoding("UTF-8");
                resp.getWriter().write("{\"redirectUrl\":\"" + req.getContextPath() + "/public/login.jsp\"}");
                return;
            }
            // Redirect to login page with success message (optional: could pass via session
            // or URL param)
            resp.sendRedirect(req.getContextPath() + "/public/login.jsp");
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            if (jsonRequest) { writeJsonError(resp, "Registration failed: " + e.getMessage()); return; }
            req.setAttribute("error", "Registration failed: " + e.getMessage());
            req.getRequestDispatcher("/public/signup.jsp").forward(req, resp);
        }
    }

    private void writeJsonError(HttpServletResponse resp, String error) throws IOException {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        String escaped = error.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
        resp.getWriter().write("{\"error\":\"" + escaped + "\"}");
    }
}
