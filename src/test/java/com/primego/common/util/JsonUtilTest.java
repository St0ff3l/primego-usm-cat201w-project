package com.primego.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonUtilTest {
    @Test
    void quotesJsonStringsAndNulls() {
        assertEquals("null", JsonUtil.quote(null));
        assertEquals("\"quote: \\\" slash: \\\\ line: \\n\"", JsonUtil.quote("quote: \" slash: \\ line: \n"));
    }
}
