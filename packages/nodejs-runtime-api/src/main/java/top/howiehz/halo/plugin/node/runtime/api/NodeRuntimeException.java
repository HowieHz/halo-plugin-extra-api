package top.howiehz.halo.plugin.node.runtime.api;

public class NodeRuntimeException extends RuntimeException {

    private final String moduleId;
    private final String functionName;
    private final String jsStack;

    public NodeRuntimeException(String message) {
        this(null, null, message, null, null);
    }

    public NodeRuntimeException(String message, Throwable cause) {
        this(null, null, message, null, cause);
    }

    public NodeRuntimeException(String moduleId, String functionName, String message,
        String jsStack, Throwable cause) {
        super(message, cause);
        this.moduleId = moduleId;
        this.functionName = functionName;
        this.jsStack = jsStack;
    }

    public String moduleId() {
        return moduleId;
    }

    public String functionName() {
        return functionName;
    }

    public String jsStack() {
        return jsStack;
    }
}
