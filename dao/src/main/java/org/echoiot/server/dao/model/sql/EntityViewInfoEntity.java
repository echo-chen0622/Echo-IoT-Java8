package org.echoiot.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.echoiot.server.common.data.EntityViewInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class EntityViewInfoEntity extends AbstractEntityViewEntity<EntityViewInfo> {

    public static final Map<String,String> entityViewInfoColumnMap = new HashMap<>();
    static {
        entityViewInfoColumnMap.put("customerTitle", "c.title");
    }

    private String customerTitle;
    private boolean customerIsPublic;

    public EntityViewInfoEntity() {
        super();
    }

    public EntityViewInfoEntity(@NotNull EntityViewEntity entityViewEntity,
                                String customerTitle,
                                @Nullable Object customerAdditionalInfo) {
        super(entityViewEntity);
        this.customerTitle = customerTitle;
        if (customerAdditionalInfo != null && ((JsonNode)customerAdditionalInfo).has("isPublic")) {
            this.customerIsPublic = ((JsonNode)customerAdditionalInfo).get("isPublic").asBoolean();
        } else {
            this.customerIsPublic = false;
        }
    }

    @NotNull
    @Override
    public EntityViewInfo toData() {
        return new EntityViewInfo(super.toEntityView(), customerTitle, customerIsPublic);
    }
}
