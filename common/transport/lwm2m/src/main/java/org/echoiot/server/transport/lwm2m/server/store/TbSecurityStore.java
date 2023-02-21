package org.echoiot.server.transport.lwm2m.server.store;

import org.echoiot.server.transport.lwm2m.secure.TbLwM2MSecurityInfo;
import org.eclipse.leshan.server.security.SecurityStore;

public interface TbSecurityStore extends SecurityStore {

    TbLwM2MSecurityInfo getTbLwM2MSecurityInfoByEndpoint(String endpoint);

}
