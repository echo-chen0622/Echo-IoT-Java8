package org.thingsboard.server.service.edge.rpc.constructor;

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.gen.edge.v1.AdminSettingsUpdateMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;

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
