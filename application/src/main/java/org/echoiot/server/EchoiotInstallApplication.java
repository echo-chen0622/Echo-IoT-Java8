package org.echoiot.server;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.install.EchoiotInstallService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import static org.echoiot.common.util.SpringUtils.SPRING_CONFIG_NAME_KEY;
import static org.echoiot.common.util.SpringUtils.updateArguments;

/**
 * 初始化类，用来进行初始化操作，主要是创建数据库，以及初始化数据 demo。
 * 如果需要加载 demo，需要在程序实参里添加：--install.load_demo=true
 *
 * @author Echo
 */
@Slf4j
@SpringBootApplication
@ComponentScan({"org.echoiot.server.install",
                "org.echoiot.server.service.component",
                "org.echoiot.server.service.install",
                "org.echoiot.server.service.security.auth.jwt.settings",
                "org.echoiot.server.dao",
                "org.echoiot.server.common.stats",
                "org.echoiot.server.common.transport.config.ssl",
                "org.echoiot.server.cache",
                "org.echoiot.server.springfox"})
public class EchoiotInstallApplication {

    /**
     * 默认的配置文件名称
     */
    private static final String DEFAULT_SPRING_CONFIG_PARAM = SPRING_CONFIG_NAME_KEY + "=" + "echoiot";

    public static void main(String[] args) throws Exception {
        SpringApplication application = new SpringApplication(EchoiotInstallApplication.class);
        // 设置启动参数，设置配置文件名称
        application.setAdditionalProfiles("install");
        // 启动应用
        ConfigurableApplicationContext context = application.run(updateArguments(args, DEFAULT_SPRING_CONFIG_PARAM));
        // 获取初始化类，并执行安装
        context.getBean(EchoiotInstallService.class).performInstall();
        // 退出应用
        SpringApplication.exit(context);
    }

}
