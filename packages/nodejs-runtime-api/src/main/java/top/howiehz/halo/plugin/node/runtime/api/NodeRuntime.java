package top.howiehz.halo.plugin.node.runtime.api;

import org.pf4j.ExtensionPoint;
import reactor.core.publisher.Mono;

public interface NodeRuntime extends ExtensionPoint {

    Mono<String> call(NodeCall call);

    Mono<NodeRuntimeStats> stats();
}
