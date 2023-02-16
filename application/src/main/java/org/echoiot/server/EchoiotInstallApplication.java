package org.echoiot.server;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.install.EchoiotInstallService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import java.util.Arrays;

@Slf4j
@SpringBootConfiguration
@ComponentScan({"org.echoiot.server.install",
        "org.echoiot.server.service.component",
        "org.echoiot.server.service.install",
        "org.echoiot.server.service.security.auth.jwt.settings",
        "org.echoiot.server.dao",
        "org.echoiot.server.common.stats",
        "org.echoiot.server.common.transport.config.ssl",
        "org.echoiot.server.cache",
        "org.echoiot.server.springfox"
})
public class EchoiotInstallApplication {

    private static final String SPRING_CONFIG_NAME_KEY = "--spring.config.name";
    private static final String DEFAULT_SPRING_CONFIG_PARAM = SPRING_CONFIG_NAME_KEY + "=" + "echoiot";

    public static void main(String[] args) {
        try {
            SpringApplication application = new SpringApplication(EchoiotInstallApplication.class);
            application.setAdditionalProfiles("install");
            ConfigurableApplicationContext context = application.run(updateArguments(args));
            context.getBean(EchoiotInstallService.class).performInstall();
        } catch (Exception e) {
            log.error(e.getMessage());
            System.exit(1);
        }
    }

    private static String[] updateArguments(String[] args) {
        if (Arrays.stream(args).noneMatch(arg -> arg.startsWith(SPRING_CONFIG_NAME_KEY))) {
            String[] modifiedArgs = new String[args.length + 1];
            System.arraycopy(args, 0, modifiedArgs, 0, args.length);
            modifiedArgs[args.length] = DEFAULT_SPRING_CONFIG_PARAM;
            return modifiedArgs;
        }
        return args;
    }
}
