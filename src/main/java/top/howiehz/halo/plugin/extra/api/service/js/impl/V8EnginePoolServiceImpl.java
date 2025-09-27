package top.howiehz.halo.plugin.extra.api.service.js.impl;

import com.caoccao.javet.exceptions.JavetException;
import com.caoccao.javet.interop.V8Runtime;
import com.caoccao.javet.interop.engine.IJavetEngine;
import com.caoccao.javet.interop.engine.IJavetEnginePool;
import com.caoccao.javet.interop.engine.JavetEngineConfig;
import com.caoccao.javet.interop.engine.JavetEnginePool;
import com.caoccao.javet.values.V8Value;
import com.caoccao.javet.values.primitive.V8ValueString;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import top.howiehz.halo.plugin.extra.api.service.js.CustomJavetEnginePool;
import top.howiehz.halo.plugin.extra.api.service.js.V8EnginePoolService;

/**
 * V8 引擎池实现
 */
@Service
public class V8EnginePoolServiceImpl implements V8EnginePoolService, InitializingBean, DisposableBean {

    private IJavetEnginePool<V8Runtime> enginePool;
    private volatile boolean initialized = false;

    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            JavetEngineConfig config = new JavetEngineConfig();
            config.setPoolMinSize(2);
            config.setPoolMaxSize(Runtime.getRuntime().availableProcessors());

            // 使用自定义引擎池，预加载 Shiki
            enginePool = new CustomJavetEnginePool(config);

            initialized = true;
            System.out.println("Custom V8 engine pool with preloaded modules initialized successfully");
        } catch (Exception e) {
            System.err.println("Failed to initialize custom engine pool: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void destroy() throws Exception {
        if (enginePool != null) {
            try {
                enginePool.close();
                System.out.println("V8 engine pool closed successfully");
            } catch (Exception e) {
                System.err.println("Failed to close engine pool: " + e.getMessage());
            }
        }
    }

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

    @Override
    public <T> T withEngine(EngineOperation<T> operation) throws JavetException {
        if (!initialized) {
            throw new IllegalStateException("Engine pool not initialized");
        }

        try (IJavetEngine<V8Runtime> engine = enginePool.getEngine()) {
            return operation.execute(engine.getV8Runtime());
        }
    }

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