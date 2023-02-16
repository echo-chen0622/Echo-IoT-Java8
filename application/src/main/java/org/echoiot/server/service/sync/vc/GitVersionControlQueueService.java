package org.echoiot.server.service.sync.vc;

import com.google.common.util.concurrent.ListenableFuture;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.ExportableEntity;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.sync.ie.EntityExportData;
import org.echoiot.server.common.data.sync.vc.request.create.VersionCreateRequest;
import org.echoiot.server.service.sync.vc.data.CommitGitRequest;
import org.echoiot.server.common.data.sync.vc.BranchInfo;
import org.echoiot.server.common.data.sync.vc.EntityVersion;
import org.echoiot.server.common.data.sync.vc.EntityVersionsDiff;
import org.echoiot.server.common.data.sync.vc.RepositorySettings;
import org.echoiot.server.common.data.sync.vc.VersionCreationResult;
import org.echoiot.server.common.data.sync.vc.VersionedEntityInfo;
import org.thingsboard.server.gen.transport.TransportProtos.VersionControlResponseMsg;

import java.util.List;

public interface GitVersionControlQueueService {

    ListenableFuture<CommitGitRequest> prepareCommit(User user, VersionCreateRequest request);

    ListenableFuture<Void> addToCommit(CommitGitRequest commit, EntityExportData<ExportableEntity<EntityId>> entityData);

    ListenableFuture<Void> deleteAll(CommitGitRequest pendingCommit, EntityType entityType);

    ListenableFuture<VersionCreationResult> push(CommitGitRequest commit);

    ListenableFuture<PageData<EntityVersion>> listVersions(TenantId tenantId, String branch, PageLink pageLink);

    ListenableFuture<PageData<EntityVersion>> listVersions(TenantId tenantId, String branch, EntityType entityType, PageLink pageLink);

    ListenableFuture<PageData<EntityVersion>> listVersions(TenantId tenantId, String branch, EntityId entityId, PageLink pageLink);

    ListenableFuture<List<VersionedEntityInfo>> listEntitiesAtVersion(TenantId tenantId, String versionId, EntityType entityType);

    ListenableFuture<List<VersionedEntityInfo>> listEntitiesAtVersion(TenantId tenantId, String versionId);

    ListenableFuture<List<BranchInfo>> listBranches(TenantId tenantId);

    ListenableFuture<EntityExportData> getEntity(TenantId tenantId, String versionId, EntityId entityId);

    ListenableFuture<List<EntityExportData>> getEntities(TenantId tenantId, String versionId, EntityType entityType, int offset, int limit);

    ListenableFuture<List<EntityVersionsDiff>> getVersionsDiff(TenantId tenantId, EntityType entityType, EntityId externalId, String versionId1, String versionId2);

    ListenableFuture<Void> initRepository(TenantId tenantId, RepositorySettings settings);

    ListenableFuture<Void> testRepository(TenantId tenantId, RepositorySettings settings);

    ListenableFuture<Void> clearRepository(TenantId tenantId);

    void processResponse(VersionControlResponseMsg vcResponseMsg);
}
