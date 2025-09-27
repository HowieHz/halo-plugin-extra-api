package top.howiehz.halo.plugin.extra.api.service.js;

import com.caoccao.javet.exceptions.JavetException;
import com.caoccao.javet.interop.V8Host;
import com.caoccao.javet.interop.V8Runtime;
import com.caoccao.javet.interop.engine.JavetEngine;
import com.caoccao.javet.interop.engine.JavetEngineConfig;
import com.caoccao.javet.interop.engine.JavetEnginePool;

public class CustomJavetEnginePool extends JavetEnginePool<V8Runtime> {

    public CustomJavetEnginePool() {
        super();
    }

    public CustomJavetEnginePool(JavetEngineConfig config) {
        super(config);
    }

    @Override
    protected JavetEngine<V8Runtime> createEngine() throws JavetException {
        // 使用 NodeRuntime 创建 Node.js 环境
        V8Runtime v8Runtime = V8Host.getNodeInstance().createV8Runtime();
        v8Runtime.allowEval(config.isAllowEval());
        v8Runtime.setLogger(config.getJavetLogger());

        return new CustomJavetEngine(this, v8Runtime);
    }
}