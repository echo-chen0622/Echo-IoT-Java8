package org.echoiot.server.service.edge.rpc.processor;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.EdgeUtils;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.edge.EdgeEvent;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.id.UserId;
import org.echoiot.server.common.data.security.UserCredentials;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.echoiot.server.gen.edge.v1.DownlinkMsg;
import org.echoiot.server.gen.edge.v1.UpdateMsgType;
import org.echoiot.server.gen.edge.v1.UserCredentialsUpdateMsg;
import org.echoiot.server.gen.transport.TransportProtos;

@Component
@Slf4j
@TbCoreComponent
public class UserEdgeProcessor extends BaseEdgeProcessor {

    @Nullable
    public DownlinkMsg convertUserEventToDownlink(@NotNull EdgeEvent edgeEvent) {
        @NotNull UserId userId = new UserId(edgeEvent.getEntityId());
        @Nullable DownlinkMsg downlinkMsg = null;
        switch (edgeEvent.getAction()) {
            case ADDED:
            case UPDATED:
                User user = userService.findUserById(edgeEvent.getTenantId(), userId);
                if (user != null) {
                    @NotNull UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addUserUpdateMsg(userMsgConstructor.constructUserUpdatedMsg(msgType, user))
                            .build();
                }
                break;
            case DELETED:
                downlinkMsg = DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addUserUpdateMsg(userMsgConstructor.constructUserDeleteMsg(userId))
                        .build();
                break;
            case CREDENTIALS_UPDATED:
                UserCredentials userCredentialsByUserId = userService.findUserCredentialsByUserId(edgeEvent.getTenantId(), userId);
                if (userCredentialsByUserId != null && userCredentialsByUserId.isEnabled()) {
                    UserCredentialsUpdateMsg userCredentialsUpdateMsg =
                            userMsgConstructor.constructUserCredentialsUpdatedMsg(userCredentialsByUserId);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addUserCredentialsUpdateMsg(userCredentialsUpdateMsg)
                            .build();
                }
        }
        return downlinkMsg;
    }

    public ListenableFuture<Void> processUserNotification(TenantId tenantId, @NotNull TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        return processEntityNotificationForAllEdges(tenantId, edgeNotificationMsg);
    }
}
