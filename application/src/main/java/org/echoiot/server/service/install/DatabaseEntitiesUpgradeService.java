package org.echoiot.server.service.install;

public interface DatabaseEntitiesUpgradeService {

    void upgradeDatabase(String fromVersion) throws Exception;

}
