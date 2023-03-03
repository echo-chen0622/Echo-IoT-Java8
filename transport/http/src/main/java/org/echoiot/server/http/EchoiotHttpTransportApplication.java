package org.echoiot.server.http;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import static org.echoiot.common.util.SpringUtils.SPRING_CONFIG_NAME_KEY;
import static org.echoiot.common.util.SpringUtils.updateArguments;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@ComponentScan({"org.echoiot.server.http", "org.echoiot.server.common", "org.echoiot.server.transport.http", "org.echoiot.server.queue", "org.echoiot.server.cache"})
public class EchoiotHttpTransportApplication {

    private static final String DEFAULT_SPRING_CONFIG_PARAM = SPRING_CONFIG_NAME_KEY + "=" + "tb-http-transport";

    public static void main(String[] args) {
        SpringApplication.run(EchoiotHttpTransportApplication.class, updateArguments(args, DEFAULT_SPRING_CONFIG_PARAM));
    }
}
