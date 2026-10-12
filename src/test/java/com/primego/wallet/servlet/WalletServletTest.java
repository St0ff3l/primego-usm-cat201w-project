package com.primego.wallet.servlet;

import com.primego.wallet.model.WalletTransaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WalletServletTest {
    @Test
    void serializesWalletTransactionsAndEscapesRemarks() {
        WalletTransaction transaction = new WalletTransaction();
        transaction.setId(7);
        transaction.setAmount(new BigDecimal("12.30"));
        transaction.setStatus("PENDING");
        transaction.setTransactionType("TOPUP");
        transaction.setRemarks("Admin said \"pending\"");

        assertEquals("[{\"id\":7,\"amount\":12.30,\"status\":\"PENDING\",\"transactionType\":\"TOPUP\",\"createdAt\":null,\"remarks\":\"Admin said \\\"pending\\\"\"}]",
            WalletServlet.transactionsJson(List.of(transaction)));
    }
}
