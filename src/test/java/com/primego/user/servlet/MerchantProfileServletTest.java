package com.primego.user.servlet;

import com.primego.user.model.MerchantProfile;
import com.primego.user.model.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MerchantProfileServletTest {
    @Test
    void serializesMerchantProfileAndMissingProfile() {
        User user = new User();
        user.setUsername("Seller \"One\"");
        MerchantProfile profile = new MerchantProfile(7, "Prime Shop", "LIC-1", "60123456789");

        assertEquals("{\"code\":200,\"msg\":\"success\",\"data\":{\"username\":\"Seller \\\"One\\\"\",\"profile\":{\"storeName\":\"Prime Shop\",\"businessLicense\":\"LIC-1\",\"contactInfo\":\"60123456789\"}}}",
            MerchantProfileServlet.profileJson(user, profile));
        assertEquals("{\"code\":200,\"msg\":\"success\",\"data\":{\"username\":\"Seller \\\"One\\\"\",\"profile\":null}}",
            MerchantProfileServlet.profileJson(user, null));
    }
}
