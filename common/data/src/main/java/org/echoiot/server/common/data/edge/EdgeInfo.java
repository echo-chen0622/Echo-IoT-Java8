package org.echoiot.server.common.data.edge;

import lombok.Data;
import org.echoiot.server.common.data.id.EdgeId;

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

    public EdgeInfo(Edge edge, String customerTitle, boolean customerIsPublic) {
        super(edge);
        this.customerTitle = customerTitle;
        this.customerIsPublic = customerIsPublic;
    }
}
