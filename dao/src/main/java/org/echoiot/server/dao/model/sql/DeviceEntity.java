package org.echoiot.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.echoiot.server.common.data.Device;
import org.echoiot.server.dao.model.ModelConstants;
import org.echoiot.server.dao.util.mapping.JsonBinaryType;
import org.echoiot.server.dao.util.mapping.JsonStringType;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Entity;
import javax.persistence.Table;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDefs({
        @TypeDef(name = "json", typeClass = JsonStringType.class),
        @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
})
@Table(name = ModelConstants.DEVICE_COLUMN_FAMILY_NAME)
public final class DeviceEntity extends AbstractDeviceEntity<Device> {

    public DeviceEntity() {
        super();
    }

    public DeviceEntity(@NotNull Device device) {
        super(device);
    }

    @Override
    public Device toData() {
        return super.toDevice();
    }
}
