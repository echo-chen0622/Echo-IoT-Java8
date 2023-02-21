package org.echoiot.server.service.telemetry.cmd.v2;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.query.AlarmData;
import org.echoiot.server.service.telemetry.sub.SubscriptionErrorCode;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@ToString
public class AlarmDataUpdate extends DataUpdate<AlarmData> {

    @Getter
    private long allowedEntities;
    @Getter
    private long totalEntities;

    public AlarmDataUpdate(int cmdId, PageData<AlarmData> data, List<AlarmData> update, long allowedEntities, long totalEntities) {
        super(cmdId, data, update, SubscriptionErrorCode.NO_ERROR.getCode(), null);
        this.allowedEntities = allowedEntities;
        this.totalEntities = totalEntities;
    }

    public AlarmDataUpdate(int cmdId, int errorCode, String errorMsg) {
        super(cmdId, null, null, errorCode, errorMsg);
    }

    @NotNull
    @Override
    public CmdUpdateType getCmdUpdateType() {
        return CmdUpdateType.ALARM_DATA;
    }

    @JsonCreator
    public AlarmDataUpdate(@JsonProperty("cmdId") int cmdId,
                           @JsonProperty("data") PageData<AlarmData> data,
                           @JsonProperty("update") List<AlarmData> update,
                           @JsonProperty("errorCode") int errorCode,
                           @JsonProperty("errorMsg") String errorMsg,
                           @JsonProperty("allowedEntities") long allowedEntities,
                           @JsonProperty("totalEntities") long totalEntities) {
        super(cmdId, data, update, errorCode, errorMsg);
        this.allowedEntities = allowedEntities;
        this.totalEntities = totalEntities;
    }
}
