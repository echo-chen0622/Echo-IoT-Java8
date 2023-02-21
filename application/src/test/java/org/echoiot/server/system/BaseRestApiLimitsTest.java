package org.echoiot.server.system;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.util.concurrent.*;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.TenantProfile;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.echoiot.server.common.data.tenant.profile.TenantProfileConfiguration;
import org.echoiot.server.common.data.tenant.profile.TenantProfileData;
import org.echoiot.server.controller.AbstractControllerTest;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Illia Barkov
 */

@Slf4j
public abstract class BaseRestApiLimitsTest extends AbstractControllerTest {

    private static final int MESSAGES_LIMIT = 10;
    private static final int TIME_FOR_LIMIT = 5;

    TenantProfile tenantProfile;

    @NotNull
    ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));

    @Before
    public void before() throws Exception {
        loginSysAdmin();
        tenantProfile = getDefaultTenantProfile();
        resetTokens();
    }

    @After
    public void after() throws Exception {
        resetTokens();
        loginSysAdmin();
        saveTenantProfileWitConfiguration(tenantProfile, new DefaultTenantProfileConfiguration());
        resetTokens();
        service.shutdown();
    }

    @Test
    public void testCustomerRestApiLimits() throws Exception {
        loginSysAdmin();

        @NotNull String customerRestLimit = MESSAGES_LIMIT + ":" + TIME_FOR_LIMIT;

        DefaultTenantProfileConfiguration configurationWithCustomerRestLimits = createTenantProfileConfigurationWithRestLimits(null, customerRestLimit);

        saveTenantProfileWitConfiguration(tenantProfile, configurationWithCustomerRestLimits);

        resetTokens();

        loginCustomerUser();

        for (int i = 0; i < MESSAGES_LIMIT; i++) {
            doGet("/api/device/types").andExpect(status().isOk());
        }
        doGet("/api/device/types").andExpect(status().is4xxClientError());
    }

    @Test
    public void testTenantRestApiLimits() throws Exception {
        loginSysAdmin();

        @NotNull String tenantRestLimit = MESSAGES_LIMIT + ":" + TIME_FOR_LIMIT;

        DefaultTenantProfileConfiguration configurationWithTenantRestLimits = createTenantProfileConfigurationWithRestLimits(tenantRestLimit, null);

        saveTenantProfileWitConfiguration(tenantProfile, configurationWithTenantRestLimits);

        resetTokens();

        loginCustomerUser();

        for (int i = 0; i < MESSAGES_LIMIT; i++) {
            doGet("/api/device/types").andExpect(status().isOk());
        }
        doGet("/api/device/types").andExpect(status().is4xxClientError());
    }

    @Test
    public void testCustomerRestApiLimitsWithAsyncMethod() throws Exception {
        loginSysAdmin();

        @NotNull String tenantRestLimit = MESSAGES_LIMIT + ":" + TIME_FOR_LIMIT;

        DefaultTenantProfileConfiguration configurationWithTenantRestLimits = createTenantProfileConfigurationWithRestLimits(tenantRestLimit, null);

        saveTenantProfileWitConfiguration(tenantProfile, configurationWithTenantRestLimits);

        resetTokens();

        loginTenantAdmin();

        @NotNull List<ListenableFuture<ResultActions>> attributesRequests = new ArrayList<>();

        doGet("/api/plugins/telemetry/" + tenantId.getEntityType() + "/" + tenantId.getId().toString() + "/values/attributes").andExpect(status().isOk());
        Thread.sleep(TimeUnit.SECONDS.toMillis(TIME_FOR_LIMIT)); // Wait to initialization for bucket4j

        for (int i = 0; i < MESSAGES_LIMIT; i++) {
            attributesRequests.add(service.submit(() -> doGet("/api/plugins/telemetry/" + tenantId.getEntityType() + "/" + tenantId.getId().toString() + "/values/attributes")));
        }

        List<ResultActions> lists = blockForResponses(attributesRequests);

        for (@NotNull ResultActions resultActions : lists) {
            resultActions.andExpect(status().isOk());
        }

        doGet("/api/plugins/telemetry/" + tenantId.getEntityType() + "/" + tenantId.getId().toString() + "/values/attributes").andExpect(status().is4xxClientError());
    }

    @NotNull
    private TenantProfile getDefaultTenantProfile() throws Exception {

        @NotNull PageLink pageLink = new PageLink(17);
        PageData<TenantProfile> pageData = doGetTypedWithPageLink("/api/tenantProfiles?",
                new TypeReference<>(){}, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
        @NotNull List<TenantProfile> tenantProfiles = new ArrayList<>(pageData.getData());

        @NotNull Optional<TenantProfile> optionalDefaultProfile = tenantProfiles.stream().filter(TenantProfile::isDefault).reduce((a, b) -> null);
        Assert.assertTrue(optionalDefaultProfile.isPresent());

        return optionalDefaultProfile.get();
    }

    List<ResultActions> blockForResponses(@NotNull List<ListenableFuture<ResultActions>> futures) throws ExecutionException {
        @NotNull ListenableFuture<List<ResultActions>> futureOfList = Futures.allAsList(futures);
        List<ResultActions> responses;
        try {
            responses = futureOfList.get(20, TimeUnit.SECONDS);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            responses = new ArrayList<>();
            for (@NotNull ListenableFuture<ResultActions> future : futures) {
                if (future.isDone()) {
                    responses.add(Uninterruptibles.getUninterruptibly(future));
                }
            }
        }
        return responses;
    }

    private DefaultTenantProfileConfiguration createTenantProfileConfigurationWithRestLimits(String tenantLimits, String customerLimits) {
        DefaultTenantProfileConfiguration.DefaultTenantProfileConfigurationBuilder builder = DefaultTenantProfileConfiguration.builder();
        builder.tenantServerRestLimitsConfiguration(tenantLimits);
        builder.customerServerRestLimitsConfiguration(customerLimits);
        return builder.build();

    }

    private void saveTenantProfileWitConfiguration(@NotNull TenantProfile tenantProfile, TenantProfileConfiguration tenantProfileConfiguration) {
        TenantProfileData tenantProfileData = tenantProfile.getProfileData();
        tenantProfileData.setConfiguration(tenantProfileConfiguration);
        TenantProfile savedTenantProfile = doPost("/api/tenantProfile", tenantProfile, TenantProfile.class);
        Assert.assertNotNull(savedTenantProfile);
    }

}
