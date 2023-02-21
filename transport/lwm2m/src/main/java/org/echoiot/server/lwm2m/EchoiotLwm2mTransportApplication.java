package org.echoiot.server.lwm2m;

import org.jetbrains.annotations.NotNull;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import static org.echoiot.common.util.ApplicationUtil.SPRING_CONFIG_NAME_KEY;
import static org.echoiot.common.util.ApplicationUtil.updateArguments;

@SpringBootConfiguration
@EnableAsync
@EnableScheduling
@EnableAutoConfiguration
@ComponentScan({"org.echoiot.server.lwm2m", "org.echoiot.server.common", "org.echoiot.server.transport.lwm2m", "org.echoiot.server.queue", "org.echoiot.server.cache"})
public class EchoiotLwm2mTransportApplication {

    private static final String DEFAULT_SPRING_CONFIG_PARAM = SPRING_CONFIG_NAME_KEY + "=" + "tb-lwm2m-transport";

    public static void main(@NotNull String[] args) {
        SpringApplication.run(EchoiotLwm2mTransportApplication.class, updateArguments(args, DEFAULT_SPRING_CONFIG_PARAM));
    }

}
