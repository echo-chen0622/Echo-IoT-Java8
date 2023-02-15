package org.thingsboard.server.service.entitiy.asset;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.TenantId;

public interface TbAssetService {

    Asset save(Asset asset, User user) throws Exception;

    ListenableFuture<Void> delete(Asset asset, User user);

    Asset assignAssetToCustomer(TenantId tenantId, AssetId assetId, Customer customer, User user) throws ThingsboardException;

    Asset unassignAssetToCustomer(TenantId tenantId, AssetId assetId, Customer customer, User user) throws ThingsboardException;

    Asset assignAssetToPublicCustomer(TenantId tenantId, AssetId assetId, User user) throws ThingsboardException;

    Asset assignAssetToEdge(TenantId tenantId, AssetId assetId, Edge edge, User user) throws ThingsboardException;

    Asset unassignAssetFromEdge(TenantId tenantId, Asset asset, Edge edge, User user) throws ThingsboardException;

}
