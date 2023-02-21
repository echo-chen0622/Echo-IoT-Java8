package org.echoiot.server.common.data.edge;

import lombok.Data;
import org.echoiot.server.common.data.id.EdgeId;
import org.jetbrains.annotations.NotNull;

@Data
public class EdgeInfo extends Edge {

    private String customerTitle;
    private boolean customerIsPublic;

    public EdgeInfo() {
        super();
    }

    public EdgeInfo(EdgeId edgeId) {
        super(edgeId);
    }

    public EdgeInfo(@NotNull Edge edge, String customerTitle, boolean customerIsPublic) {
        super(edge);
        this.customerTitle = customerTitle;
        this.customerIsPublic = customerIsPublic;
    }
}
