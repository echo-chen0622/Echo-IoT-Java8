package org.echoiot.server.common.data.security.event;

import java.io.Serializable;

public abstract class UserAuthDataChangedEvent implements Serializable {
    public abstract String getId();
    public abstract long getTs();
}
