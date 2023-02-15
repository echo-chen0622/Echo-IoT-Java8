package org.thingsboard.server.coapserver;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.api_enabled:true}'=='true' && '${transport.coap.enabled}'=='true')")
public @interface TbCoapServerComponent {
}
