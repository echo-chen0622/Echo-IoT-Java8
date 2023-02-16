package org.echoiot.server.common.data.kv;

public interface TsKvQuery {

    int getId();

    String getKey();

    long getStartTs();

    long getEndTs();

}
