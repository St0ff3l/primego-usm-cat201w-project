package com.primego.user.servlet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MerchantCreateServletTest {
    @Test
    void resultJsonEscapesTheMessage() {
        assertEquals("{\"code\":409,\"msg\":\"Username \\\"Alex\\\" already exists.\\nTry another.\"}",
            MerchantCreateServlet.resultJson(409, "Username \"Alex\" already exists.\nTry another."));
    }
}
