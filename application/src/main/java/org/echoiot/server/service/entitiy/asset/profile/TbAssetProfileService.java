package org.echoiot.server.service.entitiy.asset.profile;

import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.asset.AssetProfile;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.service.entitiy.SimpleTbEntityService;

public interface TbAssetProfileService extends SimpleTbEntityService<AssetProfile> {

    AssetProfile setDefaultAssetProfile(AssetProfile assetProfile, AssetProfile previousDefaultAssetProfile, User user) throws EchoiotException;
}
