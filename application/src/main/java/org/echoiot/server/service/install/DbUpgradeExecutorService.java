package org.echoiot.server.service.install;

import org.echoiot.server.service.executors.DbCallbackExecutorService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("install")
public class DbUpgradeExecutorService extends DbCallbackExecutorService {

}
