package org.echoiot.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.echoiot.server.common.data.event.ErrorEvent;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.dao.model.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import static org.echoiot.server.dao.model.ModelConstants.ERROR_EVENT_TABLE_NAME;
import static org.echoiot.server.dao.model.ModelConstants.EVENT_ERROR_COLUMN_NAME;
import static org.echoiot.server.dao.model.ModelConstants.EVENT_METHOD_COLUMN_NAME;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ERROR_EVENT_TABLE_NAME)
@NoArgsConstructor
public class ErrorEventEntity extends EventEntity<ErrorEvent> implements BaseEntity<ErrorEvent> {

    @Column(name = EVENT_METHOD_COLUMN_NAME)
    private String method;
    @Column(name = EVENT_ERROR_COLUMN_NAME)
    private String error;

    public ErrorEventEntity(ErrorEvent event) {
        super(event);
        this.method = event.getMethod();
        this.error = event.getError();
    }

    @Override
    public ErrorEvent toData() {
        return ErrorEvent.builder()
                .tenantId(TenantId.fromUUID(tenantId))
                .entityId(entityId)
                .serviceId(serviceId)
                .id(id)
                .ts(ts)
                .method(method)
                .error(error)
                .build();
    }

}
