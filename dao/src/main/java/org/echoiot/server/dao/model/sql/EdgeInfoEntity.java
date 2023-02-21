package org.echoiot.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.echoiot.server.common.data.edge.EdgeInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class EdgeInfoEntity extends AbstractEdgeEntity<EdgeInfo> {

    public static final Map<String,String> edgeInfoColumnMap = new HashMap<>();
    static {
        edgeInfoColumnMap.put("customerTitle", "c.title");
    }

    private String customerTitle;
    private boolean customerIsPublic;

    public EdgeInfoEntity() {
        super();
    }

    public EdgeInfoEntity(@NotNull EdgeEntity edgeEntity,
                          String customerTitle,
                          @Nullable Object customerAdditionalInfo) {
        super(edgeEntity);
        this.customerTitle = customerTitle;
        if (customerAdditionalInfo != null && ((JsonNode)customerAdditionalInfo).has("isPublic")) {
            this.customerIsPublic = ((JsonNode)customerAdditionalInfo).get("isPublic").asBoolean();
        } else {
            this.customerIsPublic = false;
        }
    }

    @NotNull
    @Override
    public EdgeInfo toData() {
        return new EdgeInfo(super.toEdge(), customerTitle, customerIsPublic);
    }

}
