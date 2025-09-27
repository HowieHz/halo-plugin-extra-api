package top.howiehz.halo.plugin.extra.api.service.js;

import com.caoccao.javet.exceptions.JavetException;
import com.caoccao.javet.interop.V8Runtime;

/**
 * V8 引擎池接口
 */
public interface V8EnginePoolService {

    /**
     * 执行 JavaScript 代码
     */
    <T> T executeScript(String script, Class<T> returnType) throws JavetException;

    /**
     * 使用引擎执行操作
     */
    <T> T withEngine(EngineOperation<T> operation) throws JavetException;

    /**
     * 获取池状态
     */
    PoolStats getPoolStats();

    /**
     * 引擎操作函数式接口
     */
    @FunctionalInterface
    interface EngineOperation<T> {
        T execute(V8Runtime runtime) throws JavetException;
    }

    /**
     * 池状态记录
     */
    record PoolStats(int minSize, int maxSize, int activeCount, int idleCount) {}
}