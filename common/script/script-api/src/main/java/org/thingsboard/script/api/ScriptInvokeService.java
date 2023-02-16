package org.thingsboard.script.api;

import com.google.common.util.concurrent.ListenableFuture;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.script.ScriptLanguage;

import java.util.UUID;

public interface ScriptInvokeService {

    ListenableFuture<UUID> eval(TenantId tenantId, ScriptType scriptType, String scriptBody, String... argNames);

    ListenableFuture<Object> invokeScript(TenantId tenantId, CustomerId customerId, UUID scriptId, Object... args);

    ListenableFuture<Void> release(UUID scriptId);

    ScriptLanguage getLanguage();

}
