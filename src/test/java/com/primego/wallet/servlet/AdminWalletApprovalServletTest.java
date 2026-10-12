package com.primego.wallet.servlet;

import com.primego.wallet.model.AdminTransactionLog;
import com.primego.wallet.model.WalletTransaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminWalletApprovalServletTest {
    @Test
    void serializesPendingRequestsWithoutLosingMoneyPrecision() {
        WalletTransaction transaction = new WalletTransaction();
        transaction.setId(19);
        transaction.setUserId(5);
        transaction.setAmount(new BigDecimal("12.30"));
        transaction.setTransactionType("TOPUP");
        transaction.setStatus("PENDING");
        transaction.setReceiptImage("assets/images/rechargephotos/receipt.jpg");
        transaction.setCreatedAt(Timestamp.valueOf("2026-10-12 10:30:00"));

        String json = AdminWalletApprovalServlet.transactionsJson(List.of(transaction));

        assertTrue(json.contains("\"amount\":\"12.30\""));
        assertTrue(json.contains("\"createdAt\":\"2026-10-12 10:30\""));
        assertTrue(json.contains("\"receiptImage\":\"assets/images/rechargephotos/receipt.jpg\""));
    }

    @Test
    void serializesHistoryAndRelatedTransactionDetails() {
        AdminTransactionLog log = new AdminTransactionLog();
        log.setId(7);
        log.setWalletTransactionId(19);
        log.setActionType("REJECT");
        log.setPreviousStatus("PENDING");
        log.setCurrentStatus("REJECTED");
        log.setRemarks("Receipt \"unclear\"\nplease resend");
        log.setAdminName("Admin One");

        WalletTransaction transaction = new WalletTransaction();
        transaction.setUserId(5);
        transaction.setAmount(new BigDecimal("12.30"));
        transaction.setTransactionType("TOPUP");

        String json = AdminWalletApprovalServlet.historyLogJson(log, transaction);

        assertTrue(json.contains("\"remarks\":\"Receipt \\\"unclear\\\"\\nplease resend\""));
        assertTrue(json.contains("\"userId\":5"));
        assertTrue(json.contains("\"amount\":\"12.30\""));
    }
}
