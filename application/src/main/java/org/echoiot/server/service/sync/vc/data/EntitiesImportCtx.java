package org.echoiot.server.service.sync.vc.data;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.relation.EntityRelation;
import org.echoiot.server.common.data.sync.ThrowingRunnable;
import org.echoiot.server.common.data.sync.ie.EntityImportResult;
import org.echoiot.server.common.data.sync.ie.EntityImportSettings;
import org.echoiot.server.common.data.sync.vc.EntityTypeLoadResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@Slf4j
@Data
public class EntitiesImportCtx {

    private final UUID requestId;
    private final User user;
    private final String versionId;

    private final Map<EntityType, EntityTypeLoadResult> results = new HashMap<>();
    private final Map<EntityType, Set<EntityId>> importedEntities = new HashMap<>();
    private final Map<EntityId, ReimportTask> toReimport = new HashMap<>();
    private final Map<EntityId, ThrowingRunnable> referenceCallbacks = new HashMap<>();
    private final List<ThrowingRunnable> eventCallbacks = new ArrayList<>();
    private final Map<EntityId, EntityId> externalToInternalIdMap = new HashMap<>();
    private final Set<EntityId> notFoundIds = new HashSet<>();

    private final Set<EntityRelation> relations = new LinkedHashSet<>();

    private boolean finalImportAttempt = false;
    private EntityImportSettings settings;
    private EntityImportResult<?> currentImportResult;

    public EntitiesImportCtx(UUID requestId, User user, String versionId) {
        this(requestId, user, versionId, null);
    }

    public EntitiesImportCtx(UUID requestId, User user, String versionId, EntityImportSettings settings) {
        this.requestId = requestId;
        this.user = user;
        this.versionId = versionId;
        this.settings = settings;
    }

    public TenantId getTenantId() {
        return user.getTenantId();
    }

    public boolean isFindExistingByName() {
        return getSettings().isFindExistingByName();
    }

    public boolean isUpdateRelations() {
        return getSettings().isUpdateRelations();
    }

    public boolean isSaveAttributes() {
        return getSettings().isSaveAttributes();
    }

    public boolean isSaveCredentials() {
        return getSettings().isSaveCredentials();
    }

    @Nullable
    public EntityId getInternalId(@NotNull EntityId externalId) {
        var result = externalToInternalIdMap.get(externalId);
        log.debug("[{}][{}] Local cache {} for id", externalId.getEntityType(), externalId.getId(), result != null ? "hit" : "miss");
        return result;
    }

    public void putInternalId(@NotNull EntityId externalId, EntityId internalId) {
        log.debug("[{}][{}] Local cache put: {}", externalId.getEntityType(), externalId.getId(), internalId);
        externalToInternalIdMap.put(externalId, internalId);
    }

    public void registerResult(EntityType entityType, boolean created) {
        @NotNull EntityTypeLoadResult result = results.computeIfAbsent(entityType, EntityTypeLoadResult::new);
        if (created) {
            result.setCreated(result.getCreated() + 1);
        } else {
            result.setUpdated(result.getUpdated() + 1);
        }
    }

    public void registerDeleted(EntityType entityType) {
        @NotNull EntityTypeLoadResult result = results.computeIfAbsent(entityType, EntityTypeLoadResult::new);
        result.setDeleted(result.getDeleted() + 1);
    }

    public void addRelations(@NotNull Collection<EntityRelation> values) {
        relations.addAll(values);
    }

    public void addReferenceCallback(EntityId externalId, @Nullable ThrowingRunnable tr) {
        if (tr != null) {
            referenceCallbacks.put(externalId, tr);
        }
    }

    public void addEventCallback(@Nullable ThrowingRunnable tr) {
        if (tr != null) {
            eventCallbacks.add(tr);
        }
    }

    public void registerNotFound(EntityId externalId) {
        notFoundIds.add(externalId);
    }

    public boolean isNotFound(EntityId externalId) {
        return notFoundIds.contains(externalId);
    }


}
