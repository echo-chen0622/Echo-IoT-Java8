package org.echoiot.server.dao.service;

import org.echoiot.server.common.data.DataConstants;
import org.echoiot.server.common.data.Tenant;
import org.echoiot.server.common.data.TenantProfile;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.id.TenantProfileId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.tenant.profile.TenantProfileData;
import org.echoiot.server.common.data.tenant.profile.TenantProfileQueueConfiguration;
import org.echoiot.server.dao.exception.DataValidationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.echoiot.server.common.data.queue.ProcessingStrategy;
import org.echoiot.server.common.data.queue.ProcessingStrategyType;
import org.echoiot.server.common.data.queue.Queue;
import org.echoiot.server.common.data.queue.SubmitStrategy;
import org.echoiot.server.common.data.queue.SubmitStrategyType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class BaseQueueServiceTest extends AbstractServiceTest {

    private final IdComparator<Queue> idComparator = new IdComparator<>();

    private TenantId tenantId;
    private TenantProfileId tenantProfileId;

    @Before
    public void before() throws NoSuchFieldException, IllegalAccessException {
        @NotNull TenantProfile tenantProfile = new TenantProfile();
        tenantProfile.setDefault(false);
        tenantProfile.setName("Isolated TB Rule Engine");
        tenantProfile.setDescription("Isolated TB Rule Engine tenant profile");
        tenantProfile.setIsolatedTbRuleEngine(true);

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
        TenantProfileData profileData = tenantProfile.getProfileData();
        profileData.setQueueConfiguration(Collections.singletonList(mainQueueConfiguration));
        tenantProfile.setProfileData(profileData);

        TenantProfile savedTenantProfile = tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, tenantProfile);
        Assert.assertNotNull(savedTenantProfile);
        tenantProfileId = savedTenantProfile.getId();

        @NotNull Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        tenant.setTenantProfileId(tenantProfileId);
        Tenant savedTenant = tenantService.saveTenant(tenant);
        Assert.assertNotNull(savedTenant);
        tenantId = savedTenant.getId();
    }

    @After
    public void after() {
        tenantService.deleteTenant(tenantId);
        tenantProfileService.deleteTenantProfile(TenantId.SYS_TENANT_ID, tenantProfileId);
    }

    @NotNull
    private ProcessingStrategy createTestProcessingStrategy() {
        @NotNull ProcessingStrategy processingStrategy = new ProcessingStrategy();
        processingStrategy.setType(ProcessingStrategyType.SKIP_ALL_FAILURES);
        processingStrategy.setRetries(3);
        processingStrategy.setFailurePercentage(0);
        processingStrategy.setPauseBetweenRetries(3);
        processingStrategy.setMaxPauseBetweenRetries(3);
        return processingStrategy;
    }

    @NotNull
    private SubmitStrategy createTestSubmitStrategy() {
        @NotNull SubmitStrategy submitStrategy = new SubmitStrategy();
        submitStrategy.setType(SubmitStrategyType.BURST);
        submitStrategy.setBatchSize(1000);
        return submitStrategy;
    }

    @Test
    public void testSaveQueue() {
        @NotNull Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("Test");
        queue.setTopic("tb_rule_engine.test");
        queue.setPollInterval(25);
        queue.setPartitions(1);
        queue.setPackProcessingTimeout(2000);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queue.setProcessingStrategy(createTestProcessingStrategy());
        Queue savedQueue = queueService.saveQueue(queue);

        Assert.assertNotNull(savedQueue);
        Assert.assertNotNull(savedQueue.getId());
        Assert.assertTrue(savedQueue.getCreatedTime() > 0);
        Assert.assertEquals(queue.getTenantId(), savedQueue.getTenantId());
        Assert.assertEquals(queue.getName(), queue.getName());

        savedQueue.setPollInterval(100);

        queueService.saveQueue(savedQueue);
        Queue foundQueue = queueService.findQueueById(tenantId, savedQueue.getId());
        Assert.assertEquals(foundQueue.getPollInterval(), savedQueue.getPollInterval());

        queueService.deleteQueue(tenantId, foundQueue.getId());
    }

    @Test(expected = DataValidationException.class)
    public void testSaveQueueWithEmptyName() {
        @NotNull Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setTopic("tb_rule_engine.test");
        queue.setPollInterval(25);
        queue.setPartitions(1);
        queue.setPackProcessingTimeout(2000);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queue.setProcessingStrategy(createTestProcessingStrategy());
        queueService.saveQueue(queue);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveQueueWithInvalidName() {
        @NotNull Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("Test 1");
        queue.setTopic("tb_rule_engine.test");
        queue.setPollInterval(25);
        queue.setPartitions(1);
        queue.setPackProcessingTimeout(2000);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queue.setProcessingStrategy(createTestProcessingStrategy());
        queueService.saveQueue(queue);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveQueueWithEmptyTopic() {
        @NotNull Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("Test");
        queue.setPollInterval(25);
        queue.setPartitions(1);
        queue.setPackProcessingTimeout(2000);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queue.setProcessingStrategy(createTestProcessingStrategy());
        queueService.saveQueue(queue);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveQueueWithInvalidTopic() {
        @NotNull Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("Test");
        queue.setTopic("tb rule engine test");
        queue.setPollInterval(25);
        queue.setPartitions(1);
        queue.setPackProcessingTimeout(2000);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queue.setProcessingStrategy(createTestProcessingStrategy());
        queueService.saveQueue(queue);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveQueueWithEmptyPollInterval() {
        @NotNull Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("Test");
        queue.setTopic("tb_rule_engine.test");
        queue.setPartitions(1);
        queue.setPackProcessingTimeout(2000);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queue.setProcessingStrategy(createTestProcessingStrategy());
        queueService.saveQueue(queue);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveQueueWithEmptyPartitions() {
        @NotNull Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("Test");
        queue.setTopic("tb_rule_engine.test");
        queue.setPollInterval(25);
        queue.setPackProcessingTimeout(2000);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queue.setProcessingStrategy(createTestProcessingStrategy());
        queueService.saveQueue(queue);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveQueueWithEmptyPackProcessingTimeout() {
        @NotNull Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("Test");
        queue.setTopic("tb_rule_engine.test");
        queue.setPollInterval(25);
        queue.setPartitions(1);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queue.setProcessingStrategy(createTestProcessingStrategy());
        queueService.saveQueue(queue);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveQueueWithEmptySubmitStrategy() {
        @NotNull Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("Test");
        queue.setTopic("tb_rule_engine.test");
        queue.setPollInterval(25);
        queue.setPartitions(1);
        queue.setPackProcessingTimeout(2000);
        queue.setProcessingStrategy(createTestProcessingStrategy());
        queueService.saveQueue(queue);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveQueueWithEmptyProcessingStrategy() {
        @NotNull Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("Test");
        queue.setTopic("tb_rule_engine.test");
        queue.setPollInterval(25);
        queue.setPartitions(1);
        queue.setPackProcessingTimeout(2000);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queueService.saveQueue(queue);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveQueueWithEmptySubmitStrategyType() {
        @NotNull Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("Test");
        queue.setTopic("tb_rule_engine.test");
        queue.setPollInterval(25);
        queue.setPartitions(1);
        queue.setPackProcessingTimeout(2000);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queue.getSubmitStrategy().setType(null);
        queue.setProcessingStrategy(createTestProcessingStrategy());
        queueService.saveQueue(queue);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveQueueWithEmptySubmitStrategyBatchSize() {
        @NotNull Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("Test");
        queue.setTopic("tb_rule_engine.test");
        queue.setPollInterval(25);
        queue.setPartitions(1);
        queue.setPackProcessingTimeout(2000);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queue.getSubmitStrategy().setType(SubmitStrategyType.BATCH);
        queue.getSubmitStrategy().setBatchSize(0);
        queue.setProcessingStrategy(createTestProcessingStrategy());
        queueService.saveQueue(queue);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveQueueWithEmptyProcessingStrategyType() {
        @NotNull Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("Test");
        queue.setTopic("tb_rule_engine.test");
        queue.setPollInterval(25);
        queue.setPartitions(1);
        queue.setPackProcessingTimeout(2000);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queue.setProcessingStrategy(createTestProcessingStrategy());
        queue.getProcessingStrategy().setType(null);
        queueService.saveQueue(queue);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveQueueWithNegativeProcessingStrategyRetries() {
        @NotNull Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("Test");
        queue.setTopic("tb_rule_engine.test");
        queue.setPollInterval(25);
        queue.setPartitions(1);
        queue.setPackProcessingTimeout(2000);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queue.setProcessingStrategy(createTestProcessingStrategy());
        queue.getProcessingStrategy().setRetries(-1);
        queueService.saveQueue(queue);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveQueueWithNegativeProcessingStrategyFailurePercentage() {
        @NotNull Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("Test");
        queue.setTopic("tb_rule_engine.test");
        queue.setPollInterval(25);
        queue.setPartitions(1);
        queue.setPackProcessingTimeout(2000);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queue.setProcessingStrategy(createTestProcessingStrategy());
        queue.getProcessingStrategy().setFailurePercentage(-1);
        queueService.saveQueue(queue);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveQueueWithNegativeProcessingStrategyPauseBetweenRetries() {
        @NotNull Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("Test");
        queue.setTopic("tb_rule_engine.test");
        queue.setPollInterval(25);
        queue.setPartitions(1);
        queue.setPackProcessingTimeout(2000);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queue.setProcessingStrategy(createTestProcessingStrategy());
        queue.getProcessingStrategy().setPauseBetweenRetries(-1);
        queueService.saveQueue(queue);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveQueueWithProcessingStrategyPauseBetweenRetriesBiggerThenMaxPauseBetweenRetries() {
        @NotNull Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("Test");
        queue.setTopic("tb_rule_engine.test");
        queue.setPollInterval(25);
        queue.setPartitions(1);
        queue.setPackProcessingTimeout(2000);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queue.setProcessingStrategy(createTestProcessingStrategy());
        queue.getProcessingStrategy().setPauseBetweenRetries(100);
        queueService.saveQueue(queue);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveQueueWithNotIsolatedTenant() {
        @NotNull Tenant tenant = new Tenant();
        tenant.setTitle("Not isolated tenant");
        Tenant savedTenant = tenantService.saveTenant(tenant);
        Assert.assertNotNull(savedTenant);

        @NotNull Queue queue = new Queue();
        queue.setTenantId(savedTenant.getId());
        queue.setName("Test");
        queue.setTopic("tb_rule_engine.test");
        queue.setPollInterval(25);
        queue.setPartitions(1);
        queue.setPackProcessingTimeout(2000);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queue.setProcessingStrategy(createTestProcessingStrategy());
        try {
            queueService.saveQueue(queue);
        } finally {
            tenantService.deleteTenant(savedTenant.getId());
        }
    }

    @Test
    public void testUpdateQueue() {
        @NotNull Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("Test");
        queue.setTopic("tb_rule_engine.test");
        queue.setPollInterval(25);
        queue.setPartitions(1);
        queue.setPackProcessingTimeout(2000);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queue.setProcessingStrategy(createTestProcessingStrategy());
        Queue savedQueue = queueService.saveQueue(queue);

        Assert.assertNotNull(savedQueue);

        queue.setPollInterval(1000);

        queueService.saveQueue(savedQueue);

        Queue foundQueue = queueService.findQueueById(tenantId, savedQueue.getId());

        Assert.assertEquals(savedQueue, foundQueue);
    }


    @Test
    public void testFindQueueById() {
        @NotNull Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("Test");
        queue.setTopic("tb_rule_engine.test");
        queue.setPollInterval(25);
        queue.setPartitions(1);
        queue.setPackProcessingTimeout(2000);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queue.setProcessingStrategy(createTestProcessingStrategy());
        Queue savedQueue = queueService.saveQueue(queue);
        Queue foundQueue = queueService.findQueueById(tenantId, savedQueue.getId());
        Assert.assertNotNull(foundQueue);
        Assert.assertEquals(savedQueue, foundQueue);
    }

    @Test
    public void testDeleteQueue() {
        @NotNull Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("Test");
        queue.setTopic("tb_rule_engine.test");
        queue.setPollInterval(25);
        queue.setPartitions(1);
        queue.setPackProcessingTimeout(2000);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queue.setProcessingStrategy(createTestProcessingStrategy());
        Queue savedQueue = queueService.saveQueue(queue);
        Queue foundQueue = queueService.findQueueById(tenantId, savedQueue.getId());
        Assert.assertNotNull(foundQueue);
        queueService.deleteQueue(tenantId, savedQueue.getId());
        foundQueue = queueService.findQueueById(tenantId, savedQueue.getId());
        Assert.assertNull(foundQueue);
    }

    @Test
    public void testFindQueueByTenantIdAndName() {
        @NotNull Queue queue = new Queue();
        queue.setTenantId(tenantId);
        queue.setName("Test");
        queue.setTopic("tb_rule_engine.test");
        queue.setPollInterval(25);
        queue.setPartitions(1);
        queue.setPackProcessingTimeout(2000);
        queue.setSubmitStrategy(createTestSubmitStrategy());
        queue.setProcessingStrategy(createTestProcessingStrategy());
        Queue savedQueue = queueService.saveQueue(queue);
        Queue foundQueue = queueService.findQueueByTenantIdAndName(tenantId, savedQueue.getName());

        Assert.assertNotNull(foundQueue);
        Assert.assertEquals(savedQueue, foundQueue);
    }

    @Test
    public void testFindQueuesByTenantId() {
        @NotNull List<Queue> queues = new ArrayList<>();
        for (int i = 1; i < 10; i++) {
            @NotNull Queue queue = new Queue();
            queue.setTenantId(tenantId);
            queue.setName("Test" + i);
            queue.setTopic("tb_rule_engine.test" + i);
            queue.setPollInterval(25);
            queue.setPartitions(1);
            queue.setPackProcessingTimeout(2000);
            queue.setSubmitStrategy(createTestSubmitStrategy());
            queue.setProcessingStrategy(createTestProcessingStrategy());

            queues.add(queueService.saveQueue(queue));
        }

        @NotNull List<Queue> loadedQueues = new ArrayList<>();
        PageLink pageLink = new PageLink(3);
        @Nullable PageData<Queue> pageData = null;
        do {
            pageData = queueService.findQueuesByTenantId(tenantId, pageLink);
            loadedQueues.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        for (int i = 0; i < loadedQueues.size(); i++) {
            Queue queue = loadedQueues.get(i);
            if (queue.getName().equals(DataConstants.MAIN_QUEUE_NAME)) {
                loadedQueues.remove(queue);
                break;
            }
        }

        Collections.sort(queues, idComparator);
        Collections.sort(loadedQueues, idComparator);

        Assert.assertEquals(queues, loadedQueues);

        queueService.deleteQueuesByTenantId(tenantId);

        pageLink = new PageLink(33);
        pageData = queueService.findQueuesByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());
    }

}
