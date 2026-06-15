package top.howiehz.halo.plugin.node.runtime;

import com.caoccao.javet.exceptions.JavetException;
import com.caoccao.javet.interop.V8Host;
import com.caoccao.javet.interop.V8Runtime;
import com.caoccao.javet.interop.engine.JavetEngine;
import com.caoccao.javet.interop.engine.JavetEngineConfig;
import com.caoccao.javet.interop.engine.JavetEnginePool;

class JavetNodeEnginePool extends JavetEnginePool<V8Runtime> {

    JavetNodeEnginePool(JavetEngineConfig config) {
        super(config);
    }

    @Override
    protected JavetEngine<V8Runtime> createEngine() throws JavetException {
        V8Runtime v8Runtime = V8Host.getNodeInstance().createV8Runtime();
        v8Runtime.allowEval(config.isAllowEval());
        v8Runtime.setLogger(config.getJavetLogger());
        return new JavetNodeEngine(this, v8Runtime);
    }
}
