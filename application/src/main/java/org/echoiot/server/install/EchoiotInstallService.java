package org.echoiot.server.install;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.service.component.ComponentDiscoveryService;
import org.echoiot.server.service.install.*;
import org.echoiot.server.service.install.update.CacheCleanupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 初始化类，用来进行初始化操作，主要是创建数据库，以及初始化数据 demo。
 *
 * @author Echo
 */
@Service
@Profile("install")
@Slf4j
public class EchoiotInstallService {

    /**
     * 是否是升级
     */
    @Value("${install.upgrade:false}")
    private Boolean isUpgrade;

    /**
     * 从哪个版本开始的升级 --- 默认 1.0.0 版本
     */
    @Value("${install.upgrade.from_version:1.0.0}")
    private String upgradeFromVersion;

    /**
     * 是否加载 demo 数据
     */
    @Value("${install.load_demo:false}")
    private Boolean loadDemo;

    @Resource
    private EntityDatabaseSchemaService entityDatabaseSchemaService;

    @Autowired(required = false)
    private NoSqlKeyspaceService noSqlKeyspaceService;

    @Resource
    private TsDatabaseSchemaService tsDatabaseSchemaService;

    @Autowired(required = false)
    private TsLatestDatabaseSchemaService tsLatestDatabaseSchemaService;

    /**
     * 数据升级服务。数据库支持不同类型的服务，所以，这里spring boot自动装配找不到应该用哪个服务。不是问题
     */
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired(required = false)
    private DatabaseTsUpgradeService databaseTsUpgradeService;

    @Resource
    private ComponentDiscoveryService componentDiscoveryService;

    @Resource
    private SystemDataLoaderService systemDataLoaderService;

    @Resource
    private CacheCleanupService cacheCleanupService;

    /**
     * 执行安装或者升级程序
     */
    @SuppressWarnings({"AlibabaSwitchStatement", "java:S128","java:S1192"})
    public void performInstall() throws Exception {
        if (Boolean.TRUE.equals(isUpgrade)) {
            log.info("开始 Echoiot 升级 , 现版本: {} ...", upgradeFromVersion);

            // 清空历史缓存。因为缓存只需要清理一次，所以放到这里单独处理，不跟数据库升级放在一起
            cacheCleanupService.clearCache(upgradeFromVersion);

            switch (upgradeFromVersion) {
                case "1.0.0":
                    log.info("Upgrading Echoiot from version 1.0.0 to 1.0.1 ...");
                    /* 升级数据库 同时需要判断，是否需要升级实体、业务数据、时间序列数据等数据
                    if (databaseTsUpgradeService != null) {
                        databaseTsUpgradeService.upgradeDatabase("1.0.0");
                    }
                    */
                case "1.0.1":
                    //逐级升级，case不加break，但是切记，调用的执行方法，要每个 case加break
                    log.info("Upgrading Echoiot from version 1.0.1 to 1.0.2 ...");
                    break;
                default:
                    throw new RuntimeException("Unable to upgrade Echoiot, unsupported fromVersion: " + upgradeFromVersion);

            }
            log.info("升级 Echoiot 成功！");

        } else {

            log.info("开始安装 Echoiot ...");

            log.info("正在安装 sql 数据库结构（建表等语句）...");

            // 这里，用这种抽象方法不见得很好，这是一种插件化类的思想，但是在这里，这种思想不太好，因为，这里的插件化，只是为了创建数据库，而不是为了扩展功能
            // 创建实体数据库
            entityDatabaseSchemaService.createDatabaseSchema();

            log.info("正在安装 sql 时序数据库...");

            if (noSqlKeyspaceService != null) {
                // 创建 NoSql 数据库
                noSqlKeyspaceService.createDatabaseSchema();
            }

            tsDatabaseSchemaService.createDatabaseSchema();

            if (tsLatestDatabaseSchemaService != null) {
                tsLatestDatabaseSchemaService.createDatabaseSchema();
            }

            log.info("加载系统业务数据中...");

            // 发现组件
            componentDiscoveryService.discoverComponents();

            systemDataLoaderService.createSysAdmin();
            systemDataLoaderService.createDefaultTenantProfiles();
            systemDataLoaderService.createAdminSettings();
            systemDataLoaderService.createRandomJwtSettings();
            systemDataLoaderService.loadSystemWidgets();
            systemDataLoaderService.createOAuth2Templates();
            systemDataLoaderService.createQueues();

            if (Boolean.TRUE.equals(loadDemo)) {
                log.info("加载 demo 数据中...");
                systemDataLoaderService.loadDemoData();
            }
            log.info("安装成功！！！");
        }


    }

}
