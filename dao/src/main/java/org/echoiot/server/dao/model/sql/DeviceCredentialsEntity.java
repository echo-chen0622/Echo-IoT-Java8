package org.echoiot.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.echoiot.server.common.data.id.DeviceCredentialsId;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.security.DeviceCredentials;
import org.echoiot.server.common.data.security.DeviceCredentialsType;
import org.echoiot.server.dao.model.BaseEntity;
import org.echoiot.server.dao.model.BaseSqlEntity;
import org.echoiot.server.dao.model.ModelConstants;
import org.jetbrains.annotations.NotNull;

import javax.persistence.*;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.DEVICE_CREDENTIALS_COLUMN_FAMILY_NAME)
public final class DeviceCredentialsEntity extends BaseSqlEntity<DeviceCredentials> implements BaseEntity<DeviceCredentials> {

    @Column(name = ModelConstants.DEVICE_CREDENTIALS_DEVICE_ID_PROPERTY)
    private UUID deviceId;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.DEVICE_CREDENTIALS_CREDENTIALS_TYPE_PROPERTY)
    private DeviceCredentialsType credentialsType;

    @Column(name = ModelConstants.DEVICE_CREDENTIALS_CREDENTIALS_ID_PROPERTY)
    private String credentialsId;

    @Column(name = ModelConstants.DEVICE_CREDENTIALS_CREDENTIALS_VALUE_PROPERTY)
    private String credentialsValue;

    public DeviceCredentialsEntity() {
        super();
    }

    public DeviceCredentialsEntity(@NotNull DeviceCredentials deviceCredentials) {
        if (deviceCredentials.getId() != null) {
            this.setUuid(deviceCredentials.getId().getId());
        }
        this.setCreatedTime(deviceCredentials.getCreatedTime());
        if (deviceCredentials.getDeviceId() != null) {
            this.deviceId = deviceCredentials.getDeviceId().getId();
        }
        this.credentialsType = deviceCredentials.getCredentialsType();
        this.credentialsId = deviceCredentials.getCredentialsId();
        this.credentialsValue = deviceCredentials.getCredentialsValue();
    }

    @NotNull
    @Override
    public DeviceCredentials toData() {
        @NotNull DeviceCredentials deviceCredentials = new DeviceCredentials(new DeviceCredentialsId(this.getUuid()));
        deviceCredentials.setCreatedTime(createdTime);
        if (deviceId != null) {
            deviceCredentials.setDeviceId(new DeviceId(deviceId));
        }
        deviceCredentials.setCredentialsType(credentialsType);
        deviceCredentials.setCredentialsId(credentialsId);
        deviceCredentials.setCredentialsValue(credentialsValue);
        return deviceCredentials;
    }

}
