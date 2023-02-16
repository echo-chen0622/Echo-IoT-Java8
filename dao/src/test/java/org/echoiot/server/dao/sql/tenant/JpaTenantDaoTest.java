package org.echoiot.server.dao.sql.tenant;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.assertj.core.api.Assertions;
import org.echoiot.server.common.data.Tenant;
import org.echoiot.server.common.data.TenantProfile;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.dao.service.AbstractServiceTest;
import org.echoiot.server.dao.service.BaseTenantProfileServiceTest;
import org.echoiot.server.dao.tenant.TenantDao;
import org.echoiot.server.dao.tenant.TenantProfileDao;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.echoiot.server.dao.AbstractJpaDaoTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * Created by Valerii Sosliuk on 4/30/2017.
 */
public class JpaTenantDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private TenantProfileDao tenantProfileDao;

    List<Tenant> createdTenants = new ArrayList<>();
    TenantProfile tenantProfile;

    @Before
    public void setUp() throws Exception {
        tenantProfile = tenantProfileDao.save(TenantId.SYS_TENANT_ID, BaseTenantProfileServiceTest.createTenantProfile("default tenant profile"));
        assertThat(tenantProfile).as("tenant profile").isNotNull();
    }

    @After
    public void tearDown() throws Exception {
        createdTenants.forEach((tenant)-> tenantDao.removeById(TenantId.SYS_TENANT_ID, tenant.getUuidId()));
        tenantProfileDao.removeById(TenantId.SYS_TENANT_ID, tenantProfile.getUuidId());
    }

    @Test
    //@DatabaseSetup("classpath:dbunit/empty_dataset.xml")
    public void testFindTenants() {
        createTenants();
        Assert.assertEquals(30, tenantDao.find(AbstractServiceTest.SYSTEM_TENANT_ID).size());

        PageLink pageLink = new PageLink(20, 0, "title");
        PageData<Tenant> tenants1 = tenantDao.findTenants(AbstractServiceTest.SYSTEM_TENANT_ID, pageLink);
        assertEquals(20, tenants1.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<Tenant> tenants2 = tenantDao.findTenants(AbstractServiceTest.SYSTEM_TENANT_ID,
                pageLink);
        assertEquals(10, tenants2.getData().size());

        pageLink = pageLink.nextPageLink();
        PageData<Tenant> tenants3 = tenantDao.findTenants(AbstractServiceTest.SYSTEM_TENANT_ID,
                pageLink);
        assertEquals(0, tenants3.getData().size());
    }

    private void createTenants() {
        for (int i = 0; i < 30; i++) {
            createTenant("TITLE", i);
        }
    }

    void createTenant(String title, int index) {
        Tenant tenant = new Tenant();
        tenant.setId(TenantId.fromUUID(Uuids.timeBased()));
        tenant.setTitle(title + "_" + index);
        tenant.setTenantProfileId(tenantProfile.getId());
        createdTenants.add(tenantDao.save(TenantId.SYS_TENANT_ID, tenant));
    }

    @Test
    //@DatabaseSetup("classpath:dbunit/empty_dataset.xml")
    public void testIsExistsTenantById() {
        final UUID uuid = Uuids.timeBased();
        final TenantId tenantId = new TenantId(uuid);
        Assertions.assertThat(tenantDao.existsById(tenantId, uuid)).as("Is tenant exists before save").isFalse();

        final Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setTitle("Tenant " + uuid);
        tenant.setTenantProfileId(tenantProfile.getId());

        createdTenants.add(tenantDao.save(TenantId.SYS_TENANT_ID, tenant));

        Assertions.assertThat(tenantDao.existsById(tenantId, uuid)).as("Is tenant exists after save").isTrue();

    }

}
