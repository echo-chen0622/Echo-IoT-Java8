package org.echoiot.server.transport.lwm2m.server.store;

import org.echoiot.server.transport.lwm2m.secure.TbLwM2MSecurityInfo;
import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;

public interface TbEditableSecurityStore extends TbSecurityStore {

    void put(TbLwM2MSecurityInfo tbSecurityInfo) throws NonUniqueSecurityInfoException;

    void remove(String endpoint);

}
