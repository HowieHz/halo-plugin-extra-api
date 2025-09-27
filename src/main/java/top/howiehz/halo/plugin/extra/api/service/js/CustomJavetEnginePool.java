package top.howiehz.halo.plugin.extra.api.service.js;

import com.caoccao.javet.exceptions.JavetException;
import com.caoccao.javet.interop.V8Host;
import com.caoccao.javet.interop.V8Runtime;
import com.caoccao.javet.interop.engine.JavetEngine;
import com.caoccao.javet.interop.engine.JavetEngineConfig;
import com.caoccao.javet.interop.engine.JavetEnginePool;

/**
 * Custom engine pool that creates Node-based V8Runtime and wraps it in CustomJavetEngine.
 * 自定义引擎池：基于 Node 创建 V8Runtime 并使用 CustomJavetEngine 封装。
 */
public class CustomJavetEnginePool extends JavetEnginePool<V8Runtime> {

    public CustomJavetEnginePool() {
        super();
    }

    public CustomJavetEnginePool(JavetEngineConfig config) {
        super(config);
    }

    /**
     * Create a new Javet engine wrapping a Node V8Runtime instance.
     * 创建并返回一个新 JavetEngine，使用 Node 的 V8Runtime。
     *
     * @return a new JavetEngine wrapped around the created V8Runtime / 包装 V8Runtime 的 JavetEngine 实例
     * @throws JavetException when runtime creation fails / 运行时创建失败时抛出
     */
    @Override
    protected JavetEngine<V8Runtime> createEngine() throws JavetException {
        // 使用 NodeRuntime 创建 Node.js 环境
        V8Runtime v8Runtime = V8Host.getNodeInstance().createV8Runtime();
        v8Runtime.allowEval(config.isAllowEval());
        v8Runtime.setLogger(config.getJavetLogger());

        return new CustomJavetEngine(this, v8Runtime);
    }
}