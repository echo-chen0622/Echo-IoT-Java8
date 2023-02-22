package org.echoiot.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.echoiot.server.common.data.EventInfo;
import org.echoiot.server.common.data.event.EventType;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.RuleChainId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.TimePageLink;
import org.echoiot.server.common.data.rule.RuleChain;
import org.echoiot.server.common.data.rule.RuleChainMetaData;
import org.echoiot.server.dao.rule.RuleChainService;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.function.Predicate;

/**
 * Created by Echo on 20.03.18.
 */
@TestPropertySource(properties = {
        "js.evaluator=mock",
})
public abstract class AbstractRuleEngineControllerTest extends AbstractControllerTest {

    @Resource
    protected RuleChainService ruleChainService;

    protected RuleChain saveRuleChain(RuleChain ruleChain) throws Exception {
        return doPost("/api/ruleChain", ruleChain, RuleChain.class);
    }

    protected RuleChain getRuleChain(RuleChainId ruleChainId) throws Exception {
        return doGet("/api/ruleChain/" + ruleChainId.getId().toString(), RuleChain.class);
    }

    protected RuleChainMetaData saveRuleChainMetaData(RuleChainMetaData ruleChainMD) throws Exception {
        return doPost("/api/ruleChain/metadata", ruleChainMD, RuleChainMetaData.class);
    }

    protected RuleChainMetaData getRuleChainMetaData(RuleChainId ruleChainId) throws Exception {
        return doGet("/api/ruleChain/metadata/" + ruleChainId.getId().toString(), RuleChainMetaData.class);
    }

    protected PageData<EventInfo> getDebugEvents(TenantId tenantId, EntityId entityId, int limit) throws Exception {
        return getEvents(tenantId, entityId, EventType.DEBUG_RULE_NODE.getOldName(), limit);
    }

    protected PageData<EventInfo> getEvents(TenantId tenantId, EntityId entityId, String eventType, int limit) throws Exception {
        TimePageLink pageLink = new TimePageLink(limit);
        return doGetTypedWithTimePageLink("/api/events/{entityType}/{entityId}/{eventType}?tenantId={tenantId}&",
                new TypeReference<PageData<EventInfo>>() {
                }, pageLink, entityId.getEntityType(), entityId.getId(), eventType, tenantId.getId());
    }


    protected JsonNode getMetadata(EventInfo outEvent) {
        String metaDataStr = outEvent.getBody().get("metadata").asText();
        try {
            return mapper.readTree(metaDataStr);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected Predicate<EventInfo> filterByCustomEvent() {
        return event -> event.getBody().get("msgType").textValue().equals("CUSTOM");
    }

}
