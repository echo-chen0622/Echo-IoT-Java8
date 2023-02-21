package org.echoiot.server.service.edge.rpc.processor;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.common.data.AdminSettings;
import org.echoiot.server.common.data.EdgeUtils;
import org.echoiot.server.common.data.edge.EdgeEvent;
import org.echoiot.server.gen.edge.v1.AdminSettingsUpdateMsg;
import org.echoiot.server.gen.edge.v1.DownlinkMsg;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@TbCoreComponent
public class AdminSettingsEdgeProcessor extends BaseEdgeProcessor {

    @NotNull
    public DownlinkMsg convertAdminSettingsEventToDownlink(@NotNull EdgeEvent edgeEvent) {
        AdminSettings adminSettings = JacksonUtil.OBJECT_MAPPER.convertValue(edgeEvent.getBody(), AdminSettings.class);
        AdminSettingsUpdateMsg adminSettingsUpdateMsg = adminSettingsMsgConstructor.constructAdminSettingsUpdateMsg(adminSettings);
        return DownlinkMsg.newBuilder()
                .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                .addAdminSettingsUpdateMsg(adminSettingsUpdateMsg)
                .build();
    }

}
