package top.howiehz.halo.plugin.node.runtime;

import com.caoccao.javet.exceptions.JavetException;
import com.caoccao.javet.interop.V8Runtime;
import com.caoccao.javet.interop.engine.IJavetEngine;
import com.caoccao.javet.interop.engine.JavetEngineConfig;
import com.caoccao.javet.interop.engine.JavetEnginePool;
import com.caoccao.javet.values.reference.V8ValuePromise;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import run.halo.app.plugin.extensionpoint.ExtensionGetter;
import top.howiehz.halo.plugin.node.runtime.api.NodeCall;
import top.howiehz.halo.plugin.node.runtime.api.NodeModuleProvider;
import top.howiehz.halo.plugin.node.runtime.api.NodeRuntime;
import top.howiehz.halo.plugin.node.runtime.api.NodeRuntimeException;
import top.howiehz.halo.plugin.node.runtime.api.NodeRuntimeMaintenance;
import top.howiehz.halo.plugin.node.runtime.api.NodeRuntimeRebuildResult;
import top.howiehz.halo.plugin.node.runtime.api.NodeRuntimeStats;
import top.howiehz.halo.plugin.node.runtime.api.NodeRuntimeValidator;

@Slf4j
@Component
@RequiredArgsConstructor
public class NodeRuntimeService
    implements NodeRuntime, NodeRuntimeMaintenance, InitializingBean, DisposableBean {

    private final NodeRuntimeConfigSupplier configSupplier;
    private final ExtensionGetter extensionGetter;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ReentrantReadWriteLock runtimeLock = new ReentrantReadWriteLock();
    private final AtomicInteger queuedCallCount = new AtomicInteger();
    private volatile JavetNodeEnginePool enginePool;
    private volatile Semaphore concurrency;
    private volatile NodeRuntimeConfig runtimeConfig = new NodeRuntimeConfig();
    private volatile Map<String, RegisteredNodeModule> registry = Map.of();

    @Override
    public void afterPropertiesSet() {
        runtimeConfig = loadConfig();
        enginePool = createPool(runtimeConfig);
        concurrency = new Semaphore(runtimeConfig.normalizedPoolMaxSize());
        log.info("Node runtime initialized with pool min={}, max={}",
            runtimeConfig.normalizedPoolMinSize(), runtimeConfig.normalizedPoolMaxSize());
    }

    @Override
    public Mono<String> call(NodeCall call) {
        return Mono.fromCallable(() -> invoke(call))
            .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<NodeRuntimeStats> stats() {
        return Mono.fromSupplier(() -> {
            JavetNodeEnginePool pool = enginePool;
            if (pool == null) {
                return new NodeRuntimeStats(0, 0, 0, 0, queuedCallCount.get(), registry.size());
            }
            return new NodeRuntimeStats(
                pool.getConfig().getPoolMinSize(),
                pool.getConfig().getPoolMaxSize(),
                pool.getActiveEngineCount(),
                pool.getIdleEngineCount(),
                queuedCallCount.get(),
                registry.size()
            );
        });
    }

    @Override
    public Mono<NodeRuntimeRebuildResult> rebuildRuntime() {
        return Mono.fromCallable(this::doRebuildRuntime)
            .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public void destroy() throws Exception {
        JavetNodeEnginePool pool = enginePool;
        if (pool != null) {
            pool.close();
        }
    }

    private String invoke(NodeCall call) throws InterruptedException {
        validateCall(call);
        queuedCallCount.incrementAndGet();
        concurrency.acquire();
        queuedCallCount.decrementAndGet();
        runtimeLock.readLock().lock();
        try (IJavetEngine<V8Runtime> engine = enginePool.getEngine()) {
            Duration timeout = runtimeConfig.normalizeTimeout(call.timeout());
            Instant deadline = Instant.now().plus(timeout);
            syncRegistryIfNeeded();
            String moduleId = call.module().fullId();
            RegisteredNodeModule module = registry.get(moduleId);
            if (module == null) {
                throw new NodeRuntimeException(moduleId, call.functionName(),
                    "Node module is not registered: " + moduleId, null, null);
            }
            JavetNodeEngine nodeEngine = (JavetNodeEngine) engine;
            ensureModuleLoaded(nodeEngine, module, deadline);
            return invokeFunction(engine.getV8Runtime(), moduleId, call.functionName(),
                call.argsJson(), deadline);
        } catch (NodeRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new NodeRuntimeException(call.module().fullId(), call.functionName(),
                "Node runtime call failed: " + e.getMessage(), null, e);
        } finally {
            runtimeLock.readLock().unlock();
            concurrency.release();
        }
    }

    private NodeRuntimeRebuildResult doRebuildRuntime() throws Exception {
        Instant start = Instant.now();
        runtimeLock.writeLock().lock();
        try {
            JavetNodeEnginePool oldPool = enginePool;
            int closedEngineCount = oldPool == null ? 0
                : oldPool.getActiveEngineCount() + oldPool.getIdleEngineCount();
            runtimeConfig = loadConfig();
            registry = discoverRegistry();
            enginePool = createPool(runtimeConfig);
            concurrency = new Semaphore(runtimeConfig.normalizedPoolMaxSize());
            if (oldPool != null) {
                oldPool.close();
            }
            return new NodeRuntimeRebuildResult(registry.size(), closedEngineCount,
                Duration.between(start, Instant.now()));
        } finally {
            runtimeLock.writeLock().unlock();
        }
    }

    private void validateCall(NodeCall call) {
        if (call == null) {
            throw new IllegalArgumentException("call must not be null");
        }
        if (call.module() == null) {
            throw new IllegalArgumentException("call.module must not be null");
        }
        call.module().fullId();
        NodeRuntimeValidator.requireFunctionName(call.functionName());
        if (call.argsJson() == null || call.argsJson().isBlank()) {
            throw new IllegalArgumentException("argsJson must not be blank");
        }
    }

    private void syncRegistryIfNeeded() {
        Map<String, RegisteredNodeModule> discovered = discoverRegistry();
        if (!discovered.equals(registry)) {
            registry = discovered;
        }
    }

    private Map<String, RegisteredNodeModule> discoverRegistry() {
        Map<String, RegisteredNodeModule> discovered = new LinkedHashMap<>();
        extensionGetter.getExtensions(NodeModuleProvider.class)
            .toStream()
            .forEach(provider -> provider.modules().forEach(module -> {
                String fullId = top.howiehz.halo.plugin.node.runtime.api.NodeModuleRef
                    .of(provider.pluginContext(), module.moduleId())
                    .fullId();
                RegisteredNodeModule registered =
                    new RegisteredNodeModule(fullId, module.source(), module.integrity());
                RegisteredNodeModule existing = discovered.putIfAbsent(fullId, registered);
                if (existing != null && !existing.integrity().equals(module.integrity())) {
                    throw new NodeRuntimeException(fullId, null,
                        "Duplicate node module id with different integrity: " + fullId, null,
                        null);
                }
            }));
        return Map.copyOf(discovered);
    }

    private void ensureModuleLoaded(JavetNodeEngine engine, RegisteredNodeModule module,
        Instant deadline) throws JavetException {
        if (engine.isModuleLoaded(module.fullId())) {
            return;
        }
        String script = """
            (function() {
              const __moduleId = %s;
              const __oldConsole = globalThis.console || {};
              globalThis.console = {
                debug: (...args) => __oldConsole.debug('[node-runtime] [' + __moduleId + ']', ...args),
                log: (...args) => __oldConsole.log('[node-runtime] [' + __moduleId + ']', ...args),
                info: (...args) => __oldConsole.info('[node-runtime] [' + __moduleId + ']', ...args),
                warn: (...args) => __oldConsole.warn('[node-runtime] [' + __moduleId + ']', ...args),
                error: (...args) => __oldConsole.error('[node-runtime] [' + __moduleId + ']', ...args),
              };
              try {
                const module = { exports: {} };
                const exports = module.exports;
                const require = function(name) {
                  throw new Error('require is not supported by nodejs-runtime: ' + name);
                };
                %s
                globalThis.__nodeRuntimeModules[__moduleId] = module.exports;
              } finally {
                globalThis.console = __oldConsole;
              }
            })();
            """.formatted(toJson(module.fullId()), module.source());
        engine.getV8Runtime().getExecutor(script)
            .setResourceName(module.fullId())
            .executeVoid();
        checkDeadline(module.fullId(), null, deadline);
        engine.markModuleLoaded(module.fullId());
    }

    private String invokeFunction(V8Runtime runtime, String moduleId, String functionName,
        String argsJson, Instant deadline) throws JavetException {
        String script = """
            (async function() {
              const __moduleId = %s;
              const __functionName = %s;
              const __oldConsole = globalThis.console || {};
              globalThis.console = {
                debug: (...args) => __oldConsole.debug('[node-runtime] [' + __moduleId + ']', ...args),
                log: (...args) => __oldConsole.log('[node-runtime] [' + __moduleId + ']', ...args),
                info: (...args) => __oldConsole.info('[node-runtime] [' + __moduleId + ']', ...args),
                warn: (...args) => __oldConsole.warn('[node-runtime] [' + __moduleId + ']', ...args),
                error: (...args) => __oldConsole.error('[node-runtime] [' + __moduleId + ']', ...args),
              };
              try {
                const exports = globalThis.__nodeRuntimeModules[__moduleId];
                if (!exports) {
                  throw new Error('Module is not loaded: ' + __moduleId);
                }
                const fn = exports[__functionName];
                if (typeof fn !== 'function') {
                  throw new Error('Export is not a function: ' + __functionName);
                }
                const input = JSON.parse(%s);
                const result = await fn(input);
                const json = JSON.stringify(result);
                if (json === undefined) {
                  throw new Error('Function result is not JSON serializable');
                }
                return json;
              } finally {
                globalThis.console = __oldConsole;
              }
            })()
            """.formatted(toJson(moduleId), toJson(functionName), toJson(argsJson));
        try (V8ValuePromise promise = runtime.getExecutor(script)
            .setResourceName(moduleId + "#" + functionName)
            .execute()) {
            while (promise.isPending()) {
                checkDeadline(moduleId, functionName, deadline);
                runtime.await();
            }
            if (promise.isFulfilled()) {
                return promise.getResultString();
            }
            String message = promise.getResultString();
            throw new NodeRuntimeException(moduleId, functionName, message, message, null);
        }
    }

    private void checkDeadline(String moduleId, String functionName, Instant deadline) {
        if (Instant.now().isAfter(deadline)) {
            throw new NodeRuntimeException(moduleId, functionName,
                "Node runtime call timed out", null, null);
        }
    }

    private String toJson(String value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to encode JSON string", e);
        }
    }

    private NodeRuntimeConfig loadConfig() {
        return configSupplier.get().blockOptional().orElseGet(NodeRuntimeConfig::new);
    }

    private JavetNodeEnginePool createPool(NodeRuntimeConfig config) {
        JavetEngineConfig engineConfig = new JavetEngineConfig();
        engineConfig.setPoolMinSize(config.normalizedPoolMinSize());
        engineConfig.setPoolMaxSize(config.normalizedPoolMaxSize());
        return new JavetNodeEnginePool(engineConfig);
    }
}
