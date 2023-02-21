package org.echoiot.server.dao.relation;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.echoiot.server.common.data.relation.EntityRelation;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.List;

@EqualsAndHashCode
@Getter
@RequiredArgsConstructor
@Builder
public class RelationCacheValue implements Serializable {

    private static final long serialVersionUID = 3911151843961657570L;

    @NotNull
    private final EntityRelation relation;
    @NotNull
    private final List<EntityRelation> relations;

}
