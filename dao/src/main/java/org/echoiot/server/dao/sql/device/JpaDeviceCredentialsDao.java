package org.echoiot.server.dao.sql.device;

import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.security.DeviceCredentials;
import org.echoiot.server.dao.DaoUtil;
import org.echoiot.server.dao.device.DeviceCredentialsDao;
import org.echoiot.server.dao.model.sql.DeviceCredentialsEntity;
import org.echoiot.server.dao.sql.JpaAbstractDao;
import org.echoiot.server.dao.util.SqlDao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
@Component
@SqlDao
public class JpaDeviceCredentialsDao extends JpaAbstractDao<DeviceCredentialsEntity, DeviceCredentials> implements DeviceCredentialsDao {

    @Resource
    private DeviceCredentialsRepository deviceCredentialsRepository;

    @Override
    protected Class<DeviceCredentialsEntity> getEntityClass() {
        return DeviceCredentialsEntity.class;
    }

    @Override
    protected JpaRepository<DeviceCredentialsEntity, UUID> getRepository() {
        return deviceCredentialsRepository;
    }

    @Transactional
    @Override
    public DeviceCredentials saveAndFlush(TenantId tenantId, DeviceCredentials deviceCredentials) {
        DeviceCredentials result = save(tenantId, deviceCredentials);
        deviceCredentialsRepository.flush();
        return result;
    }

    @Override
    public DeviceCredentials findByDeviceId(TenantId tenantId, UUID deviceId) {
        return DaoUtil.getData(deviceCredentialsRepository.findByDeviceId(deviceId));
    }

    @Override
    public DeviceCredentials findByCredentialsId(TenantId tenantId, String credentialsId) {
        return DaoUtil.getData(deviceCredentialsRepository.findByCredentialsId(credentialsId));
    }
}
