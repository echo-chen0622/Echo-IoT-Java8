package org.echoiot.script.api.tbel;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.common.util.EchoiotExecutors;
import org.echoiot.script.api.AbstractScriptInvokeService;
import org.echoiot.script.api.ScriptType;
import org.echoiot.script.api.TbScriptException;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.stats.TbApiUsageReportClient;
import org.echoiot.server.common.stats.TbApiUsageStateClient;
import org.jetbrains.annotations.Nullable;
import org.mvel2.*;
import org.mvel2.optimizers.OptimizerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@ConditionalOnProperty(prefix = "tbel", value = "enabled", havingValue = "true", matchIfMissing = true)
@Service
public class DefaultTbelInvokeService extends AbstractScriptInvokeService implements TbelInvokeService {

    protected final Map<UUID, String> scriptIdToHash = new ConcurrentHashMap<>();
    protected final Map<String, TbelScript> scriptMap = new ConcurrentHashMap<>();
    protected Cache<String, Serializable> compiledScriptsCache;

    private SandboxedParserConfiguration parserConfig;

    @Getter
    @Value("${tbel.max_total_args_size:100000}")
    private long maxTotalArgsSize;
    @Getter
    @Value("${tbel.max_result_size:300000}")
    private long maxResultSize;
    @Getter
    @Value("${tbel.max_script_body_size:50000}")
    private long maxScriptBodySize;

    @Getter
    @Value("${tbel.max_errors:3}")
    private int maxErrors;

    @Getter
    @Value("${tbel.max_black_list_duration_sec:60}")
    private int maxBlackListDurationSec;

    @Getter
    @Value("${tbel.max_requests_timeout:0}")
    private long maxInvokeRequestsTimeout;

    @Getter
    @Value("${tbel.stats.enabled:false}")
    private boolean statsEnabled;

    @Value("${tbel.thread_pool_size:50}")
    private int threadPoolSize;

    @Value("${tbel.max_memory_limit_mb:8}")
    private long maxMemoryLimitMb;

    @Value("${tbel.compiled_scripts_cache_size:1000}")
    private int compiledScriptsCacheSize;

    private ListeningExecutorService executor;

    private final Lock lock = new ReentrantLock();

    protected DefaultTbelInvokeService(Optional<TbApiUsageStateClient> apiUsageStateClient, Optional<TbApiUsageReportClient> apiUsageReportClient) {
        super(apiUsageStateClient, apiUsageReportClient);
    }

    @Scheduled(fixedDelayString = "${tbel.stats.print_interval_ms:10000}")
    public void printStats() {
        super.printStats();
    }

    @SneakyThrows
    @PostConstruct
    public void init() {
        super.init();
        OptimizerFactory.setDefaultOptimizer(OptimizerFactory.SAFE_REFLECTIVE);
        parserConfig = ParserContext.enableSandboxedMode();
        parserConfig.addImport("JSON", TbJson.class);
        parserConfig.registerDataType("Date", TbDate.class, date -> 8L);
        parserConfig.registerDataType("Random", Random.class, date -> 8L);
        parserConfig.registerDataType("Calendar", Calendar.class, date -> 8L);
        TbUtils.register(parserConfig);
        executor = MoreExecutors.listeningDecorator(EchoiotExecutors.newWorkStealingPool(threadPoolSize, "tbel-executor"));
        try {
            // Special command to warm up TBEL engine
            Serializable script = compileScript("var warmUp = {}; warmUp");
            MVEL.executeTbExpression(script, new ExecutionContext(parserConfig), Collections.emptyMap());
        } catch (Exception e) {
            // do nothing
        }
        compiledScriptsCache = Caffeine.newBuilder()
                .maximumSize(compiledScriptsCacheSize)
                .build();
    }

    @PreDestroy
    public void destroy() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Override
    protected String getStatsName() {
        return "TBEL Scripts Stats";
    }

    @Override
    protected Executor getCallbackExecutor() {
        return MoreExecutors.directExecutor();
    }

    @Override
    protected boolean isScriptPresent(UUID scriptId) {
        return scriptIdToHash.containsKey(scriptId);
    }

    @Override
    protected ListenableFuture<UUID> doEvalScript(TenantId tenantId, ScriptType scriptType, String scriptBody, UUID scriptId, String[] argNames) {
        return executor.submit(() -> {
            try {
                String scriptHash = hash(scriptBody, argNames);
                compiledScriptsCache.get(scriptHash, k -> compileScript(scriptBody));
                lock.lock();
                try {
                    scriptIdToHash.put(scriptId, scriptHash);
                    scriptMap.computeIfAbsent(scriptHash, k -> new TbelScript(scriptBody, argNames));
                } finally {
                    lock.unlock();
                }
                return scriptId;
            } catch (Exception e) {
                throw new TbScriptException(scriptId, TbScriptException.ErrorCode.COMPILATION, scriptBody, e);
            }
        });
    }

    @Override
    protected TbelScriptExecutionTask doInvokeFunction(UUID scriptId, Object[] args) {
        ExecutionContext executionContext = new ExecutionContext(this.parserConfig, maxMemoryLimitMb * 1024 * 1024);
        return new TbelScriptExecutionTask(executionContext, executor.submit(() -> {
            String scriptHash = scriptIdToHash.get(scriptId);
            if (scriptHash == null) {
                throw new TbScriptException(scriptId, TbScriptException.ErrorCode.OTHER, null, new RuntimeException("Script not found!"));
            }
            TbelScript script = scriptMap.get(scriptHash);
            @Nullable Serializable compiledScript = compiledScriptsCache.get(scriptHash, k -> compileScript(script.getScriptBody()));
            try {
                return MVEL.executeTbExpression(compiledScript, executionContext, script.createVars(args));
            } catch (ScriptMemoryOverflowException e) {
                throw new TbScriptException(scriptId, TbScriptException.ErrorCode.OTHER, script.getScriptBody(), new RuntimeException("Script memory overflow!"));
            } catch (Exception e) {
                throw new TbScriptException(scriptId, TbScriptException.ErrorCode.RUNTIME, script.getScriptBody(), e);
            }
        }));
    }

    @Override
    protected void doRelease(UUID scriptId) {
        String scriptHash = scriptIdToHash.remove(scriptId);
        if (scriptHash != null) {
            lock.lock();
            try {
                if (!scriptIdToHash.containsValue(scriptHash)) {
                    scriptMap.remove(scriptHash);
                    compiledScriptsCache.invalidate(scriptHash);
                }
            } finally {
                lock.unlock();
            }
        }
    }

    private Serializable compileScript(String scriptBody) {
        return MVEL.compileExpression(scriptBody, new ParserContext());
    }

    @SuppressWarnings("UnstableApiUsage")
    protected String hash(String scriptBody, String[] argNames) {
        Hasher hasher = Hashing.murmur3_128().newHasher();
        hasher.putUnencodedChars(scriptBody);
        for (String argName : argNames) {
            hasher.putString(argName, StandardCharsets.UTF_8);
        }
        return hasher.hash().toString();
    }

}
