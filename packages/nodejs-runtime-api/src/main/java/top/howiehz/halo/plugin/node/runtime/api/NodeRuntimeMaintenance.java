package top.howiehz.halo.plugin.node.runtime.api;

import org.pf4j.ExtensionPoint;
import reactor.core.publisher.Mono;

public interface NodeRuntimeMaintenance extends ExtensionPoint {

    Mono<NodeRuntimeRebuildResult> rebuildRuntime();
}
