package top.howiehz.halo.plugin.node.runtime.api;

import java.time.Duration;

public record NodeCall(
    NodeModuleRef module,
    String functionName,
    String argsJson,
    Duration timeout
) {
}
