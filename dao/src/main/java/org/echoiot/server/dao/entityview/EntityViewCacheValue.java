package org.echoiot.server.dao.entityview;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.echoiot.server.common.data.EntityView;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.List;

@Getter
@EqualsAndHashCode
@Builder
public class EntityViewCacheValue implements Serializable {

    private static final long serialVersionUID = 1959004642076413174L;

    @NotNull
    private final EntityView entityView;
    @NotNull
    private final List<EntityView> entityViews;

}
