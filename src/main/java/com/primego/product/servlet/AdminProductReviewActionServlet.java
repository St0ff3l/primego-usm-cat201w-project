package com.primego.product.servlet;

import com.primego.common.util.JsonUtil;
import com.primego.product.dao.ProductDAO;
import com.primego.user.model.Role;
import com.primego.user.model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet("/admin/product/review/action")
public class AdminProductReviewActionServlet extends HttpServlet {

    private final ProductDAO productDAO = new ProductDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        boolean jsonRequest = isJsonRequest(req);
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            if (jsonRequest) {
                writeFailure(resp, HttpServletResponse.SC_UNAUTHORIZED, "Please log in to review products.");
                return;
            }
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        User user = (User) session.getAttribute("user");
        if (user.getRole() != Role.ADMIN) {
            if (jsonRequest) {
                writeFailure(resp, HttpServletResponse.SC_FORBIDDEN, "Only administrators can review products.");
                return;
            }
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied");
            return;
        }

        String idStr = req.getParameter("productId");
        String action = req.getParameter("action");
        String reason = req.getParameter("reason");

        if (idStr == null || action == null) {
            respond(req, resp, "/admin/product/review/list", "error");
            return;
        }

        int productId;
        try {
            productId = Integer.parseInt(idStr);
            if (productId < 1) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            respond(req, resp, "/admin/product/review/list", "error");
            return;
        }

        boolean ok;
        String msg;

        if ("approve".equals(action)) {
            ok = productDAO.updateProductAuditByAdmin(productId, "APPROVED", null);
            msg = ok ? "approved" : "error";
        } else if ("reject".equals(action)) {
            String trimmedReason = (reason == null) ? null : reason.trim();
            ok = productDAO.updateProductAuditByAdmin(productId, "REJECTED", trimmedReason);
            msg = ok ? "rejected" : "error";
        } else {
            respond(req, resp, "/admin/product/review?productId=" + productId, "error");
            return;
        }

        respond(req, resp, "/admin/product/review?productId=" + productId + "&msg=" + msg, msg);
    }

    private boolean isJsonRequest(HttpServletRequest req) {
        String accept = req.getHeader("Accept");
        return accept != null && accept.contains("application/json");
    }

    private void respond(HttpServletRequest req, HttpServletResponse resp, String redirect, String message)
            throws IOException {
        if (!isJsonRequest(req)) {
            resp.sendRedirect(req.getContextPath() + redirect);
            return;
        }
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        resp.getWriter().write("{\"code\":200,\"msg\":\"success\",\"data\":{\"message\":"
            + JsonUtil.quote(message) + ",\"redirect\":" + JsonUtil.quote(redirect) + "}}");
    }

    private void writeFailure(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        resp.getWriter().write("{\"code\":" + status + ",\"msg\":" + JsonUtil.quote(message) + "}");
    }
}
