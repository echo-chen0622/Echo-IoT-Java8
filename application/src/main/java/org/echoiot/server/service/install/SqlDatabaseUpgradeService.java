package org.echoiot.server.service.install;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.util.TbPair;
import org.echoiot.server.dao.asset.AssetDao;
import org.echoiot.server.dao.asset.AssetProfileService;
import org.echoiot.server.dao.dashboard.DashboardService;
import org.echoiot.server.dao.device.DeviceProfileService;
import org.echoiot.server.dao.device.DeviceService;
import org.echoiot.server.dao.queue.QueueService;
import org.echoiot.server.dao.sql.tenant.TenantRepository;
import org.echoiot.server.dao.tenant.TenantService;
import org.echoiot.server.dao.usagerecord.ApiUsageStateService;
import org.echoiot.server.queue.settings.TbRuleEngineQueueConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.echoiot.server.common.data.queue.ProcessingStrategy;
import org.echoiot.server.common.data.queue.ProcessingStrategyType;
import org.echoiot.server.common.data.queue.Queue;
import org.echoiot.server.common.data.queue.SubmitStrategy;
import org.echoiot.server.common.data.queue.SubmitStrategyType;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Profile("install")
@Slf4j
public class SqlDatabaseUpgradeService implements DatabaseEntitiesUpgradeService {

    private static final String SCHEMA_UPDATE_SQL = "schema_update.sql";

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUserName;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private InstallScripts installScripts;

    @Autowired
    private SystemDataLoaderService systemDataLoaderService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private AssetDao assetDao;

    @Autowired
    private DeviceProfileService deviceProfileService;

    @Autowired
    private AssetProfileService assetProfileService;

    @Autowired
    private ApiUsageStateService apiUsageStateService;

    @Lazy
    @Autowired
    private QueueService queueService;

    @Autowired
    private TbRuleEngineQueueConfigService queueConfig;

    @Autowired
    private DbUpgradeExecutorService dbUpgradeExecutor;

    @Override
    public void upgradeDatabase(String fromVersion) throws Exception {
        Path schemaUpdateFile;
        switch (fromVersion) {
            case "3.4.1":
                try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
                    log.info("Updating schema ...");
                    runSchemaUpdateScript(conn, "3.4.1");
                    if (isOldSchema(conn, 3004001)) {
                        try {
                            conn.createStatement().execute("ALTER TABLE asset ADD COLUMN asset_profile_id uuid");
                        } catch (Exception e) {
                        }

                        schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "3.4.1", "schema_update_before.sql");
                        loadSql(schemaUpdateFile, conn);

                        conn.createStatement().execute("DELETE FROM asset a WHERE NOT exists(SELECT id FROM tenant WHERE id = a.tenant_id);");

                        log.info("Creating default asset profiles...");

                        PageLink pageLink = new PageLink(1000);
                        PageData<TenantId> tenantIds;
                        do {
                            List<ListenableFuture<?>> futures = new ArrayList<>();
                            tenantIds = tenantService.findTenantsIds(pageLink);
                            for (TenantId tenantId : tenantIds.getData()) {
                                futures.add(dbUpgradeExecutor.submit(() -> {
                                    try {
                                        assetProfileService.createDefaultAssetProfile(tenantId);
                                    } catch (Exception e) {}
                                }));
                            }
                            Futures.allAsList(futures).get();
                            pageLink = pageLink.nextPageLink();
                        } while (tenantIds.hasNext());

                        pageLink = new PageLink(1000);
                        PageData<TbPair<UUID, String>> pairs;
                        do {
                            List<ListenableFuture<?>> futures = new ArrayList<>();
                            pairs = assetDao.getAllAssetTypes(pageLink);
                            for (TbPair<UUID, String> pair : pairs.getData()) {
                                TenantId tenantId = new TenantId(pair.getFirst());
                                String assetType = pair.getSecond();
                                if (!"default".equals(assetType)) {
                                    futures.add(dbUpgradeExecutor.submit(() -> {
                                        try {
                                            assetProfileService.findOrCreateAssetProfile(tenantId, assetType);
                                        } catch (Exception e) {}
                                    }));
                                }
                            }
                            Futures.allAsList(futures).get();
                            pageLink = pageLink.nextPageLink();
                        } while (pairs.hasNext());

                        log.info("Updating asset profiles...");
                        conn.createStatement().execute("call update_asset_profiles()");

                        schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "3.4.1", "schema_update_after.sql");
                        loadSql(schemaUpdateFile, conn);

                        conn.createStatement().execute("UPDATE tb_schema_settings SET schema_version = 3004002;");
                    }
                    log.info("Schema updated.");
                } catch (Exception e) {
                    log.error("Failed updating schema!!!", e);
                }
                break;
            default:
                throw new RuntimeException("Unable to upgrade SQL database, unsupported fromVersion: " + fromVersion);
        }
    }

    private void runSchemaUpdateScript(Connection connection, String version) throws Exception {
        Path schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", version, SCHEMA_UPDATE_SQL);
        loadSql(schemaUpdateFile, connection);
    }

    private void loadSql(Path sqlFile, Connection conn) throws Exception {
        String sql = new String(Files.readAllBytes(sqlFile), Charset.forName("UTF-8"));
        Statement st = conn.createStatement();
        st.setQueryTimeout((int) TimeUnit.HOURS.toSeconds(3));
        st.execute(sql);//NOSONAR, ignoring because method used to execute echoiot database upgrade script
        printWarnings(st);
        Thread.sleep(5000);
    }

    protected void printWarnings(Statement statement) throws SQLException {
        SQLWarning warnings = statement.getWarnings();
        if (warnings != null) {
            log.info("{}", warnings.getMessage());
            SQLWarning nextWarning = warnings.getNextWarning();
            while (nextWarning != null) {
                log.info("{}", nextWarning.getMessage());
                nextWarning = nextWarning.getNextWarning();
            }
        }
    }

    protected boolean isOldSchema(Connection conn, long fromVersion) {
        boolean isOldSchema = true;
        try {
            Statement statement = conn.createStatement();
            statement.execute("CREATE TABLE IF NOT EXISTS tb_schema_settings ( schema_version bigint NOT NULL, CONSTRAINT tb_schema_settings_pkey PRIMARY KEY (schema_version));");
            Thread.sleep(1000);
            ResultSet resultSet = statement.executeQuery("SELECT schema_version FROM tb_schema_settings;");
            if (resultSet.next()) {
                isOldSchema = resultSet.getLong(1) <= fromVersion;
            } else {
                resultSet.close();
                statement.execute("INSERT INTO tb_schema_settings (schema_version) VALUES (" + fromVersion + ")");
            }
            statement.close();
        } catch (InterruptedException | SQLException e) {
            log.info("Failed to check current PostgreSQL schema due to: {}", e.getMessage());
        }
        return isOldSchema;
    }

    private Queue queueConfigToQueue(TbRuleEngineQueueConfiguration queueSettings) {
        Queue queue = new Queue();
        queue.setTenantId(TenantId.SYS_TENANT_ID);
        queue.setName(queueSettings.getName());
        queue.setTopic(queueSettings.getTopic());
        queue.setPollInterval(queueSettings.getPollInterval());
        queue.setPartitions(queueSettings.getPartitions());
        queue.setPackProcessingTimeout(queueSettings.getPackProcessingTimeout());
        SubmitStrategy submitStrategy = new SubmitStrategy();
        submitStrategy.setBatchSize(queueSettings.getSubmitStrategy().getBatchSize());
        submitStrategy.setType(SubmitStrategyType.valueOf(queueSettings.getSubmitStrategy().getType()));
        queue.setSubmitStrategy(submitStrategy);
        ProcessingStrategy processingStrategy = new ProcessingStrategy();
        processingStrategy.setType(ProcessingStrategyType.valueOf(queueSettings.getProcessingStrategy().getType()));
        processingStrategy.setRetries(queueSettings.getProcessingStrategy().getRetries());
        processingStrategy.setFailurePercentage(queueSettings.getProcessingStrategy().getFailurePercentage());
        processingStrategy.setPauseBetweenRetries(queueSettings.getProcessingStrategy().getPauseBetweenRetries());
        processingStrategy.setMaxPauseBetweenRetries(queueSettings.getProcessingStrategy().getMaxPauseBetweenRetries());
        queue.setProcessingStrategy(processingStrategy);
        queue.setConsumerPerPartition(queueSettings.isConsumerPerPartition());
        return queue;
    }

}
