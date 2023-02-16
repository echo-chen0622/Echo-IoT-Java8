package org.echoiot.edge.rpc;

import org.echoiot.server.gen.edge.v1.DownlinkMsg;
import org.echoiot.server.gen.edge.v1.DownlinkResponseMsg;
import org.echoiot.server.gen.edge.v1.EdgeConfiguration;
import org.echoiot.server.gen.edge.v1.UplinkMsg;
import org.echoiot.server.gen.edge.v1.UplinkResponseMsg;

import java.util.function.Consumer;

public interface EdgeRpcClient {

    void connect(String integrationKey,
                 String integrationSecret,
                 Consumer<UplinkResponseMsg> onUplinkResponse,
                 Consumer<EdgeConfiguration> onEdgeUpdate,
                 Consumer<DownlinkMsg> onDownlink,
                 Consumer<Exception> onError);

    void disconnect(boolean onError) throws InterruptedException;

    void sendSyncRequestMsg(boolean syncRequired);

    void sendSyncRequestMsg(boolean syncRequired, boolean fullSync);

    void sendUplinkMsg(UplinkMsg uplinkMsg);

    void sendDownlinkResponseMsg(DownlinkResponseMsg downlinkResponseMsg);
}
