package org.echoiot.server;

import org.echoiot.common.util.SpringUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import static org.echoiot.common.util.SpringUtils.SPRING_CONFIG_NAME_KEY;

@SpringBootConfiguration
@EnableAsync
@EnableScheduling
@ComponentScan({"org.echoiot.server", "org.echoiot.script"})
public class EchoiotServerApplication {

    private static final String DEFAULT_SPRING_CONFIG_PARAM = SPRING_CONFIG_NAME_KEY + "=" + "echoiot";

    public static void main(String[] args) {
        SpringApplication.run(EchoiotServerApplication.class, SpringUtils.updateArguments(args, DEFAULT_SPRING_CONFIG_PARAM));
    }

}
