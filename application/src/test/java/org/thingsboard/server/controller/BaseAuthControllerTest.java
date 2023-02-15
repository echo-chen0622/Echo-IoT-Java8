package org.thingsboard.server.controller;

import org.junit.Test;
import org.thingsboard.server.common.data.security.Authority;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class BaseAuthControllerTest extends AbstractControllerTest {

    @Test
    public void testGetUser() throws Exception {

        doGet("/api/auth/user")
        .andExpect(status().isUnauthorized());

        loginSysAdmin();
        doGet("/api/auth/user")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authority",is(Authority.SYS_ADMIN.name())))
        .andExpect(jsonPath("$.email",is(SYS_ADMIN_EMAIL)));

        loginTenantAdmin();
        doGet("/api/auth/user")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authority",is(Authority.TENANT_ADMIN.name())))
        .andExpect(jsonPath("$.email",is(TENANT_ADMIN_EMAIL)));

        loginCustomerUser();
        doGet("/api/auth/user")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authority",is(Authority.CUSTOMER_USER.name())))
        .andExpect(jsonPath("$.email",is(CUSTOMER_USER_EMAIL)));
    }

    @Test
    public void testLoginLogout() throws Exception {
        loginSysAdmin();
        doGet("/api/auth/user")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authority",is(Authority.SYS_ADMIN.name())))
        .andExpect(jsonPath("$.email",is(SYS_ADMIN_EMAIL)));

        TimeUnit.SECONDS.sleep(1); //We need to make sure that event for invalidating token was successfully processed

        logout();
        doGet("/api/auth/user")
        .andExpect(status().isUnauthorized());

        resetTokens();
    }

    @Test
    public void testRefreshToken() throws Exception {
        loginSysAdmin();
        doGet("/api/auth/user")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authority",is(Authority.SYS_ADMIN.name())))
                .andExpect(jsonPath("$.email",is(SYS_ADMIN_EMAIL)));

        refreshToken();
        doGet("/api/auth/user")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authority",is(Authority.SYS_ADMIN.name())))
                .andExpect(jsonPath("$.email",is(SYS_ADMIN_EMAIL)));
    }
}
