package org.echoiot.server.service.edge.rpc.constructor;

import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.gen.edge.v1.EdgeConfiguration;

@Component
@TbCoreComponent
public class EdgeMsgConstructor {

    @NotNull
    public EdgeConfiguration constructEdgeConfiguration(@NotNull Edge edge) {
        EdgeConfiguration.Builder builder = EdgeConfiguration.newBuilder()
                .setEdgeIdMSB(edge.getId().getId().getMostSignificantBits())
                .setEdgeIdLSB(edge.getId().getId().getLeastSignificantBits())
                .setTenantIdMSB(edge.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(edge.getTenantId().getId().getLeastSignificantBits())
                .setName(edge.getName())
                .setType(edge.getType())
                .setRoutingKey(edge.getRoutingKey())
                .setSecret(edge.getSecret())
                .setAdditionalInfo(JacksonUtil.toString(edge.getAdditionalInfo()))
                .setCloudType("CE");
        if (edge.getCustomerId() != null) {
            builder.setCustomerIdMSB(edge.getCustomerId().getId().getMostSignificantBits())
                    .setCustomerIdLSB(edge.getCustomerId().getId().getLeastSignificantBits());
        }
        return builder.build();
    }
}
