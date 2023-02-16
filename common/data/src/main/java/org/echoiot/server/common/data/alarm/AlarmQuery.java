package org.echoiot.server.common.data.alarm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.page.TimePageLink;

/**
 * Created by Echo on 11.05.17.
 */
@Data
@Builder
@AllArgsConstructor
public class AlarmQuery {

    private EntityId affectedEntityId;
    private TimePageLink pageLink;
    private AlarmSearchStatus searchStatus;
    private AlarmStatus status;
    private Boolean fetchOriginator;

}
