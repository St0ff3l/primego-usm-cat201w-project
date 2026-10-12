package com.primego.user.servlet;

import com.primego.user.model.AdminProfile;
import com.primego.user.model.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProfileServletTest {
    @Test
    void serializesTheCurrentAdminProfileWithoutChangingTheLegacyView() {
        User user = new User();
        user.setUsername("Admin \"One\"");
        AdminProfile profile = new AdminProfile(1, "Operations", 2);

        assertEquals("{\"code\":200,\"msg\":\"success\",\"data\":{\"username\":\"Admin \\\"One\\\"\",\"profile\":{\"department\":\"Operations\",\"level\":2}}}",
            ProfileServlet.adminProfileJson(user, profile));
        assertEquals("{\"code\":200,\"msg\":\"success\",\"data\":{\"username\":\"Admin \\\"One\\\"\",\"profile\":null}}",
            ProfileServlet.adminProfileJson(user, null));
    }
}
