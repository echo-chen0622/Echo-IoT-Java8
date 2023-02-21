package org.echoiot.server.dao.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.echoiot.common.util.EchoiotExecutors;
import org.echoiot.server.common.data.Customer;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.Tenant;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.dao.exception.DataValidationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class BaseCustomerServiceTest extends AbstractServiceTest {
    static final int TIMEOUT = 30;

    ListeningExecutorService executor;

    private TenantId tenantId;

    @Before
    public void before() {
        executor = MoreExecutors.listeningDecorator(EchoiotExecutors.newWorkStealingPool(8, getClass()));

        @NotNull Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = tenantService.saveTenant(tenant);
        Assert.assertNotNull(savedTenant);
        tenantId = savedTenant.getId();
    }

    @After
    public void after() {
        executor.shutdownNow();

        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testSaveCustomer() {
        @NotNull Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setTitle("My customer");
        Customer savedCustomer = customerService.saveCustomer(customer);

        Assert.assertNotNull(savedCustomer);
        Assert.assertNotNull(savedCustomer.getId());
        Assert.assertTrue(savedCustomer.getCreatedTime() > 0);
        Assert.assertEquals(customer.getTenantId(), savedCustomer.getTenantId());
        Assert.assertEquals(customer.getTitle(), savedCustomer.getTitle());


        savedCustomer.setTitle("My new customer");

        customerService.saveCustomer(savedCustomer);
        Customer foundCustomer = customerService.findCustomerById(tenantId, savedCustomer.getId());
        Assert.assertEquals(foundCustomer.getTitle(), savedCustomer.getTitle());

        customerService.deleteCustomer(tenantId, savedCustomer.getId());
    }

    @Test
    public void testFindCustomerById() {
        @NotNull Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setTitle("My customer");
        Customer savedCustomer = customerService.saveCustomer(customer);
        Customer foundCustomer = customerService.findCustomerById(tenantId, savedCustomer.getId());
        Assert.assertNotNull(foundCustomer);
        Assert.assertEquals(savedCustomer, foundCustomer);
        customerService.deleteCustomer(tenantId, savedCustomer.getId());
    }

    @Test(expected = DataValidationException.class)
    public void testSaveCustomerWithEmptyTitle() {
        @NotNull Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customerService.saveCustomer(customer);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveCustomerWithEmptyTenant() {
        @NotNull Customer customer = new Customer();
        customer.setTitle("My customer");
        customerService.saveCustomer(customer);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveCustomerWithInvalidTenant() {
        @NotNull Customer customer = new Customer();
        customer.setTitle("My customer");
        customer.setTenantId(TenantId.fromUUID(Uuids.timeBased()));
        customerService.saveCustomer(customer);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveCustomerWithInvalidEmail() {
        @NotNull Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setTitle("My customer");
        customer.setEmail("invalid@mail");
        customerService.saveCustomer(customer);
    }

    @Test
    public void testDeleteCustomer() {
        @NotNull Customer customer = new Customer();
        customer.setTitle("My customer");
        customer.setTenantId(tenantId);
        Customer savedCustomer = customerService.saveCustomer(customer);
        customerService.deleteCustomer(tenantId, savedCustomer.getId());
        Customer foundCustomer = customerService.findCustomerById(tenantId, savedCustomer.getId());
        Assert.assertNull(foundCustomer);
    }

    @Test
    public void testFindCustomersByTenantId() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant = tenantService.saveTenant(tenant);

        TenantId tenantId = tenant.getId();

        @NotNull List<ListenableFuture<Customer>> futures = new ArrayList<>(135);
        for (int i = 0; i < 135; i++) {
            @NotNull Customer customer = new Customer();
            customer.setTenantId(tenantId);
            customer.setTitle("Customer" + i);
            futures.add(executor.submit(() ->
                    customerService.saveCustomer(customer)));
        }
        List<Customer> customers = Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS);

        @NotNull List<Customer> loadedCustomers = new ArrayList<>(135);
        PageLink pageLink = new PageLink(23);
        @Nullable PageData<Customer> pageData = null;
        do {
            pageData = customerService.findCustomersByTenantId(tenantId, pageLink);
            loadedCustomers.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assertThat(customers).containsExactlyInAnyOrderElementsOf(loadedCustomers);

        customerService.deleteCustomersByTenantId(tenantId);

        pageLink = new PageLink(33);
        pageData = customerService.findCustomersByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());

        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testFindCustomersByTenantIdAndTitle() throws Exception {
        @NotNull String title1 = "Customer title 1";
        @NotNull List<ListenableFuture<Customer>> futures = new ArrayList<>(143);
        for (int i = 0; i < 143; i++) {
            @NotNull Customer customer = new Customer();
            customer.setTenantId(tenantId);
            @NotNull String suffix = StringUtils.randomAlphanumeric((int) (5 + Math.random() * 10));
            @NotNull String title = title1 + suffix;
            title = i % 2 == 0 ? title.toLowerCase() : title.toUpperCase();
            customer.setTitle(title);
            futures.add(executor.submit(() -> customerService.saveCustomer(customer)));
        }
        List<Customer> customersTitle1 = Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS);

        @NotNull String title2 = "Customer title 2";
        futures = new ArrayList<>(175);
        for (int i = 0; i < 175; i++) {
            @NotNull Customer customer = new Customer();
            customer.setTenantId(tenantId);
            @NotNull String suffix = StringUtils.randomAlphanumeric((int) (5 + Math.random() * 10));
            @NotNull String title = title2 + suffix;
            title = i % 2 == 0 ? title.toLowerCase() : title.toUpperCase();
            customer.setTitle(title);
            futures.add(executor.submit(() -> customerService.saveCustomer(customer)));
        }
        List<Customer> customersTitle2 = Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS);

        @NotNull List<Customer> loadedCustomersTitle1 = new ArrayList<>(143);
        PageLink pageLink = new PageLink(15, 0, title1);
        @Nullable PageData<Customer> pageData = null;
        do {
            pageData = customerService.findCustomersByTenantId(tenantId, pageLink);
            loadedCustomersTitle1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assertThat(customersTitle1).as(title1).containsExactlyInAnyOrderElementsOf(loadedCustomersTitle1);

        @NotNull List<Customer> loadedCustomersTitle2 = new ArrayList<>(175);
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = customerService.findCustomersByTenantId(tenantId, pageLink);
            loadedCustomersTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assertThat(customersTitle2).as(title2).containsExactlyInAnyOrderElementsOf(loadedCustomersTitle2);

        @NotNull List<ListenableFuture<Void>> deleteFutures = new ArrayList<>(143);
        for (@NotNull Customer customer : loadedCustomersTitle1) {
            deleteFutures.add(executor.submit(() -> {
                customerService.deleteCustomer(tenantId, customer.getId());
                return null;
            }));
        }
        Futures.allAsList(deleteFutures).get(TIMEOUT, TimeUnit.SECONDS);

        pageLink = new PageLink(4, 0, title1);
        pageData = customerService.findCustomersByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        deleteFutures = new ArrayList<>(175);
        for (@NotNull Customer customer : loadedCustomersTitle2) {
            deleteFutures.add(executor.submit(() -> {
                customerService.deleteCustomer(tenantId, customer.getId());
                return null;
            }));
        }
        Futures.allAsList(deleteFutures).get(TIMEOUT, TimeUnit.SECONDS);

        pageLink = new PageLink(4, 0, title2);
        pageData = customerService.findCustomersByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }
}
