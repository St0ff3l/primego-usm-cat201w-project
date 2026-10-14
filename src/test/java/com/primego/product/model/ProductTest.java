package com.primego.product.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductTest {
    @Test
    void checksMerchantOwnership() {
        Product product = new Product();
        product.setMerchantId(7);

        assertTrue(product.belongsToMerchant(7));
        assertFalse(product.belongsToMerchant(8));
    }
}
