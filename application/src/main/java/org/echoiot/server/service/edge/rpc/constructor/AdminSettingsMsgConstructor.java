package org.echoiot.server.service.edge.rpc.constructor;

import org.echoiot.server.common.data.AdminSettings;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.gen.edge.v1.AdminSettingsUpdateMsg;

@Component
@TbCoreComponent
public class AdminSettingsMsgConstructor {

    public AdminSettingsUpdateMsg constructAdminSettingsUpdateMsg(AdminSettings adminSettings) {
        AdminSettingsUpdateMsg.Builder builder = AdminSettingsUpdateMsg.newBuilder()
                .setKey(adminSettings.getKey())
                .setJsonValue(JacksonUtil.toString(adminSettings.getJsonValue()));
        if (adminSettings.getId() != null) {
            builder.setIsSystem(true);
        }
        return builder.build();
    }

}
