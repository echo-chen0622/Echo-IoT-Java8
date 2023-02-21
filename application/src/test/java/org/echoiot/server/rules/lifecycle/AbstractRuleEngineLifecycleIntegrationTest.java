package org.echoiot.server.rules.lifecycle;

import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.echoiot.rule.engine.metadata.TbGetAttributesNodeConfiguration;
import org.echoiot.server.actors.ActorSystemContext;
import org.echoiot.server.common.data.DataConstants;
import org.echoiot.server.common.data.Device;
import org.echoiot.server.common.data.EventInfo;
import org.echoiot.server.common.data.event.EventType;
import org.echoiot.server.common.data.kv.BaseAttributeKvEntry;
import org.echoiot.server.common.data.kv.StringDataEntry;
import org.echoiot.server.common.data.rule.RuleChain;
import org.echoiot.server.common.data.rule.RuleChainMetaData;
import org.echoiot.server.common.data.rule.RuleNode;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.TbMsgMetaData;
import org.echoiot.server.common.msg.queue.QueueToRuleEngineMsg;
import org.echoiot.server.common.msg.queue.TbMsgCallback;
import org.echoiot.server.controller.AbstractRuleEngineControllerTest;
import org.echoiot.server.dao.attributes.AttributesService;
import org.echoiot.server.dao.event.EventService;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @author Valerii Sosliuk
 */
@Slf4j
public abstract class AbstractRuleEngineLifecycleIntegrationTest extends AbstractRuleEngineControllerTest {

    @Resource
    protected ActorSystemContext actorSystem;

    @Resource
    protected AttributesService attributesService;

    @Resource
    protected EventService eventService;

    @Before
    public void beforeTest() throws Exception {
        loginTenantAdmin();
        ruleChainService.deleteRuleChainsByTenantId(tenantId);
    }

    @After
    public void afterTest() throws Exception {
    }

    @Test
    public void testRuleChainWithOneRule() throws Exception {
        // Creating Rule Chain
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName("Simple Rule Chain");
        ruleChain.setTenantId(tenantId);
        ruleChain.setRoot(true);
        ruleChain.setDebugMode(true);
        ruleChain = saveRuleChain(ruleChain);
        Assert.assertNull(ruleChain.getFirstRuleNodeId());

        RuleChainMetaData metaData = new RuleChainMetaData();
        metaData.setRuleChainId(ruleChain.getId());

        @NotNull RuleNode ruleNode = new RuleNode();
        ruleNode.setName("Simple Rule Node");
        ruleNode.setType(org.echoiot.rule.engine.metadata.TbGetAttributesNode.class.getName());
        ruleNode.setDebugMode(true);
        @NotNull TbGetAttributesNodeConfiguration configuration = new TbGetAttributesNodeConfiguration();
        configuration.setServerAttributeNames(Collections.singletonList("serverAttributeKey"));
        ruleNode.setConfiguration(mapper.valueToTree(configuration));

        metaData.setNodes(Collections.singletonList(ruleNode));
        metaData.setFirstNodeIndex(0);

        metaData = saveRuleChainMetaData(metaData);
        Assert.assertNotNull(metaData);

        final RuleChain ruleChainFinal = getRuleChain(ruleChain.getId());
        Assert.assertNotNull(ruleChainFinal.getFirstRuleNodeId());

        //TODO find out why RULE_NODE update event did not appear all the time
        List<EventInfo> rcEvents = Awaitility.await("Rule Node started successfully")
                .pollInterval(10, MILLISECONDS)
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> {
                            @NotNull List<EventInfo> debugEvents = getEvents(tenantId, ruleChainFinal.getFirstRuleNodeId(), EventType.LC_EVENT.getOldName(), 1000)
                                    .getData().stream().filter(e -> {
                                        var body = e.getBody();
                                        return body.has("event") && body.get("event").asText().equals("STARTED")
                                                && body.has("success") && body.get("success").asBoolean();
                                    }).collect(Collectors.toList());
                            debugEvents.forEach((e) -> log.trace("event: {}", e));
                            return debugEvents;
                        },
                        x -> x.size() == 1);

        // Saving the device
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        device = doPost("/api/device", device, Device.class);

        log.warn("before update attr");
        attributesService.save(device.getTenantId(), device.getId(), DataConstants.SERVER_SCOPE,
                Collections.singletonList(new BaseAttributeKvEntry(new StringDataEntry("serverAttributeKey", "serverAttributeValue"), System.currentTimeMillis())))
                .get(TIMEOUT, TimeUnit.SECONDS);
        log.warn("attr updated");
        TbMsgCallback tbMsgCallback = Mockito.mock(TbMsgCallback.class);
        Mockito.when(tbMsgCallback.isMsgValid()).thenReturn(true);
        @NotNull TbMsg tbMsg = TbMsg.newMsg("CUSTOM", device.getId(), new TbMsgMetaData(), "{}", tbMsgCallback);
        @NotNull QueueToRuleEngineMsg qMsg = new QueueToRuleEngineMsg(tenantId, tbMsg, null, null);
        // Pushing Message to the system
        log.warn("before tell tbMsgCallback");
        actorSystem.tell(qMsg);
        log.warn("awaiting tbMsgCallback");
        Mockito.verify(tbMsgCallback, Mockito.timeout(TimeUnit.SECONDS.toMillis(TIMEOUT))).onSuccess();
        log.warn("awaiting events");
        List<EventInfo> events = Awaitility.await("get debug by custom event")
                .pollInterval(10, MILLISECONDS)
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> {
                            @NotNull List<EventInfo> debugEvents = getDebugEvents(tenantId, ruleChainFinal.getFirstRuleNodeId(), 1000)
                                    .getData().stream().filter(filterByCustomEvent()).collect(Collectors.toList());
                            log.warn("filtered debug events [{}]", debugEvents.size());
                            debugEvents.forEach((e) -> log.warn("event: {}", e));
                            return debugEvents;
                        },
                        x -> x.size() == 2);
        log.warn("asserting..");

        @NotNull EventInfo inEvent = events.stream().filter(e -> e.getBody().get("type").asText().equals(DataConstants.IN)).findFirst().get();
        Assert.assertEquals(ruleChainFinal.getFirstRuleNodeId(), inEvent.getEntityId());
        Assert.assertEquals(device.getId().getId().toString(), inEvent.getBody().get("entityId").asText());

        @NotNull EventInfo outEvent = events.stream().filter(e -> e.getBody().get("type").asText().equals(DataConstants.OUT)).findFirst().get();
        Assert.assertEquals(ruleChainFinal.getFirstRuleNodeId(), outEvent.getEntityId());
        Assert.assertEquals(device.getId().getId().toString(), outEvent.getBody().get("entityId").asText());

        log.warn("OUT event {}", outEvent);
        log.warn("OUT event metadata {}", getMetadata(outEvent));

        Assert.assertNotNull("metadata has ss_serverAttributeKey", getMetadata(outEvent).get("ss_serverAttributeKey"));
        Assert.assertEquals("serverAttributeValue", getMetadata(outEvent).get("ss_serverAttributeKey").asText());
    }

}
