package top.howiehz.halo.plugin.extra.api.service.js.runtime.engine.impl;

import com.caoccao.javet.exceptions.JavetException;
import com.caoccao.javet.interop.V8Runtime;
import com.caoccao.javet.interop.engine.IJavetEngine;
import com.caoccao.javet.interop.engine.IJavetEnginePool;
import com.caoccao.javet.interop.engine.JavetEngineConfig;
import com.caoccao.javet.interop.engine.JavetEnginePool;
import com.caoccao.javet.values.V8Value;
import com.caoccao.javet.values.primitive.V8ValueString;
import com.google.common.base.Throwables;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import top.howiehz.halo.plugin.extra.api.service.js.runtime.engine.CustomJavetEnginePool;
import top.howiehz.halo.plugin.extra.api.service.js.runtime.engine.V8EnginePoolService;

/**
 * V8 engine pool service implementation.
 * V8 引擎池服务实现类，负责初始化/关闭引擎池并提供脚本执行入口。
 */
@Slf4j
@Service
public class V8EnginePoolServiceImpl
    implements V8EnginePoolService, InitializingBean, DisposableBean {

    private IJavetEnginePool<V8Runtime> enginePool;
    private volatile boolean initialized = false;

    /**
     * Initialize the custom engine pool and preload modules.
     * 初始化引擎池并预加载必要的模块（如 Shiki）。
     *
     * @throws Exception when initialization fails / 初始化失败时抛出
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            JavetEngineConfig config = new JavetEngineConfig();
            config.setPoolMinSize(2);
            config.setPoolMaxSize(Runtime.getRuntime().availableProcessors());

            // 使用自定义引擎池，预加载模块
            enginePool = new CustomJavetEnginePool(config);

            initialized = true;
            log.info("Custom V8 engine pool with preloaded modules initialized successfully");
        } catch (Exception e) {
            log.info("Failed to initialize custom engine pool: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Destroy the engine pool and release resources.
     * 销毁引擎池并释放资源。
     *
     * @throws Exception when destruction fails / 销毁失败时抛出
     */
    @Override
    public void destroy() throws Exception {
        if (enginePool != null) {
            try {
                enginePool.close();
                log.info("V8 engine pool closed successfully");
            } catch (Exception e) {
                log.error("Failed to close engine pool:", Throwables.getRootCause(e));
            }
        }
    }

    /**
     * Execute JavaScript code using an engine from the pool.
     * 使用池中的引擎执行 JavaScript 代码并按指定类型返回结果。
     *
     * @param script the JS script to execute / 要执行的 JS 脚本
     * @param returnType expected return type class / 期望的返回类型
     * @param <T> generic return type / 泛型返回类型
     * @return execution result cast to returnType / 转换后的执行结果
     * @throws JavetException if execution fails / 执行失败时抛出
     */
    @Override
    public <T> T executeScript(String script, Class<T> returnType) throws JavetException {
        return withEngine(runtime -> {
            try {
                if (returnType == String.class) {
                    return returnType.cast(runtime.getExecutor(script).executeString());
                } else if (returnType == String[].class) {
                    try (com.caoccao.javet.values.reference.V8ValueArray v8ValueArray =
                             runtime.getExecutor(script).execute()) {
                        if (v8ValueArray == null || v8ValueArray.isNullOrUndefined()) {
                            return returnType.cast(new String[0]);
                        }
                        int len = v8ValueArray.getLength();
                        String[] arr = new String[len];
                        for (int i = 0; i < len; i++) {
                            try (V8ValueString v =
                                     v8ValueArray.get(i)) {
                                arr[i] = (v == null || v.isNullOrUndefined()) ? null : v.getValue();
                            }
                        }
                        return returnType.cast(arr);
                    }
                } else if (returnType == java.util.Set.class) {
                    try (com.caoccao.javet.values.reference.V8ValueSet v8ValueSet =
                             runtime.getExecutor(script).execute()) {
                        if (v8ValueSet == null || v8ValueSet.isNullOrUndefined()) {
                            return returnType.cast(java.util.Collections.emptySet());
                        }
                        java.util.Set<String> set = new java.util.LinkedHashSet<>();
                        v8ValueSet.forEach((V8ValueString value) -> {
                            set.add(value.getValue());
                        });
                        return returnType.cast(set);
                    }
                } else if (returnType == java.util.Map.class) {
                    try (com.caoccao.javet.values.reference.V8ValueMap v8ValueMap =
                             runtime.getExecutor(script).execute()) {
                        if (v8ValueMap == null || v8ValueMap.isNullOrUndefined()) {
                            return returnType.cast(java.util.Collections.emptyMap());
                        }
                        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
                        v8ValueMap.forEach((V8ValueString key,
                            V8Value value) -> map.put(key.getValue(), runtime.toObject(value)));
                        return returnType.cast(map);
                    }
                } else if (returnType == Integer.class) {
                    return returnType.cast(runtime.getExecutor(script).executeInteger());
                } else if (returnType == Boolean.class) {
                    return returnType.cast(runtime.getExecutor(script).executeBoolean());
                } else {
                    return runtime.getExecutor(script).executeObject();
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to execute script: " + script, e);
            }
        });
    }

    /**
     * Acquire an engine and perform the provided operation.
     * 获取一个引擎并执行传入的操作，操作完成后自动归还引擎。
     *
     * @param operation the operation to perform with the runtime / 使用 runtime 执行的操作
     * @param <T> result type / 结果类型
     * @return operation result / 操作结果
     * @throws JavetException if engine acquisition or operation fails / 获取引擎或执行失败时抛出
     */
    @Override
    public <T> T withEngine(EngineOperation<T> operation) throws JavetException {
        if (!initialized) {
            throw new IllegalStateException("Engine pool not initialized");
        }

        // Diagnostic logging: record pool stats before/after acquisition and after release
        String threadInfo =
            Thread.currentThread().getName() + "[" + Thread.currentThread().getId() + "]";
        if (enginePool instanceof JavetEnginePool<V8Runtime> poolBefore) {
            log.debug("[V8 POOL] {} requesting engine - before: active={}, idle={}", threadInfo,
                poolBefore.getActiveEngineCount(), poolBefore.getIdleEngineCount());
        }

        try (IJavetEngine<V8Runtime> engine = enginePool.getEngine()) {
            if (enginePool instanceof JavetEnginePool<V8Runtime> poolAcquired) {
                log.debug("[V8 POOL] {} acquired engine - during: active={}, idle={}", threadInfo,
                    poolAcquired.getActiveEngineCount(), poolAcquired.getIdleEngineCount());
            }

            return operation.execute(engine.getV8Runtime());
        } finally {
            if (enginePool instanceof JavetEnginePool<V8Runtime> poolAfter) {
                // Resource closed by try-with-resources before finally runs
                log.debug("[V8 POOL] {} released engine - after: active={}, idle={}", threadInfo,
                    poolAfter.getActiveEngineCount(), poolAfter.getIdleEngineCount());
            }
        }
    }

    /**
     * Get statistics of the engine pool.
     * 获取引擎池状态统计信息（最小/最大/活跃/空闲）。
     *
     * @return pool statistics / 池状态
     */
    @Override
    public PoolStats getPoolStats() {
        if (enginePool instanceof JavetEnginePool<V8Runtime> pool) {
            return new PoolStats(
                pool.getConfig().getPoolMinSize(),
                pool.getConfig().getPoolMaxSize(),
                pool.getActiveEngineCount(),
                pool.getIdleEngineCount()
            );
        }
        return new PoolStats(0, 0, 0, 0);
    }
}