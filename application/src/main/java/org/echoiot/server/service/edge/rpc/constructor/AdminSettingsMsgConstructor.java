package org.echoiot.server.service.edge.rpc.constructor;

import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.common.data.AdminSettings;
import org.echoiot.server.gen.edge.v1.AdminSettingsUpdateMsg;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
@TbCoreComponent
public class AdminSettingsMsgConstructor {

    @NotNull
    public AdminSettingsUpdateMsg constructAdminSettingsUpdateMsg(@NotNull AdminSettings adminSettings) {
        AdminSettingsUpdateMsg.Builder builder = AdminSettingsUpdateMsg.newBuilder()
                .setKey(adminSettings.getKey())
                .setJsonValue(JacksonUtil.toString(adminSettings.getJsonValue()));
        if (adminSettings.getId() != null) {
            builder.setIsSystem(true);
        }
        return builder.build();
    }

}
