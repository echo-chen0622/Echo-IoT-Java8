package org.thingsboard.server.common.data.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "eventType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = RuleNodeDebugEventFilter.class, name = "DEBUG_RULE_NODE"),
        @JsonSubTypes.Type(value = RuleChainDebugEventFilter.class, name = "DEBUG_RULE_CHAIN"),
        @JsonSubTypes.Type(value = ErrorEventFilter.class, name = "ERROR"),
        @JsonSubTypes.Type(value = LifeCycleEventFilter.class, name = "LC_EVENT"),
        @JsonSubTypes.Type(value = StatisticsEventFilter.class, name = "STATS")
})
public interface EventFilter {

    @ApiModelProperty(position = 1, required = true, value = "String value representing the event type", example = "STATS")
    EventType getEventType();

    boolean isNotEmpty();

}
