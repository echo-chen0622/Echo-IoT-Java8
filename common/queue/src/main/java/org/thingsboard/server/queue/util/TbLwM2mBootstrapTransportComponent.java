package org.thingsboard.server.queue.util;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@ConditionalOnExpression("('${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.api_enabled:true}'=='true' && '${transport.lwm2m.enabled}'=='true')) && '${transport.lwm2m.bootstrap.enabled:false}'=='true'")
public @interface TbLwM2mBootstrapTransportComponent {
}
