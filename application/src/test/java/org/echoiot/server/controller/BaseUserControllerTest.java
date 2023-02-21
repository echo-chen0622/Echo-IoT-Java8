package org.echoiot.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.echoiot.server.common.data.Customer;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.Tenant;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.audit.ActionType;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.id.UserId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.security.Authority;
import org.echoiot.server.dao.exception.DataValidationException;
import org.echoiot.server.dao.user.UserDao;
import org.echoiot.server.service.mail.TestMailService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.echoiot.server.dao.model.ModelConstants.SYSTEM_TENANT;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ContextConfiguration(classes = {BaseUserControllerTest.Config.class})
public abstract class BaseUserControllerTest extends AbstractControllerTest {

    private final IdComparator<User> idComparator = new IdComparator<>();

    private final CustomerId customerNUULId = (CustomerId) createEntityId_NULL_UUID(new Customer());

    @Resource
    private UserDao userDao;

    static class Config {
        @Bean
        @Primary
        public UserDao userDao(UserDao userDao) {
            return Mockito.mock(UserDao.class, AdditionalAnswers.delegatesTo(userDao));
        }
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();
    }

    @Test
    public void testSaveUser() throws Exception {
        loginSysAdmin();

        @NotNull String email = "tenant2@echoiot.org";
        @NotNull User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(tenantId);
        user.setEmail(email);
        user.setFirstName("Joe");
        user.setLastName("Downs");

        Mockito.reset(tbClusterService, auditLogService);

        User savedUser = doPost("/api/user", user, User.class);
        Assert.assertNotNull(savedUser);
        Assert.assertNotNull(savedUser.getId());
        Assert.assertTrue(savedUser.getCreatedTime() > 0);
        Assert.assertEquals(user.getEmail(), savedUser.getEmail());

        User foundUser = doGet("/api/user/" + savedUser.getId().getId().toString(), User.class);
        Assert.assertEquals(foundUser, savedUser);

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(foundUser, foundUser,
                SYSTEM_TENANT, customerNUULId, null, SYS_ADMIN_EMAIL,
                ActionType.ADDED, ActionType.ADDED, 1, 1, 1);
        Mockito.reset(tbClusterService, auditLogService);

        resetTokens();
        doGet("/api/noauth/activate?activateToken={activateToken}", TestMailService.currentActivateToken)
                .andExpect(status().isSeeOther())
                .andExpect(header().string(HttpHeaders.LOCATION, "/login/createPassword?activateToken=" + TestMailService.currentActivateToken));

        JsonNode activateRequest = new ObjectMapper().createObjectNode()
                .put("activateToken", TestMailService.currentActivateToken)
                .put("password", "testPassword");

        @NotNull JsonNode tokenInfo = readResponse(doPost("/api/noauth/activate", activateRequest).andExpect(status().isOk()), JsonNode.class);
        validateAndSetJwtToken(tokenInfo, email);

        doGet("/api/auth/user")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authority", is(Authority.TENANT_ADMIN.name())))
                .andExpect(jsonPath("$.email", is(email)));

        resetTokens();

        login(email, "testPassword");

        doGet("/api/auth/user")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authority", is(Authority.TENANT_ADMIN.name())))
                .andExpect(jsonPath("$.email", is(email)));

        loginSysAdmin();
        foundUser = doGet("/api/user/" + savedUser.getId().getId().toString(), User.class);

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/user/" + savedUser.getId().getId().toString())
                .andExpect(status().isOk());

        testNotifyEntityAllOneTimeLogEntityActionEntityEqClass(foundUser, foundUser.getId(), foundUser.getId(),
                SYSTEM_TENANT, customerNUULId, null, SYS_ADMIN_EMAIL,
                ActionType.DELETED, SYSTEM_TENANT.getId().toString());
    }

    @Test
    public void testSaveUserWithViolationOfFiledValidation() throws Exception {
        loginSysAdmin();

        Mockito.reset(tbClusterService, auditLogService);

        @NotNull String email = "tenant2@echoiot.org";
        @NotNull User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(tenantId);
        user.setEmail(email);
        user.setFirstName(StringUtils.randomAlphabetic(300));
        user.setLastName("Downs");
        String msgError = msgErrorFieldLength("first name");
        doPost("/api/user", user)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(user,
                SYSTEM_TENANT, null, SYS_ADMIN_EMAIL,
                ActionType.ADDED, new DataValidationException(msgError));
        Mockito.reset(tbClusterService, auditLogService);

        user.setFirstName("Normal name");
        msgError = msgErrorFieldLength("last name");
        user.setLastName(StringUtils.randomAlphabetic(300));
        doPost("/api/user", user)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(user,
                SYSTEM_TENANT, null, SYS_ADMIN_EMAIL,
                ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    public void testUpdateUserFromDifferentTenant() throws Exception {
        loginSysAdmin();

        User tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(tenantId);
        tenantAdmin.setEmail("tenant2@echoiot.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");
        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");

        loginDifferentTenant();

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/user", tenantAdmin)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));

        testNotifyEntityNever(tenantAdmin.getId(), tenantAdmin);

        deleteDifferentTenant();
    }

    @Test
    public void testResetPassword() throws Exception {
        loginSysAdmin();

        @NotNull String email = "tenant2@echoiot.org";
        @NotNull User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(tenantId);
        user.setEmail(email);
        user.setFirstName("Joe");
        user.setLastName("Downs");

        User savedUser = createUserAndLogin(user, "testPassword1");
        resetTokens();

        JsonNode resetPasswordByEmailRequest = new ObjectMapper().createObjectNode()
                .put("email", email);

        doPost("/api/noauth/resetPasswordByEmail", resetPasswordByEmailRequest)
                .andExpect(status().isOk());
        Thread.sleep(1000);
        doGet("/api/noauth/resetPassword?resetToken={resetToken}", TestMailService.currentResetPasswordToken)
                .andExpect(status().isSeeOther())
                .andExpect(header().string(HttpHeaders.LOCATION, "/login/resetPassword?resetToken=" + TestMailService.currentResetPasswordToken));

        JsonNode resetPasswordRequest = new ObjectMapper().createObjectNode()
                .put("resetToken", TestMailService.currentResetPasswordToken)
                .put("password", "testPassword2");

        @NotNull JsonNode tokenInfo = readResponse(
                doPost("/api/noauth/resetPassword", resetPasswordRequest)
                        .andExpect(status().isOk()), JsonNode.class);
        validateAndSetJwtToken(tokenInfo, email);

        doGet("/api/auth/user")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authority", is(Authority.TENANT_ADMIN.name())))
                .andExpect(jsonPath("$.email", is(email)));

        resetTokens();

        login(email, "testPassword2");
        doGet("/api/auth/user")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authority", is(Authority.TENANT_ADMIN.name())))
                .andExpect(jsonPath("$.email", is(email)));

        loginSysAdmin();
        doDelete("/api/user/" + savedUser.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testFindUserById() throws Exception {
        loginSysAdmin();

        @NotNull String email = "tenant2@echoiot.org";
        @NotNull User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(tenantId);
        user.setEmail(email);
        user.setFirstName("Joe");
        user.setLastName("Downs");

        User savedUser = doPost("/api/user", user, User.class);
        User foundUser = doGet("/api/user/" + savedUser.getId().getId().toString(), User.class);
        Assert.assertNotNull(foundUser);
        Assert.assertEquals(savedUser, foundUser);
    }

    @Test
    public void testSaveUserWithSameEmail() throws Exception {
        loginSysAdmin();

        Mockito.reset(tbClusterService, auditLogService);

        @NotNull String email = TENANT_ADMIN_EMAIL;
        @NotNull User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(tenantId);
        user.setEmail(email);
        user.setFirstName("Joe");
        user.setLastName("Downs");

        @NotNull String msgError = "User with email '" + email + "'  already present in database";
        doPost("/api/user", user)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(user,
                SYSTEM_TENANT, null, SYS_ADMIN_EMAIL,
                ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    public void testSaveUserWithInvalidEmail() throws Exception {
        loginSysAdmin();

        Mockito.reset(tbClusterService, auditLogService);

        @NotNull String email = "tenant_echoiot.org";
        @NotNull User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(tenantId);
        user.setEmail(email);
        user.setFirstName("Joe");
        user.setLastName("Downs");

        @NotNull String msgError = "Invalid email address format '" + email + "'";
        doPost("/api/user", user)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(user,
                SYSTEM_TENANT, null, SYS_ADMIN_EMAIL,
                ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    public void testSaveUserWithEmptyEmail() throws Exception {
        loginSysAdmin();

        Mockito.reset(tbClusterService, auditLogService);

        @NotNull User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(tenantId);
        user.setFirstName("Joe");
        user.setLastName("Downs");

        @NotNull String msgError = "User email " + msgErrorShouldBeSpecified;
        doPost("/api/user", user)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("User email " + msgErrorShouldBeSpecified)));

        testNotifyEntityEqualsOneTimeServiceNeverError(user,
                SYSTEM_TENANT, null, SYS_ADMIN_EMAIL,
                ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    public void testSaveUserWithoutTenant() throws Exception {
        loginSysAdmin();

        Mockito.reset(tbClusterService, auditLogService);

        @NotNull User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setEmail("tenant2@echoiot.org");
        user.setFirstName("Joe");
        user.setLastName("Downs");

        @NotNull String msgError = "Tenant administrator should be assigned to tenant";
        doPost("/api/user", user)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(user,
                SYSTEM_TENANT, null, SYS_ADMIN_EMAIL,
                ActionType.ADDED, new DataValidationException(msgError));

    }

    @Test
    public void testDeleteUser() throws Exception {
        loginSysAdmin();

        @NotNull String email = "tenant2@echoiot.org";
        @NotNull User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(tenantId);
        user.setEmail(email);
        user.setFirstName("Joe");
        user.setLastName("Downs");

        User savedUser = doPost("/api/user", user, User.class);
        User foundUser = doGet("/api/user/" + savedUser.getId().getId().toString(), User.class);
        Assert.assertNotNull(foundUser);

        doDelete("/api/user/" + savedUser.getId().getId().toString())
                .andExpect(status().isOk());

        String userIdStr = savedUser.getId().getId().toString();
        doGet("/api/user/" + userIdStr)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString( msgErrorNoFound("User",userIdStr))));
    }

    @Test
    public void testFindTenantAdmins() throws Exception {
        loginSysAdmin();

        //here created a new tenant despite already created on AbstractWebTest and then delete the tenant properly on the last line
        @NotNull Tenant tenant = new Tenant();
        tenant.setTitle("My tenant with many admins");
        Tenant savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        TenantId tenantId = savedTenant.getId();

        Mockito.reset(tbClusterService, auditLogService);

        int cntEntity = 64;
        @NotNull List<User> tenantAdmins = new ArrayList<>();
        for (int i = 0; i < cntEntity; i++) {
            @NotNull User user = new User();
            user.setAuthority(Authority.TENANT_ADMIN);
            user.setTenantId(tenantId);
            user.setEmail("testTenant" + i + "@echoiot.org");
            tenantAdmins.add(doPost("/api/user", user, User.class));
        }

        @NotNull User testManyUser = new User();
        testManyUser.setTenantId(tenantId);
        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(testManyUser, testManyUser,
                SYSTEM_TENANT, customerNUULId, null, SYS_ADMIN_EMAIL,
                ActionType.ADDED, ActionType.ADDED, cntEntity, cntEntity, cntEntity);

        @NotNull List<User> loadedTenantAdmins = new ArrayList<>();
        PageLink pageLink = new PageLink(33);
        @Nullable PageData<User> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/" + tenantId.getId().toString() + "/users?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedTenantAdmins.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(tenantAdmins, idComparator);
        Collections.sort(loadedTenantAdmins, idComparator);

        assertThat(tenantAdmins).as("admins list size").hasSameSizeAs(loadedTenantAdmins);
        assertThat(tenantAdmins).as("admins list content").isEqualTo(loadedTenantAdmins);

        doDelete("/api/tenant/" + tenantId.getId().toString())
                .andExpect(status().isOk());

        pageLink = new PageLink(33);
        pageData = doGetTypedWithPageLink("/api/tenant/" + tenantId.getId().toString() + "/users?",
                new TypeReference<>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());
    }

    @Test
    public void testFindTenantAdminsByEmail() throws Exception {

        loginSysAdmin();

        @NotNull String email1 = "testEmail1";
        @NotNull List<User> tenantAdminsEmail1 = new ArrayList<>();

        final int NUMBER_OF_USERS = 124;

        for (int i = 0; i < NUMBER_OF_USERS; i++) {
            @NotNull User user = new User();
            user.setAuthority(Authority.TENANT_ADMIN);
            user.setTenantId(tenantId);
            @NotNull String suffix = StringUtils.randomAlphanumeric((int) (5 + Math.random() * 10));
            @NotNull String email = email1 + suffix + "@echoiot.org";
            email = i % 2 == 0 ? email.toLowerCase() : email.toUpperCase();
            user.setEmail(email);
            tenantAdminsEmail1.add(doPost("/api/user", user, User.class));
        }

        @NotNull String email2 = "testEmail2";
        @NotNull List<User> tenantAdminsEmail2 = new ArrayList<>();

        for (int i = 0; i < 112; i++) {
            @NotNull User user = new User();
            user.setAuthority(Authority.TENANT_ADMIN);
            user.setTenantId(tenantId);
            @NotNull String suffix = StringUtils.randomAlphanumeric((int) (5 + Math.random() * 10));
            @NotNull String email = email2 + suffix + "@echoiot.org";
            email = i % 2 == 0 ? email.toLowerCase() : email.toUpperCase();
            user.setEmail(email);
            tenantAdminsEmail2.add(doPost("/api/user", user, User.class));
        }

        @NotNull List<User> loadedTenantAdminsEmail1 = new ArrayList<>();
        PageLink pageLink = new PageLink(33, 0, email1);
        @Nullable PageData<User> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/" + tenantId.getId().toString() + "/users?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedTenantAdminsEmail1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(tenantAdminsEmail1, idComparator);
        Collections.sort(loadedTenantAdminsEmail1, idComparator);

        Assert.assertEquals(tenantAdminsEmail1, loadedTenantAdminsEmail1);

        @NotNull List<User> loadedTenantAdminsEmail2 = new ArrayList<>();
        pageLink = new PageLink(16, 0, email2);
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/" + tenantId.getId().toString() + "/users?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedTenantAdminsEmail2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(tenantAdminsEmail2, idComparator);
        Collections.sort(loadedTenantAdminsEmail2, idComparator);

        Assert.assertEquals(tenantAdminsEmail2, loadedTenantAdminsEmail2);

        Mockito.reset(tbClusterService, auditLogService);

        int cntEntity = loadedTenantAdminsEmail1.size();
        for (@NotNull User user : loadedTenantAdminsEmail1) {
            doDelete("/api/user/" + user.getId().getId().toString())
                    .andExpect(status().isOk());
        }
        @NotNull User testManyUser = new User();
        testManyUser.setTenantId(tenantId);
        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(testManyUser, testManyUser,
                                                                SYSTEM_TENANT, customerNUULId, null, SYS_ADMIN_EMAIL,
                                                                ActionType.DELETED, ActionType.DELETED, cntEntity, NUMBER_OF_USERS, cntEntity, ""
                                                               );

        pageLink = new PageLink(4, 0, email1);
        pageData = doGetTypedWithPageLink("/api/tenant/" + tenantId.getId().toString() + "/users?",
                new TypeReference<>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (@NotNull User user : loadedTenantAdminsEmail2) {
            doDelete("/api/user/" + user.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4, 0, email2);
        pageData = doGetTypedWithPageLink("/api/tenant/" + tenantId.getId().toString() + "/users?",
                new TypeReference<>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testFindCustomerUsers() throws Exception {
        loginSysAdmin();

        @NotNull User tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(tenantId);
        tenantAdmin.setEmail("tenant2@echoiot.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        createUserAndLogin(tenantAdmin, "testPassword1");

        @NotNull Customer customer = new Customer();
        customer.setTitle("My customer");
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);

        CustomerId customerId = savedCustomer.getId();

        @NotNull List<User> customerUsers = new ArrayList<>();
        for (int i = 0; i < 56; i++) {
            @NotNull User user = new User();
            user.setAuthority(Authority.CUSTOMER_USER);
            user.setCustomerId(customerId);
            user.setEmail("testCustomer" + i + "@echoiot.org");
            customerUsers.add(doPost("/api/user", user, User.class));
        }

        @NotNull List<User> loadedCustomerUsers = new ArrayList<>();
        PageLink pageLink = new PageLink(33);
        @Nullable PageData<User> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/users?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedCustomerUsers.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(customerUsers, idComparator);
        Collections.sort(loadedCustomerUsers, idComparator);

        Assert.assertEquals(customerUsers, loadedCustomerUsers);

        doDelete("/api/customer/" + customerId.getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testFindCustomerUsersByEmail() throws Exception {
        loginSysAdmin();

        @NotNull User tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(tenantId);
        tenantAdmin.setEmail("tenant2@echoiot.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        createUserAndLogin(tenantAdmin, "testPassword1");

        @NotNull Customer customer = new Customer();
        customer.setTitle("My customer");
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);

        CustomerId customerId = savedCustomer.getId();

        @NotNull String email1 = "testEmail1";
        @NotNull List<User> customerUsersEmail1 = new ArrayList<>();

        for (int i = 0; i < 74; i++) {
            @NotNull User user = new User();
            user.setAuthority(Authority.CUSTOMER_USER);
            user.setCustomerId(customerId);
            @NotNull String suffix = StringUtils.randomAlphanumeric((int) (5 + Math.random() * 10));
            @NotNull String email = email1 + suffix + "@echoiot.org";
            email = i % 2 == 0 ? email.toLowerCase() : email.toUpperCase();
            user.setEmail(email);
            customerUsersEmail1.add(doPost("/api/user", user, User.class));
        }

        @NotNull String email2 = "testEmail2";
        @NotNull List<User> customerUsersEmail2 = new ArrayList<>();

        for (int i = 0; i < 92; i++) {
            @NotNull User user = new User();
            user.setAuthority(Authority.CUSTOMER_USER);
            user.setCustomerId(customerId);
            @NotNull String suffix = StringUtils.randomAlphanumeric((int) (5 + Math.random() * 10));
            @NotNull String email = email2 + suffix + "@echoiot.org";
            email = i % 2 == 0 ? email.toLowerCase() : email.toUpperCase();
            user.setEmail(email);
            customerUsersEmail2.add(doPost("/api/user", user, User.class));
        }

        @NotNull List<User> loadedCustomerUsersEmail1 = new ArrayList<>();
        PageLink pageLink = new PageLink(33, 0, email1);
        PageData<User> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/users?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedCustomerUsersEmail1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(customerUsersEmail1, idComparator);
        Collections.sort(loadedCustomerUsersEmail1, idComparator);

        Assert.assertEquals(customerUsersEmail1, loadedCustomerUsersEmail1);

        @NotNull List<User> loadedCustomerUsersEmail2 = new ArrayList<>();
        pageLink = new PageLink(16, 0, email2);
        do {
            pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/users?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedCustomerUsersEmail2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(customerUsersEmail2, idComparator);
        Collections.sort(loadedCustomerUsersEmail2, idComparator);

        Assert.assertEquals(customerUsersEmail2, loadedCustomerUsersEmail2);

        for (@NotNull User user : loadedCustomerUsersEmail1) {
            doDelete("/api/user/" + user.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4, 0, email1);
        pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/users?",
                new TypeReference<>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (@NotNull User user : loadedCustomerUsersEmail2) {
            doDelete("/api/user/" + user.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4, 0, email2);
        pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/users?",
                new TypeReference<>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        doDelete("/api/customer/" + customerId.getId().toString())
                .andExpect(status().isOk());
    }


    @Test
    public void testDeleteUserWithDeleteRelationsOk() throws Exception {
        UserId userId = createUser().getId();
        testEntityDaoWithRelationsOk(tenantId, userId, "/api/user/" + userId);
    }

    @Test
    public void testDeleteUserExceptionWithRelationsTransactional() throws Exception {
        UserId userId = createUser().getId();
        testEntityDaoWithRelationsTransactionalException(userDao, tenantId, userId, "/api/user/" + userId);
    }

    @Test
    public void givenInvalidPageLink_thenReturnError() throws Exception {
        loginTenantAdmin();

        @NotNull String invalidSortProperty = "abc(abc)";

        @NotNull ResultActions result = doGet("/api/users?page={page}&pageSize={pageSize}&sortProperty={sortProperty}", 0, 100, invalidSortProperty)
                .andExpect(status().isBadRequest());
        assertThat(getErrorMessage(result)).containsIgnoringCase("invalid sort property");
    }

    private User createUser() throws Exception {
        loginSysAdmin();
        @NotNull String email = "tenant2@echoiot.org";
        @NotNull User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(tenantId);
        user.setEmail(email);
        user.setFirstName("Joe");
        user.setLastName("Downs");
        return doPost("/api/user", user, User.class);
    }
}
