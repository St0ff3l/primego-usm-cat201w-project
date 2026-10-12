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
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.util.UUID;

@WebServlet("/WithdrawServlet")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024 * 2,
        maxFileSize = 1024 * 1024 * 10,
        maxRequestSize = 1024 * 1024 * 12
)
public class WithdrawServlet extends HttpServlet {

    private WalletDAO walletDAO;

    public void init() {
        walletDAO = new WalletDAO();
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        boolean jsonRequest = isJsonRequest(request);
        HttpSession session = jsonRequest ? request.getSession(false) : request.getSession();
        User user = session == null ? null : (User) session.getAttribute("user");

        if (user == null) {
            if (jsonRequest) {
                writeFailure(response, HttpServletResponse.SC_UNAUTHORIZED, "Please log in to submit a withdrawal.");
                return;
            }
            response.sendRedirect(request.getContextPath() + "/public/login.jsp");
            return;
        }
        if (jsonRequest && user.getRole() != Role.CUSTOMER && user.getRole() != Role.MERCHANT) {
            writeFailure(response, HttpServletResponse.SC_FORBIDDEN, "This account cannot withdraw wallet funds.");
            return;
        }

        try {
            // 1. Get amount
            String amountStr = request.getParameter("amount");
            BigDecimal amount = new BigDecimal(amountStr);
            if (amount.signum() <= 0) {
                if (jsonRequest) {
                    writeFailure(response, HttpServletResponse.SC_BAD_REQUEST, "Enter a valid amount greater than zero.");
                } else {
                    writeFailure(response, HttpServletResponse.SC_BAD_REQUEST, "Please enter a valid amount greater than 0.");
                }
                return;
            }

            // 2. Check balance
            BigDecimal currentBalance = walletDAO.getBalance(user.getId());
            if (currentBalance.compareTo(amount) < 0) {
                if (jsonRequest) {
                    writeFailure(response, HttpServletResponse.SC_BAD_REQUEST, "Withdrawal amount exceeds your available balance.");
                } else {
                    writeFailure(response, HttpServletResponse.SC_BAD_REQUEST, "Withdrawal amount exceeds your available balance.");
                }
                return;
            }

            // 3. Handle image upload
            Part filePart = request.getPart("receiptImage");
            String fileName = null;

            if (filePart != null && filePart.getSize() > 0 && filePart.getSubmittedFileName() != null
                    && !filePart.getSubmittedFileName().trim().isEmpty() && filePart.getContentType() != null
                    && filePart.getContentType().startsWith("image/")) {
                // Generate unique filename
                String originalFileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
                String fileExt = "";
                int dotIndex = originalFileName.lastIndexOf(".");
                if (dotIndex > 0) {
                    fileExt = originalFileName.substring(dotIndex);
                }
                fileName = UUID.randomUUID().toString() + fileExt;

                // Save to withdrawphotos folder
                // 1. Deployment directory
                String uploadPath = getServletContext().getRealPath("") + File.separator + "assets" + File.separator + "images" + File.separator + "withdrawphotos";

                // 2. Source directory - Use PathUtil
                String projectPath = PathUtil.getUploadDir(getServletContext(), "withdrawphotos");

                // DEBUG LOGGING
                System.out.println("[WithdrawServlet] Upload Dir (Runtime): " + uploadPath);
                System.out.println("[WithdrawServlet] Source Dir (Local): " + projectPath);

                // Automatically create directories
                File uploadDir = new File(uploadPath);
                if (!uploadDir.exists()) uploadDir.mkdirs();

                File sourceDir = new File(projectPath);
                if (!sourceDir.exists()) sourceDir.mkdirs();

                // Save file to deployment directory
                filePart.write(uploadPath + File.separator + fileName);

                // Copy to source directory
                if (!new File(uploadPath).getAbsolutePath().equals(new File(projectPath).getAbsolutePath())) {
                    try {
                        java.nio.file.Files.copy(
                                new File(uploadPath + File.separator + fileName).toPath(),
                                new File(projectPath + File.separator + fileName).toPath(),
                                java.nio.file.StandardCopyOption.REPLACE_EXISTING
                        );
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                if (jsonRequest) {
                    writeFailure(response, HttpServletResponse.SC_BAD_REQUEST, "Please upload a valid QR code image.");
                } else {
                    writeFailure(response, HttpServletResponse.SC_BAD_REQUEST, "Please upload a valid QR code image.");
                }
                return;
            }

            // 4. Create transaction object
            WalletTransaction transaction = new WalletTransaction();
            transaction.setUserId(user.getId());
            transaction.setAmount(amount);
            transaction.setStatus("PENDING");
            transaction.setTransactionType("WITHDRAW");
            // Save relative path
            transaction.setReceiptImage("assets/images/withdrawphotos/" + fileName);

            // 5. Save to database
            boolean success = walletDAO.requestWithdraw(transaction);

            if (success) {
                String message = "Withdrawal request submitted! QR Code uploaded.";
                if (jsonRequest) {
                    writeSuccess(response, message);
                } else {
                    session.setAttribute("message", message);
                    response.sendRedirect(request.getContextPath() + "/public/wallet/wallet.jsp");
                }
            } else {
                if (jsonRequest) {
                    writeFailure(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to submit request.");
                } else {
                    writeFailure(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to submit request.");
                }
            }

        } catch (NumberFormatException e) {
            if (jsonRequest) {
                writeFailure(response, HttpServletResponse.SC_BAD_REQUEST, "Enter a valid withdrawal amount.");
            } else {
                writeFailure(response, HttpServletResponse.SC_BAD_REQUEST, "Enter a valid withdrawal amount.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (jsonRequest) {
                writeFailure(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to submit your withdrawal request.");
            } else {
                writeFailure(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to submit your withdrawal request.");
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
