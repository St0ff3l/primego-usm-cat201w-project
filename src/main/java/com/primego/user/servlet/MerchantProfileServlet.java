package com.primego.user.servlet;

import com.primego.common.util.JsonUtil;
import com.primego.user.dao.ProfileDAO;
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

@WebServlet("/api/merchant/profile")
public class MerchantProfileServlet extends HttpServlet {
    private final ProfileDAO profileDAO = new ProfileDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            writeFailure(resp, HttpServletResponse.SC_UNAUTHORIZED, "Please log in to view your merchant profile.");
            return;
        }
        if (user.getRole() != Role.MERCHANT) {
            writeFailure(resp, HttpServletResponse.SC_FORBIDDEN, "Merchant profile is not available for this account.");
            return;
        }

        try {
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.setHeader("Cache-Control", "no-store");
            resp.getWriter().write(profileJson(user, profileDAO.getMerchantProfile(user.getId())));
        } catch (RuntimeException e) {
            writeFailure(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Unable to load your merchant profile.");
        }
    }

    static String profileJson(User user, MerchantProfile profile) {
        String profileJson = profile == null ? "null" : "{\"storeName\":" + JsonUtil.quote(profile.getStoreName())
            + ",\"businessLicense\":" + JsonUtil.quote(profile.getBusinessLicense())
            + ",\"contactInfo\":" + JsonUtil.quote(profile.getContactInfo()) + "}";
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
