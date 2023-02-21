package org.echoiot.server.service.install;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.common.util.EchoiotThreadFactory;
import org.echoiot.server.common.data.*;
import org.echoiot.server.common.data.alarm.AlarmSeverity;
import org.echoiot.server.common.data.device.profile.*;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.DeviceProfileId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.kv.*;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.query.*;
import org.echoiot.server.common.data.queue.*;
import org.echoiot.server.common.data.rule.RuleChainType;
import org.echoiot.server.common.data.security.Authority;
import org.echoiot.server.common.data.security.DeviceCredentials;
import org.echoiot.server.common.data.security.UserCredentials;
import org.echoiot.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.echoiot.server.common.data.tenant.profile.TenantProfileData;
import org.echoiot.server.common.data.tenant.profile.TenantProfileQueueConfiguration;
import org.echoiot.server.common.data.widget.WidgetsBundle;
import org.echoiot.server.dao.attributes.AttributesService;
import org.echoiot.server.dao.customer.CustomerService;
import org.echoiot.server.dao.device.DeviceCredentialsService;
import org.echoiot.server.dao.device.DeviceProfileService;
import org.echoiot.server.dao.device.DeviceService;
import org.echoiot.server.dao.exception.DataValidationException;
import org.echoiot.server.dao.queue.QueueService;
import org.echoiot.server.dao.rule.RuleChainService;
import org.echoiot.server.dao.settings.AdminSettingsService;
import org.echoiot.server.dao.tenant.TenantProfileService;
import org.echoiot.server.dao.tenant.TenantService;
import org.echoiot.server.dao.timeseries.TimeseriesService;
import org.echoiot.server.dao.user.UserService;
import org.echoiot.server.dao.widget.WidgetsBundleService;
import org.echoiot.server.service.security.auth.jwt.settings.JwtSettingsService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Profile("install")
@Slf4j
public class DefaultSystemDataLoaderService implements SystemDataLoaderService {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    public static final String CUSTOMER_CRED = "customer";
    public static final String DEFAULT_DEVICE_TYPE = "default";
    public static final String ACTIVITY_STATE = "active";

    @Resource
    private InstallScripts installScripts;

    @Resource
    private BCryptPasswordEncoder passwordEncoder;

    @Resource
    private UserService userService;

    @Resource
    private AdminSettingsService adminSettingsService;

    @Resource
    private WidgetsBundleService widgetsBundleService;

    @Resource
    private TenantService tenantService;

    @Resource
    private TenantProfileService tenantProfileService;

    @Resource
    private CustomerService customerService;

    @Resource
    private DeviceService deviceService;

    @Resource
    private DeviceProfileService deviceProfileService;

    @Resource
    private AttributesService attributesService;

    @Resource
    private DeviceCredentialsService deviceCredentialsService;

    @Resource
    private RuleChainService ruleChainService;

    @Resource
    private TimeseriesService tsService;

    @Value("${state.persistToTelemetry:false}")
    @Getter
    private boolean persistActivityToTelemetry;

    @Lazy
    @Resource
    private QueueService queueService;

    @Resource
    private JwtSettingsService jwtSettingsService;

    @NotNull
    @Bean
    protected BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private ExecutorService tsCallBackExecutor;

    @PostConstruct
    public void initExecutor() {
        tsCallBackExecutor = Executors.newSingleThreadExecutor(EchoiotThreadFactory.forName("sys-loader-ts-callback"));
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (tsCallBackExecutor != null) {
            tsCallBackExecutor.shutdownNow();
        }
    }

    @Override
    public void createSysAdmin() {
        createUser(Authority.SYS_ADMIN, null, null, "sysadmin@echoiot.org", "sysadmin");
    }

    @Override
    public void createDefaultTenantProfiles() throws Exception {
        tenantProfileService.findOrCreateDefaultTenantProfile(TenantId.SYS_TENANT_ID);

        @NotNull TenantProfileData isolatedRuleEngineTenantProfileData = new TenantProfileData();
        isolatedRuleEngineTenantProfileData.setConfiguration(new DefaultTenantProfileConfiguration());

        @NotNull TenantProfileQueueConfiguration mainQueueConfiguration = new TenantProfileQueueConfiguration();
        mainQueueConfiguration.setName(DataConstants.MAIN_QUEUE_NAME);
        mainQueueConfiguration.setTopic(DataConstants.MAIN_QUEUE_TOPIC);
        mainQueueConfiguration.setPollInterval(25);
        mainQueueConfiguration.setPartitions(10);
        mainQueueConfiguration.setConsumerPerPartition(true);
        mainQueueConfiguration.setPackProcessingTimeout(2000);
        @NotNull SubmitStrategy mainQueueSubmitStrategy = new SubmitStrategy();
        mainQueueSubmitStrategy.setType(SubmitStrategyType.BURST);
        mainQueueSubmitStrategy.setBatchSize(1000);
        mainQueueConfiguration.setSubmitStrategy(mainQueueSubmitStrategy);
        @NotNull ProcessingStrategy mainQueueProcessingStrategy = new ProcessingStrategy();
        mainQueueProcessingStrategy.setType(ProcessingStrategyType.SKIP_ALL_FAILURES);
        mainQueueProcessingStrategy.setRetries(3);
        mainQueueProcessingStrategy.setFailurePercentage(0);
        mainQueueProcessingStrategy.setPauseBetweenRetries(3);
        mainQueueProcessingStrategy.setMaxPauseBetweenRetries(3);
        mainQueueConfiguration.setProcessingStrategy(mainQueueProcessingStrategy);

        isolatedRuleEngineTenantProfileData.setQueueConfiguration(Collections.singletonList(mainQueueConfiguration));

        @NotNull TenantProfile isolatedTbRuleEngineProfile = new TenantProfile();
        isolatedTbRuleEngineProfile.setDefault(false);
        isolatedTbRuleEngineProfile.setName("Isolated TB Rule Engine");
        isolatedTbRuleEngineProfile.setDescription("Isolated TB Rule Engine tenant profile");
        isolatedTbRuleEngineProfile.setIsolatedTbRuleEngine(true);
        isolatedTbRuleEngineProfile.setProfileData(isolatedRuleEngineTenantProfileData);

        try {
            tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, isolatedTbRuleEngineProfile);
        } catch (DataValidationException e) {
            log.warn(e.getMessage());
        }
    }

    @Override
    public void createAdminSettings() throws Exception {
        @NotNull AdminSettings generalSettings = new AdminSettings();
        generalSettings.setTenantId(TenantId.SYS_TENANT_ID);
        generalSettings.setKey("general");
        ObjectNode node = objectMapper.createObjectNode();
        node.put("baseUrl", "http://localhost:8080");
        node.put("prohibitDifferentUrl", false);
        generalSettings.setJsonValue(node);
        adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, generalSettings);

        @NotNull AdminSettings mailSettings = new AdminSettings();
        mailSettings.setTenantId(TenantId.SYS_TENANT_ID);
        mailSettings.setKey("mail");
        node = objectMapper.createObjectNode();
        node.put("mailFrom", "Echoiot <sysadmin@localhost.localdomain>");
        node.put("smtpProtocol", "smtp");
        node.put("smtpHost", "localhost");
        node.put("smtpPort", "25");
        node.put("timeout", "10000");
        node.put("enableTls", false);
        node.put("username", "");
        node.put("password", "");
        node.put("tlsVersion", "TLSv1.2");//NOSONAR, key used to identify password field (not password value itself)
        node.put("enableProxy", false);
        node.put("showChangePassword", false);
        mailSettings.setJsonValue(node);
        adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, mailSettings);
    }

    @Override
    public void createRandomJwtSettings() throws Exception {
        jwtSettingsService.createRandomJwtSettings();
    }

    @Override
    public void saveLegacyYmlSettings() throws Exception {
        jwtSettingsService.saveLegacyYmlSettings();
    }

    @Override
    public void createOAuth2Templates() throws Exception {
        installScripts.createOAuth2Templates();
    }

    @Override
    public void loadDemoData() throws Exception {
        Tenant demoTenant = new Tenant();
        demoTenant.setRegion("Global");
        demoTenant.setTitle("Tenant");
        demoTenant = tenantService.saveTenant(demoTenant);
        installScripts.loadDemoRuleChains(demoTenant.getId());
        createUser(Authority.TENANT_ADMIN, demoTenant.getId(), null, "tenant@echoiot.org", "tenant");

        Customer customerA = new Customer();
        customerA.setTenantId(demoTenant.getId());
        customerA.setTitle("Customer A");
        customerA = customerService.saveCustomer(customerA);
        Customer customerB = new Customer();
        customerB.setTenantId(demoTenant.getId());
        customerB.setTitle("Customer B");
        customerB = customerService.saveCustomer(customerB);
        Customer customerC = new Customer();
        customerC.setTenantId(demoTenant.getId());
        customerC.setTitle("Customer C");
        customerC = customerService.saveCustomer(customerC);
        createUser(Authority.CUSTOMER_USER, demoTenant.getId(), customerA.getId(), "customer@echoiot.org", CUSTOMER_CRED);
        createUser(Authority.CUSTOMER_USER, demoTenant.getId(), customerA.getId(), "customerA@echoiot.org", CUSTOMER_CRED);
        createUser(Authority.CUSTOMER_USER, demoTenant.getId(), customerB.getId(), "customerB@echoiot.org", CUSTOMER_CRED);
        createUser(Authority.CUSTOMER_USER, demoTenant.getId(), customerC.getId(), "customerC@echoiot.org", CUSTOMER_CRED);

        DeviceProfile defaultDeviceProfile = this.deviceProfileService.findOrCreateDeviceProfile(demoTenant.getId(), DEFAULT_DEVICE_TYPE);

        createDevice(demoTenant.getId(), customerA.getId(), defaultDeviceProfile.getId(), "Test Device A1", "A1_TEST_TOKEN", null);
        createDevice(demoTenant.getId(), customerA.getId(), defaultDeviceProfile.getId(), "Test Device A2", "A2_TEST_TOKEN", null);
        createDevice(demoTenant.getId(), customerA.getId(), defaultDeviceProfile.getId(), "Test Device A3", "A3_TEST_TOKEN", null);
        createDevice(demoTenant.getId(), customerB.getId(), defaultDeviceProfile.getId(), "Test Device B1", "B1_TEST_TOKEN", null);
        createDevice(demoTenant.getId(), customerC.getId(), defaultDeviceProfile.getId(), "Test Device C1", "C1_TEST_TOKEN", null);

        createDevice(demoTenant.getId(), null, defaultDeviceProfile.getId(), "DHT11 Demo Device", "DHT11_DEMO_TOKEN", "Demo device that is used in sample " +
                "applications that upload data from DHT11 temperature and humidity sensor");

        createDevice(demoTenant.getId(), null, defaultDeviceProfile.getId(), "Raspberry Pi Demo Device", "RASPBERRY_PI_DEMO_TOKEN", "Demo device that is used in " +
                "Raspberry Pi GPIO control sample application");

        @NotNull DeviceProfile thermostatDeviceProfile = new DeviceProfile();
        thermostatDeviceProfile.setTenantId(demoTenant.getId());
        thermostatDeviceProfile.setDefault(false);
        thermostatDeviceProfile.setName("thermostat");
        thermostatDeviceProfile.setType(DeviceProfileType.DEFAULT);
        thermostatDeviceProfile.setTransportType(DeviceTransportType.DEFAULT);
        thermostatDeviceProfile.setProvisionType(DeviceProfileProvisionType.DISABLED);
        thermostatDeviceProfile.setDescription("Thermostat device profile");
        thermostatDeviceProfile.setDefaultRuleChainId(ruleChainService.findTenantRuleChainsByType(
                demoTenant.getId(), RuleChainType.CORE, new PageLink(1, 0, "Thermostat")).getData().get(0).getId());

        @NotNull DeviceProfileData deviceProfileData = new DeviceProfileData();
        @NotNull DefaultDeviceProfileConfiguration configuration = new DefaultDeviceProfileConfiguration();
        @NotNull DefaultDeviceProfileTransportConfiguration transportConfiguration = new DefaultDeviceProfileTransportConfiguration();
        @NotNull DisabledDeviceProfileProvisionConfiguration provisionConfiguration = new DisabledDeviceProfileProvisionConfiguration(null);
        deviceProfileData.setConfiguration(configuration);
        deviceProfileData.setTransportConfiguration(transportConfiguration);
        deviceProfileData.setProvisionConfiguration(provisionConfiguration);
        thermostatDeviceProfile.setProfileData(deviceProfileData);

        @NotNull DeviceProfileAlarm highTemperature = new DeviceProfileAlarm();
        highTemperature.setId("highTemperatureAlarmID");
        highTemperature.setAlarmType("High Temperature");
        @NotNull AlarmRule temperatureRule = new AlarmRule();
        @NotNull AlarmCondition temperatureCondition = new AlarmCondition();
        temperatureCondition.setSpec(new SimpleAlarmConditionSpec());

        @NotNull AlarmConditionFilter temperatureAlarmFlagAttributeFilter = new AlarmConditionFilter();
        temperatureAlarmFlagAttributeFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, "temperatureAlarmFlag"));
        temperatureAlarmFlagAttributeFilter.setValueType(EntityKeyValueType.BOOLEAN);
        @NotNull BooleanFilterPredicate temperatureAlarmFlagAttributePredicate = new BooleanFilterPredicate();
        temperatureAlarmFlagAttributePredicate.setOperation(BooleanFilterPredicate.BooleanOperation.EQUAL);
        temperatureAlarmFlagAttributePredicate.setValue(new FilterPredicateValue<>(Boolean.TRUE));
        temperatureAlarmFlagAttributeFilter.setPredicate(temperatureAlarmFlagAttributePredicate);

        @NotNull AlarmConditionFilter temperatureTimeseriesFilter = new AlarmConditionFilter();
        temperatureTimeseriesFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        temperatureTimeseriesFilter.setValueType(EntityKeyValueType.NUMERIC);
        @NotNull NumericFilterPredicate temperatureTimeseriesFilterPredicate = new NumericFilterPredicate();
        temperatureTimeseriesFilterPredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        @NotNull FilterPredicateValue<Double> temperatureTimeseriesPredicateValue =
                new FilterPredicateValue<>(25.0, null,
                        new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "temperatureAlarmThreshold"));
        temperatureTimeseriesFilterPredicate.setValue(temperatureTimeseriesPredicateValue);
        temperatureTimeseriesFilter.setPredicate(temperatureTimeseriesFilterPredicate);
        temperatureCondition.setCondition(Arrays.asList(temperatureAlarmFlagAttributeFilter, temperatureTimeseriesFilter));
        temperatureRule.setAlarmDetails("Current temperature = ${temperature}");
        temperatureRule.setCondition(temperatureCondition);
        highTemperature.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.MAJOR, temperatureRule)));

        @NotNull AlarmRule clearTemperatureRule = new AlarmRule();
        @NotNull AlarmCondition clearTemperatureCondition = new AlarmCondition();
        clearTemperatureCondition.setSpec(new SimpleAlarmConditionSpec());

        @NotNull AlarmConditionFilter clearTemperatureTimeseriesFilter = new AlarmConditionFilter();
        clearTemperatureTimeseriesFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        clearTemperatureTimeseriesFilter.setValueType(EntityKeyValueType.NUMERIC);
        @NotNull NumericFilterPredicate clearTemperatureTimeseriesFilterPredicate = new NumericFilterPredicate();
        clearTemperatureTimeseriesFilterPredicate.setOperation(NumericFilterPredicate.NumericOperation.LESS_OR_EQUAL);
        @NotNull FilterPredicateValue<Double> clearTemperatureTimeseriesPredicateValue =
                new FilterPredicateValue<>(25.0, null,
                        new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "temperatureAlarmThreshold"));

        clearTemperatureTimeseriesFilterPredicate.setValue(clearTemperatureTimeseriesPredicateValue);
        clearTemperatureTimeseriesFilter.setPredicate(clearTemperatureTimeseriesFilterPredicate);
        clearTemperatureCondition.setCondition(Collections.singletonList(clearTemperatureTimeseriesFilter));
        clearTemperatureRule.setCondition(clearTemperatureCondition);
        clearTemperatureRule.setAlarmDetails("Current temperature = ${temperature}");
        highTemperature.setClearRule(clearTemperatureRule);

        @NotNull DeviceProfileAlarm lowHumidity = new DeviceProfileAlarm();
        lowHumidity.setId("lowHumidityAlarmID");
        lowHumidity.setAlarmType("Low Humidity");
        @NotNull AlarmRule humidityRule = new AlarmRule();
        @NotNull AlarmCondition humidityCondition = new AlarmCondition();
        humidityCondition.setSpec(new SimpleAlarmConditionSpec());

        @NotNull AlarmConditionFilter humidityAlarmFlagAttributeFilter = new AlarmConditionFilter();
        humidityAlarmFlagAttributeFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, "humidityAlarmFlag"));
        humidityAlarmFlagAttributeFilter.setValueType(EntityKeyValueType.BOOLEAN);
        @NotNull BooleanFilterPredicate humidityAlarmFlagAttributePredicate = new BooleanFilterPredicate();
        humidityAlarmFlagAttributePredicate.setOperation(BooleanFilterPredicate.BooleanOperation.EQUAL);
        humidityAlarmFlagAttributePredicate.setValue(new FilterPredicateValue<>(Boolean.TRUE));
        humidityAlarmFlagAttributeFilter.setPredicate(humidityAlarmFlagAttributePredicate);

        @NotNull AlarmConditionFilter humidityTimeseriesFilter = new AlarmConditionFilter();
        humidityTimeseriesFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "humidity"));
        humidityTimeseriesFilter.setValueType(EntityKeyValueType.NUMERIC);
        @NotNull NumericFilterPredicate humidityTimeseriesFilterPredicate = new NumericFilterPredicate();
        humidityTimeseriesFilterPredicate.setOperation(NumericFilterPredicate.NumericOperation.LESS);
        @NotNull FilterPredicateValue<Double> humidityTimeseriesPredicateValue =
                new FilterPredicateValue<>(60.0, null,
                        new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "humidityAlarmThreshold"));
        humidityTimeseriesFilterPredicate.setValue(humidityTimeseriesPredicateValue);
        humidityTimeseriesFilter.setPredicate(humidityTimeseriesFilterPredicate);
        humidityCondition.setCondition(Arrays.asList(humidityAlarmFlagAttributeFilter, humidityTimeseriesFilter));

        humidityRule.setCondition(humidityCondition);
        humidityRule.setAlarmDetails("Current humidity = ${humidity}");
        lowHumidity.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.MINOR, humidityRule)));

        @NotNull AlarmRule clearHumidityRule = new AlarmRule();
        @NotNull AlarmCondition clearHumidityCondition = new AlarmCondition();
        clearHumidityCondition.setSpec(new SimpleAlarmConditionSpec());

        @NotNull AlarmConditionFilter clearHumidityTimeseriesFilter = new AlarmConditionFilter();
        clearHumidityTimeseriesFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "humidity"));
        clearHumidityTimeseriesFilter.setValueType(EntityKeyValueType.NUMERIC);
        @NotNull NumericFilterPredicate clearHumidityTimeseriesFilterPredicate = new NumericFilterPredicate();
        clearHumidityTimeseriesFilterPredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER_OR_EQUAL);
        @NotNull FilterPredicateValue<Double> clearHumidityTimeseriesPredicateValue =
                new FilterPredicateValue<>(60.0, null,
                        new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "humidityAlarmThreshold"));

        clearHumidityTimeseriesFilterPredicate.setValue(clearHumidityTimeseriesPredicateValue);
        clearHumidityTimeseriesFilter.setPredicate(clearHumidityTimeseriesFilterPredicate);
        clearHumidityCondition.setCondition(Collections.singletonList(clearHumidityTimeseriesFilter));
        clearHumidityRule.setCondition(clearHumidityCondition);
        clearHumidityRule.setAlarmDetails("Current humidity = ${humidity}");
        lowHumidity.setClearRule(clearHumidityRule);

        deviceProfileData.setAlarms(Arrays.asList(highTemperature, lowHumidity));

        DeviceProfile savedThermostatDeviceProfile = deviceProfileService.saveDeviceProfile(thermostatDeviceProfile);

        DeviceId t1Id = createDevice(demoTenant.getId(), null, savedThermostatDeviceProfile.getId(), "Thermostat T1", "T1_TEST_TOKEN", "Demo device for Thermostats dashboard").getId();
        DeviceId t2Id = createDevice(demoTenant.getId(), null, savedThermostatDeviceProfile.getId(), "Thermostat T2", "T2_TEST_TOKEN", "Demo device for Thermostats dashboard").getId();

        attributesService.save(demoTenant.getId(), t1Id, DataConstants.SERVER_SCOPE,
                Arrays.asList(new BaseAttributeKvEntry(System.currentTimeMillis(), new DoubleDataEntry("latitude", 37.3948)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new DoubleDataEntry("longitude", -122.1503)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new BooleanDataEntry("temperatureAlarmFlag", true)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new BooleanDataEntry("humidityAlarmFlag", true)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new LongDataEntry("temperatureAlarmThreshold", (long) 20)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new LongDataEntry("humidityAlarmThreshold", (long) 50))));

        attributesService.save(demoTenant.getId(), t2Id, DataConstants.SERVER_SCOPE,
                Arrays.asList(new BaseAttributeKvEntry(System.currentTimeMillis(), new DoubleDataEntry("latitude", 37.493801)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new DoubleDataEntry("longitude", -121.948769)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new BooleanDataEntry("temperatureAlarmFlag", true)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new BooleanDataEntry("humidityAlarmFlag", true)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new LongDataEntry("temperatureAlarmThreshold", (long) 25)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new LongDataEntry("humidityAlarmThreshold", (long) 30))));

        installScripts.loadDashboards(demoTenant.getId(), null);
    }

    @Override
    public void deleteSystemWidgetBundle(String bundleAlias) throws Exception {
        WidgetsBundle widgetsBundle = widgetsBundleService.findWidgetsBundleByTenantIdAndAlias(TenantId.SYS_TENANT_ID, bundleAlias);
        if (widgetsBundle != null) {
            widgetsBundleService.deleteWidgetsBundle(TenantId.SYS_TENANT_ID, widgetsBundle.getId());
        }
    }

    @Override
    public void loadSystemWidgets() throws Exception {
        installScripts.loadSystemWidgets();
    }

    @Override
    public void updateSystemWidgets() throws Exception {
        this.deleteSystemWidgetBundle("charts");
        this.deleteSystemWidgetBundle("cards");
        this.deleteSystemWidgetBundle("maps");
        this.deleteSystemWidgetBundle("analogue_gauges");
        this.deleteSystemWidgetBundle("digital_gauges");
        this.deleteSystemWidgetBundle("gpio_widgets");
        this.deleteSystemWidgetBundle("alarm_widgets");
        this.deleteSystemWidgetBundle("control_widgets");
        this.deleteSystemWidgetBundle("maps_v2");
        this.deleteSystemWidgetBundle("gateway_widgets");
        this.deleteSystemWidgetBundle("input_widgets");
        this.deleteSystemWidgetBundle("date");
        this.deleteSystemWidgetBundle("entity_admin_widgets");
        this.deleteSystemWidgetBundle("navigation_widgets");
        this.deleteSystemWidgetBundle("edge_widgets");
        installScripts.loadSystemWidgets();
    }

    @NotNull
    private User createUser(Authority authority,
                            TenantId tenantId,
                            CustomerId customerId,
                            String email,
                            String password) {
        User user = new User();
        user.setAuthority(authority);
        user.setEmail(email);
        user.setTenantId(tenantId);
        user.setCustomerId(customerId);
        user = userService.saveUser(user);
        UserCredentials userCredentials = userService.findUserCredentialsByUserId(TenantId.SYS_TENANT_ID, user.getId());
        userCredentials.setPassword(passwordEncoder.encode(password));
        userCredentials.setEnabled(true);
        userCredentials.setActivateToken(null);
        userService.saveUserCredentials(TenantId.SYS_TENANT_ID, userCredentials);
        return user;
    }

    @NotNull
    private Device createDevice(TenantId tenantId,
                                CustomerId customerId,
                                DeviceProfileId deviceProfileId,
                                String name,
                                String accessToken,
                                @org.jetbrains.annotations.Nullable String description) {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setCustomerId(customerId);
        device.setDeviceProfileId(deviceProfileId);
        device.setName(name);
        if (description != null) {
            ObjectNode additionalInfo = objectMapper.createObjectNode();
            additionalInfo.put("description", description);
            device.setAdditionalInfo(additionalInfo);
        }
        device = deviceService.saveDevice(device);
        save(device.getId(), ACTIVITY_STATE, false);
        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(TenantId.SYS_TENANT_ID, device.getId());
        deviceCredentials.setCredentialsId(accessToken);
        deviceCredentialsService.updateDeviceCredentials(TenantId.SYS_TENANT_ID, deviceCredentials);
        return device;
    }

    private void save(DeviceId deviceId, String key, boolean value) {
        if (persistActivityToTelemetry) {
            ListenableFuture<Integer> saveFuture = tsService.save(
                    TenantId.SYS_TENANT_ID,
                    deviceId,
                    Collections.singletonList(new BasicTsKvEntry(System.currentTimeMillis(), new BooleanDataEntry(key, value))), 0L);
            addTsCallback(saveFuture, new TelemetrySaveCallback<>(deviceId, key, value));
        } else {
            ListenableFuture<List<String>> saveFuture = attributesService.save(TenantId.SYS_TENANT_ID, deviceId, DataConstants.SERVER_SCOPE,
                    Collections.singletonList(new BaseAttributeKvEntry(new BooleanDataEntry(key, value)
                    , System.currentTimeMillis())));
            addTsCallback(saveFuture, new TelemetrySaveCallback<>(deviceId, key, value));
        }
    }

    private static class TelemetrySaveCallback<T> implements FutureCallback<T> {
        private final DeviceId deviceId;
        private final String key;
        private final Object value;

        TelemetrySaveCallback(DeviceId deviceId, String key, Object value) {
            this.deviceId = deviceId;
            this.key = key;
            this.value = value;
        }

        @Override
        public void onSuccess(@Nullable T result) {
            log.trace("[{}] Successfully updated attribute [{}] with value [{}]", deviceId, key, value);
        }

        @Override
        public void onFailure(Throwable t) {
            log.warn("[{}] Failed to update attribute [{}] with value [{}]", deviceId, key, value, t);
        }
    }

    private <S> void addTsCallback(@NotNull ListenableFuture<S> saveFuture, @NotNull final FutureCallback<S> callback) {
        Futures.addCallback(saveFuture, new FutureCallback<S>() {
            @Override
            public void onSuccess(@Nullable S result) {
                callback.onSuccess(result);
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure(t);
            }
        }, tsCallBackExecutor);
    }

    @Override
    public void createQueues() {
        Queue mainQueue = queueService.findQueueByTenantIdAndName(TenantId.SYS_TENANT_ID, DataConstants.MAIN_QUEUE_NAME);
        if (mainQueue == null) {
            mainQueue = new Queue();
            mainQueue.setTenantId(TenantId.SYS_TENANT_ID);
            mainQueue.setName(DataConstants.MAIN_QUEUE_NAME);
            mainQueue.setTopic(DataConstants.MAIN_QUEUE_TOPIC);
            mainQueue.setPollInterval(25);
            mainQueue.setPartitions(10);
            mainQueue.setConsumerPerPartition(true);
            mainQueue.setPackProcessingTimeout(2000);
            @NotNull SubmitStrategy mainQueueSubmitStrategy = new SubmitStrategy();
            mainQueueSubmitStrategy.setType(SubmitStrategyType.BURST);
            mainQueueSubmitStrategy.setBatchSize(1000);
            mainQueue.setSubmitStrategy(mainQueueSubmitStrategy);
            @NotNull ProcessingStrategy mainQueueProcessingStrategy = new ProcessingStrategy();
            mainQueueProcessingStrategy.setType(ProcessingStrategyType.SKIP_ALL_FAILURES);
            mainQueueProcessingStrategy.setRetries(3);
            mainQueueProcessingStrategy.setFailurePercentage(0);
            mainQueueProcessingStrategy.setPauseBetweenRetries(3);
            mainQueueProcessingStrategy.setMaxPauseBetweenRetries(3);
            mainQueue.setProcessingStrategy(mainQueueProcessingStrategy);
            queueService.saveQueue(mainQueue);
        }

        Queue highPriorityQueue = queueService.findQueueByTenantIdAndName(TenantId.SYS_TENANT_ID, DataConstants.HP_QUEUE_NAME);
        if (highPriorityQueue == null) {
            highPriorityQueue = new Queue();
            highPriorityQueue.setTenantId(TenantId.SYS_TENANT_ID);
            highPriorityQueue.setName(DataConstants.HP_QUEUE_NAME);
            highPriorityQueue.setTopic(DataConstants.HP_QUEUE_TOPIC);
            highPriorityQueue.setPollInterval(25);
            highPriorityQueue.setPartitions(10);
            highPriorityQueue.setConsumerPerPartition(true);
            highPriorityQueue.setPackProcessingTimeout(2000);
            @NotNull SubmitStrategy highPriorityQueueSubmitStrategy = new SubmitStrategy();
            highPriorityQueueSubmitStrategy.setType(SubmitStrategyType.BURST);
            highPriorityQueueSubmitStrategy.setBatchSize(100);
            highPriorityQueue.setSubmitStrategy(highPriorityQueueSubmitStrategy);
            @NotNull ProcessingStrategy highPriorityQueueProcessingStrategy = new ProcessingStrategy();
            highPriorityQueueProcessingStrategy.setType(ProcessingStrategyType.RETRY_FAILED_AND_TIMED_OUT);
            highPriorityQueueProcessingStrategy.setRetries(0);
            highPriorityQueueProcessingStrategy.setFailurePercentage(0);
            highPriorityQueueProcessingStrategy.setPauseBetweenRetries(5);
            highPriorityQueueProcessingStrategy.setMaxPauseBetweenRetries(5);
            highPriorityQueue.setProcessingStrategy(highPriorityQueueProcessingStrategy);
            queueService.saveQueue(highPriorityQueue);
        }

        Queue sequentialByOriginatorQueue = queueService.findQueueByTenantIdAndName(TenantId.SYS_TENANT_ID, DataConstants.SQ_QUEUE_NAME);
        if (sequentialByOriginatorQueue == null) {
            sequentialByOriginatorQueue = new Queue();
            sequentialByOriginatorQueue.setTenantId(TenantId.SYS_TENANT_ID);
            sequentialByOriginatorQueue.setName(DataConstants.SQ_QUEUE_NAME);
            sequentialByOriginatorQueue.setTopic(DataConstants.SQ_QUEUE_TOPIC);
            sequentialByOriginatorQueue.setPollInterval(25);
            sequentialByOriginatorQueue.setPartitions(10);
            sequentialByOriginatorQueue.setPackProcessingTimeout(2000);
            sequentialByOriginatorQueue.setConsumerPerPartition(true);
            @NotNull SubmitStrategy sequentialByOriginatorQueueSubmitStrategy = new SubmitStrategy();
            sequentialByOriginatorQueueSubmitStrategy.setType(SubmitStrategyType.SEQUENTIAL_BY_ORIGINATOR);
            sequentialByOriginatorQueueSubmitStrategy.setBatchSize(100);
            sequentialByOriginatorQueue.setSubmitStrategy(sequentialByOriginatorQueueSubmitStrategy);
            @NotNull ProcessingStrategy sequentialByOriginatorQueueProcessingStrategy = new ProcessingStrategy();
            sequentialByOriginatorQueueProcessingStrategy.setType(ProcessingStrategyType.RETRY_FAILED_AND_TIMED_OUT);
            sequentialByOriginatorQueueProcessingStrategy.setRetries(3);
            sequentialByOriginatorQueueProcessingStrategy.setFailurePercentage(0);
            sequentialByOriginatorQueueProcessingStrategy.setPauseBetweenRetries(5);
            sequentialByOriginatorQueueProcessingStrategy.setMaxPauseBetweenRetries(5);
            sequentialByOriginatorQueue.setProcessingStrategy(sequentialByOriginatorQueueProcessingStrategy);
            queueService.saveQueue(sequentialByOriginatorQueue);
        }
    }

}
