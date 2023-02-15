package org.thingsboard.server.service.install;

public interface DatabaseEntitiesUpgradeService {

    void upgradeDatabase(String fromVersion) throws Exception;

}
