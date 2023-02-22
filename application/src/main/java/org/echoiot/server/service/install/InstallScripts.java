package org.echoiot.server.service.install;

import cn.hutool.core.text.CharSequenceUtil;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.Dashboard;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.oauth2.OAuth2ClientRegistrationTemplate;
import org.echoiot.server.common.data.rule.RuleChain;
import org.echoiot.server.common.data.rule.RuleChainMetaData;
import org.echoiot.server.common.data.widget.WidgetTypeDetails;
import org.echoiot.server.common.data.widget.WidgetsBundle;
import org.echoiot.server.dao.dashboard.DashboardService;
import org.echoiot.server.dao.oauth2.OAuth2ConfigTemplateService;
import org.echoiot.server.dao.resource.ResourceService;
import org.echoiot.server.dao.rule.RuleChainService;
import org.echoiot.server.dao.widget.WidgetTypeService;
import org.echoiot.server.dao.widget.WidgetsBundleService;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.echoiot.server.service.install.DatabaseHelper.objectMapper;

/**
 * 安装脚本
 */
@SuppressWarnings("AlibabaUndefineMagicConstant")
@Component
@Slf4j
public class InstallScripts {

    public static final String APP_DIR = "application";
    public static final String SRC_DIR = "src";
    public static final String MAIN_DIR = "main";
    public static final String DATA_DIR = "data";
    public static final String JSON_DIR = "json";
    public static final String SYSTEM_DIR = "system";
    public static final String TENANT_DIR = "tenant";
    public static final String DEVICE_PROFILE_DIR = "device_profile";
    public static final String DEMO_DIR = "demo";
    public static final String RULE_CHAINS_DIR = "rule_chains";
    public static final String WIDGET_BUNDLES_DIR = "widget_bundles";
    public static final String OAUTH2_CONFIG_TEMPLATES_DIR = "oauth2_config_templates";
    public static final String DASHBOARDS_DIR = "dashboards";
    public static final String MODELS_DIR = "models";
    public static final String CREDENTIALS_DIR = "credentials";

    public static final String EDGE_MANAGEMENT = "edge_management";

    public static final String JSON_EXT = ".json";
    public static final String XML_EXT = ".xml";

    /**
     * sql 所在目录
     */
    @Value("${install.data_dir:}")
    private String dataDir;

    @Resource
    private RuleChainService ruleChainService;

    @Resource
    private DashboardService dashboardService;

    @Resource
    private WidgetTypeService widgetTypeService;

    @Resource
    private WidgetsBundleService widgetsBundleService;

    @Resource
    private OAuth2ConfigTemplateService oAuth2TemplateService;

    @Resource
    private ResourceService resourceService;

    private Path getTenantRuleChainsDir() {
        return Paths.get(getDataDir(), JSON_DIR, TENANT_DIR, RULE_CHAINS_DIR);
    }

    private Path getDeviceProfileDefaultRuleChainTemplateFilePath() {
        return Paths.get(getDataDir(), JSON_DIR, TENANT_DIR, DEVICE_PROFILE_DIR, "rule_chain_template.json");
    }

    private Path getEdgeRuleChainsDir() {
        return Paths.get(getDataDir(), JSON_DIR, TENANT_DIR, EDGE_MANAGEMENT, RULE_CHAINS_DIR);
    }

    /**
     * 获取 sql 文件所在目录
     */
    public String getDataDir() {
        if (CharSequenceUtil.isNotEmpty(dataDir)) {
            //如果配置文件配置了 dataDir 不为空，检查是否是一个目录
            if (!Paths.get(dataDir).toFile().isDirectory()) {
                throw new RuntimeException("'install.data_dir' 配置的值 " + dataDir + " 不是有效的目录!");
            }
            return dataDir;
        } else {
            //如果配置文件没有配置 dataDir，检查当前目录是否是 application 模块目录
            String workDir = System.getProperty("user.dir");
            if (workDir.endsWith("application")) {
                //如果是 application 模块目录，直接返回 src/main/data 目录
                return Paths.get(workDir, SRC_DIR, MAIN_DIR, DATA_DIR).toString();
            } else {
                //如果不是 application 模块目录，默认返回 application 模块的 src/main/data 目录
                Path dataDirPath = Paths.get(workDir, APP_DIR, SRC_DIR, MAIN_DIR, DATA_DIR);
                if (Files.exists(dataDirPath)) {
                    return dataDirPath.toString();
                } else {
                    throw new RuntimeException("无效的工作目录: " + workDir + ". 请使用任一根项目目录, application 模块目录 或 指定有效 \"install.data_dir\" ENV 环境变量来避免自动数据目录查找!");
                }
            }
        }
    }

    public void createDefaultRuleChains(TenantId tenantId) throws IOException {
        Path tenantChainsDir = getTenantRuleChainsDir();
        loadRuleChainsFromPath(tenantId, tenantChainsDir);
    }

    public void createDefaultEdgeRuleChains(TenantId tenantId) throws IOException {
        Path edgeChainsDir = getEdgeRuleChainsDir();
        loadRuleChainsFromPath(tenantId, edgeChainsDir);
    }

    private void loadRuleChainsFromPath(TenantId tenantId, Path ruleChainsPath) throws IOException {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(ruleChainsPath, path -> path.toString().endsWith(InstallScripts.JSON_EXT))) {
            dirStream.forEach(path -> {
                try {
                    createRuleChainFromFile(tenantId, path, null);
                } catch (Exception e) {
                    log.error("Unable to load rule chain from json: [{}]", path.toString());
                    throw new RuntimeException("Unable to load rule chain from json", e);
                }
            });
        }
    }

    public RuleChain createDefaultRuleChain(TenantId tenantId, String ruleChainName) throws IOException {
        return createRuleChainFromFile(tenantId, getDeviceProfileDefaultRuleChainTemplateFilePath(), ruleChainName);
    }

    public RuleChain createRuleChainFromFile(TenantId tenantId, Path templateFilePath, String newRuleChainName) throws IOException {
        JsonNode ruleChainJson = objectMapper.readTree(templateFilePath.toFile());
        RuleChain ruleChain = objectMapper.treeToValue(ruleChainJson.get("ruleChain"), RuleChain.class);
        RuleChainMetaData ruleChainMetaData = objectMapper.treeToValue(ruleChainJson.get("metadata"), RuleChainMetaData.class);

        ruleChain.setTenantId(tenantId);
        if (!StringUtils.isEmpty(newRuleChainName)) {
            ruleChain.setName(newRuleChainName);
        }
        ruleChain = ruleChainService.saveRuleChain(ruleChain);

        ruleChainMetaData.setRuleChainId(ruleChain.getId());
        ruleChainService.saveRuleChainMetaData(TenantId.SYS_TENANT_ID, ruleChainMetaData);

        return ruleChain;
    }

    public void loadSystemWidgets() throws Exception {
        Path widgetBundlesDir = Paths.get(getDataDir(), JSON_DIR, SYSTEM_DIR, WIDGET_BUNDLES_DIR);
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(widgetBundlesDir, path -> path.toString().endsWith(JSON_EXT))) {
            dirStream.forEach(
                    path -> {
                        try {
                            JsonNode widgetsBundleDescriptorJson = objectMapper.readTree(path.toFile());
                            JsonNode widgetsBundleJson = widgetsBundleDescriptorJson.get("widgetsBundle");
                            WidgetsBundle widgetsBundle = objectMapper.treeToValue(widgetsBundleJson, WidgetsBundle.class);
                            WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);
                            JsonNode widgetTypesArrayJson = widgetsBundleDescriptorJson.get("widgetTypes");
                            widgetTypesArrayJson.forEach(
                                    widgetTypeJson -> {
                                        try {
                                            WidgetTypeDetails widgetTypeDetails = objectMapper.treeToValue(widgetTypeJson, WidgetTypeDetails.class);
                                            widgetTypeDetails.setBundleAlias(savedWidgetsBundle.getAlias());
                                            widgetTypeService.saveWidgetType(widgetTypeDetails);
                                        } catch (Exception e) {
                                            log.error("Unable to load widget type from json: [{}]", path);
                                            throw new RuntimeException("Unable to load widget type from json", e);
                                        }
                                    }
                            );
                        } catch (Exception e) {
                            log.error("Unable to load widgets bundle from json: [{}]", path.toString());
                            throw new RuntimeException("Unable to load widgets bundle from json", e);
                        }
                    }
            );
        }
    }

    public void loadDashboards(TenantId tenantId, @Nullable CustomerId customerId) throws Exception {
        Path dashboardsDir = Paths.get(getDataDir(), JSON_DIR, DEMO_DIR, DASHBOARDS_DIR);
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(dashboardsDir, path -> path.toString().endsWith(JSON_EXT))) {
            dirStream.forEach(
                    path -> {
                        try {
                            JsonNode dashboardJson = objectMapper.readTree(path.toFile());
                            Dashboard dashboard = objectMapper.treeToValue(dashboardJson, Dashboard.class);
                            dashboard.setTenantId(tenantId);
                            Dashboard savedDashboard = dashboardService.saveDashboard(dashboard);
                            if (customerId != null && !customerId.isNullUid()) {
                                dashboardService.assignDashboardToCustomer(TenantId.SYS_TENANT_ID, savedDashboard.getId(), customerId);
                            }
                        } catch (Exception e) {
                            log.error("Unable to load dashboard from json: [{}]", path.toString());
                            throw new RuntimeException("Unable to load dashboard from json", e);
                        }
                    }
            );
        }
    }

    public void loadDemoRuleChains(TenantId tenantId) {
        try {
            createDefaultRuleChains(tenantId);
            createDefaultRuleChain(tenantId, "Thermostat");
            createDefaultEdgeRuleChains(tenantId);
        } catch (Exception e) {
            log.error("Unable to load rule chain from json", e);
            throw new RuntimeException("Unable to load rule chain from json", e);
        }
    }

    public void createOAuth2Templates() throws Exception {
        Path oauth2ConfigTemplatesDir = Paths.get(getDataDir(), JSON_DIR, SYSTEM_DIR, OAUTH2_CONFIG_TEMPLATES_DIR);
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(oauth2ConfigTemplatesDir, path -> path.toString().endsWith(JSON_EXT))) {
            dirStream.forEach(
                    path -> {
                        try {
                            JsonNode oauth2ConfigTemplateJson = objectMapper.readTree(path.toFile());
                            OAuth2ClientRegistrationTemplate clientRegistrationTemplate = objectMapper.treeToValue(oauth2ConfigTemplateJson, OAuth2ClientRegistrationTemplate.class);
                            Optional<OAuth2ClientRegistrationTemplate> existingClientRegistrationTemplate =
                                    oAuth2TemplateService.findClientRegistrationTemplateByProviderId(clientRegistrationTemplate.getProviderId());
                            if (existingClientRegistrationTemplate.isPresent()) {
                                clientRegistrationTemplate.setId(existingClientRegistrationTemplate.get().getId());
                            }
                            oAuth2TemplateService.saveClientRegistrationTemplate(clientRegistrationTemplate);
                        } catch (Exception e) {
                            log.error("Unable to load oauth2 config templates from json: [{}]", path.toString());
                            throw new RuntimeException("Unable to load oauth2 config templates from json", e);
                        }
                    }
            );
        }
    }
}
