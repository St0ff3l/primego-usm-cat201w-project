package com.primego.wallet.servlet;

import com.primego.common.util.JsonUtil;
import com.primego.user.model.Role;
import com.primego.user.model.User;
import com.primego.wallet.dao.WalletDAO;
import com.primego.wallet.model.WalletTransaction;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.List;

@WebServlet("/api/wallet")
public class WalletServlet extends HttpServlet {
    private final WalletDAO walletDAO = new WalletDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            writeFailure(resp, HttpServletResponse.SC_UNAUTHORIZED, "Please log in to view your wallet.");
            return;
        }
        if (user.getRole() != Role.CUSTOMER && user.getRole() != Role.MERCHANT) {
            writeFailure(resp, HttpServletResponse.SC_FORBIDDEN, "This wallet is not available for your account.");
            return;
        }

        try {
            BigDecimal balance = walletDAO.getBalance(user.getId());
            List<WalletTransaction> transactions = walletDAO.getUserTransactions(user.getId());
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.setHeader("Cache-Control", "no-store");
            resp.getWriter().write("{\"code\":200,\"msg\":\"success\",\"data\":{\"balance\":"
                + (balance == null ? "0.00" : balance.toPlainString())
                + ",\"transactions\":" + transactionsJson(transactions) + "}}");
        } catch (RuntimeException e) {
            writeFailure(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Unable to load wallet data.");
        }
    }

    static String transactionsJson(List<WalletTransaction> transactions) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < transactions.size(); i++) {
            if (i > 0) json.append(',');
            WalletTransaction transaction = transactions.get(i);
            json.append("{\"id\":").append(transaction.getId())
                .append(",\"amount\":").append(transaction.getAmount() == null ? "null" : transaction.getAmount().toPlainString())
                .append(",\"status\":").append(JsonUtil.quote(transaction.getStatus()))
                .append(",\"transactionType\":").append(JsonUtil.quote(transaction.getTransactionType()))
                .append(",\"createdAt\":").append(transaction.getCreatedAt() == null
                    ? "null" : JsonUtil.quote(dateFormat.format(transaction.getCreatedAt())))
                .append(",\"remarks\":").append(JsonUtil.quote(transaction.getRemarks())).append('}');
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
