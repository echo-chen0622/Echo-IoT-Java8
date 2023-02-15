package org.thingsboard.server.dao.sql.device;

import org.springframework.data.jpa.repository.JpaRepository;
import org.thingsboard.server.dao.model.sql.DeviceCredentialsEntity;

import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
public interface DeviceCredentialsRepository extends JpaRepository<DeviceCredentialsEntity, UUID> {

    DeviceCredentialsEntity findByDeviceId(UUID deviceId);

    DeviceCredentialsEntity findByCredentialsId(String credentialsId);
}
