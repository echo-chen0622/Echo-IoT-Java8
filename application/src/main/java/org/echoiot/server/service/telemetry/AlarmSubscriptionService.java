package org.echoiot.server.service.telemetry;

import org.echoiot.rule.engine.api.RuleEngineAlarmService;
import org.echoiot.server.queue.discovery.event.PartitionChangeEvent;
import org.springframework.context.ApplicationListener;

/**
 * Created by Echo on 27.03.18.
 */
public interface AlarmSubscriptionService extends RuleEngineAlarmService, ApplicationListener<PartitionChangeEvent> {

}
