package top.howiehz.halo.plugin.extra.api.service.js.runtime.engine;

import com.caoccao.javet.exceptions.JavetException;
import com.caoccao.javet.interop.V8Host;
import com.caoccao.javet.interop.V8Runtime;
import com.caoccao.javet.interop.engine.JavetEngine;
import com.caoccao.javet.interop.engine.JavetEngineConfig;
import com.caoccao.javet.interop.engine.JavetEnginePool;
import lombok.extern.slf4j.Slf4j;

/**
 * Custom engine pool that creates Node-based V8Runtime and wraps it in CustomJavetEngine.
 * 自定义引擎池：基于 Node 创建 V8Runtime 并使用 CustomJavetEngine 封装。
 */
@Slf4j
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
     * <p><b>关键</b>: 根据 Javet Issue #124 和官方文档,在插件重新加载场景中,
     * 原生库已经被加载过,但新的 classloader 无法访问已绑定到旧 classloader 的原生方法。
     * 解决方案是让 Javet 的 LibLoadingListener 抑制错误,允许它重用已加载的库。
     * 系统属性 javet.lib.loading.suppress.error=true 已在插件主类的静态初始化块中设置。</p>
     *
     * @return a new JavetEngine wrapped around the created V8Runtime / 包装 V8Runtime 的
     * JavetEngine 实例
     * @throws JavetException when runtime creation fails / 运行时创建失败时抛出
     */
    @Override
    protected JavetEngine<V8Runtime> createEngine() throws JavetException {
        // 直接使用 V8Host.getNodeInstance() - 不要尝试缓存或共享实例
        // Javet 内部会处理原生库已加载的情况
        V8Runtime v8Runtime = V8Host.getNodeInstance().createV8Runtime();
        v8Runtime.allowEval(config.isAllowEval());
        v8Runtime.setLogger(config.getJavetLogger());

        return new CustomJavetEngine(this, v8Runtime);
    }
}
