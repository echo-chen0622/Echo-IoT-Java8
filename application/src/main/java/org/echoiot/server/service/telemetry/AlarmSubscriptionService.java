package org.echoiot.server.service.telemetry;

import org.echoiot.server.queue.discovery.event.PartitionChangeEvent;
import org.springframework.context.ApplicationListener;
import org.thingsboard.rule.engine.api.RuleEngineAlarmService;

/**
 * Created by Echo on 27.03.18.
 */
public interface AlarmSubscriptionService extends RuleEngineAlarmService, ApplicationListener<PartitionChangeEvent> {

}
