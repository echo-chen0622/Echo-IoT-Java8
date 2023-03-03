package org.echoiot.server.common.data.plugin;

import java.io.Serializable;

/**
 * @author Echo
 */
public enum ComponentLifecycleEvent implements Serializable {
    CREATED, STARTED, ACTIVATED, SUSPENDED, UPDATED, STOPPED, DELETED
}
