package org.thingsboard.server.queue.environment;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.Environment;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * Created by igor on 11/24/16.
 */

@Service("environmentLogService")
@ConditionalOnProperty(prefix = "zk", value = "enabled", havingValue = "false", matchIfMissing = true)
@Slf4j
public class EnvironmentLogService {

    @PostConstruct
    public void init() {
        Environment.logEnv("ThingsBoard server environment: ", log);
    }

}
