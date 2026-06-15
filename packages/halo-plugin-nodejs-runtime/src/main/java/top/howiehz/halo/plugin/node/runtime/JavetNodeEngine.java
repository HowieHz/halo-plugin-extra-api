package top.howiehz.halo.plugin.node.runtime;

import com.caoccao.javet.exceptions.JavetException;
import com.caoccao.javet.interception.logging.JavetStandardConsoleInterceptor;
import com.caoccao.javet.interop.V8Runtime;
import com.caoccao.javet.interop.engine.IJavetEnginePool;
import com.caoccao.javet.interop.engine.JavetEngine;
import java.util.HashSet;
import java.util.Set;

class JavetNodeEngine extends JavetEngine<V8Runtime> {

    private final Set<String> loadedModuleIds = new HashSet<>();
    private JavetStandardConsoleInterceptor consoleInterceptor;

    JavetNodeEngine(IJavetEnginePool<V8Runtime> enginePool, V8Runtime v8Runtime)
        throws JavetException {
        super(enginePool, v8Runtime);
        consoleInterceptor = new JavetStandardConsoleInterceptor(v8Runtime);
        consoleInterceptor.register(v8Runtime.getGlobalObject());
        v8Runtime.setPromiseRejectCallback((event, promise, value) -> {
            try {
                getConfig().getJavetLogger().logError("Promise rejected: " + value);
            } catch (Exception e) {
                getConfig().getJavetLogger()
                    .logError("Error handling promise rejection: " + e.getMessage());
            }
        });
        v8Runtime.getExecutor("globalThis.__nodeRuntimeModules = Object.create(null);")
            .executeVoid();
    }

    boolean isModuleLoaded(String moduleId) {
        return loadedModuleIds.contains(moduleId);
    }

    void markModuleLoaded(String moduleId) {
        loadedModuleIds.add(moduleId);
    }

    @Override
    protected void close(boolean forceClose) throws JavetException {
        if (forceClose && consoleInterceptor != null) {
            consoleInterceptor.unregister(v8Runtime.getGlobalObject());
            v8Runtime.lowMemoryNotification();
            consoleInterceptor = null;
        }
        super.close(forceClose);
    }
}
