package com.primego.wallet.servlet;

import com.primego.common.util.JsonUtil;
import com.primego.user.model.Role;
import com.primego.user.model.User;
import com.primego.wallet.dao.WalletDAO;
import com.primego.wallet.model.AdminTransactionLog;
import com.primego.wallet.model.WalletTransaction;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/admin/api/wallet-approvals")
public class AdminWalletApprovalServlet extends HttpServlet {
    private final WalletDAO walletDAO = new WalletDAO();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null || user.getRole() != Role.ADMIN) {
            writeFailure(resp, user == null ? HttpServletResponse.SC_UNAUTHORIZED : HttpServletResponse.SC_FORBIDDEN,
                "Only administrators can access wallet approvals.");
            return;
        }

        String view = "history".equals(req.getParameter("view")) ? "history" : "pending";
        String type = req.getParameter("type");
        if (type == null || type.isEmpty()) type = "TOPUP";
        String transactions = "[]";
        String history = "[]";
        if ("history".equals(view)) {
            history = historyJson(walletDAO.getProcessedTransactions(), walletDAO);
        } else {
            List<WalletTransaction> pending = new ArrayList<>();
            for (WalletTransaction transaction : walletDAO.getPendingTransactions()) {
                if (transaction.getTransactionType() != null && transaction.getTransactionType().equalsIgnoreCase(type)) {
                    pending.add(transaction);
                }
            }
            transactions = transactionsJson(pending);
        }

        String message = session.getAttribute("message") instanceof String ? (String) session.getAttribute("message") : null;
        String error = session.getAttribute("error") instanceof String ? (String) session.getAttribute("error") : null;
        session.removeAttribute("message");
        session.removeAttribute("error");
        writeSuccess(resp, "{\"view\":" + JsonUtil.quote(view) + ",\"type\":" + JsonUtil.quote(type)
            + ",\"transactions\":" + transactions + ",\"history\":" + history
            + ",\"message\":" + JsonUtil.quote(message) + ",\"error\":" + JsonUtil.quote(error) + "}");
    }

    static String transactionsJson(List<WalletTransaction> transactions) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < transactions.size(); i++) {
            if (i > 0) json.append(',');
            WalletTransaction transaction = transactions.get(i);
            json.append("{\"id\":").append(transaction.getId())
                .append(",\"userId\":").append(transaction.getUserId())
                .append(",\"amount\":").append(decimalJson(transaction.getAmount()))
                .append(",\"transactionType\":").append(JsonUtil.quote(transaction.getTransactionType()))
                .append(",\"status\":").append(JsonUtil.quote(transaction.getStatus()))
                .append(",\"receiptImage\":").append(JsonUtil.quote(transaction.getReceiptImage()))
                .append(",\"createdAt\":").append(timestampJson(transaction.getCreatedAt())).append('}');
        }
        return json.append(']').toString();
    }

    static String historyLogJson(AdminTransactionLog log, WalletTransaction transaction) {
        return "{\"id\":" + log.getId()
            + ",\"walletTransactionId\":" + log.getWalletTransactionId()
            + ",\"actionType\":" + JsonUtil.quote(log.getActionType())
            + ",\"previousStatus\":" + JsonUtil.quote(log.getPreviousStatus())
            + ",\"currentStatus\":" + JsonUtil.quote(log.getCurrentStatus())
            + ",\"remarks\":" + JsonUtil.quote(log.getRemarks())
            + ",\"adminName\":" + JsonUtil.quote(log.getAdminName())
            + ",\"createdAt\":" + timestampJson(log.getCreatedAt())
            + ",\"amount\":" + decimalJson(transaction == null ? null : transaction.getAmount())
            + ",\"userId\":" + (transaction == null ? "null" : Integer.toString(transaction.getUserId()))
            + ",\"transactionType\":" + JsonUtil.quote(transaction == null ? null : transaction.getTransactionType())
            + ",\"receiptImage\":" + JsonUtil.quote(transaction == null ? null : transaction.getReceiptImage()) + '}';
    }

    private static String historyJson(List<AdminTransactionLog> logs, WalletDAO dao) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < logs.size(); i++) {
            if (i > 0) json.append(',');
            AdminTransactionLog log = logs.get(i);
            json.append(historyLogJson(log, dao.getTransactionById(log.getWalletTransactionId())));
        }
        return json.append(']').toString();
    }

    private static String decimalJson(BigDecimal value) {
        return JsonUtil.quote(value == null ? null : value.toPlainString());
    }

    private static String timestampJson(Timestamp value) {
        return JsonUtil.quote(value == null ? null : value.toLocalDateTime().format(DATE_FORMAT));
    }

    private static void writeSuccess(HttpServletResponse resp, String data) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        resp.getWriter().write("{\"code\":200,\"msg\":\"success\",\"data\":" + data + "}");
    }

    private static void writeFailure(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        resp.getWriter().write("{\"code\":" + status + ",\"msg\":" + JsonUtil.quote(message) + "}");
    }
}
