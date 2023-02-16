package org.echoiot.server.service.sync.vc;

import com.google.common.util.concurrent.ListenableFuture;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.exception.ThingsboardException;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.sync.vc.request.create.VersionCreateRequest;
import org.echoiot.server.common.data.sync.vc.request.load.VersionLoadRequest;
import org.echoiot.server.common.data.sync.vc.BranchInfo;
import org.echoiot.server.common.data.sync.vc.EntityDataDiff;
import org.echoiot.server.common.data.sync.vc.EntityDataInfo;
import org.echoiot.server.common.data.sync.vc.EntityVersion;
import org.echoiot.server.common.data.sync.vc.RepositorySettings;
import org.echoiot.server.common.data.sync.vc.VersionCreationResult;
import org.echoiot.server.common.data.sync.vc.VersionLoadResult;
import org.echoiot.server.common.data.sync.vc.VersionedEntityInfo;

import java.util.List;
import java.util.UUID;

public interface EntitiesVersionControlService {

    ListenableFuture<UUID> saveEntitiesVersion(User user, VersionCreateRequest request) throws Exception;

    VersionCreationResult getVersionCreateStatus(User user, UUID requestId) throws ThingsboardException;

    ListenableFuture<PageData<EntityVersion>> listEntityVersions(TenantId tenantId, String branch, EntityId externalId, PageLink pageLink) throws Exception;

    ListenableFuture<PageData<EntityVersion>> listEntityTypeVersions(TenantId tenantId, String branch, EntityType entityType, PageLink pageLink) throws Exception;

    ListenableFuture<PageData<EntityVersion>> listVersions(TenantId tenantId, String branch, PageLink pageLink) throws Exception;

    ListenableFuture<List<VersionedEntityInfo>> listEntitiesAtVersion(TenantId tenantId, String versionId, EntityType entityType) throws Exception;

    ListenableFuture<List<VersionedEntityInfo>> listAllEntitiesAtVersion(TenantId tenantId, String versionId) throws Exception;

    UUID loadEntitiesVersion(User user, VersionLoadRequest request) throws Exception;

    VersionLoadResult getVersionLoadStatus(User user, UUID requestId) throws ThingsboardException;

    ListenableFuture<EntityDataDiff> compareEntityDataToVersion(User user, EntityId entityId, String versionId) throws Exception;

    ListenableFuture<List<BranchInfo>> listBranches(TenantId tenantId) throws Exception;

    RepositorySettings getVersionControlSettings(TenantId tenantId);

    ListenableFuture<RepositorySettings> saveVersionControlSettings(TenantId tenantId, RepositorySettings versionControlSettings);

    ListenableFuture<Void> deleteVersionControlSettings(TenantId tenantId) throws Exception;

    ListenableFuture<Void> checkVersionControlAccess(TenantId tenantId, RepositorySettings settings) throws Exception;

    ListenableFuture<UUID> autoCommit(User user, EntityId entityId) throws Exception;

    ListenableFuture<UUID> autoCommit(User user, EntityType entityType, List<UUID> entityIds) throws Exception;

    ListenableFuture<EntityDataInfo> getEntityDataInfo(User user, EntityId entityId, String versionId);

}
