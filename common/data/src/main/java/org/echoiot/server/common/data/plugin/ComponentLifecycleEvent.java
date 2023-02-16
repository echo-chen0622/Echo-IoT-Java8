package org.echoiot.server.common.data.plugin;

import java.io.Serializable;

/**
 * @author Andrew Shvayka
 */
public enum ComponentLifecycleEvent implements Serializable {
    CREATED, STARTED, ACTIVATED, SUSPENDED, UPDATED, STOPPED, DELETED
}
