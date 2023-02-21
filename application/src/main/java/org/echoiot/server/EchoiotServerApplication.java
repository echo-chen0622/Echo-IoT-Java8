package org.echoiot.server;

import org.echoiot.common.util.ApplicationUtil;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import static org.echoiot.common.util.ApplicationUtil.SPRING_CONFIG_NAME_KEY;

@SpringBootConfiguration
@EnableAsync
@EnableScheduling
@ComponentScan({"org.echoiot.server", "org.echoiot.script"})
public class EchoiotServerApplication {

    private static final String DEFAULT_SPRING_CONFIG_PARAM = SPRING_CONFIG_NAME_KEY + "=" + "echoiot";

    public static void main(@NotNull String[] args) {
        SpringApplication.run(EchoiotServerApplication.class, ApplicationUtil.updateArguments(args, DEFAULT_SPRING_CONFIG_PARAM));
    }

}
