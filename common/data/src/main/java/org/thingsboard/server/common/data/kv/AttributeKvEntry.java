package org.thingsboard.server.common.data.kv;

/**
 * @author Andrew Shvayka
 */
public interface AttributeKvEntry extends KvEntry {

    long getLastUpdateTs();

}
