package com.primego.user.servlet;

import com.primego.common.listener.ActiveSessionListener;
import com.primego.common.util.JsonUtil;
import com.primego.common.util.PasswordUtil;
import com.primego.order.dao.OrderDAO;
import com.primego.user.dao.LogDAO;
import com.primego.user.dao.ProfileDAO;
import com.primego.user.dao.UserDAO;
import com.primego.user.dao.VisitDAO;
import com.primego.user.model.AdminProfile;
import com.primego.user.model.Role;
import com.primego.user.model.SystemLog;
import com.primego.user.model.User;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Servlet for the Admin Dashboard.
 * Aggregates statistics, user management, and system logs for the admin view.
 */
@WebServlet("/admin/dashboard")
public class AdminDashboardServlet extends HttpServlet {
    private UserDAO userDAO = new UserDAO();
    private ProfileDAO profileDAO = new ProfileDAO();
    private OrderDAO orderDAO = new OrderDAO();
    private LogDAO logDAO = new LogDAO();

    /**
     * Renders the admin dashboard with real-time statistics and data.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        boolean jsonRequest = isJsonRequest(req);
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            if (jsonRequest) {
                writeFailure(resp, HttpServletResponse.SC_UNAUTHORIZED, "Please log in to view the admin dashboard.");
                return;
            }
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        User user = (User) session.getAttribute("user");
        if (user.getRole() != Role.ADMIN) {
            if (jsonRequest) {
                writeFailure(resp, HttpServletResponse.SC_FORBIDDEN, "Only administrators can access the dashboard.");
                return;
            }
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied");
            return;
        }

        // Log this access (Optional: limit frequency or move to filter)
        logDAO.addLog("INFO", "Admin '" + user.getUsername() + "' accessed dashboard");

        // Load Admin Profile
        AdminProfile adminProfile = profileDAO.getAdminProfile(user.getId());
        req.setAttribute("adminProfile", adminProfile);

        // --- 1. Dashboard Overview Data (Real) ---
        int totalUsers = userDAO.countUsers();
        int activeSessions = ActiveSessionListener.getActiveSessionsCount();
        int dailyVisits = new VisitDAO().getTodayVisits();
        int revenue = orderDAO.countTotalTransactions();
        req.setAttribute("totalUsers", totalUsers);
        req.setAttribute("activeSessions", activeSessions);
        req.setAttribute("dailyVisits", dailyVisits);
        req.setAttribute("revenue", revenue);

        // --- Real Chart Data (Last 7 Days) ---
        Map<java.time.LocalDate, Integer> visitsMap = new VisitDAO().getLast7DaysVisits();

        StringBuilder labelsBuilder = new StringBuilder();
        StringBuilder dataBuilder = new StringBuilder();

        int i = 0;
        for (Map.Entry<java.time.LocalDate, Integer> entry : visitsMap.entrySet()) {
            if (i > 0) {
                labelsBuilder.append(", ");
                dataBuilder.append(", ");
            }
            // Format: 'Mon'
            String dayName = entry.getKey().getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            labelsBuilder.append("'").append(dayName).append("'");
            dataBuilder.append(entry.getValue());
            i++;
        }

        req.setAttribute("chartLabels", labelsBuilder.toString());
        req.setAttribute("chartData", dataBuilder.toString());

        // --- Real User Distribution Data ---
        int customerCount = userDAO.countUsersByRole(Role.CUSTOMER);
        int merchantCount = userDAO.countUsersByRole(Role.MERCHANT);
        int adminCount = userDAO.countUsersByRole(Role.ADMIN);
        req.setAttribute("userChartData", customerCount + ", " + merchantCount + ", " + adminCount);

        // Fetch Order Lists for Transaction Modal
        List<com.primego.order.model.Order> paidOrders = orderDAO.getOrdersByStatusForAdmin("PAID");
        List<com.primego.order.model.Order> shippedOrders = orderDAO.getOrdersByStatusForAdmin("SHIPPED");
        List<com.primego.order.model.Order> completedOrders = orderDAO.getOrdersByStatusForAdmin("COMPLETED");
        List<com.primego.order.model.Order> cancelledOrders = orderDAO.getOrdersByStatusForAdmin("CANCELLED");
        req.setAttribute("paidOrders", paidOrders);
        req.setAttribute("shippedOrders", shippedOrders);
        req.setAttribute("completedOrders", completedOrders);
        req.setAttribute("cancelledOrders", cancelledOrders);

        // --- Real System Logs ---
        List<SystemLog> logs = logDAO.getRecentLogs(5);
        req.setAttribute("logs", logs);

        // --- 2. User Management Data (Real) ---
        List<User> allUsers = userDAO.findAllUsers();
        req.setAttribute("userList", allUsers);

        if (jsonRequest) {
            Object globalMessage = session.getAttribute("globalMessage");
            session.removeAttribute("globalMessage");
            session.removeAttribute("globalMessageType");
            writeDashboardJson(resp, totalUsers, activeSessions, dailyVisits, revenue, visitsMap,
                customerCount, merchantCount, adminCount, logs, allUsers, adminProfile,
                paidOrders, shippedOrders, completedOrders, cancelledOrders, (String) globalMessage);
            return;
        }

        resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, "The Vue app renders this page.");
    }

    /**
     * Handles admin profile updates and password changes from the dashboard.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        boolean jsonRequest = isJsonRequest(req);
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            if (jsonRequest) {
                writeFailure(resp, HttpServletResponse.SC_UNAUTHORIZED, "Please log in to update admin settings.");
                return;
            }
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        User user = (User) session.getAttribute("user");
        if (user.getRole() != Role.ADMIN) {
            if (jsonRequest) {
                writeFailure(resp, HttpServletResponse.SC_FORBIDDEN, "Only administrators can update settings.");
                return;
            }
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied");
            return;
        }

        String action = req.getParameter("action");
        String message = "";
        String messageType = "info";

        if ("updateProfile".equals(action)) {
            String department = req.getParameter("department");
            AdminProfile profile = new AdminProfile(user.getId(), department, 0); // Level not editable from UI
            if (profileDAO.updateAdminProfile(profile)) {
                message = "Profile updated successfully.";
                messageType = "success";
            } else {
                message = "Failed to update profile.";
                messageType = "error";
            }
        } else if ("changePassword".equals(action)) {
            String currentPassword = req.getParameter("currentPassword");
            String newPassword = req.getParameter("newPassword");
            String confirmPassword = req.getParameter("confirmPassword");

            if (currentPassword == null || currentPassword.isEmpty()
                    || !PasswordUtil.checkPassword(currentPassword, user.getPassword())) {
                message = "Current password is incorrect.";
                messageType = "error";
            } else if (newPassword == null || newPassword.isEmpty() || !newPassword.equals(confirmPassword)) {
                message = "New passwords do not match.";
                messageType = "error";
            } else {
                if (userDAO.updatePassword(user.getId(), newPassword)) {
                    // Update session user password
                    user.setPassword(PasswordUtil.hashPassword(newPassword));
                    message = "Password changed successfully.";
                    messageType = "success";
                } else {
                    message = "Failed to change password.";
                    messageType = "error";
                }
            }
        }

        if (jsonRequest) {
            AdminProfile savedProfile = profileDAO.getAdminProfile(user.getId());
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.setHeader("Cache-Control", "no-store");
            resp.getWriter().write("{\"code\":200,\"msg\":\"success\",\"data\":{\"settingsMessage\":"
                + JsonUtil.quote(message) + ",\"settingsMessageType\":" + JsonUtil.quote(messageType)
                + ",\"department\":" + JsonUtil.quote(savedProfile == null ? null : savedProfile.getDepartment()) + "}}");
            return;
        }

        resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, "The Vue app renders this page.");
    }

    static String usersJson(List<User> users) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < users.size(); i++) {
            if (i > 0) json.append(',');
            User user = users.get(i);
            json.append("{\"id\":").append(user.getId())
                .append(",\"username\":").append(JsonUtil.quote(user.getUsername()))
                .append(",\"role\":").append(JsonUtil.quote(user.getRole() == null ? null : user.getRole().name()))
                .append(",\"status\":").append(user.getStatus())
                .append(",\"createdAt\":").append(timestampJson(user.getCreatedAt())).append('}');
        }
        return json.append(']').toString();
    }

    static String ordersJson(List<com.primego.order.model.Order> orders, boolean includeTracking) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < orders.size(); i++) {
            if (i > 0) json.append(',');
            com.primego.order.model.Order order = orders.get(i);
            json.append("{\"ordersId\":").append(order.getOrdersId())
                .append(",\"customerId\":").append(order.getCustomerId())
                .append(",\"totalAmount\":").append(decimalJson(order.getTotalAmount()))
                .append(",\"createdAt\":").append(timestampJson(order.getCreatedAt()));
            if (includeTracking) json.append(",\"trackingNumber\":").append(JsonUtil.quote(order.getTrackingNumber()));
            json.append('}');
        }
        return json.append(']').toString();
    }

    private String logsJson(List<SystemLog> logs) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < logs.size(); i++) {
            if (i > 0) json.append(',');
            SystemLog log = logs.get(i);
            json.append("{\"level\":").append(JsonUtil.quote(log.getLevel()))
                .append(",\"message\":").append(JsonUtil.quote(log.getMessage()))
                .append(",\"time\":").append(JsonUtil.quote(log.getTime())).append('}');
        }
        return json.append(']').toString();
    }

    private String visitsJson(Map<LocalDate, Integer> visits) {
        StringBuilder json = new StringBuilder("[");
        int i = 0;
        for (Map.Entry<LocalDate, Integer> entry : visits.entrySet()) {
            if (i++ > 0) json.append(',');
            String label = entry.getKey().getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            json.append("{\"label\":").append(JsonUtil.quote(label))
                .append(",\"count\":").append(entry.getValue()).append('}');
        }
        return json.append(']').toString();
    }

    private void writeDashboardJson(HttpServletResponse resp, int totalUsers, int activeSessions,
            int dailyVisits, int revenue, Map<LocalDate, Integer> visits, int customers, int merchants,
            int admins, List<SystemLog> logs, List<User> users, AdminProfile profile,
            List<com.primego.order.model.Order> paidOrders, List<com.primego.order.model.Order> shippedOrders,
            List<com.primego.order.model.Order> completedOrders, List<com.primego.order.model.Order> cancelledOrders,
            String globalMessage) throws IOException {
        String profileJson = profile == null ? "null" : "{\"department\":" + JsonUtil.quote(profile.getDepartment())
            + ",\"level\":" + profile.getLevel() + "}";
        String data = "\"totalUsers\":" + totalUsers
            + ",\"activeSessions\":" + activeSessions
            + ",\"dailyVisits\":" + dailyVisits
            + ",\"revenue\":" + revenue
            + ",\"visits\":" + visitsJson(visits)
            + ",\"userDistribution\":[" + customers + ',' + merchants + ',' + admins + ']'
            + ",\"logs\":" + logsJson(logs)
            + ",\"users\":" + usersJson(users)
            + ",\"adminProfile\":" + profileJson
            + ",\"transactions\":{\"paid\":" + ordersJson(paidOrders, false)
            + ",\"shipped\":" + ordersJson(shippedOrders, true)
            + ",\"completed\":" + ordersJson(completedOrders, false)
            + ",\"cancelled\":" + ordersJson(cancelledOrders, false) + '}'
            + ",\"globalMessage\":" + JsonUtil.quote(globalMessage);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        resp.getWriter().write("{\"code\":200,\"msg\":\"success\",\"data\":{" + data + "}}");
    }

    private static String timestampJson(java.sql.Timestamp value) {
        return JsonUtil.quote(value == null ? null : value.toString());
    }

    private static String decimalJson(BigDecimal value) {
        return JsonUtil.quote(value == null ? null : value.toPlainString());
    }

    private boolean isJsonRequest(HttpServletRequest req) {
        String accept = req.getHeader("Accept");
        return accept != null && accept.contains("application/json");
    }

    private void writeFailure(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        resp.getWriter().write("{\"code\":" + status + ",\"msg\":" + JsonUtil.quote(message) + "}");
    }

}
