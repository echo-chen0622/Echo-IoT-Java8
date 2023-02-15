package org.thingsboard.server.service.edge.rpc.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.gen.edge.v1.AdminSettingsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Component
@Slf4j
@TbCoreComponent
public class AdminSettingsEdgeProcessor extends BaseEdgeProcessor {

    public DownlinkMsg convertAdminSettingsEventToDownlink(EdgeEvent edgeEvent) {
        AdminSettings adminSettings = JacksonUtil.OBJECT_MAPPER.convertValue(edgeEvent.getBody(), AdminSettings.class);
        AdminSettingsUpdateMsg adminSettingsUpdateMsg = adminSettingsMsgConstructor.constructAdminSettingsUpdateMsg(adminSettings);
        return DownlinkMsg.newBuilder()
                .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                .addAdminSettingsUpdateMsg(adminSettingsUpdateMsg)
                .build();
    }

}
