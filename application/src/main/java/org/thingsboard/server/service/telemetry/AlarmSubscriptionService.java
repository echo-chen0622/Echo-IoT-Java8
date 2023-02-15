package org.thingsboard.server.service.telemetry;

import org.springframework.context.ApplicationListener;
import org.thingsboard.rule.engine.api.RuleEngineAlarmService;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;

/**
 * Created by Echo on 27.03.18.
 */
public interface AlarmSubscriptionService extends RuleEngineAlarmService, ApplicationListener<PartitionChangeEvent> {

}
