package org.echoiot.server.transport.lwm2m.server.store;

import org.echoiot.server.transport.lwm2m.secure.TbLwM2MSecurityInfo;
import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;

public interface TbMainSecurityStore extends TbSecurityStore {

    void putX509(TbLwM2MSecurityInfo tbSecurityInfo) throws NonUniqueSecurityInfoException;

    void registerX509(String endpoint, String registrationId);

    void remove(String endpoint, String registrationId);

}
