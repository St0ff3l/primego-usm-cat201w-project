package com.primego.wallet.servlet;

import com.primego.wallet.dao.WalletDAO;
import com.primego.wallet.model.WalletTransaction;
import com.primego.user.model.Role;
import com.primego.user.model.User;
import com.primego.common.util.JsonUtil;
import com.primego.common.util.PathUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.util.UUID;

@WebServlet("/TopUpServlet")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024 * 2, // 2MB
        maxFileSize = 1024 * 1024 * 10,      // 10MB
        maxRequestSize = 1024 * 1024 * 50    // 50MB
)
public class TopUpServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        boolean jsonRequest = isJsonRequest(request);
        HttpSession session = jsonRequest ? request.getSession(false) : request.getSession();
        User user = session == null ? null : (User) session.getAttribute("user");

        if (user == null) {
            if (jsonRequest) {
                writeFailure(response, HttpServletResponse.SC_UNAUTHORIZED, "Please log in to submit a top-up.");
                return;
            }
            response.sendRedirect(request.getContextPath() + "/public/login.jsp");
            return;
        }
        if (jsonRequest && user.getRole() != Role.CUSTOMER) {
            writeFailure(response, HttpServletResponse.SC_FORBIDDEN, "Only customers can submit wallet top-ups.");
            return;
        }

        String amountStr = request.getParameter("amount");

        try {
            BigDecimal amount = new BigDecimal(amountStr);
            if (amount.compareTo(BigDecimal.ONE) < 0) {
                if (jsonRequest) {
                    writeFailure(response, HttpServletResponse.SC_BAD_REQUEST, "Top-up amount must be at least RM 1.00.");
                } else {
                    writeFailure(response, HttpServletResponse.SC_BAD_REQUEST, "Please enter an amount of at least RM 1.00.");
                }
                return;
            }

            Part filePart = request.getPart("receipt");
            if (filePart == null || filePart.getSize() == 0 || filePart.getSubmittedFileName() == null
                    || filePart.getSubmittedFileName().trim().isEmpty() || filePart.getContentType() == null
                    || !filePart.getContentType().startsWith("image/")) {
                if (jsonRequest) {
                    writeFailure(response, HttpServletResponse.SC_BAD_REQUEST, "Please upload a valid receipt image.");
                } else {
                    writeFailure(response, HttpServletResponse.SC_BAD_REQUEST, "Please upload a valid receipt image.");
                }
                return;
            }

            // 3. Handle file upload
            // Get deployment directory path (runtime usage)
            String uploadPath = request.getServletContext().getRealPath("") + File.separator + "assets" + File.separator + "images" + File.separator + "rechargephotos";

            // Get source directory path (for persistence) - using PathUtil
            String projectPath = PathUtil.getUploadDir(request.getServletContext(), "rechargephotos");

            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) uploadDir.mkdirs(); // Create directory if it doesn't exist

            File sourceDir = new File(projectPath);
            if (!sourceDir.exists()) sourceDir.mkdirs();

            String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
            // Prevent filename conflicts by adding a UUID prefix
            String uniqueFileName = UUID.randomUUID().toString() + "_" + fileName;

            // Save to deployment directory
            filePart.write(uploadPath + File.separator + uniqueFileName);

            // Save to source directory (Sync for local development persistence)
            // Only copy if the paths differ to prevent errors
            if (!new File(uploadPath).getAbsolutePath().equals(new File(projectPath).getAbsolutePath())) {
                try {
                    java.nio.file.Files.copy(
                            new File(uploadPath + File.separator + uniqueFileName).toPath(),
                            new File(projectPath + File.separator + uniqueFileName).toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING
                    );
                } catch (Exception e) {
                    e.printStackTrace(); // Ignore source write failure, does not affect main flow
                }
            }

            // 4. Save to database
            WalletTransaction transaction = new WalletTransaction();
            transaction.setUserId(user.getId());
            transaction.setAmount(amount);
            transaction.setStatus("PENDING");
            transaction.setTransactionType("TOPUP");
            // Save relative path for frontend reference
            transaction.setReceiptImage("assets/images/rechargephotos/" + uniqueFileName);

            WalletDAO dao = new WalletDAO();
            boolean success = dao.requestTopUp(transaction);

            if (success) {
                String message = "Top-up request submitted! Waiting for approval.";
                if (jsonRequest) {
                    writeSuccess(response, message);
                } else {
                    session.setAttribute("message", message);
                    response.sendRedirect(request.getContextPath() + "/public/wallet/wallet.jsp");
                }
            } else {
                if (jsonRequest) {
                    writeFailure(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error. Please try again.");
                } else {
                    writeFailure(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error. Please try again.");
                }
            }

        } catch (NumberFormatException e) {
            if (jsonRequest) {
                writeFailure(response, HttpServletResponse.SC_BAD_REQUEST, "Enter a valid top-up amount.");
            } else {
                writeFailure(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid amount format.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (jsonRequest) {
                writeFailure(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to submit your top-up request.");
            } else {
                writeFailure(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to submit your top-up request.");
            }
        }
    }

    private boolean isJsonRequest(HttpServletRequest request) {
        return request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json");
    }

    private void writeSuccess(HttpServletResponse response, String message) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write("{\"code\":200,\"msg\":\"success\",\"data\":{\"message\":" + JsonUtil.quote(message) + "}}");
    }

    private void writeFailure(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write("{\"code\":" + status + ",\"msg\":" + JsonUtil.quote(message) + "}");
    }
}
