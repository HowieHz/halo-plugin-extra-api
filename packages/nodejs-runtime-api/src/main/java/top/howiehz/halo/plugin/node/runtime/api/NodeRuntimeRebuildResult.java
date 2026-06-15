package top.howiehz.halo.plugin.node.runtime.api;

import java.time.Duration;

public record NodeRuntimeRebuildResult(
    int registeredModuleCount,
    int closedEngineCount,
    Duration elapsed
) {
}
