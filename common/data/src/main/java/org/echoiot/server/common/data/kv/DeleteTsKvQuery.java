package org.echoiot.server.common.data.kv;

public interface DeleteTsKvQuery extends TsKvQuery {

    Boolean getRewriteLatestIfDeleted();

}
