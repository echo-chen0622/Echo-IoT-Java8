package org.echoiot.server.dao.sql.device;

import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.security.DeviceCredentials;
import org.echoiot.server.common.data.security.DeviceCredentialsType;
import org.echoiot.server.dao.device.DeviceCredentialsDao;
import org.echoiot.server.dao.service.AbstractServiceTest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.echoiot.server.dao.AbstractJpaDaoTest;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
public class JpaDeviceCredentialsDaoTest extends AbstractJpaDaoTest {

    @Autowired
    DeviceCredentialsDao deviceCredentialsDao;

    List<DeviceCredentials> deviceCredentialsList;
    DeviceCredentials neededDeviceCredentials;

    @Before
    public void setUp() {
        deviceCredentialsList = List.of(createAndSaveDeviceCredentials(), createAndSaveDeviceCredentials());
        neededDeviceCredentials = deviceCredentialsList.get(0);
        assertNotNull(neededDeviceCredentials);
    }

    DeviceCredentials createAndSaveDeviceCredentials() {
        DeviceCredentials deviceCredentials = new DeviceCredentials();
        deviceCredentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
        deviceCredentials.setCredentialsId(UUID.randomUUID().toString());
        deviceCredentials.setCredentialsValue("CHECK123");
        deviceCredentials.setDeviceId(new DeviceId(UUID.randomUUID()));
        return deviceCredentialsDao.save(TenantId.SYS_TENANT_ID, deviceCredentials);
    }

    @After
    public void deleteDeviceCredentials() {
        for (DeviceCredentials credentials : deviceCredentialsList) {
            deviceCredentialsDao.removeById(TenantId.SYS_TENANT_ID, credentials.getUuidId());
        }
    }

    @Test
    public void testFindByDeviceId() {
        DeviceCredentials foundedDeviceCredentials = deviceCredentialsDao.findByDeviceId(AbstractServiceTest.SYSTEM_TENANT_ID, neededDeviceCredentials.getDeviceId().getId());
        assertNotNull(foundedDeviceCredentials);
        Assert.assertEquals(neededDeviceCredentials.getId(), foundedDeviceCredentials.getId());
        assertEquals(neededDeviceCredentials.getCredentialsId(), foundedDeviceCredentials.getCredentialsId());
    }

    @Test
    public void findByCredentialsId() {
        DeviceCredentials foundedDeviceCredentials = deviceCredentialsDao.findByCredentialsId(AbstractServiceTest.SYSTEM_TENANT_ID, neededDeviceCredentials.getCredentialsId());
        assertNotNull(foundedDeviceCredentials);
        Assert.assertEquals(neededDeviceCredentials.getId(), foundedDeviceCredentials.getId());
    }
}
