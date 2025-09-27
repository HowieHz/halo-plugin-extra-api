package top.howiehz.halo.plugin.extra.api.service.js.engine;

import com.caoccao.javet.exceptions.JavetException;
import com.caoccao.javet.interop.V8Runtime;

/**
 * V8 engine pool service interface.
 * V8 引擎池服务接口，提供脚本执行和池管理能力。
 */
public interface V8EnginePoolService {

    /**
     * Execute JavaScript code.
     * 执行 JavaScript 代码并以指定类型返回结果。
     *
     * @param script JS script / JS 脚本
     * @param returnType expected return type / 期望的返回类型
     * @param <T> generic return type / 泛型返回类型
     * @return execution result / 执行结果
     * @throws JavetException when execution fails / 执行失败时抛出
     */
    <T> T executeScript(String script, Class<T> returnType) throws JavetException;

    /**
     * Use an engine to perform an operation.
     * 使用池中引擎执行给定的操作，操作执行后引擎会被归还。
     *
     * @param operation operation to run with runtime / 使用 runtime 执行的操作
     * @param <T> result type / 结果类型
     * @return operation result / 操作结果
     * @throws JavetException when operation fails / 操作失败时抛出
     */
    <T> T withEngine(EngineOperation<T> operation) throws JavetException;

    /**
     * Get pool statistics.
     * 获取引擎池统计信息。
     *
     * @return pool stats / 池统计
     */
    PoolStats getPoolStats();

    /**
     * Functional interface for engine operations.
     * 引擎操作的函数式接口，接受 V8Runtime 并返回结果。
     */
    @FunctionalInterface
    interface EngineOperation<T> {
        T execute(V8Runtime runtime) throws JavetException;
    }

    /**
     * Pool statistics record.
     * 池统计信息记录。
     *
     * @param minSize minimum pool size / 池的最小大小
     * @param maxSize maximum pool size / 池的最大大小
     * @param activeCount number of active engines / 活跃引擎数量
     * @param idleCount number of idle engines / 空闲引擎数量
     */
    record PoolStats(int minSize, int maxSize, int activeCount, int idleCount) {
    }
}