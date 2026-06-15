package top.howiehz.halo.plugin.node.runtime.api;

public record NodeRuntimeStats(
    int poolMinSize,
    int poolMaxSize,
    int activeEngineCount,
    int idleEngineCount,
    int queuedCallCount,
    int registeredModuleCount
) {
}
