package com.primego.wallet.servlet;

import com.primego.common.util.JsonUtil;
import com.primego.wallet.dao.WalletDAO;
import com.primego.user.model.User;
import com.primego.wallet.model.AdminTransactionLog;
import com.primego.wallet.model.WalletTransaction;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet("/WalletAdminServlet")
public class WalletAdminServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        boolean jsonRequest = isJsonRequest(request);

        // 1. Get current user session
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");

        // 2. Permission check: Ensure only ADMIN can operate
        String role = (user != null && user.getRole() != null) ? user.getRole().toString() : "";

        if (!"ADMIN".equals(role)) {
            if (jsonRequest) {
                writeJsonResult(response, user == null ? HttpServletResponse.SC_UNAUTHORIZED : HttpServletResponse.SC_FORBIDDEN,
                    "Access Denied: You are not an admin.");
                return;
            }
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied: You are not an admin.");
            return;
        }

        // 3. Get parameters from frontend
        String idStr = request.getParameter("id");
        String action = request.getParameter("action"); // "approve" or "reject"
        String remarks = request.getParameter("remarks"); // Get remarks
        int resultCode = HttpServletResponse.SC_BAD_REQUEST;
        String resultMessage;

        if (idStr != null && action != null) {
            try {
                int transactionId = Integer.parseInt(idStr);
                WalletDAO dao = new WalletDAO();
                boolean success = false;

                if (!"approve".equalsIgnoreCase(action) && !"reject".equalsIgnoreCase(action)) {
                    resultMessage = "Invalid action.";
                    if (jsonRequest) {
                        writeJsonResult(response, resultCode, resultMessage);
                        return;
                    }
                    request.getSession().setAttribute("error", resultMessage);
                    response.sendRedirect(request.getContextPath() + "/admin/wallet/admin_approval.jsp");
                    return;
                }

                // Get previous status (for logging)
                WalletTransaction oldTxn = dao.getTransactionById(transactionId);
                String previousStatus = (oldTxn != null) ? oldTxn.getStatus() : "UNKNOWN";

                // 4. Update database based on action
                String newStatus = "";
                if ("approve".equalsIgnoreCase(action)) {
                    newStatus = "APPROVED";
                    success = dao.updateTransactionStatus(transactionId, newStatus);
                } else if ("reject".equalsIgnoreCase(action)) {
                    newStatus = "REJECTED";
                    success = dao.updateTransactionStatus(transactionId, newStatus);
                }

                // 5. Record to admin_transaction_logs
                if (success) {
                    AdminTransactionLog log = new AdminTransactionLog();
                    log.setAdminId(user.getId());
                    log.setWalletTransactionId(transactionId);
                    log.setActionType(action.toUpperCase());
                    log.setPreviousStatus(previousStatus);
                    log.setCurrentStatus(newStatus);

                    // Use remarks from frontend, or default if empty
                    if (remarks == null || remarks.trim().isEmpty()) {
                        remarks = "Admin " + action + "d the request.";
                    }
                    log.setRemarks(remarks);

                    dao.addAdminLog(log);

                    resultMessage = "Transaction " + ("approve".equalsIgnoreCase(action) ? "approved" : "rejected") + " successfully.";
                    resultCode = HttpServletResponse.SC_OK;
                    if (!jsonRequest) session.setAttribute("message", resultMessage);
                } else {
                    resultMessage = "Failed to update transaction in database.";
                    resultCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                    if (!jsonRequest) session.setAttribute("error", resultMessage);
                }

            } catch (NumberFormatException e) {
                e.printStackTrace();
                resultMessage = "Invalid ID format.";
                if (!jsonRequest) request.getSession().setAttribute("error", resultMessage);
            } catch (Exception e) {
                e.printStackTrace();
                resultMessage = "System error: " + e.getMessage();
                resultCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                if (!jsonRequest) request.getSession().setAttribute("error", resultMessage);
            }
        } else {
            resultMessage = "Missing parameters.";
            if (!jsonRequest) request.getSession().setAttribute("error", resultMessage);
        }

        if (jsonRequest) {
            writeJsonResult(response, resultCode, resultMessage);
            return;
        }

        // 6. Redirect back to approval page
        response.sendRedirect(request.getContextPath() + "/admin/wallet/admin_approval.jsp");
    }

    private boolean isJsonRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("application/json");
    }

    private void writeJsonResult(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write("{\"code\":" + status + ",\"msg\":" + JsonUtil.quote(message) + "}");
    }
}
