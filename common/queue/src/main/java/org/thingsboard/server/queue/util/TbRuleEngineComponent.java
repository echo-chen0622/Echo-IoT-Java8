package org.thingsboard.server.queue.util;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@ConditionalOnExpression("'${service.type:null}'=='monolith' || '${service.type:null}'=='tb-rule-engine'")
public @interface TbRuleEngineComponent {
}
