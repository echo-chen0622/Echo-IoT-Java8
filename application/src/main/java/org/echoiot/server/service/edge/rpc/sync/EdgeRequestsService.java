package org.echoiot.server.service.edge.rpc.sync;

import com.google.common.util.concurrent.ListenableFuture;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.gen.edge.v1.*;

public interface EdgeRequestsService {

    ListenableFuture<Void> processRuleChainMetadataRequestMsg(TenantId tenantId, Edge edge, RuleChainMetadataRequestMsg ruleChainMetadataRequestMsg);

    ListenableFuture<Void> processAttributesRequestMsg(TenantId tenantId, Edge edge, AttributesRequestMsg attributesRequestMsg);

    ListenableFuture<Void> processRelationRequestMsg(TenantId tenantId, Edge edge, RelationRequestMsg relationRequestMsg);

    ListenableFuture<Void> processDeviceCredentialsRequestMsg(TenantId tenantId, Edge edge, DeviceCredentialsRequestMsg deviceCredentialsRequestMsg);

    ListenableFuture<Void> processUserCredentialsRequestMsg(TenantId tenantId, Edge edge, UserCredentialsRequestMsg userCredentialsRequestMsg);

    ListenableFuture<Void> processWidgetBundleTypesRequestMsg(TenantId tenantId, Edge edge, WidgetBundleTypesRequestMsg widgetBundleTypesRequestMsg);

    ListenableFuture<Void> processEntityViewsRequestMsg(TenantId tenantId, Edge edge, EntityViewsRequestMsg entityViewsRequestMsg);
}
