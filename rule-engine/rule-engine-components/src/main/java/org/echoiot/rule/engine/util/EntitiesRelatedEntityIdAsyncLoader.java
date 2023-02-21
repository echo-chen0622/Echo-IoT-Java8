package org.echoiot.rule.engine.util;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.apache.commons.collections.CollectionUtils;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.rule.engine.data.RelationsQuery;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.relation.EntityRelation;
import org.echoiot.server.common.data.relation.EntityRelationsQuery;
import org.echoiot.server.common.data.relation.EntitySearchDirection;
import org.echoiot.server.common.data.relation.RelationsSearchParameters;
import org.echoiot.server.dao.relation.RelationService;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class EntitiesRelatedEntityIdAsyncLoader {

    @NotNull
    public static ListenableFuture<EntityId> findEntityAsync(@NotNull TbContext ctx, @NotNull EntityId originator,
                                                             @NotNull RelationsQuery relationsQuery) {
        RelationService relationService = ctx.getRelationService();
        @NotNull EntityRelationsQuery query = buildQuery(originator, relationsQuery);
        ListenableFuture<List<EntityRelation>> asyncRelation = relationService.findByQuery(ctx.getTenantId(), query);
        if (relationsQuery.getDirection() == EntitySearchDirection.FROM) {
            return Futures.transformAsync(asyncRelation, r -> CollectionUtils.isNotEmpty(r) ? Futures.immediateFuture(r.get(0).getTo())
                    : Futures.immediateFuture(null), MoreExecutors.directExecutor());
        } else if (relationsQuery.getDirection() == EntitySearchDirection.TO) {
            return Futures.transformAsync(asyncRelation, r -> CollectionUtils.isNotEmpty(r) ? Futures.immediateFuture(r.get(0).getFrom())
                    : Futures.immediateFuture(null), MoreExecutors.directExecutor());
        }
        return Futures.immediateFailedFuture(new IllegalStateException("Unknown direction"));
    }

    @NotNull
    private static EntityRelationsQuery buildQuery(@NotNull EntityId originator, @NotNull RelationsQuery relationsQuery) {
        @NotNull EntityRelationsQuery query = new EntityRelationsQuery();
        @NotNull RelationsSearchParameters parameters = new RelationsSearchParameters(originator,
                                                                                      relationsQuery.getDirection(), relationsQuery.getMaxLevel(), relationsQuery.isFetchLastLevelOnly());
        query.setParameters(parameters);
        query.setFilters(relationsQuery.getFilters());
        return query;
    }
}
