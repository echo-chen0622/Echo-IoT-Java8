package org.thingsboard.script.api.js;

import com.google.common.hash.Hashing;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.thingsboard.script.api.AbstractScriptInvokeService;
import org.thingsboard.script.api.RuleNodeScriptFactory;
import org.thingsboard.script.api.ScriptType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.common.stats.TbApiUsageStateClient;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Echo on 26.09.18.
 */
@Slf4j
public abstract class AbstractJsInvokeService extends AbstractScriptInvokeService implements JsInvokeService {

    protected final Map<UUID, JsScriptInfo> scriptInfoMap = new ConcurrentHashMap<>();

    @Getter
    @Value("${js.max_total_args_size:100000}")
    private long maxTotalArgsSize;
    @Getter
    @Value("${js.max_result_size:300000}")
    private long maxResultSize;
    @Getter
    @Value("${js.max_script_body_size:50000}")
    private long maxScriptBodySize;

    protected AbstractJsInvokeService(Optional<TbApiUsageStateClient> apiUsageStateClient, Optional<TbApiUsageReportClient> apiUsageReportClient) {
        super(apiUsageStateClient, apiUsageReportClient);
    }

    @Override
    protected boolean isScriptPresent(UUID scriptId) {
        return scriptInfoMap.containsKey(scriptId);
    }

    @Override
    protected JsScriptExecutionTask doInvokeFunction(UUID scriptId, Object[] args) {
        return new JsScriptExecutionTask(doInvokeFunction(scriptId, scriptInfoMap.get(scriptId), args));
    }

    @Override
    protected ListenableFuture<UUID> doEvalScript(TenantId tenantId, ScriptType scriptType, String scriptBody, UUID scriptId, String[] argNames) {
        String scriptHash = hash(tenantId, scriptBody);
        String functionName = constructFunctionName(scriptId, scriptHash);
        String jsScript = generateJsScript(scriptType, functionName, scriptBody, argNames);
        return doEval(scriptId, new JsScriptInfo(scriptHash, functionName), jsScript);
    }

    @Override
    protected void doRelease(UUID scriptId) throws Exception {
        doRelease(scriptId, scriptInfoMap.remove(scriptId));
    }

    protected abstract ListenableFuture<UUID> doEval(UUID scriptId, JsScriptInfo jsInfo, String scriptBody);

    protected abstract ListenableFuture<Object> doInvokeFunction(UUID scriptId, JsScriptInfo jsInfo, Object[] args);

    protected abstract void doRelease(UUID scriptId, JsScriptInfo scriptInfo) throws Exception;

    private String generateJsScript(ScriptType scriptType, String functionName, String scriptBody, String... argNames) {
        if (scriptType == ScriptType.RULE_NODE_SCRIPT) {
            return RuleNodeScriptFactory.generateRuleNodeScript(functionName, scriptBody, argNames);
        }
        throw new RuntimeException("No script factory implemented for scriptType: " + scriptType);
    }

    protected String constructFunctionName(UUID scriptId, String scriptHash) {
        return "invokeInternal_" + scriptId.toString().replace('-', '_');
    }

    protected String hash(TenantId tenantId, String scriptBody) {
        return Hashing.murmur3_128().newHasher()
                .putLong(tenantId.getId().getMostSignificantBits())
                .putLong(tenantId.getId().getLeastSignificantBits())
                .putUnencodedChars(scriptBody)
                .hash().toString();
    }

}
